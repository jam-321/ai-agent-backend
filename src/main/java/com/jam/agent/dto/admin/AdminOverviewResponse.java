package com.jam.agent.dto.admin;

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
        long toolErrorCount) {
}
