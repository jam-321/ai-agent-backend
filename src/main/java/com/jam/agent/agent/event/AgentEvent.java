package com.jam.agent.agent.event;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.Set;

/** Immutable event snapshot passed to plugins. */
public record AgentEvent(
        String name,
        AgentExecutionContext execution,
        AgentTurnContext turn,
        int attemptNo,
        Integer roundNo,
        Integer callIndex,
        String toolName,
        String toolCallId,
        String content,
        boolean error,
        Set<String> enabledPlugins) {

    public AgentEvent {
        enabledPlugins = enabledPlugins == null ? Set.of() : Set.copyOf(enabledPlugins);
    }

    public static AgentEvent turnStart(AgentTurnContext turn) {
        return new AgentEvent(
                "turn_start",
                turn.execution(),
                turn,
                1,
                null,
                null,
                null,
                null,
                null,
                false,
                turn.config().enabledPlugins());
    }

    public static AgentEvent lifecycle(
            AgentExecutionContext execution,
            int attemptNo,
            Integer roundNo,
            String content) {
        return base("lifecycle", execution, attemptNo, roundNo, null, null, null, content, false);
    }

    public static AgentEvent toolStart(
            AgentExecutionContext execution,
            int attemptNo,
            int roundNo,
            int callIndex,
            String toolName,
            String toolCallId,
            String arguments) {
        return base("tool_call", execution, attemptNo, roundNo, callIndex, toolName, toolCallId, arguments, false);
    }

    public static AgentEvent toolEnd(
            AgentExecutionContext execution,
            int attemptNo,
            int roundNo,
            int callIndex,
            String toolName,
            String toolCallId,
            String result,
            boolean error) {
        return base("tool_result", execution, attemptNo, roundNo, callIndex, toolName, toolCallId, result, error);
    }

    public static AgentEvent assistant(
            AgentExecutionContext execution,
            int attemptNo,
            int roundNo,
            String content) {
        return base("assistant", execution, attemptNo, roundNo, null, "assistant_reply", null, content, false);
    }

    public static AgentEvent generate(
            AgentExecutionContext execution,
            int attemptNo,
            String content,
            boolean error) {
        return base("generate", execution, attemptNo, null, null, "generate", null, content, error);
    }

    public static AgentEvent modelCallStart(
            AgentExecutionContext execution,
            int attemptNo,
            Integer roundNo,
            String callId,
            String content) {
        return base("model_call_start", execution, attemptNo, roundNo,
                null, "model", callId, content, false);
    }

    public static AgentEvent modelCallEnd(
            AgentExecutionContext execution,
            int attemptNo,
            Integer roundNo,
            String callId,
            String content,
            boolean error) {
        return base("model_call_end", execution, attemptNo, roundNo,
                null, "model", callId, content, error);
    }

    public static AgentEvent workflowStepStart(
            AgentExecutionContext execution,
            int attemptNo,
            int stepNo,
            String stepId,
            String runKey,
            String content) {
        return base("workflow_step_start", execution, attemptNo, stepNo, null,
                stepId, runKey, content, false);
    }

    public static AgentEvent workflowStepEnd(
            AgentExecutionContext execution,
            int attemptNo,
            int stepNo,
            String stepId,
            String runKey,
            String content,
            boolean error) {
        return base("workflow_step_end", execution, attemptNo, stepNo, null,
                stepId, runKey, content, error);
    }

    private static AgentEvent base(
            String name,
            AgentExecutionContext execution,
            int attemptNo,
            Integer roundNo,
            Integer callIndex,
            String toolName,
            String toolCallId,
            String content,
            boolean error) {
        return new AgentEvent(
                name,
                execution,
                null,
                attemptNo,
                roundNo,
                callIndex,
                toolName,
                toolCallId,
                content,
                error,
                execution.agentConfig().enabledPlugins());
    }
}
