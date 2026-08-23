package com.jam.agent.workflow.step;

import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.tool.ToolExecutor;
import com.jam.agent.workflow.definition.WorkflowStep;
import com.jam.agent.workflow.runtime.WorkflowContext;
import com.jam.agent.workflow.runtime.WorkflowStepHandler;
import com.jam.agent.workflow.runtime.WorkflowStepResult;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

/** Executes one explicitly configured tool call inside a workflow. */
@Component
public class ToolWorkflowStepHandler implements WorkflowStepHandler {

    private final ToolExecutor tools;
    private final Dispatcher events;

    public ToolWorkflowStepHandler(ToolExecutor tools, Dispatcher events) {
        this.tools = tools;
        this.events = events;
    }

    @Override
    public String type() {
        return "TOOL";
    }

    @Override
    public WorkflowStepResult execute(WorkflowContext context, WorkflowStep step, int stepNo) {
        Map<String, Object> parameters = step.parameters();
        String toolName = context.render(parameters.get("tool"));
        String arguments = context.render(parameters.getOrDefault("arguments", "{}"));
        String callId = context.execution().traceId()
                + ":workflow:" + step.id() + ":" + UUID.randomUUID();
        AssistantMessage.ToolCall call = new AssistantMessage.ToolCall(
                callId,
                "function",
                toolName,
                arguments);

        events.toolStart(
                context.execution(),
                context.attemptNo(),
                stepNo,
                0,
                toolName,
                callId,
                arguments);
        ToolExecutor.ToolResult result = tools.execute(
                context.execution(),
                context.attemptNo(),
                stepNo,
                0,
                call);
        return result.error()
                ? WorkflowStepResult.failure(result.responseData())
                : WorkflowStepResult.success(result.responseData());
    }
}
