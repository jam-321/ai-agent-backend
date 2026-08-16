package com.jam.agent.agent.event;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.repository.ConversationNodeRepository;
import org.springframework.stereotype.Component;

@Component
public class DatabaseEventPublisher implements EventPublisher {
    private final ConversationNodeRepository nodes;
    public DatabaseEventPublisher(ConversationNodeRepository nodes) { this.nodes = nodes; }
    @Override public void lifecycle(AgentExecutionContext c, int a, Integer r, String content) {
        nodes.insert(c.conversationId(), c.turnId(), c.traceId(), a, r, null, "lifecycle", "执行状态", null, "LIFECYCLE", "INFO", content);
    }
    @Override public void toolStart(AgentExecutionContext c, int a, int r, int i, String name, String id, String args) {
        nodes.insert(c.conversationId(), c.turnId(), c.traceId(), a, r, i, name, name, id, "TOOL_CALL", "START", args);
    }
    @Override public void toolEnd(AgentExecutionContext c, int a, int r, int i, String name, String id, String result, boolean error) {
        nodes.insert(c.conversationId(), c.turnId(), c.traceId(), a, r, i, name, name, id, "TOOL_CALL", error ? "ERROR" : "SUCCESS", result);
    }
    @Override public void assistant(AgentExecutionContext c, int a, int r, String content) {
        nodes.insert(c.conversationId(), c.turnId(), c.traceId(), a, r, null, "assistant_reply", "助手回复", null, "ASSISTANT_REPLY", "SUCCESS", content);
    }
    @Override public void generate(AgentExecutionContext c, int a, String content, boolean error) {
        nodes.insert(c.conversationId(), c.turnId(), c.traceId(), a, null, null, "generate", "最终回答", null, "GENERATE", error ? "ERROR" : "COMPLETE", content);
    }
}
