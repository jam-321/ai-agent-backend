package com.jam.agent.workflow.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.AgentRunResult;
import com.jam.agent.workflow.definition.WorkflowDefinition;
import com.jam.agent.workflow.definition.WorkflowStep;
import com.jam.agent.workflow.registry.WorkflowRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkflowAgentExecutorTest {

    @Test
    void runsStepsAndReturnsTerminalAnswer() {
        WorkflowRegistry registry = mock(WorkflowRegistry.class);
        Dispatcher events = mock(Dispatcher.class);
        WorkflowDefinition definition = new WorkflowDefinition(
                "test",
                "answer",
                Map.of("answer", new WorkflowStep(
                        "answer", "ANSWER", null, null, Map.of("content", "done"))));
        when(registry.require("test")).thenReturn(definition);

        WorkflowStepHandler handler = new WorkflowStepHandler() {
            @Override
            public String type() {
                return "ANSWER";
            }

            @Override
            public WorkflowStepResult execute(WorkflowContext context, WorkflowStep step, int stepNo) {
                return WorkflowStepResult.answer("done");
            }
        };
        WorkflowAgentExecutor executor = new WorkflowAgentExecutor(
                registry,
                events,
                List.of(handler));

        AgentExecutionContext context = new AgentExecutionContext(
                1L,
                2L,
                1,
                "trace",
                "query",
                new AgentConfigSnapshot("test", "prompt", Set.of(), Set.of(), "{}", "WORKFLOW", "test"),
                1,
                2,
                1,
                0,
                2,
                4,
                Instant.now().plusSeconds(30));

        AgentRunResult result = executor.execute(context, List.of());

        assertEquals("done", result.answer());
        verify(events).workflowStepStart(context, 1, 1, "answer", "trace:workflow-step:1:answer", "工作流步骤开始：ANSWER");
        verify(events).workflowStepEnd(context, 1, 1, "answer", "trace:workflow-step:1:answer", "done", false);
    }
}
