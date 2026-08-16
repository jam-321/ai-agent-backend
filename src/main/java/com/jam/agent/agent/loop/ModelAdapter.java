package com.jam.agent.agent.loop;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
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

    private final ChatModel model;
    private final boolean enabled;

    public ModelAdapter(
            ChatModel model,
            @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.model = model;
        this.enabled = apiKey != null
                && !apiKey.isBlank()
                && !apiKey.contains("dummy");
    }

    public ModelResult call(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context) {
        if (!enabled) {
            return mockResponse(context);
        }

        try {
            ChatResponse response = model.call(new Prompt(
                    withSystemPrompt(messages),
                    buildOptions(tools, context)));
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
            AgentExecutionContext context) {
        return ToolCallingChatOptions.builder()
                .toolCallbacks(tools)
                .internalToolExecutionEnabled(false)
                .toolContext(Map.of(
                        "userId", context.userId(),
                        "conversationId", context.conversationId(),
                        "turnId", context.turnId(),
                        "traceId", context.traceId()))
                .build();
    }

    private List<Message> withSystemPrompt(List<Message> messages) {
        List<Message> promptMessages = new ArrayList<>(messages.size() + 1);
        promptMessages.add(new SystemMessage(SYSTEM_PROMPT));
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

    private ModelResult mockResponse(AgentExecutionContext context) {
        return new ModelResult(new AssistantMessage(
                "[mock] 尚未配置 DEEPSEEK_API_KEY。你的问题是：" + context.currentQuery()));
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
