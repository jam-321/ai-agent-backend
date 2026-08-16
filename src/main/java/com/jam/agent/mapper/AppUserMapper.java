package com.jam.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.entity.AppUserEntity;
import org.apache.ibatis.annotations.Param;

public interface AppUserMapper extends BaseMapper<AppUserEntity> {

    AppUserEntity selectByUsername(@Param("username") String username);
}
