package com.jam.agent.workflow.registry;

import com.jam.agent.workflow.definition.WorkflowDefinition;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Registry for code-defined workflow graphs. */
@Component
public class WorkflowRegistry {

    private final Map<String, WorkflowDefinition> definitions;

    public WorkflowRegistry(List<WorkflowProvider> providers) {
        this.definitions = providers.stream()
                .flatMap(provider -> provider.definitions().stream())
                .collect(Collectors.toUnmodifiableMap(
                        WorkflowDefinition::key,
                        Function.identity()));
    }

    public WorkflowDefinition require(String key) {
        WorkflowDefinition definition = definitions.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("未找到工作流定义：" + key);
        }
        return definition;
    }
}
