package com.jam.agent.agent.model.dto;

import com.jam.agent.agent.model.ModelDescriptor;
import java.util.List;

public record ProviderAdminResponse(
        long id,
        Long userId,
        String providerKey,
        String providerName,
        String protocolType,
        String baseUrl,
        String endpointPath,
        boolean apiKeyConfigured,
        List<ModelDescriptor> models,
        boolean enabled) {
}
