package com.jam.agent.agent.loop;

import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.tool.ToolExecutor;
import com.jam.agent.agent.tool.ToolExecutor.ToolResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Executes one Agent attempt as a ReAct-style model/tool loop.
 *
 * <p>The loop owns only the in-memory working transcript. Durable user/assistant turns
 * are written by {@code TurnFinalizer}; intermediate events are written through
 * {@link Dispatcher}.
 */
@Component
public class AgentLoop {

    private static final String DEGENERATE_NUDGE =
            "上一轮未生成有效正文且没有调用工具。若需要信息，请调用可用工具；否则请直接给出最终答案。";

    private final ModelAdapter model;
    private final ToolExecutor tools;
    private final Dispatcher events;
    private final Executor toolExecutor;

    public AgentLoop(
            ModelAdapter model,
            ToolExecutor tools,
            Dispatcher events,
            @Qualifier("agentToolExecutor") Executor toolExecutor) {
        this.model = model;
        this.tools = tools;
        this.events = events;
        this.toolExecutor = toolExecutor;
    }

    /** Runs model rounds until the model returns a final answer without tool calls. */
    public String run(
            AgentExecutionContext context,
            int attemptNo,
            List<Message> turnMessages) {
        // 每次 Attempt 使用独立副本，失败 Attempt 的工具消息不会污染下一次重试。
        List<Message> messages = new ArrayList<>(turnMessages);

        List<ToolCallback> callbacks = tools.callbacks().entrySet().stream()
                .filter(entry -> context.agentConfig().isToolEnabled(entry.getKey()))
                .map(java.util.Map.Entry::getValue)
                .toList();
        ToolRepetitionGuard repetitionGuard =
                new ToolRepetitionGuard(context.maxSameToolSignature());
        int degenerateRetries = 0;

        for (int roundNo = 1; roundNo <= context.maxToolRounds(); roundNo++) {
            context.checkDeadline();
            events.lifecycle(context, attemptNo, roundNo, "round_start");

            AssistantMessage response = model.call(messages, callbacks, context).message();
            if (response.hasToolCalls()) {
                executeToolRound(
                        context,
                        attemptNo,
                        roundNo,
                        response,
                        messages,
                        repetitionGuard);
                degenerateRetries = 0;
                continue;
            }

            String answer = response.getText();
            if (isMeaningful(answer)) {
                messages.add(response);
                return answer;
            }

            if (degenerateRetries >= context.maxDegenerateRetries()) {
                throw new IllegalStateException("模型连续未生成有效回答。");
            }

            degenerateRetries++;
            messages.add(new UserMessage(DEGENERATE_NUDGE));
        }

        throw new IllegalStateException(
                "工具循环超过 " + context.maxToolRounds() + " 轮上限。");
    }

    private void executeToolRound(
            AgentExecutionContext context,
            int attemptNo,
            int roundNo,
            AssistantMessage response,
            List<Message> messages,
            ToolRepetitionGuard repetitionGuard) {
        List<AssistantMessage.ToolCall> calls = response.getToolCalls();
        validateToolCallCount(context, calls);

        // The assistant tool-call message must precede all matching tool results.
        messages.add(response);
        if (isMeaningful(response.getText())) {
                events.assistant(context, attemptNo, roundNo, response.getText());
        }

        publishAllToolStarts(context, attemptNo, roundNo, calls);
        List<CompletableFuture<ToolResult>> futures =
                submitToolCalls(context, attemptNo, roundNo, calls);
        List<ToolResponseMessage.ToolResponse> responses =
                joinToolResults(calls, futures, repetitionGuard);

        // Parallel completion order is ignored; results are appended in call_index order.
        messages.add(ToolResponseMessage.builder().responses(responses).build());
    }

    private void validateToolCallCount(
            AgentExecutionContext context,
            List<AssistantMessage.ToolCall> calls) {
        if (calls.size() > context.maxToolsPerRound()) {
            throw new IllegalStateException("单轮工具调用数量超过上限。");
        }
    }

    private void publishAllToolStarts(
            AgentExecutionContext context,
            int attemptNo,
            int roundNo,
            List<AssistantMessage.ToolCall> calls) {
        for (int callIndex = 0; callIndex < calls.size(); callIndex++) {
            AssistantMessage.ToolCall call = calls.get(callIndex);
            events.toolStart(
                    context,
                    attemptNo,
                    roundNo,
                    callIndex,
                    call.name(),
                    call.id(),
                    call.arguments());
        }
    }

    private List<CompletableFuture<ToolResult>> submitToolCalls(
            AgentExecutionContext context,
            int attemptNo,
            int roundNo,
            List<AssistantMessage.ToolCall> calls) {
        List<CompletableFuture<ToolResult>> futures = new ArrayList<>();
        for (int callIndex = 0; callIndex < calls.size(); callIndex++) {
            AssistantMessage.ToolCall call = calls.get(callIndex);
            int originalCallIndex = callIndex;
            futures.add(CompletableFuture.supplyAsync(
                    () -> tools.execute(
                            context,
                            attemptNo,
                            roundNo,
                            originalCallIndex,
                            call),
                    toolExecutor));
        }
        return futures;
    }

    private List<ToolResponseMessage.ToolResponse> joinToolResults(
            List<AssistantMessage.ToolCall> calls,
            List<CompletableFuture<ToolResult>> futures,
            ToolRepetitionGuard repetitionGuard) {
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        for (int callIndex = 0; callIndex < futures.size(); callIndex++) {
            ToolResult result = futures.get(callIndex).join();
            responses.add(new ToolResponseMessage.ToolResponse(
                    result.id(),
                    result.name(),
                    result.responseData()));
            repetitionGuard.record(calls.get(callIndex));
        }
        return responses;
    }

    private boolean isMeaningful(String text) {
        return text != null
                && !text.isBlank()
                && !text.trim().matches("[.。]+");
    }

    /** Stops a model from issuing the same tool request indefinitely. */
    private static final class ToolRepetitionGuard {
        private final int limit;
        private String previousSignature;
        private int consecutiveCount;

        private ToolRepetitionGuard(int limit) {
            this.limit = limit;
        }

        private void record(AssistantMessage.ToolCall call) {
            String arguments = call.arguments() == null
                    ? "{}"
                    : call.arguments().replaceAll("\\s+", "");
            String signature = call.name() + ":" + arguments;

            if (signature.equals(previousSignature)) {
                consecutiveCount++;
            } else {
                previousSignature = signature;
                consecutiveCount = 1;
            }

            if (consecutiveCount >= limit) {
                throw new IllegalStateException("检测到工具重复调用。");
            }
        }
    }
}
