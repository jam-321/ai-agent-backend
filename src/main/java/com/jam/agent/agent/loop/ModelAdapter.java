package com.jam.agent.agent.loop;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.model.ModelRegistry;
import com.jam.agent.agent.model.ModelRegistry.ResolvedModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Isolates Spring AI protocol types and model-specific configuration from the loop.
 *
 * <p>Automatic tool execution is deliberately disabled. The project must persist every
 * tool transition before the corresponding result is sent back to the model.
 */
@Component
public class ModelAdapter {

    private static final int MAX_ERROR_LENGTH = 500;
    private static final String SYSTEM_PROMPT = """
            你是一个友好、专业的中文 AI Agent。
            需要准确时间时调用 current_time，需要精确算术时调用 calculate，
            需要查询历史工具完整数据时调用 query_conversation_node。不要编造工具结果。
            """;

    private final ModelRegistry models;

    public ModelAdapter(ModelRegistry models) {
        this.models = models;
    }

    public ModelResult call(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context) {
        ResolvedModel selected = models.resolve(context.agentConfig().modelConfig());
        if (!selected.enabled()) {
            return mockResponse(messages, context);
        }

        try {
            ChatResponse response = selected.model().call(new Prompt(
                    withSystemPrompt(messages, context.agentConfig().systemPrompt()),
                    buildOptions(tools, context, selected)));
            return extractResult(response);
        } catch (RetryableModelException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RetryableModelException(
                    "模型调用失败：" + safeMessage(exception),
                    exception);
        }
    }

    private ToolCallingChatOptions buildOptions(
            List<ToolCallback> tools,
            AgentExecutionContext context,
            ResolvedModel selected) {
        return ToolCallingChatOptions.builder()
                .model(selected.modelName())
                .temperature(selected.temperature())
                .toolCallbacks(tools)
                .internalToolExecutionEnabled(false)
                .toolContext(Map.of(
                        "userId", context.userId(),
                        "conversationId", context.conversationId(),
                        "turnId", context.turnId(),
                        "traceId", context.traceId()))
                .build();
    }

    private List<Message> withSystemPrompt(List<Message> messages, String configuredPrompt) {
        List<Message> promptMessages = new ArrayList<>(messages.size() + 1);
        String systemPrompt = configuredPrompt == null || configuredPrompt.isBlank()
                ? SYSTEM_PROMPT
                : configuredPrompt;
        promptMessages.add(new SystemMessage(systemPrompt));
        promptMessages.addAll(messages);
        return promptMessages;
    }

    private ModelResult extractResult(ChatResponse response) {
        if (response == null
                || response.getResult() == null
                || response.getResult().getOutput() == null) {
            throw new RetryableModelException("模型未返回有效响应。", null);
        }
        return new ModelResult(response.getResult().getOutput());
    }

    private ModelResult mockResponse(List<Message> messages, AgentExecutionContext context) {
        String query = context.currentQuery();
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (messages.get(index) instanceof org.springframework.ai.chat.messages.UserMessage user
                    && user.getText() != null) {
                query = user.getText();
                break;
            }
        }
        return new ModelResult(new AssistantMessage(
                "[mock] 当前 Agent 未配置有效的模型 API Key。你的问题是：" + query));
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

    public record ModelResult(AssistantMessage message) {
    }

    /** A transient model failure that may restart the current Agent attempt. */
    public static class RetryableModelException extends RuntimeException {
        public RetryableModelException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
