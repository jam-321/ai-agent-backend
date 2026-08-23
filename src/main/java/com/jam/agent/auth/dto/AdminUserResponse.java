package com.jam.agent.auth.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        long id,
        String username,
        boolean enabled,
        boolean admin,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
