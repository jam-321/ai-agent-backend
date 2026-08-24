package com.jam.agent.agent.persistence.repository;

import com.jam.agent.agent.persistence.entity.MediaAssetEntity;
import com.jam.agent.agent.persistence.mapper.MediaAssetMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MediaAssetRepository {
    private final MediaAssetMapper mapper;

    public MediaAssetRepository(MediaAssetMapper mapper) { this.mapper = mapper; }

    public MediaAssetEntity save(MediaAssetEntity entity) {
        mapper.insert(entity);
        return entity;
    }

    public Optional<MediaAssetEntity> findOwned(long userId, long assetId) {
        return Optional.ofNullable(mapper.selectOwned(userId, assetId));
    }

    public Optional<MediaAssetEntity> find(long assetId) {
        return Optional.ofNullable(mapper.selectById(assetId));
    }

    public void saveSummary(long assetId, String summary, String modelName) {
        MediaAssetEntity entity = mapper.selectById(assetId);
        if (entity == null) return;
        entity.setSummary(summary);
        entity.setSummaryModel(modelName);
        entity.setSummaryCreatedAt(LocalDateTime.now());
        mapper.updateById(entity);
    }

    public void markBound(long assetId) {
        MediaAssetEntity entity = mapper.selectById(assetId);
        if (entity == null) return;
        entity.setStatus("BOUND");
        mapper.updateById(entity);
    }
}
