package com.jam.agent.monitoring.dto;

import java.time.LocalDateTime;

public record AdminNodeResponse(
        long id,
        int turnId,
        String traceId,
        int attemptNo,
        Integer roundNo,
        Integer callIndex,
        String nodeId,
        String nodeName,
        String aggrKey,
        String type,
        String status,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
