package com.jam.agent.auth.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.auth.persistence.entity.AppUserEntity;
import org.apache.ibatis.annotations.Param;

public interface AppUserMapper extends BaseMapper<AppUserEntity> {

    AppUserEntity selectByUsername(@Param("username") String username);
}
