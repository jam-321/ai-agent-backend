package com.jam.agent.dto.admin;

public record AdminToolStatisticsResponse(
        String toolName,
        long callCount,
        long successCount,
        long errorCount,
        long runningCount,
        Long averageDurationMs) {
}
