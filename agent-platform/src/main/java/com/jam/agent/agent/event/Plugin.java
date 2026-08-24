package com.jam.agent.agent.event;

/** Extension point invoked at a fixed Agent event. */
public interface Plugin {

    AgentEvent execute(AgentEvent event);
}
