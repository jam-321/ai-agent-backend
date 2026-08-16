package com.jam.agent.conversation.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jam.agent.conversation.persistence.entity.ConversationTurnEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ConversationTurnMapper extends BaseMapper<ConversationTurnEntity> {

    Integer findMaxTurnId(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId);

    int insertOwned(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId,
            @Param("turnId") int turnId,
            @Param("type") String type,
            @Param("content") String content,
            @Param("traceId") String traceId,
            @Param("errorMessage") String errorMessage);

    ConversationTurnEntity selectForUser(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId,
            @Param("turnId") int turnId,
            @Param("type") String type);

    List<ConversationTurnEntity> selectCompletedBefore(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId,
            @Param("currentTurnId") int currentTurnId,
            @Param("limit") int limit);

    List<ConversationTurnEntity> selectIncompleteTurns();

    List<ConversationTurnEntity> selectTurnsForUser(
            @Param("userId") long userId,
            @Param("conversationId") long conversationId);

    List<IncompleteTurnContext> selectIncompleteTurnContexts();

    int countAssistant(
            @Param("conversationId") long conversationId,
            @Param("turnId") int turnId);

    record IncompleteTurnContext(
            long userId,
            long conversationId,
            int turnId,
            String content,
            String traceId) {
    }
}
