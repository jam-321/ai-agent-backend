package com.jam.agent.agent.model.persistence.repository;

import com.jam.agent.agent.model.persistence.entity.ModelProviderConfigEntity;
import com.jam.agent.agent.model.persistence.mapper.ModelProviderConfigMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ModelProviderConfigRepository {

    private final ModelProviderConfigMapper mapper;

    public ModelProviderConfigRepository(ModelProviderConfigMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<ProviderRecord> findEnabledByKey(String providerKey) {
        if (providerKey == null || providerKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectEnabledByProviderKey(providerKey))
                .map(this::toRecord);
    }

    private ProviderRecord toRecord(ModelProviderConfigEntity entity) {
        return new ProviderRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getProviderKey(),
                entity.getProviderName(),
                entity.getProtocolType(),
                entity.getBaseUrl(),
                entity.getApiKey());
    }

    public record ProviderRecord(
            long id,
            Long userId,
            String providerKey,
            String providerName,
            String protocolType,
            String baseUrl,
            String apiKey) {

        @Override
        public String toString() {
            return "ProviderRecord[id=" + id
                    + ", userId=" + userId
                    + ", providerKey=" + providerKey
                    + ", apiKey=***]";
        }
    }
}
