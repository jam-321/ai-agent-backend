package com.jam.agent.monitoring.dto;

import java.time.LocalDateTime;

public record AdminTurnResponse(
        long id,
        int turnId,
        String type,
        String content,
        boolean hidden,
        String errorMessage,
        String traceId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
