package com.jam.agent.agent.model;

/** Turn 提交时捕获的不可变模型配置，保证运行过程中配置不漂移。 */
public record AgentModelConfig(
        String providerKey,
        String providerName,
        String protocolType,
        String baseUrl,
        String endpointPath,
        String apiKey,
        String modelName,
        Double temperature,
        boolean supportsImageInput,
        boolean supportsTools) {

    public AgentModelConfig(
            String providerKey,
            String providerName,
            String protocolType,
            String baseUrl,
            String endpointPath,
            String apiKey,
            String modelName,
            Double temperature) {
        this(providerKey, providerName, protocolType, baseUrl, endpointPath, apiKey,
                modelName, temperature, false, true);
    }

    public AgentModelConfig {
        providerKey = normalize(providerKey);
        providerName = normalize(providerName);
        protocolType = normalize(protocolType);
        baseUrl = normalize(baseUrl);
        endpointPath = normalize(endpointPath);
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
                + ", endpointPath=" + endpointPath
                + ", apiKey=***"
                + ", modelName=" + modelName
                + ", temperature=" + temperature + "]";
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
