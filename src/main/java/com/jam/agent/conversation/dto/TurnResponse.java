package com.jam.agent.conversation.dto;

import java.time.LocalDateTime;

public record TurnResponse(Integer turnId, String type, String content, String errorMessage,
                           String traceId, LocalDateTime createdAt, LocalDateTime updatedAt) {}
