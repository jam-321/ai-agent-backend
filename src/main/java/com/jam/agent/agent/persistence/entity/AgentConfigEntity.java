package com.jam.agent.agent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("agent_config")
public class AgentConfigEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String agentKey;
    private String executionType;
    private String executionKey;
    private String systemPrompt;
    private String enabledPlugins;
    private String enabledTools;
    private String magicParams;
    private String imageHistoryMode;
    private String modelProviderKey;
    private String modelName;
    private Double modelTemperature;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAgentKey() { return agentKey; }
    public void setAgentKey(String agentKey) { this.agentKey = agentKey; }
    public String getExecutionType() { return executionType; }
    public void setExecutionType(String executionType) { this.executionType = executionType; }
    public String getExecutionKey() { return executionKey; }
    public void setExecutionKey(String executionKey) { this.executionKey = executionKey; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getEnabledPlugins() { return enabledPlugins; }
    public void setEnabledPlugins(String enabledPlugins) { this.enabledPlugins = enabledPlugins; }
    public String getEnabledTools() { return enabledTools; }
    public void setEnabledTools(String enabledTools) { this.enabledTools = enabledTools; }
    public String getMagicParams() { return magicParams; }
    public void setMagicParams(String magicParams) { this.magicParams = magicParams; }
    public String getImageHistoryMode() { return imageHistoryMode; }
    public void setImageHistoryMode(String imageHistoryMode) { this.imageHistoryMode = imageHistoryMode; }
    public String getModelProviderKey() { return modelProviderKey; }
    public void setModelProviderKey(String modelProviderKey) { this.modelProviderKey = modelProviderKey; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Double getModelTemperature() { return modelTemperature; }
    public void setModelTemperature(Double modelTemperature) { this.modelTemperature = modelTemperature; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
