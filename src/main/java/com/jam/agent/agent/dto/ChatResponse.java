package com.jam.agent.agent.dto;

/**
 * 聊天响应。
 */
public record ChatResponse(Long conversationId, Integer turnId, String traceId, String turnStatus) {
}
