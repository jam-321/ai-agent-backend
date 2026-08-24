package com.jam.agent.monitoring.dto;

public record AdminToolStatisticsResponse(
        String toolName,
        long callCount,
        long successCount,
        long errorCount,
        long runningCount,
        Long averageDurationMs) {
}
