package com.jam.agent.agent.runtime;

/** One complete execution strategy for an Agent turn. */
public interface AgentExecutor {

    String executionType();

    AgentRunResult execute(AgentExecutionContext context);
}
