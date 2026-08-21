package com.jam.agent.agent.dto;

/**
 * 聊天请求。
 */
public record ChatRequest(Long conversationId, String message, String agentKey) {
}
