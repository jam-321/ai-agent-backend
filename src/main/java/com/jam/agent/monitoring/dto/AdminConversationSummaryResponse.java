package com.jam.agent.monitoring.dto;

import java.time.LocalDateTime;

public record AdminConversationSummaryResponse(
        long id,
        long userId,
        String username,
        String title,
        long turnCount,
        long nodeCount,
        String latestStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
