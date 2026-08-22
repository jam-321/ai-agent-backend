package com.jam.agent.agent.model;

/** Immutable model recipe captured with an Agent at Turn submission time. */
public record AgentModelConfig(
        String providerKey,
        String providerName,
        String protocolType,
        String baseUrl,
        String apiKey,
        String modelName,
        Double temperature) {

    public AgentModelConfig {
        providerKey = normalize(providerKey);
        providerName = normalize(providerName);
        protocolType = normalize(protocolType);
        baseUrl = normalize(baseUrl);
        apiKey = normalize(apiKey);
        modelName = normalize(modelName);
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("模型 temperature 必须在 0 到 2 之间。");
        }
    }

    public boolean apiKeyConfigured() {
        return apiKey != null;
    }

    @Override
    public String toString() {
        // API Key 不进入日志、异常上下文或调试输出。
        return "AgentModelConfig[providerKey=" + providerKey
                + ", protocolType=" + protocolType
                + ", baseUrl=" + baseUrl
                + ", apiKey=***"
                + ", modelName=" + modelName
                + ", temperature=" + temperature + "]";
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
