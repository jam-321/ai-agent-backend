package com.jam.agent.workflow.step;

import com.jam.agent.workflow.definition.WorkflowStep;
import com.jam.agent.workflow.runtime.WorkflowContext;
import com.jam.agent.workflow.runtime.WorkflowStepHandler;
import com.jam.agent.workflow.runtime.WorkflowStepResult;
import org.springframework.stereotype.Component;

/** Converts a workflow variable or template into the canonical Agent answer. */
@Component
public class AnswerWorkflowStepHandler implements WorkflowStepHandler {

    @Override
    public String type() {
        return "ANSWER";
    }

    @Override
    public WorkflowStepResult execute(WorkflowContext context, WorkflowStep step, int stepNo) {
        Object source = step.parameters().get("source");
        String answer = source == null
                ? context.render(step.parameters().getOrDefault("content", "{{query}}"))
                : context.get(String.valueOf(source));
        if (answer == null || answer.isBlank()) {
            return WorkflowStepResult.failure("工作流最终答案为空。 ");
        }
        return WorkflowStepResult.answer(answer);
    }
}
