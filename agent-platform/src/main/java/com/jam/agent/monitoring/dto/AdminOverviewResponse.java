package com.jam.agent.monitoring.dto;

public record AdminOverviewResponse(
        long userCount,
        long enabledUserCount,
        long conversationCount,
        long turnCount,
        long nodeCount,
        long completedRunCount,
        long failedRunCount,
        long reasoningRunCount,
        long toolCallCount,
        long toolSuccessCount,
        long toolErrorCount,
        long modelCallCount,
        long inputTokens,
        long outputTokens,
        long cachedInputTokens,
        long cacheMissInputTokens,
        long cacheUsageReportedCalls,
        long totalTokens) {
}
