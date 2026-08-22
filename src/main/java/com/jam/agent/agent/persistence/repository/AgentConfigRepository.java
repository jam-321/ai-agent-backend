package com.jam.agent.agent.persistence.repository;

import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.persistence.entity.AgentConfigEntity;
import com.jam.agent.agent.persistence.mapper.AgentConfigMapper;
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

    public AgentConfigRepository(AgentConfigMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
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
        AgentConfigEntity entity = new AgentConfigEntity();
        entity.setAgentKey(agentKey);
        entity.setExecutionType(executionType);
        entity.setExecutionKey(executionKey);
        entity.setSystemPrompt(systemPrompt);
        entity.setEnabledPlugins(enabledPlugins);
        entity.setEnabledTools(enabledTools);
        entity.setMagicParams(magicParams);
        mapper.insert(entity);
    }

    private AgentConfigSnapshot toSnapshot(AgentConfigEntity entity) {
        return new AgentConfigSnapshot(
                entity.getAgentKey(),
                entity.getSystemPrompt(),
                parsePlugins(entity.getEnabledPlugins()),
                parseTools(entity.getEnabledTools()),
                entity.getMagicParams(),
                entity.getExecutionType(),
                entity.getExecutionKey());
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
