package com.jam.agent.agent.model.protocol;

import org.springframework.ai.chat.messages.AssistantMessage;

/** 不同模型协议统一返回给 AgentLoop 的结果。 */
public record ModelCallResult(
        AssistantMessage message,
        String responseId,
        String returnedModel,
        Long inputTokens,
        Long outputTokens,
        Long cachedInputTokens,
        Long cacheMissInputTokens,
        Long cacheWriteInputTokens,
        Long reasoningTokens,
        Long totalTokens) {

    public ModelCallResult(AssistantMessage message) {
        this(message, null, null, null, null, null, null, null, null, null);
    }

    public ModelCallResult(
            AssistantMessage message,
            String responseId,
            String returnedModel,
            Long inputTokens,
            Long outputTokens) {
        this(message, responseId, returnedModel, inputTokens, outputTokens,
                null, null, null, null, sum(inputTokens, outputTokens));
    }

    private static Long sum(Long input, Long output) {
        return input == null && output == null
                ? null
                : (input == null ? 0 : input) + (output == null ? 0 : output);
    }
}
