package com.jam.agent.workflow.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.config.AgentProperties;
import org.junit.jupiter.api.Test;

class WorkflowConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolvesBoundedWorkflowStepBudget() {
        AgentProperties.Workflow defaults = new AgentProperties.Workflow();

        WorkflowConfig actual = WorkflowConfig.resolve(
                defaults,
                "{\"workflow\":{\"maxSteps\":6}}",
                objectMapper);

        assertEquals(6, actual.maxSteps());
    }

    @Test
    void doesNotAllowRecipeToExceedGlobalLimit() {
        AgentProperties.Workflow defaults = new AgentProperties.Workflow();

        WorkflowConfig actual = WorkflowConfig.resolve(
                defaults,
                "{\"workflow\":{\"maxSteps\":999}}",
                objectMapper);

        assertEquals(defaults.getMaxSteps(), actual.maxSteps());
    }
}
