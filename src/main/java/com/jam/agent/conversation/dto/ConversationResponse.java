package com.jam.agent.conversation.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        String title,
        String agentKey,
        String modelProviderKey,
        String modelName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
