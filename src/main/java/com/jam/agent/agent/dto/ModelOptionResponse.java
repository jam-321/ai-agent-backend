package com.jam.agent.agent.dto;

/** 前端可见的模型选项，只包含脱敏元数据，不携带供应商凭据。 */
public record ModelOptionResponse(
        String providerKey,
        String providerName,
        String modelName,
        String displayName,
        boolean available,
        String unavailableReason,
        boolean supportsImageInput,
        boolean supportsTools) {
}
