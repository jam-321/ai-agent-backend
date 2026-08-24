package com.jam.agent.agent.model;

import com.jam.agent.agent.dto.ModelOptionResponse;
import com.jam.agent.agent.model.persistence.repository.ModelProviderConfigRepository;
import com.jam.agent.agent.model.protocol.ModelProtocolRegistry;
import com.jam.agent.auth.security.AuthenticatedUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 查询系统模型和当前用户私有模型，不向前端暴露供应商凭据。 */
@RestController
@RequestMapping("/api/models")
public class ModelCatalogController {

    private final ModelProviderConfigRepository providers;
    private final ModelProtocolRegistry protocols;

    public ModelCatalogController(
            ModelProviderConfigRepository providers,
            ModelProtocolRegistry protocols) {
        this.providers = providers;
        this.protocols = protocols;
    }

    @GetMapping
    public List<ModelOptionResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return providers.findAvailableForUser(user.id()).stream()
                .flatMap(provider -> provider.models().stream().map(model -> {
                    boolean available = protocols.supports(provider.protocolType());
                    return new ModelOptionResponse(
                            provider.providerKey(),
                            provider.providerName(),
                            model.modelName(),
                            model.displayName(),
                            available,
                            available ? null : "当前后端尚未支持 " + provider.protocolType() + " 协议",
                            model.supportsImageInput(),
                            model.supportsTools());
                }))
                .toList();
    }
}
