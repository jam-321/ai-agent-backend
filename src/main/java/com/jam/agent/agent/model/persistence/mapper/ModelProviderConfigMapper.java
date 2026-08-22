package com.jam.agent.agent.model.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.agent.model.persistence.entity.ModelProviderConfigEntity;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ModelProviderConfigMapper extends BaseMapper<ModelProviderConfigEntity> {

    ModelProviderConfigEntity selectEnabledByProviderKey(
            @Param("providerKey") String providerKey);

    ModelProviderConfigEntity selectAvailableByProviderKey(
            @Param("userId") long userId,
            @Param("providerKey") String providerKey);

    List<ModelProviderConfigEntity> selectAvailableForUser(@Param("userId") long userId);
}
