package com.jam.agent.agent.model.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.agent.model.persistence.entity.ModelProviderConfigEntity;
import org.apache.ibatis.annotations.Param;

public interface ModelProviderConfigMapper extends BaseMapper<ModelProviderConfigEntity> {

    ModelProviderConfigEntity selectEnabledByProviderKey(
            @Param("providerKey") String providerKey);
}
