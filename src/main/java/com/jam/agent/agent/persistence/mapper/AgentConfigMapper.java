package com.jam.agent.agent.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.agent.persistence.entity.AgentConfigEntity;
import org.apache.ibatis.annotations.Param;

public interface AgentConfigMapper extends BaseMapper<AgentConfigEntity> {

    AgentConfigEntity selectByAgentKey(@Param("agentKey") String agentKey);

    void updateBuiltinRuntimeDefaults(@Param("defaults") String defaults);
}
