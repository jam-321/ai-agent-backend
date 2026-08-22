package com.jam.agent.workflow.runtime;

import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.loop.ModelAdapter.RetryableModelException;
import com.jam.agent.agent.runtime.AgentExecutor;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.AgentRunException;
import com.jam.agent.agent.runtime.AgentRunResult;
import com.jam.agent.workflow.definition.WorkflowDefinition;
import com.jam.agent.workflow.definition.WorkflowStep;
import com.jam.agent.workflow.registry.WorkflowRegistry;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

/** Runs a registered workflow graph as an alternate Agent execution strategy. */
@Component
public class WorkflowAgentExecutor implements AgentExecutor {

    private final WorkflowRegistry workflows;
    private final Dispatcher events;
    private final Map<String, WorkflowStepHandler> handlers;

    public WorkflowAgentExecutor(
            WorkflowRegistry workflows,
            Dispatcher events,
            List<WorkflowStepHandler> handlers) {
        this.workflows = workflows;
        this.events = events;
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                handler -> handler.type().toUpperCase(),
                Function.identity()));
    }

    @Override
    public String executionType() {
        return "WORKFLOW";
    }

    @Override
    public AgentRunResult execute(
            AgentExecutionContext context,
            List<Message> turnMessages) {
        if (context.agentConfig().executionKey() == null) {
            throw new IllegalArgumentException("WORKFLOW Agent 未配置 executionKey。 ");
        }
        WorkflowDefinition definition = workflows.require(context.agentConfig().executionKey());
        RetryableModelException lastFailure = null;

        for (int attemptNo = 1; attemptNo <= context.maxAttempts(); attemptNo++) {
            context.checkDeadline();
            events.lifecycle(context, attemptNo, null, "workflow_attempt_start:" + definition.key());
            try {
                return new AgentRunResult(
                        attemptNo,
                        runOnce(context, attemptNo, definition, turnMessages));
            } catch (RetryableModelException exception) {
                lastFailure = exception;
                events.lifecycle(context, attemptNo, null, "workflow_attempt_retryable_error");
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AgentRunException("工作流执行失败。", false);
    }

    private String runOnce(
            AgentExecutionContext execution,
            int attemptNo,
            WorkflowDefinition definition,
            List<Message> turnMessages) {
        WorkflowContext context = new WorkflowContext(execution, attemptNo, turnMessages);
        String stepId = definition.startStep();

        for (int stepNo = 1; stepNo <= execution.maxWorkflowSteps(); stepNo++) {
            execution.checkDeadline();
            WorkflowStep step = definition.steps().get(stepId);
            if (step == null) {
                throw new IllegalStateException("工作流引用了不存在的步骤：" + stepId);
            }

            String runKey = execution.traceId() + ":workflow-step:" + attemptNo + ":" + step.id();
            events.workflowStepStart(
                    execution,
                    attemptNo,
                    stepNo,
                    step.id(),
                    runKey,
                    "工作流步骤开始：" + step.type());

            WorkflowStepResult result;
            try {
                WorkflowStepHandler handler = handlers.get(step.type().toUpperCase());
                if (handler == null) {
                    throw new IllegalArgumentException("不支持的工作流步骤类型：" + step.type());
                }
                result = handler.execute(context, step, stepNo);
            } catch (RuntimeException exception) {
                events.workflowStepEnd(
                        execution,
                        attemptNo,
                        stepNo,
                        step.id(),
                        runKey,
                        safeMessage(exception),
                        true);
                throw exception;
            }

            context.put(step.id(), result.output());
            String outputKey = String.valueOf(step.parameters().getOrDefault("outputKey", ""));
            context.put(outputKey, result.output());
            events.workflowStepEnd(
                    execution,
                    attemptNo,
                    stepNo,
                    step.id(),
                    runKey,
                    result.output(),
                    !result.success());

            if (!result.success()) {
                if (step.errorStep() == null) {
                    throw new IllegalStateException(result.output());
                }
                stepId = step.errorStep();
                continue;
            }
            if (result.terminal()) {
                return result.answer();
            }
            stepId = result.nextStep() == null ? step.nextStep() : result.nextStep();
            if (stepId == null) {
                throw new IllegalStateException("工作流步骤没有后继节点：" + step.id());
            }
        }

        throw new IllegalStateException("工作流步骤超过上限：" + execution.maxWorkflowSteps());
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
