package com.jam.agent.agent.runtime;

/** Common terminal result returned by every Agent execution strategy. */
public record AgentRunResult(int attemptNo, String answer) {
}
