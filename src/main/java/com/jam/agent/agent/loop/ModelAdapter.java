package com.jam.agent.agent.loop;

import com.jam.agent.agent.model.protocol.ModelCallResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.memory.TokenEstimator;
import com.jam.agent.agent.model.ModelCallScope;
import com.jam.agent.agent.model.protocol.ModelProtocolRegistry;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.AgentRunException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 隔离 AgentLoop 与不同供应商的模型线协议。
 *
 * <p>具体协议适配器统一返回 AssistantMessage，AgentLoop 无需了解 Chat Completions、
 * Responses 或未来 Anthropic Messages 的报文差异。
 */
@Component
public class ModelAdapter {

    private static final int MAX_ERROR_LENGTH = 500;
    private static final String SYSTEM_PROMPT = """
            你是一个友好、专业的中文 AI Agent。
            需要准确时间时调用 current_time，需要精确算术时调用 calculate，
            需要查询历史工具节点时调用 query_conversation_node，被压缩的大结果按提示调用 query_tool_output。不要编造工具结果。
            """;

    private final ModelProtocolRegistry protocols;
    private final Dispatcher events;
    private final ObjectMapper objectMapper;
    private final TokenEstimator tokenEstimator;

    public ModelAdapter(
            ModelProtocolRegistry protocols,
            Dispatcher events,
            ObjectMapper objectMapper,
            TokenEstimator tokenEstimator) {
        this.protocols = protocols;
        this.events = events;
        this.objectMapper = objectMapper;
        this.tokenEstimator = tokenEstimator;
    }

    public ModelCallResult call(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context) {
        return call(messages, tools, context, ModelCallScope.agentRound(1, 1));
    }

    public ModelCallResult call(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context,
            ModelCallScope scope) {
        List<Message> prompt = withSystemPrompt(messages, context);
        int estimatedInputTokens = tokenEstimator.estimate(prompt, tools);
        ensureContextWindow(context, estimatedInputTokens);
        if (scope.countAgainstTurnBudget()) {
            context.tokenBudget().ensureCallAllowed(
                    estimatedInputTokens, context.budgetConfig().maxOutputTokens());
        }

        String callId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        events.modelCallStart(context, scope.attemptNo(), scope.roundNo(), callId,
                json(startContent(context, scope, estimatedInputTokens)));
        try {
            ModelCallResult result = protocols.require(context.modelConfig().protocolType())
                    .call(prompt, tools, context);
            if (scope.countAgainstTurnBudget()) {
                context.tokenBudget().record(result);
            }
            events.modelCallEnd(context, scope.attemptNo(), scope.roundNo(), callId,
                    json(successContent(context, scope, result, estimatedInputTokens, elapsedMillis(startedAt))),
                    false);
            return result;
        } catch (RetryableModelException exception) {
            events.modelCallEnd(context, scope.attemptNo(), scope.roundNo(), callId,
                    json(errorContent(context, scope, exception, estimatedInputTokens, elapsedMillis(startedAt))), true);
            throw exception;
        } catch (RuntimeException exception) {
            events.modelCallEnd(context, scope.attemptNo(), scope.roundNo(), callId,
                    json(errorContent(context, scope, exception, estimatedInputTokens, elapsedMillis(startedAt))), true);
            throw new RetryableModelException(
                    "模型调用失败：" + safeMessage(exception),
                    exception);
        }
    }

    private void ensureContextWindow(AgentExecutionContext context, int estimatedInputTokens) {
        long required = (long) estimatedInputTokens
                + context.budgetConfig().maxOutputTokens()
                + context.budgetConfig().safetyMarginTokens();
        if (required > context.budgetConfig().maxContextTokens()) {
            throw new AgentRunException(
                    "模型上下文预计需要 " + required + " Token，超过当前 Agent 上限 "
                            + context.budgetConfig().maxContextTokens() + "。",
                    false);
        }
    }

    private Map<String, Object> startContent(
            AgentExecutionContext context,
            ModelCallScope scope,
            int estimatedInputTokens) {
        Map<String, Object> value = baseContent(context, scope);
        value.put("estimatedInputTokens", estimatedInputTokens);
        value.put("usageSource", "ESTIMATED");
        return value;
    }

    private Map<String, Object> successContent(
            AgentExecutionContext context,
            ModelCallScope scope,
            ModelCallResult result,
            int estimatedInputTokens,
            long durationMs) {
        Map<String, Object> value = baseContent(context, scope);
        put(value, "responseId", result.responseId());
        put(value, "returnedModel", result.returnedModel());
        put(value, "inputTokens", result.inputTokens());
        put(value, "outputTokens", result.outputTokens());
        put(value, "cachedInputTokens", result.cachedInputTokens());
        put(value, "cacheMissInputTokens", result.cacheMissInputTokens());
        put(value, "cacheWriteInputTokens", result.cacheWriteInputTokens());
        put(value, "reasoningTokens", result.reasoningTokens());
        put(value, "totalTokens", result.totalTokens());
        value.put("estimatedInputTokens", estimatedInputTokens);
        value.put("durationMs", durationMs);
        value.put("usageSource", result.inputTokens() == null && result.outputTokens() == null
                ? "ESTIMATED" : "PROVIDER");
        return value;
    }

    private Map<String, Object> errorContent(
            AgentExecutionContext context,
            ModelCallScope scope,
            RuntimeException exception,
            int estimatedInputTokens,
            long durationMs) {
        Map<String, Object> value = baseContent(context, scope);
        value.put("estimatedInputTokens", estimatedInputTokens);
        value.put("durationMs", durationMs);
        value.put("error", safeMessage(exception));
        value.put("usageSource", "UNKNOWN");
        return value;
    }

    private Map<String, Object> baseContent(
            AgentExecutionContext context,
            ModelCallScope scope) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("purpose", scope.purpose());
        value.put("providerKey", context.modelConfig().providerKey());
        value.put("requestedModel", context.modelConfig().modelName());
        return value;
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) target.put(key, value);
    }

    private String json(Map<String, Object> content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception exception) {
            return "{\"usageSource\":\"UNKNOWN\"}";
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private List<Message> withSystemPrompt(
            List<Message> messages,
            AgentExecutionContext context) {
        List<Message> promptMessages = new ArrayList<>(messages.size() + 1);
        String configuredPrompt = context.agentConfig().systemPrompt();
        String systemPrompt = configuredPrompt == null || configuredPrompt.isBlank()
                ? SYSTEM_PROMPT
                : configuredPrompt;
        promptMessages.add(new SystemMessage(systemPrompt + runtimeIdentity(context)));
        promptMessages.addAll(messages);
        return promptMessages;
    }

    private String runtimeIdentity(AgentExecutionContext context) {
        String provider = context.modelConfig().providerName() == null
                ? context.modelConfig().providerKey()
                : context.modelConfig().providerName();
        return """


                运行时配置：当前 Agent 是 %s，模型供应商是 %s，配置模型是 %s。
                用户询问你的模型身份时，按上述运行时配置回答；不要依据训练语料猜测自己是 Claude、GPT 或其他模型。
                """.formatted(
                context.agentConfig().agentKey(),
                provider == null ? "默认供应商" : provider,
                context.modelConfig().modelName() == null
                        ? "默认模型"
                        : context.modelConfig().modelName());
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }

    /** 可由 OuterLoop 重试的一次临时模型调用失败。 */
    public static class RetryableModelException extends RuntimeException {
        public RetryableModelException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
