package com.jam.agent.agent.runtime;

import java.time.Duration;
import java.time.Instant;

public record AgentExecutionContext(long userId, long conversationId, int turnId, String traceId,
                                    String currentQuery, int maxAttempts, int maxToolRounds,
                                    int maxToolsPerRound, int maxDegenerateRetries, int maxSameToolSignature,
                                    Instant deadline) {
    public void checkDeadline() {
        if (Thread.currentThread().isInterrupted()) throw new AgentRunException("任务已中断。", false);
        if (Instant.now().isAfter(deadline)) throw new AgentRunException("本次处理超时。", false);
    }
    public static Instant deadline(Duration duration) { return Instant.now().plus(duration); }
}
