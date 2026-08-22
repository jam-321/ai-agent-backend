package com.jam.agent.workflow.step;

import com.jam.agent.agent.loop.ModelAdapter;
import com.jam.agent.workflow.definition.WorkflowStep;
import com.jam.agent.workflow.runtime.WorkflowContext;
import com.jam.agent.workflow.runtime.WorkflowStepHandler;
import com.jam.agent.workflow.runtime.WorkflowStepResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/** Performs one bounded model call; tool decisions remain explicit workflow steps. */
@Component
public class ModelWorkflowStepHandler implements WorkflowStepHandler {

    private final ModelAdapter model;

    public ModelWorkflowStepHandler(ModelAdapter model) {
        this.model = model;
    }

    @Override
    public String type() {
        return "MODEL";
    }

    @Override
    public WorkflowStepResult execute(WorkflowContext context, WorkflowStep step, int stepNo) {
        String prompt = context.render(step.parameters().getOrDefault(
                "prompt",
                "请回答用户问题：{{query}}"));
        List<Message> messages = new ArrayList<>(context.history());
        messages.add(new UserMessage(prompt));

        AssistantMessage response = model.call(messages, List.of(), context.execution()).message();
        if (response.hasToolCalls() || response.getText() == null || response.getText().isBlank()) {
            throw new IllegalStateException("工作流模型步骤未生成有效正文。 ");
        }
        return WorkflowStepResult.success(response.getText());
    }
}
