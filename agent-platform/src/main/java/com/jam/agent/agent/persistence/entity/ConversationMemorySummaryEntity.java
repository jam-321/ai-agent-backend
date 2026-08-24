package com.jam.agent.agent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("conversation_memory_summary")
public class ConversationMemorySummaryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Integer coveredFromTurnId;
    private Integer coveredUntilTurnId;
    private String content;
    private String modelProviderKey;
    private String modelName;
    private Long inputTokens;
    private Long outputTokens;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long value) { conversationId = value; }
    public Integer getCoveredFromTurnId() { return coveredFromTurnId; }
    public void setCoveredFromTurnId(Integer value) { coveredFromTurnId = value; }
    public Integer getCoveredUntilTurnId() { return coveredUntilTurnId; }
    public void setCoveredUntilTurnId(Integer value) { coveredUntilTurnId = value; }
    public String getContent() { return content; }
    public void setContent(String value) { content = value; }
    public String getModelProviderKey() { return modelProviderKey; }
    public void setModelProviderKey(String value) { modelProviderKey = value; }
    public String getModelName() { return modelName; }
    public void setModelName(String value) { modelName = value; }
    public Long getInputTokens() { return inputTokens; }
    public void setInputTokens(Long value) { inputTokens = value; }
    public Long getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Long value) { outputTokens = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
