package com.jam.agent.agent.event;

import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.List;
import org.springframework.ai.chat.messages.Message;

/** Mutable message context for one model turn. Turn-start plugins may adjust it. */
public final class AgentTurnContext {

    private final AgentExecutionContext execution;
    private final List<Message> messages;

    public AgentTurnContext(AgentExecutionContext execution, List<Message> messages) {
        this.execution = execution;
        this.messages = messages;
    }

    public AgentExecutionContext execution() {
        return execution;
    }

    public List<Message> messages() {
        return messages;
    }

    public AgentConfigSnapshot config() {
        return execution.agentConfig();
    }
}
