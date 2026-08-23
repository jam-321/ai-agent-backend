package com.jam.agent.monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminTurnResponse(
        long id,
        int turnId,
        String type,
        String content,
        boolean hidden,
        String errorMessage,
        String traceId,
        String agentKey,
        String modelProviderKey,
        String modelName,
        String protocolType,
        List<Long> attachmentIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public AdminTurnResponse withAttachmentIds(List<Long> ids) {
        return new AdminTurnResponse(
                id, turnId, type, content, hidden, errorMessage, traceId, agentKey,
                modelProviderKey, modelName, protocolType, ids, createdAt, updatedAt);
    }
}
