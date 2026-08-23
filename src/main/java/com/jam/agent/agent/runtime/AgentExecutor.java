package com.jam.agent.agent.runtime;

import java.util.List;
import org.springframework.ai.chat.messages.Message;

/** One complete execution strategy for an Agent turn. */
public interface AgentExecutor {

    String executionType();

    AgentRunResult execute(AgentExecutionContext context, List<Message> turnMessages);
}
