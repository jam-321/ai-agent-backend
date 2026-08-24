package com.jam.agent.agent.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.agent.persistence.entity.ConversationNodeOutputEntity;
import org.apache.ibatis.annotations.Param;

public interface ConversationNodeOutputMapper extends BaseMapper<ConversationNodeOutputEntity> {
    ConversationNodeOutputEntity selectOwned(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId,
            @Param("turnId") int turnId,
            @Param("aggrKey") String aggrKey);
}
