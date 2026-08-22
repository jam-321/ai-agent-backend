package com.jam.agent.common.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {

    List<AuditLogEntity> selectPage(@Param("offset") int offset, @Param("size") int size);

    long countAll();
}
