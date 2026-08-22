package com.jam.agent.workflow.definition;

import java.util.Map;

/** Immutable graph definition registered by application code. */
public record WorkflowDefinition(
        String key,
        String startStep,
        Map<String, WorkflowStep> steps) {

    public WorkflowDefinition {
        if (key == null || key.isBlank() || startStep == null || startStep.isBlank()) {
            throw new IllegalArgumentException("工作流标识和起始步骤不能为空。 ");
        }
        steps = Map.copyOf(steps);
        if (!steps.containsKey(startStep)) {
            throw new IllegalArgumentException("工作流起始步骤不存在：" + startStep);
        }
        for (WorkflowStep step : steps.values()) {
            requireStepReference(steps, step.nextStep());
            requireStepReference(steps, step.errorStep());
        }
    }

    private static void requireStepReference(Map<String, WorkflowStep> steps, String stepId) {
        if (stepId != null && !steps.containsKey(stepId)) {
            throw new IllegalArgumentException("工作流引用了不存在的步骤：" + stepId);
        }
    }
}
