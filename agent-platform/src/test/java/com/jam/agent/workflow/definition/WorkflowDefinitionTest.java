package com.jam.agent.workflow.definition;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowDefinitionTest {

    @Test
    void rejectsUnknownTransition() {
        assertThrows(IllegalArgumentException.class, () -> new WorkflowDefinition(
                "broken",
                "start",
                Map.of("start", new WorkflowStep(
                        "start", "ANSWER", "missing", null, Map.of()))));
    }
}
