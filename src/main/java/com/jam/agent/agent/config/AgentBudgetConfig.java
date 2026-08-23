package com.jam.agent.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 当前 Turn 使用的预算快照，数据库配置不能突破 YAML 全局安全上限。 */
public record AgentBudgetConfig(
        long maxTokensPerTurn,
        int maxContextTokens,
        int maxOutputTokens,
        int safetyMarginTokens,
        int maxUserInputTokens) {

    /** 兼容旧测试和内部调用方，新增输入上限使用全局默认值。 */
    public AgentBudgetConfig(
            long maxTokensPerTurn,
            int maxContextTokens,
            int maxOutputTokens,
            int safetyMarginTokens) {
        this(maxTokensPerTurn, maxContextTokens, maxOutputTokens, safetyMarginTokens, 32000);
    }

    public static AgentBudgetConfig resolve(
            AgentProperties.Budget defaults,
            String magicParams,
            ObjectMapper objectMapper) {
        JsonNode node = section(magicParams, "budget", objectMapper);
        return new AgentBudgetConfig(
                boundedLong(node, "maxTokensPerTurn", defaults.getMaxTokensPerTurn(), 1, defaults.getMaxTokensPerTurn()),
                boundedInt(node, "maxContextTokens", defaults.getMaxContextTokens(), 1024, defaults.getMaxContextTokens()),
                boundedInt(node, "maxOutputTokens", defaults.getMaxOutputTokens(), 128, defaults.getMaxOutputTokens()),
                boundedInt(node, "safetyMarginTokens", defaults.getSafetyMarginTokens(), 0, defaults.getSafetyMarginTokens()),
                boundedInt(node, "maxUserInputTokens", defaults.getMaxUserInputTokens(), 1,
                        defaults.getMaxUserInputTokens()));
    }

    static JsonNode section(String magicParams, String name, ObjectMapper objectMapper) {
        if (magicParams == null || magicParams.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(magicParams);
            JsonNode value = root == null ? null : root.get(name);
            return value != null && value.isObject() ? value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    static int boundedInt(JsonNode node, String field, int fallback, int minimum, int maximum) {
        if (node == null || !node.path(field).canConvertToInt()) return fallback;
        return Math.max(minimum, Math.min(maximum, node.path(field).asInt()));
    }

    static long boundedLong(JsonNode node, String field, long fallback, long minimum, long maximum) {
        if (node == null || !node.path(field).canConvertToLong()) return fallback;
        return Math.max(minimum, Math.min(maximum, node.path(field).asLong()));
    }
}
