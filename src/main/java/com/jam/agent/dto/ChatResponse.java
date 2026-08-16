package com.jam.agent.dto;

/**
 * 聊天响应。
 */
public record ChatResponse(Long conversationId, Integer turnId, String traceId, String turnStatus) {
}
