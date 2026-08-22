package com.jam.agent.agent.config;

import com.jam.agent.agent.persistence.entity.AgentConfigEntity;
import com.jam.agent.agent.persistence.repository.AgentConfigRepository;
import com.jam.agent.agent.model.persistence.repository.ModelProviderConfigRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentConfigAdminService {

    private final AgentConfigRepository agents;
    private final ModelProviderConfigRepository providers;

    public AgentConfigAdminService(
            AgentConfigRepository agents,
            ModelProviderConfigRepository providers) {
        this.agents = agents;
        this.providers = providers;
    }

    public List<AgentConfigSnapshot> list() {
        return agents.findAll();
    }

    public void create(AgentConfigAdminRequest request) {
        if (request == null || request.agentKey() == null || request.agentKey().isBlank()) {
            throw new IllegalArgumentException("Agent 标识不能为空。");
        }
        if (agents.findByKey(request.agentKey()).isPresent()) {
            throw new IllegalArgumentException("Agent 标识已存在。");
        }
        agents.save(toEntity(request));
    }

    public void update(String agentKey, AgentConfigAdminRequest request) {
        AgentConfigEntity entity = agents.findEntityByKey(agentKey)
                .orElseThrow(() -> new IllegalArgumentException("Agent 不存在。"));
        apply(entity, request);
        agents.save(entity);
    }

    public void delete(String agentKey) {
        if ("general".equals(agentKey)) {
            throw new IllegalArgumentException("不能删除 general Agent。");
        }
        agents.deleteByKey(agentKey);
    }

    private AgentConfigEntity toEntity(AgentConfigAdminRequest request) {
        AgentConfigEntity entity = new AgentConfigEntity();
        entity.setAgentKey(request.agentKey().trim());
        apply(entity, request);
        return entity;
    }

    private void apply(AgentConfigEntity entity, AgentConfigAdminRequest request) {
        if (request == null || request.modelProviderKey() == null || request.modelName() == null) {
            throw new IllegalArgumentException("Agent 模型配置不能为空。");
        }
        providers.findEnabledByKey(request.modelProviderKey())
                .orElseThrow(() -> new IllegalArgumentException("模型供应商不存在或未启用。"));
        entity.setExecutionType(defaultValue(request.executionType(), "LOOP"));
        entity.setExecutionKey(request.executionKey());
        entity.setSystemPrompt(request.systemPrompt());
        entity.setEnabledPlugins(request.enabledPlugins());
        entity.setEnabledTools(request.enabledTools());
        entity.setMagicParams(request.magicParams());
        entity.setImageHistoryMode(defaultValue(request.imageHistoryMode(), "SUMMARY_TOOL"));
        entity.setModelProviderKey(request.modelProviderKey());
        entity.setModelName(request.modelName());
        entity.setModelTemperature(request.modelTemperature() == null ? 0.7 : request.modelTemperature());
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
