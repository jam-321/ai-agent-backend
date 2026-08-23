package com.jam.agent.auth.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.auth.persistence.entity.AppUserEntity;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface AppUserMapper extends BaseMapper<AppUserEntity> {

    AppUserEntity selectByUsername(@Param("username") String username);

    List<AppUserEntity> selectAdminPage(@Param("offset") int offset, @Param("size") int size);

    long countAllUsers();

    long countAdmins();
}
