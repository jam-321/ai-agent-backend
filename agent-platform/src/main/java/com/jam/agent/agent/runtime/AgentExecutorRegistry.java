package com.jam.agent.agent.runtime;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Selects the execution strategy captured in the current Agent recipe. */
@Component
public class AgentExecutorRegistry {

    private final Map<String, AgentExecutor> executors;

    public AgentExecutorRegistry(List<AgentExecutor> executors) {
        this.executors = executors.stream().collect(Collectors.toUnmodifiableMap(
                executor -> executor.executionType().toUpperCase(),
                Function.identity()));
    }

    public AgentExecutor resolve(String executionType) {
        String key = executionType == null ? "LOOP" : executionType.toUpperCase();
        AgentExecutor executor = executors.get(key);
        if (executor == null) {
            throw new AgentRunException("不支持的 Agent 执行类型：" + key, false);
        }
        return executor;
    }

    /** 管理端表单只展示后端实际注册成功的执行类型。 */
    public List<String> executionTypes() {
        return executors.keySet().stream().sorted().toList();
    }
}
