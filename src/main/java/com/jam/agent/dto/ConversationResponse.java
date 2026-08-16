package com.jam.agent.dto;

import java.time.LocalDateTime;

public record ConversationResponse(Long id, String title, LocalDateTime createdAt, LocalDateTime updatedAt) {}
