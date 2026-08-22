package com.jam.agent.agent.model.protocol;

import org.springframework.ai.chat.messages.AssistantMessage;

/** 不同模型协议统一返回给 AgentLoop 的结果。 */
public record ModelCallResult(
        AssistantMessage message,
        String responseId,
        String returnedModel,
        Long inputTokens,
        Long outputTokens) {

    public ModelCallResult(AssistantMessage message) {
        this(message, null, null, null, null);
    }
}
