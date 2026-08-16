package com.jam.agent.agent.event;

import com.jam.agent.agent.runtime.AgentExecutionContext;

public interface EventPublisher {
    void lifecycle(AgentExecutionContext context, int attemptNo, Integer roundNo, String content);
    void toolStart(AgentExecutionContext context, int attemptNo, int roundNo, int callIndex, String toolName, String toolCallId, String arguments);
    void toolEnd(AgentExecutionContext context, int attemptNo, int roundNo, int callIndex, String toolName, String toolCallId, String result, boolean error);
    void assistant(AgentExecutionContext context, int attemptNo, int roundNo, String content);
    void generate(AgentExecutionContext context, int attemptNo, String content, boolean error);
}
