package com.jam.agent.agent.model;

import com.jam.agent.agent.runtime.AgentRunException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Builds and caches model clients while keeping per-Agent model options independent. */
@Component
public class ModelRegistry {

    private static final String OPENAI_COMPATIBLE = "OPENAI_COMPATIBLE";

    private final ChatModel defaultModel;
    private final String defaultBaseUrl;
    private final String defaultApiKey;
    private final String defaultModelName;
    private final double defaultTemperature;
    private final Map<ConnectionKey, ChatModel> models = new ConcurrentHashMap<>();

    public ModelRegistry(
            ChatModel defaultModel,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String defaultBaseUrl,
            @Value("${spring.ai.openai.api-key:}") String defaultApiKey,
            @Value("${spring.ai.openai.chat.options.model:deepseek-v4-flash}") String defaultModelName,
            @Value("${spring.ai.openai.chat.options.temperature:0.7}") double defaultTemperature) {
        this.defaultModel = defaultModel;
        this.defaultBaseUrl = normalizeBaseUrl(defaultBaseUrl);
        this.defaultApiKey = defaultApiKey;
        this.defaultModelName = defaultModelName;
        this.defaultTemperature = defaultTemperature;
    }

    public ResolvedModel resolve(AgentModelConfig configured) {
        AgentModelConfig config = configured == null
                ? new AgentModelConfig(null, null, null, null, null, null, null)
                : configured;
        validateProvider(config);

        String baseUrl = config.baseUrl() == null
                ? defaultBaseUrl
                : normalizeBaseUrl(config.baseUrl());
        String apiKey = config.apiKey() == null
                ? defaultApiKey
                : resolveApiKey(config.apiKey());
        String modelName = config.modelName() == null
                ? defaultModelName
                : config.modelName();
        double temperature = config.temperature() == null
                ? defaultTemperature
                : config.temperature();

        if (!hasUsableApiKey(apiKey)) {
            return new ResolvedModel(defaultModel, modelName, temperature, false);
        }

        ChatModel model = usesDefaultConnection(baseUrl, apiKey)
                ? defaultModel
                : models.computeIfAbsent(
                        new ConnectionKey(baseUrl, apiKey),
                        key -> createOpenAiCompatibleModel(key, modelName, temperature));
        return new ResolvedModel(model, modelName, temperature, true);
    }

    private void validateProvider(AgentModelConfig config) {
        if (config.providerKey() != null && config.baseUrl() == null) {
            throw new AgentRunException(
                    "模型供应商不可用或未启用：" + config.providerKey(),
                    false);
        }
        if (config.protocolType() != null
                && !OPENAI_COMPATIBLE.equalsIgnoreCase(config.protocolType())) {
            throw new AgentRunException(
                    "暂不支持模型协议：" + config.protocolType(),
                    false);
        }
    }

    private ChatModel createOpenAiCompatibleModel(
            ConnectionKey connection,
            String modelName,
            double temperature) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(connection.baseUrl())
                .apiKey(connection.apiKey())
                .build();
        OpenAiChatOptions defaults = OpenAiChatOptions.builder()
                .model(modelName)
                .temperature(temperature)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(defaults)
                .build();
    }

    private String resolveApiKey(String configured) {
        if (!configured.regionMatches(true, 0, "env:", 0, 4)) {
            return configured;
        }
        String variableName = configured.substring(4).trim();
        return variableName.isEmpty() ? null : System.getenv(variableName);
    }

    private boolean usesDefaultConnection(String baseUrl, String apiKey) {
        return baseUrl.equals(defaultBaseUrl) && apiKey.equals(defaultApiKey);
    }

    private boolean hasUsableApiKey(String apiKey) {
        return apiKey != null
                && !apiKey.isBlank()
                && !apiKey.toLowerCase().contains("dummy");
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public record ResolvedModel(
            ChatModel model,
            String modelName,
            double temperature,
            boolean enabled) {
    }

    /** API Key participates in cache identity but is never exposed outside this class. */
    private record ConnectionKey(String baseUrl, String apiKey) {
    }
}
