package com.jam.agent.agent.dto;

import com.jam.agent.agent.config.AgentConfigSnapshot;
import java.util.Set;

/** Public Agent recipe; model credentials are intentionally excluded. */
public record AgentConfigResponse(
        String agentKey,
        String systemPrompt,
        Set<String> enabledPlugins,
        Set<String> enabledTools,
        String magicParams,
        String imageHistoryMode,
        String executionType,
        String executionKey,
        String modelProviderKey,
        String modelProviderName,
        String modelBaseUrl,
        String modelName,
        Double modelTemperature,
        boolean modelApiKeyConfigured) {

    public static AgentConfigResponse from(AgentConfigSnapshot snapshot) {
        return new AgentConfigResponse(
                snapshot.agentKey(),
                snapshot.systemPrompt(),
                snapshot.enabledPlugins(),
                snapshot.enabledTools(),
                snapshot.magicParams(),
                snapshot.imageHistoryMode(),
                snapshot.executionType(),
                snapshot.executionKey(),
                snapshot.modelConfig().providerKey(),
                snapshot.modelConfig().providerName(),
                snapshot.modelConfig().baseUrl(),
                snapshot.modelConfig().modelName(),
                snapshot.modelConfig().temperature(),
                snapshot.modelConfig().apiKeyConfigured());
    }
}
