package com.jam.agent.agent.event;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Dispatches fixed Agent events and isolates plugin failures. */
@Component
public class Dispatcher {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);

    private final EventRegistry registry;

    public Dispatcher(EventRegistry registry) {
        this.registry = registry;
    }

    public void turnStart(AgentTurnContext turn) {
        dispatch(AgentEvent.turnStart(turn));
    }

    public void lifecycle(AgentExecutionContext context, int attemptNo, Integer roundNo, String content) {
        dispatch(AgentEvent.lifecycle(context, attemptNo, roundNo, content));
    }

    public void toolStart(
            AgentExecutionContext context,
            int attemptNo,
            int roundNo,
            int callIndex,
            String toolName,
            String toolCallId,
            String arguments) {
        dispatch(AgentEvent.toolStart(
                context, attemptNo, roundNo, callIndex, toolName, toolCallId, arguments));
    }

    public void toolEnd(
            AgentExecutionContext context,
            int attemptNo,
            int roundNo,
            int callIndex,
            String toolName,
            String toolCallId,
            String result,
            boolean error) {
        dispatch(AgentEvent.toolEnd(
                context, attemptNo, roundNo, callIndex, toolName, toolCallId, result, error));
    }

    public void assistant(AgentExecutionContext context, int attemptNo, int roundNo, String content) {
        dispatch(AgentEvent.assistant(context, attemptNo, roundNo, content));
    }

    public void generate(AgentExecutionContext context, int attemptNo, String content, boolean error) {
        dispatch(AgentEvent.generate(context, attemptNo, content, error));
    }

    private void dispatch(AgentEvent event) {
        for (EventRegistry.Entry entry : registry.entriesOf(event.name())) {
            if (!entry.system() && !event.enabledPlugins().contains(entry.id())) {
                continue;
            }
            try {
                entry.plugin().execute(event);
            } catch (Exception exception) {
                log.error("Agent plugin failed but was isolated: id={}, event={}",
                        entry.id(), event.name(), exception);
            }
        }
    }
}
