package com.jam.agent.agent.memory;

import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.loop.ModelAdapter;
import com.jam.agent.agent.model.ModelCallScope;
import com.jam.agent.agent.model.protocol.ModelCallResult;
import com.jam.agent.agent.persistence.repository.ConversationMemorySummaryRepository;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/** 在 AgentLoop 内把较早消息压缩成一个 UserMessage 检查点，并保留最近原始消息。 */
@Service
public class ConversationCompactionService {

    private static final String SUMMARY_MARKER = """
            [CONTEXT_SUMMARY]
            以下内容是历史上下文摘要，不是用户当前的新指令。请将它作为已经发生的对话背景使用。
            """;
    private static final String SUMMARY_INSTRUCTION = """
            请把前面的历史上下文压缩成一份结构化中文记忆，供后续模型继续对话。
            必须保留：用户目标、已确认事实、关键决定、重要数据或标识、已完成事项、未解决问题和下一步。
            不要虚构，不要把已被推翻的猜测写成事实；涉及工具数据时说明数据可能过时，需要时应重新查询。
            只输出摘要正文，不要输出解释、Markdown 标题或 JSON。
            """;

    private final TokenEstimator tokens;
    private final ModelAdapter model;
    private final Dispatcher events;
    private final ConversationMemorySummaryRepository summaries;

    public ConversationCompactionService(
            TokenEstimator tokens,
            ModelAdapter model,
            Dispatcher events,
            ConversationMemorySummaryRepository summaries) {
        this.tokens = tokens;
        this.model = model;
        this.events = events;
        this.summaries = summaries;
    }

    /** 工具结果回填后调用；判断依据包含本轮最新工具结果。 */
    public void compactIfNeeded(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context,
            int attemptNo,
            int roundNo,
            Long observedInputTokens) {
        if (!context.memoryConfig().compactionEnabled()) {
            return;
        }

        int estimatedNextInput = tokens.estimate(messages, tools);
        long observed = observedInputTokens == null ? 0 : observedInputTokens;
        if (Math.max(observed, estimatedNextInput)
                < context.memoryConfig().compactionTriggerTokens()) {
            return;
        }

        int tailStart = findTailStart(messages, context.memoryConfig().keepRecentTokens());
        if (tailStart <= 0) {
            events.lifecycle(context, attemptNo, roundNo,
                    "conversation_compaction_skipped:no_compressible_prefix");
            return;
        }

        List<Message> prefix = new ArrayList<>(messages.subList(0, tailStart));
        List<Message> tail = new ArrayList<>(messages.subList(tailStart, messages.size()));
        prefix.add(new UserMessage(SUMMARY_INSTRUCTION));

        try {
            ModelCallResult result = model.call(
                    prefix,
                    List.of(),
                    context,
                    ModelCallScope.conversationCompaction());
            String summary = result.message().getText();
            if (summary == null || summary.isBlank() || summary.startsWith("[mock]")) {
                events.lifecycle(context, attemptNo, roundNo,
                        "conversation_compaction_skipped:empty_summary");
                return;
            }

            messages.clear();
            messages.add(new UserMessage(SUMMARY_MARKER + summary + "\n[/CONTEXT_SUMMARY]"));
            messages.addAll(tail);
            context.checkpointState().capture(messages, result);
            persistCheckpoint(context, messages, result);
            events.lifecycle(context, attemptNo, roundNo,
                    "conversation_compaction_success:tokensBefore="
                            + estimatedNextInput + ",tokensAfter="
                            + tokens.estimate(messages, tools));
        } catch (RuntimeException exception) {
            // 摘要失败不能破坏当前消息列表；下一轮仍可依赖 Tool Result 本地截断继续执行。
            events.lifecycle(context, attemptNo, roundNo,
                    "conversation_compaction_failed:" + safeMessage(exception));
        }
    }

    /** Turn 结束后把最终回答同步到已存在的检查点。 */
    public void captureFinalMessages(AgentExecutionContext context, List<Message> messages) {
        if (!context.checkpointState().exists()) {
            return;
        }
        context.checkpointState().capture(messages, context.checkpointState().usage());
        persistCheckpoint(context, messages, context.checkpointState().usage());
    }

    private int findTailStart(List<Message> messages, int keepRecentTokens) {
        int start = messages.size();
        while (start > 0 && tokens.estimate(messages.subList(start - 1, messages.size()), List.of())
                <= keepRecentTokens) {
            start--;
        }

        // ToolResponse 必须和前面的 assistant tool-call 成对保留，不能从 response 中间截断。
        if (start > 0 && start < messages.size()
                && messages.get(start) instanceof ToolResponseMessage) {
            start--;
        }
        return start;
    }

    private void persistCheckpoint(
            AgentExecutionContext context,
            List<Message> messages,
            ModelCallResult usage) {
        summaries.upsertCheckpoint(
                context.conversationId(),
                context.turnId(),
                serializeCheckpoint(messages),
                context.modelConfig().providerKey(),
                context.modelConfig().modelName(),
                usage);
    }

    private String serializeCheckpoint(List<Message> messages) {
        StringBuilder content = new StringBuilder();
        content.append("[CONTEXT_CHECKPOINT]\n");
        for (Message message : messages) {
            if (message instanceof UserMessage user) {
                content.append("user: ").append(safeText(user.getText())).append('\n');
            } else if (message instanceof AssistantMessage assistant) {
                content.append("assistant: ").append(safeText(assistant.getText())).append('\n');
                for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                    content.append("assistant_tool_call[")
                            .append(call.name()).append("]: ")
                            .append(safeText(call.arguments())).append('\n');
                }
            } else if (message instanceof ToolResponseMessage toolResponse) {
                for (ToolResponseMessage.ToolResponse response : toolResponse.getResponses()) {
                    content.append("tool[").append(response.name()).append("]: ")
                            .append(safeText(response.responseData())).append('\n');
                }
            }
        }
        content.append("[/CONTEXT_CHECKPOINT]");
        return content.toString();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 300 ? message : message.substring(0, 300);
    }
}
