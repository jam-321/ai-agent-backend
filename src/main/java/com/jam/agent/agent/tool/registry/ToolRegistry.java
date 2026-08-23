package com.jam.agent.agent.tool.registry;

import com.jam.agent.agent.tool.definition.AgentToolProvider;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

/** Discovers all Tool Provider beans and builds the immutable callback registry. */
@Component
public class ToolRegistry {

    private final Map<String, ToolCallback> callbacks;

    public ToolRegistry(List<AgentToolProvider> providers) {
        ToolCallback[] discovered = MethodToolCallbackProvider.builder()
                .toolObjects(providers.toArray())
                .build()
                .getToolCallbacks();

        Map<String, ToolCallback> registered = new LinkedHashMap<>();
        for (ToolCallback callback : discovered) {
            String name = callback.getToolDefinition().name();
            if (registered.putIfAbsent(name, callback) != null) {
                throw new IllegalStateException("工具名称重复：" + name);
            }
        }
        this.callbacks = Map.copyOf(registered);
    }

    public Map<String, ToolCallback> callbacks() {
        return callbacks;
    }

    public ToolCallback require(String name) {
        ToolCallback callback = callbacks.get(name);
        if (callback == null) {
            throw new IllegalArgumentException("未知工具：" + name);
        }
        return callback;
    }
}
