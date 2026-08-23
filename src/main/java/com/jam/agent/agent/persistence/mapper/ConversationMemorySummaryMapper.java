package com.jam.agent.agent.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.agent.persistence.entity.ConversationMemorySummaryEntity;
import org.apache.ibatis.annotations.Param;

public interface ConversationMemorySummaryMapper extends BaseMapper<ConversationMemorySummaryEntity> {
    ConversationMemorySummaryEntity selectLatestOwned(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId);
}
