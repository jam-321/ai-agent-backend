package com.jam.agent.agent.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("conversation_node_output")
public class ConversationNodeOutputEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Integer turnId;
    private String nodeName;
    private String aggrKey;
    private String traceId;
    private String type;
    private String content;
    private Integer contentTokens;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long value) { conversationId = value; }
    public Integer getTurnId() { return turnId; }
    public void setTurnId(Integer value) { turnId = value; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String value) { nodeName = value; }
    public String getAggrKey() { return aggrKey; }
    public void setAggrKey(String value) { aggrKey = value; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String value) { traceId = value; }
    public String getType() { return type; }
    public void setType(String value) { type = value; }
    public String getContent() { return content; }
    public void setContent(String value) { content = value; }
    public Integer getContentTokens() { return contentTokens; }
    public void setContentTokens(Integer value) { contentTokens = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
