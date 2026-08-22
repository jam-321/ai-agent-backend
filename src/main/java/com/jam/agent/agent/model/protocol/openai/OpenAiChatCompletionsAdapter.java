package com.jam.agent.agent.model.protocol.openai;

import com.jam.agent.agent.model.ModelRegistry;
import com.jam.agent.agent.model.ModelRegistry.ResolvedModel;
import com.jam.agent.agent.model.protocol.ModelCallResult;
import com.jam.agent.agent.model.protocol.ModelProtocol;
import com.jam.agent.agent.model.protocol.ModelProtocolAdapter;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/** 使用 Spring AI 调用 OpenAI Chat Completions 兼容接口。 */
@Component
public class OpenAiChatCompletionsAdapter implements ModelProtocolAdapter {

    private final ModelRegistry models;

    public OpenAiChatCompletionsAdapter(ModelRegistry models) {
        this.models = models;
    }

    @Override
    public String protocolType() {
        return ModelProtocol.OPENAI_CHAT_COMPLETIONS;
    }

    @Override
    public ModelCallResult call(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context) {
        ResolvedModel selected = models.resolve(context.modelConfig());
        if (!selected.enabled()) {
            return mockResponse(messages, context);
        }

        ChatResponse response = selected.model().call(new Prompt(
                messages,
                buildOptions(tools, context, selected)));
        if (response == null
                || response.getResult() == null
                || response.getResult().getOutput() == null) {
            throw new IllegalStateException("模型未返回有效响应。");
        }

        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata == null ? null : metadata.getUsage();
        return new ModelCallResult(
                response.getResult().getOutput(),
                metadata == null ? null : metadata.getId(),
                metadata == null ? null : metadata.getModel(),
                usage == null || usage.getPromptTokens() == null
                        ? null
                        : usage.getPromptTokens().longValue(),
                usage == null || usage.getCompletionTokens() == null
                        ? null
                        : usage.getCompletionTokens().longValue());
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

    private ModelCallResult mockResponse(
            List<Message> messages,
            AgentExecutionContext context) {
        String query = context.currentQuery();
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (messages.get(index) instanceof UserMessage user && user.getText() != null) {
                query = user.getText();
                break;
            }
        }
        return new ModelCallResult(new AssistantMessage(
                "[mock] 当前 Agent 未配置有效的模型 API Key。你的问题是：" + query));
    }
}
