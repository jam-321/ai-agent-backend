package com.jam.agent.workflow.step;

import com.jam.agent.workflow.definition.WorkflowStep;
import com.jam.agent.workflow.runtime.WorkflowContext;
import com.jam.agent.workflow.runtime.WorkflowStepHandler;
import com.jam.agent.workflow.runtime.WorkflowStepResult;
import org.springframework.stereotype.Component;

/** Selects a transition using a deterministic workflow variable comparison. */
@Component
public class ConditionWorkflowStepHandler implements WorkflowStepHandler {

    @Override
    public String type() {
        return "CONDITION";
    }

    @Override
    public WorkflowStepResult execute(WorkflowContext context, WorkflowStep step, int stepNo) {
        String variable = String.valueOf(step.parameters().getOrDefault("variable", ""));
        String expected = context.render(step.parameters().getOrDefault("equals", ""));
        String actual = context.get(variable);
        String next = expected.equals(actual)
                ? String.valueOf(step.parameters().get("trueStep"))
                : String.valueOf(step.parameters().get("falseStep"));
        if (next == null || "null".equals(next) || next.isBlank()) {
            return WorkflowStepResult.failure("条件步骤缺少目标节点。 ");
        }
        return WorkflowStepResult.branch(next, actual);
    }
}
