package com.jam.agent.conversation.dto;

import java.time.LocalDateTime;

public record TurnResponse(
        Integer turnId,
        String type,
        String content,
        String errorMessage,
        String traceId,
        String agentKey,
        String modelProviderKey,
        String modelName,
        String protocolType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
