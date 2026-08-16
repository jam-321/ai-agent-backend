package com.jam.agent.dto.admin;

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
