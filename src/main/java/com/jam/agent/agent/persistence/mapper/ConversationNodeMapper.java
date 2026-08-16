package com.jam.agent.agent.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.agent.persistence.entity.ConversationNodeEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ConversationNodeMapper extends BaseMapper<ConversationNodeEntity> {

    List<ConversationNodeEntity> selectByTurn(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId,
            @Param("turnId") int turnId);

    List<ConversationNodeEntity> selectByTurnUnscoped(
            @Param("conversationId") long conversationId,
            @Param("turnId") int turnId);

    List<ConversationNodeEntity> selectHistoryTools(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId,
            @Param("currentTurnId") int currentTurnId);

    List<ConversationNodeEntity> selectToolNodes(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId,
            @Param("targetTurnId") int targetTurnId,
            @Param("aggrKey") String aggrKey);
}
