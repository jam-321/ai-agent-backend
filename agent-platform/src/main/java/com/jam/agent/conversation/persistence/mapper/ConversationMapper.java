package com.jam.agent.conversation.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.conversation.persistence.entity.ConversationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ConversationMapper extends BaseMapper<ConversationEntity> {

    ConversationEntity selectForUser(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId);

    List<ConversationEntity> selectListForUser(@Param("userId") long userId);

    int updateTitleIfEmpty(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId,
            @Param("title") String title);

    int updateAgentKey(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId,
            @Param("agentKey") String agentKey);

    int updateExecutionSelection(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId,
            @Param("agentKey") String agentKey,
            @Param("modelProviderKey") String modelProviderKey,
            @Param("modelName") String modelName);

    int touch(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId);

    List<Long> lockForUpdate(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId);

    int softDelete(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId);
}
