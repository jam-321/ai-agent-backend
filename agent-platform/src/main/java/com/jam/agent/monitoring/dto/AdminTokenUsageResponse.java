package com.jam.agent.monitoring.dto;

/** 管理员监控使用的模型调用聚合数据。 */
public record AdminTokenUsageResponse(
        long modelCallCount,
        long inputTokens,
        long outputTokens,
        long cachedInputTokens,
        long cacheMissInputTokens,
        long cacheWriteInputTokens,
        long cacheUsageReportedCalls,
        long reasoningTokens,
        long totalTokens,
        long durationMs) {

    public static AdminTokenUsageResponse empty() {
        return new AdminTokenUsageResponse(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
