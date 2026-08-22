package com.jam.agent.agent.loop;

import com.jam.agent.agent.model.protocol.ModelCallResult;
import com.jam.agent.agent.model.protocol.ModelProtocolRegistry;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.ArrayList;
import java.util.List;
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
            需要查询历史工具完整数据时调用 query_conversation_node。不要编造工具结果。
            """;

    private final ModelProtocolRegistry protocols;

    public ModelAdapter(ModelProtocolRegistry protocols) {
        this.protocols = protocols;
    }

    public ModelCallResult call(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context) {
        try {
            return protocols.require(context.modelConfig().protocolType())
                    .call(withSystemPrompt(messages, context), tools, context);
        } catch (RetryableModelException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RetryableModelException(
                    "模型调用失败：" + safeMessage(exception),
                    exception);
        }
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
