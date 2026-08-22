package com.jam.agent.agent.runtime;

import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.model.AgentModelConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Immutable identity and execution limits for one user turn.
 *
 * <p>HTTP session objects are intentionally excluded so the asynchronous Agent thread
 * does not depend on web request state.
 */
public record AgentExecutionContext(
        long userId,
        long conversationId,
        int turnId,
        String traceId,
        String currentQuery,
        List<Long> attachmentIds,
        AgentConfigSnapshot agentConfig,
        AgentModelConfig modelConfig,
        int maxAttempts,
        int maxToolRounds,
        int maxToolsPerRound,
        int maxDegenerateRetries,
        int maxSameToolSignature,
        int maxWorkflowSteps,
        Instant deadline) {

    public AgentExecutionContext {
        attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
    }

    /** 兼容恢复任务和不带附件的旧调用方。 */
    public AgentExecutionContext(
            long userId,
            long conversationId,
            int turnId,
            String traceId,
            String currentQuery,
            AgentConfigSnapshot agentConfig,
            AgentModelConfig modelConfig,
            int maxAttempts,
            int maxToolRounds,
            int maxToolsPerRound,
            int maxDegenerateRetries,
            int maxSameToolSignature,
            int maxWorkflowSteps,
            Instant deadline) {
        this(userId, conversationId, turnId, traceId, currentQuery, List.of(), agentConfig,
                modelConfig, maxAttempts, maxToolRounds, maxToolsPerRound,
                maxDegenerateRetries, maxSameToolSignature, maxWorkflowSteps, deadline);
    }

    public void checkDeadline() {
        if (Thread.currentThread().isInterrupted()) {
            throw new AgentRunException("任务已中断。", false);
        }
        if (Instant.now().isAfter(deadline)) {
            throw new AgentRunException("本次处理超时。", false);
        }
    }

    public static Instant deadline(Duration duration) {
        return Instant.now().plus(duration);
    }
}
