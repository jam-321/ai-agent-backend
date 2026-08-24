package com.jam.agent.agent.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.agent.persistence.entity.MediaAssetEntity;
import org.apache.ibatis.annotations.Param;

public interface MediaAssetMapper extends BaseMapper<MediaAssetEntity> {
    MediaAssetEntity selectOwned(@Param("userId") long userId, @Param("assetId") long assetId);
    MediaAssetEntity selectByIdForUpdate(@Param("assetId") long assetId);
}
