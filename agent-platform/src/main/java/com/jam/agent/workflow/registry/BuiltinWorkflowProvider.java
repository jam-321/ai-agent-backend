package com.jam.agent.workflow.registry;

import com.jam.agent.workflow.definition.WorkflowDefinition;
import com.jam.agent.workflow.definition.WorkflowStep;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** First executable workflow used to verify the alternate Agent runtime. */
@Component
public class BuiltinWorkflowProvider implements WorkflowProvider {

    @Override
    public List<WorkflowDefinition> definitions() {
        return List.of(new WorkflowDefinition(
                "time_report",
                "get_time",
                Map.of(
                        "get_time", new WorkflowStep(
                                "get_time",
                                "TOOL",
                                "compose_answer",
                                null,
                                Map.of(
                                        "tool", "current_time",
                                        "arguments", "{}",
                                        "outputKey", "currentTime")),
                        "compose_answer", new WorkflowStep(
                                "compose_answer",
                                "MODEL",
                                "answer",
                                null,
                                Map.of(
                                        "outputKey", "draftAnswer",
                                        "prompt", "请根据当前时间工具结果 {{currentTime}}，回答用户问题：{{query}}")),
                        "answer", new WorkflowStep(
                                "answer",
                                "ANSWER",
                                null,
                                null,
                                Map.of("source", "draftAnswer")))));
    }
}
