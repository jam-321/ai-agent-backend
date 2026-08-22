package com.jam.agent.agent.config;

public record AgentConfigAdminRequest(
        String agentKey,
        String executionType,
        String executionKey,
        String systemPrompt,
        String enabledPlugins,
        String enabledTools,
        String magicParams,
        String imageHistoryMode,
        String modelProviderKey,
        String modelName,
        Double modelTemperature) {
}
