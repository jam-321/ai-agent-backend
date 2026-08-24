package com.jam.agent.workflow.runtime;

import com.jam.agent.workflow.definition.WorkflowStep;

/** Extension point for a workflow step type. */
public interface WorkflowStepHandler {

    String type();

    WorkflowStepResult execute(WorkflowContext context, WorkflowStep step, int stepNo);
}
