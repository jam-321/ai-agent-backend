package com.jam.agent.agent.config;

import java.util.Set;

/** Immutable agent recipe captured for one turn. */
public record AgentConfigSnapshot(
        String agentKey,
        String systemPrompt,
        Set<String> enabledPlugins,
        Set<String> enabledTools,
        String magicParams) {

    public AgentConfigSnapshot {
        enabledPlugins = enabledPlugins == null ? Set.of() : Set.copyOf(enabledPlugins);
        // null means legacy recipe: all registered tools remain available.
        enabledTools = enabledTools == null ? null : Set.copyOf(enabledTools);
    }

    public boolean isToolEnabled(String toolName) {
        return enabledTools == null || enabledTools.contains(toolName);
    }

    public static AgentConfigSnapshot defaultConfig() {
        return new AgentConfigSnapshot(
                "general",
                "你是一个友好、专业的中文 AI Agent。需要准确时间时调用 current_time，需要精确算术时调用 calculate，需要查询历史工具完整数据时调用 query_conversation_node。不要编造工具结果。",
                Set.of(),
                null,
                "{}");
    }
}
