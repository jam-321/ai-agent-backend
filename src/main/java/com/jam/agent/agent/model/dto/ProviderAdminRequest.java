package com.jam.agent.agent.model.dto;

import com.jam.agent.agent.model.ModelDescriptor;
import java.util.List;

public record ProviderAdminRequest(
        String providerKey,
        String providerName,
        String protocolType,
        String baseUrl,
        String endpointPath,
        String apiKey,
        List<ModelDescriptor> models,
        Boolean enabled) {
}
