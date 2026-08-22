package com.jam.agent.workflow.runtime;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.Message;

/** Mutable data scope shared by steps during one workflow execution. */
public final class WorkflowContext {

    private final AgentExecutionContext execution;
    private final int attemptNo;
    private final List<Message> history;
    private final Map<String, String> variables = new LinkedHashMap<>();

    public WorkflowContext(
            AgentExecutionContext execution,
            int attemptNo,
            List<Message> history) {
        this.execution = execution;
        this.attemptNo = attemptNo;
        this.history = List.copyOf(history);
        variables.put("query", execution.currentQuery());
    }

    public AgentExecutionContext execution() { return execution; }
    public int attemptNo() { return attemptNo; }
    public List<Message> history() { return history; }

    public void put(String name, String value) {
        if (name != null && !name.isBlank()) {
            variables.put(name, value == null ? "" : value);
        }
    }

    public String get(String name) {
        return variables.get(name);
    }

    public String render(Object value) {
        String template = value == null ? "" : String.valueOf(value);
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }
}
