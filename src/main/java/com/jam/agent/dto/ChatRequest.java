package com.jam.agent.dto;

/**
 * 聊天请求。
 */
public record ChatRequest(Long conversationId, String message) {
}
