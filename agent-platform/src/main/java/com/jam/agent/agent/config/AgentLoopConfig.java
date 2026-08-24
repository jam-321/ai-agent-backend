package com.jam.agent.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;

/**
 * Effective limits for one AgentLoop run.
 *
 * <p>Values come from the Agent recipe first, but can never exceed the global
 * limits from {@link AgentProperties}. This keeps per-Agent tuning flexible
 * without allowing a database mistake to create an unbounded model run.
 */
public record AgentLoopConfig(
        int maxAttempts,
        int maxToolRounds,
        int maxToolsPerRound,
        Duration maxRunDuration,
        int maxDegenerateRetries,
        int maxSameToolSignature) {

    public static AgentLoopConfig resolve(
            AgentProperties.Loop defaults,
            String magicParams,
            ObjectMapper objectMapper) {
        JsonNode loop = readLoopNode(magicParams, objectMapper);

        return new AgentLoopConfig(
                boundedInt(loop, "maxAttempts", defaults.getMaxAttempts(), 1, defaults.getMaxAttempts()),
                boundedInt(loop, "maxToolRounds", defaults.getMaxToolRounds(), 1, defaults.getMaxToolRounds()),
                boundedInt(loop, "maxToolsPerRound", defaults.getMaxToolsPerRound(), 0, defaults.getMaxToolsPerRound()),
                boundedDuration(loop, defaults),
                boundedInt(loop, "maxDegenerateRetries", defaults.getMaxDegenerateRetries(), 0, defaults.getMaxDegenerateRetries()),
                boundedInt(loop, "maxSameToolSignature", defaults.getMaxSameToolSignature(), 1, defaults.getMaxSameToolSignature()));
    }

    private static JsonNode readLoopNode(String magicParams, ObjectMapper objectMapper) {
        if (magicParams == null || magicParams.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(magicParams);
            if (root == null || root.isNull() || !root.isObject()) {
                return null;
            }
            JsonNode loop = root.get("loop");
            return loop == null || !loop.isObject() ? root : loop;
        } catch (Exception ignored) {
            // 配置错误时回退到 YAML 默认值，不能因为一个 Agent 配方阻塞整个服务启动。
            return null;
        }
    }

    private static int boundedInt(
            JsonNode node,
            String field,
            int defaultValue,
            int minimum,
            int maximum) {
        if (node == null || !node.has(field) || !node.get(field).canConvertToInt()) {
            return defaultValue;
        }
        return Math.max(minimum, Math.min(maximum, node.get(field).asInt()));
    }

    private static Duration boundedDuration(JsonNode node, AgentProperties.Loop defaults) {
        Duration configured = defaults.getMaxRunDuration();
        if (node != null && node.has("maxRunDurationSeconds")
                && node.get("maxRunDurationSeconds").canConvertToLong()) {
            configured = Duration.ofSeconds(Math.max(1, node.get("maxRunDurationSeconds").asLong()));
        }
        return configured.compareTo(defaults.getMaxRunDuration()) > 0
                ? defaults.getMaxRunDuration()
                : configured;
    }
}
