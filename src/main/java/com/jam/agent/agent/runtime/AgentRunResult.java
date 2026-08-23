package com.jam.agent.agent.runtime;

import com.jam.agent.agent.model.AgentModelConfig;

/** Common terminal result returned by every Agent execution strategy. */
public record AgentRunResult(int attemptNo, String answer, AgentModelConfig modelConfig) {

    public AgentRunResult(int attemptNo, String answer) {
        this(attemptNo, answer, null);
    }
}
