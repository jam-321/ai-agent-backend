package com.jam.agent.agent.model.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.model.AgentModelConfig;
import com.jam.agent.agent.model.ModelDescriptor;
import com.jam.agent.agent.model.protocol.ModelProtocolRegistry;
import com.jam.agent.agent.model.persistence.entity.ModelProviderConfigEntity;
import com.jam.agent.agent.model.persistence.mapper.ModelProviderConfigMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ModelProviderConfigRepository {

    private final ModelProviderConfigMapper mapper;
    private final ObjectMapper objectMapper;
    private final ModelProtocolRegistry protocols;

    public ModelProviderConfigRepository(
            ModelProviderConfigMapper mapper,
            ObjectMapper objectMapper,
            ModelProtocolRegistry protocols) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.protocols = protocols;
    }

    public Optional<ProviderRecord> findAvailableForUser(
            long userId,
            String providerKey) {
        if (providerKey == null || providerKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectAvailableByProviderKey(userId, providerKey))
                .map(this::toRecord);
    }

    public List<ProviderRecord> findAvailableForUser(long userId) {
        return mapper.selectAvailableForUser(userId).stream()
                .map(this::toRecord)
                .toList();
    }

    public AgentModelConfig requireModel(
            long userId,
            String providerKey,
            String modelName,
            Double temperature) {
        ProviderRecord provider = findAvailableForUser(userId, providerKey)
                .orElseThrow(() -> new IllegalArgumentException("模型供应商不存在或不可用。"));
        if (!protocols.supports(provider.protocolType())) {
            throw new IllegalArgumentException("当前后端尚未支持该模型供应商的协议。");
        }
        if (modelName == null || modelName.isBlank()
                || provider.models().stream().noneMatch(model -> model.modelName().equals(modelName))) {
            throw new IllegalArgumentException("当前供应商不支持该模型。");
        }
        return provider.toModelConfig(modelName, temperature);
    }

    public Optional<ProviderRecord> findEnabledByKey(String providerKey) {
        if (providerKey == null || providerKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectEnabledByProviderKey(providerKey))
                .map(this::toRecord);
    }

    private ProviderRecord toRecord(ModelProviderConfigEntity entity) {
        return new ProviderRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getProviderKey(),
                entity.getProviderName(),
                entity.getProtocolType(),
                entity.getBaseUrl(),
                entity.getEndpointPath(),
                entity.getApiKey(),
                parseModels(entity.getModels()));
    }

    private List<ModelDescriptor> parseModels(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return List.of();
            }
            List<ModelDescriptor> models = new ArrayList<>();
            for (JsonNode item : root) {
                String name = item.isTextual() ? item.asText() : item.path("modelName").asText(null);
                if (name == null || name.isBlank()) {
                    name = item.path("model").asText(null);
                }
                if (name == null || name.isBlank()) {
                    continue;
                }
                String displayName = item.isTextual()
                        ? name
                        : item.path("displayName").asText(name);
                models.add(new ModelDescriptor(name, displayName));
            }
            return List.copyOf(models);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public record ProviderRecord(
            long id,
            Long userId,
            String providerKey,
            String providerName,
            String protocolType,
            String baseUrl,
            String endpointPath,
            String apiKey,
            List<ModelDescriptor> models) {

        public AgentModelConfig toModelConfig(String modelName, Double temperature) {
            return new AgentModelConfig(
                    providerKey,
                    providerName,
                    protocolType,
                    baseUrl,
                    endpointPath,
                    apiKey,
                    modelName,
                    temperature);
        }

        @Override
        public String toString() {
            return "ProviderRecord[id=" + id
                    + ", userId=" + userId
                    + ", providerKey=" + providerKey
                    + ", apiKey=***]";
        }
    }
}
