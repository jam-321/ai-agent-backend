package com.jam.agent.agent.model;

import com.jam.agent.agent.runtime.AgentRunException;
import com.jam.agent.agent.model.protocol.ModelCredentials;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 根据供应商连接构建并缓存模型客户端，同时保留每个 Turn 的独立模型参数。 */
@Component
public class ModelRegistry {

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
                ? new AgentModelConfig(null, null, null, null, null, null, null, null)
                : configured;
        validateProvider(config);

        String baseUrl = config.baseUrl() == null
                ? defaultBaseUrl
                : normalizeBaseUrl(config.baseUrl());
        String apiKey = config.apiKey() == null
                ? defaultApiKey
                : ModelCredentials.resolve(config.apiKey());
        String modelName = config.modelName() == null
                ? defaultModelName
                : config.modelName();
        double temperature = config.temperature() == null
                ? defaultTemperature
                : config.temperature();

        if (!ModelCredentials.isUsable(apiKey)) {
            return new ResolvedModel(defaultModel, modelName, temperature, false);
        }

        String completionsPath = config.endpointPath() == null
                ? "/v1/chat/completions"
                : config.endpointPath();
        ChatModel model = usesDefaultConnection(baseUrl, completionsPath, apiKey)
                ? defaultModel
                : models.computeIfAbsent(
                        new ConnectionKey(baseUrl, completionsPath, apiKey),
                        key -> createOpenAiCompatibleModel(key, modelName, temperature));
        return new ResolvedModel(model, modelName, temperature, true);
    }

    private void validateProvider(AgentModelConfig config) {
        if (config.providerKey() != null && config.baseUrl() == null) {
            throw new AgentRunException(
                    "模型供应商不可用或未启用：" + config.providerKey(),
                    false);
        }
    }

    private ChatModel createOpenAiCompatibleModel(
            ConnectionKey connection,
            String modelName,
            double temperature) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(connection.baseUrl())
                .completionsPath(connection.completionsPath())
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

    private boolean usesDefaultConnection(
            String baseUrl,
            String completionsPath,
            String apiKey) {
        return baseUrl.equals(defaultBaseUrl)
                && "/v1/chat/completions".equals(completionsPath)
                && apiKey.equals(defaultApiKey);
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

    /** API Key 参与客户端缓存标识，但不会离开本类或进入日志。 */
    private record ConnectionKey(
            String baseUrl,
            String completionsPath,
            String apiKey) {
    }
}
