package com.jam.agent.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 当前 Agent 的上下文压缩参数快照。 */
public record AgentMemoryConfig(
        boolean compactionEnabled,
        int compactionTriggerTokens,
        int keepRecentTokens,
        int maxToolResultTokens,
        int compactedToolPreviewChars,
        int maxToolPairsPerTurn) {

    /** 兼容旧调用方，历史工具对数量使用全局默认值。 */
    public AgentMemoryConfig(
            boolean compactionEnabled,
            int compactionTriggerTokens,
            int keepRecentTokens,
            int maxToolResultTokens,
            int compactedToolPreviewChars) {
        this(compactionEnabled, compactionTriggerTokens, keepRecentTokens,
                maxToolResultTokens, compactedToolPreviewChars, 3);
    }

    public static AgentMemoryConfig resolve(
            AgentProperties.Memory defaults,
            String magicParams,
            ObjectMapper objectMapper) {
        JsonNode node = AgentBudgetConfig.section(magicParams, "memory", objectMapper);
        boolean enabled = node == null || !node.has("compactionEnabled")
                ? defaults.isCompactionEnabled()
                : node.path("compactionEnabled").asBoolean(defaults.isCompactionEnabled());
        int trigger = AgentBudgetConfig.boundedInt(node, "compactionTriggerTokens",
                defaults.getCompactionTriggerTokens(), 1024, defaults.getCompactionTriggerTokens());
        int keepRecent = AgentBudgetConfig.boundedInt(node, "keepRecentTokens",
                defaults.getKeepRecentTokens(), 512, defaults.getKeepRecentTokens());
        return new AgentMemoryConfig(
                enabled,
                trigger,
                Math.min(keepRecent, Math.max(512, trigger - 512)),
                AgentBudgetConfig.boundedInt(node, "maxToolResultTokens",
                        defaults.getMaxToolResultTokens(), 256, defaults.getMaxToolResultTokens()),
                AgentBudgetConfig.boundedInt(node, "compactedToolPreviewChars",
                        defaults.getCompactedToolPreviewChars(), 100, defaults.getCompactedToolPreviewChars()),
                AgentBudgetConfig.boundedInt(node, "maxToolPairsPerTurn",
                        defaults.getMaxToolPairsPerTurn(), 0, defaults.getMaxToolPairsPerTurn()));
    }
}
