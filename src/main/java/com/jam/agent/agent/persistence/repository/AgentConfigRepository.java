package com.jam.agent.agent.persistence.repository;

import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.persistence.entity.AgentConfigEntity;
import com.jam.agent.agent.persistence.mapper.AgentConfigMapper;
import com.jam.agent.agent.model.AgentModelConfig;
import com.jam.agent.agent.model.persistence.repository.ModelProviderConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AgentConfigRepository {

    private final AgentConfigMapper mapper;
    private final ObjectMapper objectMapper;
    private final ModelProviderConfigRepository modelProviders;

    public AgentConfigRepository(
            AgentConfigMapper mapper,
            ObjectMapper objectMapper,
            ModelProviderConfigRepository modelProviders) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.modelProviders = modelProviders;
    }

    public Optional<AgentConfigSnapshot> findByKey(String agentKey) {
        return Optional.ofNullable(mapper.selectByAgentKey(agentKey)).map(this::toSnapshot);
    }

    public List<AgentConfigSnapshot> findAll() {
        return mapper.selectList(null).stream().map(this::toSnapshot).toList();
    }

    public long count() {
        return mapper.selectCount(null);
    }

    public Optional<AgentConfigEntity> findEntityByKey(String agentKey) {
        return Optional.ofNullable(mapper.selectByAgentKey(agentKey));
    }

    public void save(AgentConfigEntity entity) {
        if (entity.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
    }

    public void deleteByKey(String agentKey) {
        AgentConfigEntity entity = mapper.selectByAgentKey(agentKey);
        if (entity == null) {
            throw new IllegalArgumentException("Agent 不存在。");
        }
        mapper.deleteById(entity.getId());
    }

    public void updateBuiltinRuntimeDefaults(String defaults) {
        mapper.updateBuiltinRuntimeDefaults(defaults);
    }

    public void insert(
            String agentKey,
            String systemPrompt,
            String enabledPlugins,
            String enabledTools,
            String magicParams) {
        insert(agentKey, systemPrompt, enabledPlugins, enabledTools, magicParams, "LOOP", null);
    }

    public void insert(
            String agentKey,
            String systemPrompt,
            String enabledPlugins,
            String enabledTools,
            String magicParams,
            String executionType,
            String executionKey) {
        insert(agentKey, systemPrompt, enabledPlugins, enabledTools, magicParams,
                executionType, executionKey, null);
    }

    public void insert(
            String agentKey,
            String systemPrompt,
            String enabledPlugins,
            String enabledTools,
            String magicParams,
            String executionType,
            String executionKey,
            AgentModelConfig modelConfig) {
        AgentConfigEntity entity = new AgentConfigEntity();
        entity.setAgentKey(agentKey);
        entity.setExecutionType(executionType);
        entity.setExecutionKey(executionKey);
        entity.setSystemPrompt(systemPrompt);
        entity.setEnabledPlugins(enabledPlugins);
        entity.setEnabledTools(enabledTools);
        entity.setMagicParams(magicParams);
        if (modelConfig != null) {
            entity.setModelProviderKey(modelConfig.providerKey());
            entity.setModelName(modelConfig.modelName());
            entity.setModelTemperature(modelConfig.temperature());
        }
        mapper.insert(entity);
    }

    private AgentConfigSnapshot toSnapshot(AgentConfigEntity entity) {
        ModelProviderConfigRepository.ProviderRecord provider = modelProviders
                .findEnabledByKey(entity.getModelProviderKey())
                .orElse(null);
        AgentModelConfig modelConfig = provider == null
                ? new AgentModelConfig(
                        entity.getModelProviderKey(), null, null, null, null, null,
                        entity.getModelName(), entity.getModelTemperature())
                : provider.toModelConfig(entity.getModelName(), entity.getModelTemperature());
        AgentModelConfig fallbackModelConfig = resolveOptionalModel(
                entity.getFallbackModelProviderKey(),
                entity.getFallbackModelName(),
                entity.getModelTemperature());
        return new AgentConfigSnapshot(
                entity.getAgentKey(),
                Boolean.TRUE.equals(entity.getAdminOnly()),
                entity.getSystemPrompt(),
                parsePlugins(entity.getEnabledPlugins()),
                parseTools(entity.getEnabledTools()),
                entity.getMagicParams(),
                entity.getImageHistoryMode(),
                entity.getExecutionType(),
                entity.getExecutionKey(),
                modelConfig,
                fallbackModelConfig);
    }

    private AgentModelConfig resolveOptionalModel(
            String providerKey,
            String modelName,
            Double temperature) {
        if (providerKey == null || providerKey.isBlank() || modelName == null || modelName.isBlank()) {
            return null;
        }
        return modelProviders.findEnabledByKey(providerKey)
                .filter(provider -> provider.models().stream()
                        .anyMatch(model -> model.modelName().equals(modelName)))
                .map(provider -> provider.toModelConfig(modelName, temperature))
                .orElse(null);
    }

    private java.util.Set<String> parsePlugins(String json) {
        if (json == null || json.isBlank()) {
            return java.util.Set.of();
        }
        try {
            return new HashSet<>(objectMapper.readValue(json, new TypeReference<List<String>>() { }));
        } catch (Exception exception) {
            return java.util.Set.of();
        }
    }

    /**
     * A null column keeps backward compatibility and means all registered tools.
     * An explicit empty JSON array means this Agent intentionally has no tools.
     */
    private java.util.Set<String> parseTools(String json) {
        if (json == null || json.isBlank() || "null".equalsIgnoreCase(json.trim())) {
            return null;
        }
        try {
            return new HashSet<>(objectMapper.readValue(json, new TypeReference<List<String>>() { }));
        } catch (Exception exception) {
            return java.util.Set.of();
        }
    }
}
