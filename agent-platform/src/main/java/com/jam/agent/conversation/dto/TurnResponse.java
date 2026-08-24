package com.jam.agent.conversation.dto;

import java.time.LocalDateTime;
import java.util.List;

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
        List<Long> attachmentIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
