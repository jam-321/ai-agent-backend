package com.jam.agent.agent.model;

import com.jam.agent.agent.model.dto.ProviderAdminRequest;
import com.jam.agent.agent.model.dto.ProviderAdminResponse;
import com.jam.agent.agent.model.persistence.repository.ModelProviderConfigRepository;
import com.jam.agent.common.audit.AuditAction;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/providers")
public class ProviderAdminController {

    private final ModelProviderConfigRepository providers;

    public ProviderAdminController(ModelProviderConfigRepository providers) {
        this.providers = providers;
    }

    @GetMapping
    public List<ProviderAdminResponse> list() {
        return providers.findAllAdmin().stream().map(this::toResponse).toList();
    }

    @PostMapping
    @AuditAction(action = "CREATE_PROVIDER", targetType = "MODEL_PROVIDER")
    public void create(@RequestBody ProviderAdminRequest request) {
        providers.create(
                request.providerKey(), request.providerName(), request.protocolType(),
                request.baseUrl(), request.endpointPath(), request.apiKey(), request.models(),
                !Boolean.FALSE.equals(request.enabled()));
    }

    @PutMapping("/{providerKey}")
    @AuditAction(action = "UPDATE_PROVIDER", targetType = "MODEL_PROVIDER")
    public void update(
            @PathVariable String providerKey,
            @RequestBody ProviderAdminRequest request) {
        providers.update(
                providerKey, request.providerName(), request.protocolType(), request.baseUrl(),
                request.endpointPath(), request.apiKey(), request.models(), request.enabled());
    }

    private ProviderAdminResponse toResponse(ModelProviderConfigRepository.ProviderRecord provider) {
        return new ProviderAdminResponse(
                provider.id(), provider.userId(), provider.providerKey(), provider.providerName(),
                provider.protocolType(), provider.baseUrl(), provider.endpointPath(),
                provider.apiKey() != null && !provider.apiKey().isBlank(), provider.models(),
                provider.enabled());
    }
}
