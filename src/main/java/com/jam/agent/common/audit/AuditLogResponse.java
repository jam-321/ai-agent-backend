package com.jam.agent.common.audit;

import java.time.LocalDateTime;

public record AuditLogResponse(
        long id,
        long userId,
        String username,
        String action,
        String targetType,
        String targetId,
        String requestMethod,
        String requestUri,
        String result,
        String detail,
        LocalDateTime createdAt) {

    static AuditLogResponse from(AuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(), entity.getUserId(), entity.getUsername(), entity.getAction(),
                entity.getTargetType(), entity.getTargetId(), entity.getRequestMethod(),
                entity.getRequestUri(), entity.getResult(), entity.getDetail(), entity.getCreatedAt());
    }
}
