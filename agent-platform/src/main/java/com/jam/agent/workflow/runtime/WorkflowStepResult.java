package com.jam.agent.workflow.runtime;

/** Handler result used to persist output and select the next workflow step. */
public record WorkflowStepResult(
        boolean success,
        String output,
        boolean terminal,
        String answer,
        String nextStep) {

    public static WorkflowStepResult success(String output) {
        return new WorkflowStepResult(true, output, false, null, null);
    }

    public static WorkflowStepResult answer(String answer) {
        return new WorkflowStepResult(true, answer, true, answer, null);
    }

    public static WorkflowStepResult failure(String output) {
        return new WorkflowStepResult(false, output, false, null, null);
    }

    public static WorkflowStepResult branch(String nextStep, String output) {
        return new WorkflowStepResult(true, output, false, null, nextStep);
    }
}
