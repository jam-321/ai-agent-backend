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
    private String systemPrompt;
    private String enabledPlugins;
    private String magicParams;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAgentKey() { return agentKey; }
    public void setAgentKey(String agentKey) { this.agentKey = agentKey; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getEnabledPlugins() { return enabledPlugins; }
    public void setEnabledPlugins(String enabledPlugins) { this.enabledPlugins = enabledPlugins; }
    public String getMagicParams() { return magicParams; }
    public void setMagicParams(String magicParams) { this.magicParams = magicParams; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
