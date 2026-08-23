package com.jam.agent.agent.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AgentConfigSnapshotTest {

    @Test
    void legacyRecipeWithoutToolColumnKeepsAllToolsEnabled() {
        AgentConfigSnapshot snapshot = new AgentConfigSnapshot(
                "legacy",
                "prompt",
                Set.of(),
                null,
                "{}");

        assertTrue(snapshot.isToolEnabled("current_time"));
        assertTrue(snapshot.isToolEnabled("calculate"));
    }

    @Test
    void explicitToolListRestrictsAvailableTools() {
        AgentConfigSnapshot snapshot = new AgentConfigSnapshot(
                "time-only",
                "prompt",
                Set.of(),
                Set.of("current_time"),
                "{}");

        assertTrue(snapshot.isToolEnabled("current_time"));
        assertFalse(snapshot.isToolEnabled("calculate"));
    }

    @Test
    void explicitEmptyToolListDisablesAllTools() {
        AgentConfigSnapshot snapshot = new AgentConfigSnapshot(
                "no-tools",
                "prompt",
                Set.of(),
                Set.of(),
                "{}");

        assertFalse(snapshot.isToolEnabled("current_time"));
    }

    @Test
    void normalizesWorkflowExecutionType() {
        AgentConfigSnapshot snapshot = new AgentConfigSnapshot(
                "time-workflow",
                "prompt",
                Set.of(),
                Set.of("current_time"),
                "{}",
                " workflow ",
                "time_report");

        assertEquals("WORKFLOW", snapshot.executionType());
        assertEquals("time_report", snapshot.executionKey());
    }
}
