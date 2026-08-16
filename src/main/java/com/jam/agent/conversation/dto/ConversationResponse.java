package com.jam.agent.conversation.dto;

import java.time.LocalDateTime;

public record ConversationResponse(Long id, String title, LocalDateTime createdAt, LocalDateTime updatedAt) {}
