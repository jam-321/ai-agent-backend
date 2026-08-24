package com.jam.agent.workflow.definition;

import java.util.Map;

/** One named step and its explicit success/error transitions. */
public record WorkflowStep(
        String id,
        String type,
        String nextStep,
        String errorStep,
        Map<String, Object> parameters) {

    public WorkflowStep {
        if (id == null || id.isBlank() || type == null || type.isBlank()) {
            throw new IllegalArgumentException("工作流步骤标识和类型不能为空。 ");
        }
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
