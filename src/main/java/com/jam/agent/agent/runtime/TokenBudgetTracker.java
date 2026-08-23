package com.jam.agent.agent.runtime;

import com.jam.agent.agent.model.protocol.ModelCallResult;

/** 一个 Turn 内跨 Attempt、Round 和 Workflow 步骤共享的真实 Token 计数器。 */
public final class TokenBudgetTracker {

    private final long limit;
    private long inputTokens;
    private long outputTokens;
    private long cachedInputTokens;
    private long reasoningTokens;
    private int modelCalls;

    public TokenBudgetTracker(long limit) {
        this.limit = Math.max(1, limit);
    }

    public synchronized void ensureCallAllowed(long estimatedInputTokens, long reservedOutputTokens) {
        long estimated = Math.max(0, estimatedInputTokens) + Math.max(0, reservedOutputTokens);
        if (modelCalls > 0 && usedTokens() + estimated > limit) {
            throw new TokenBudgetExceededException(limit, usedTokens());
        }
    }

    public synchronized void record(ModelCallResult result) {
        inputTokens += nonNegative(result.inputTokens());
        outputTokens += nonNegative(result.outputTokens());
        cachedInputTokens += nonNegative(result.cachedInputTokens());
        reasoningTokens += nonNegative(result.reasoningTokens());
        modelCalls++;
    }

    public synchronized boolean exhausted() {
        return usedTokens() >= limit;
    }

    public synchronized long usedTokens() {
        return inputTokens + outputTokens;
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(limit, inputTokens, outputTokens, cachedInputTokens,
                reasoningTokens, usedTokens(), modelCalls);
    }

    private long nonNegative(Long value) {
        return value == null ? 0 : Math.max(0, value);
    }

    public record Snapshot(
            long limit,
            long inputTokens,
            long outputTokens,
            long cachedInputTokens,
            long reasoningTokens,
            long usedTokens,
            int modelCalls) {
    }

    public static class TokenBudgetExceededException extends AgentRunException {
        public TokenBudgetExceededException(long limit, long used) {
            super("本次执行已达到 Token 预算（" + used + "/" + limit + "）。", false);
        }
    }
}
