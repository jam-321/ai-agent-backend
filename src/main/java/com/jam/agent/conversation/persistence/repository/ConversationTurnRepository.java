package com.jam.agent.conversation.persistence.repository;

import com.jam.agent.agent.model.AgentModelConfig;
import com.jam.agent.conversation.persistence.entity.ConversationTurnEntity;
import com.jam.agent.conversation.persistence.mapper.ConversationTurnMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationTurnRepository {

    private final ConversationTurnMapper mapper;

    public ConversationTurnRepository(ConversationTurnMapper mapper) {
        this.mapper = mapper;
    }

    public int nextTurnId(long userId, long conversationId) {
        Integer maxTurnId = mapper.findMaxTurnId(userId, conversationId);
        return maxTurnId == null ? 1 : maxTurnId + 1;
    }

    public void insert(
            long userId,
            long conversationId,
            int turnId,
            String type,
            String content,
            String traceId,
            String agentKey,
            AgentModelConfig modelConfig,
            String errorMessage) {
        int count = mapper.insertOwned(
                userId,
                conversationId,
                turnId,
                type,
                content,
                traceId,
                agentKey,
                modelConfig == null ? null : modelConfig.providerKey(),
                modelConfig == null ? null : modelConfig.modelName(),
                modelConfig == null ? null : modelConfig.protocolType(),
                errorMessage);
        if (count != 1) {
            throw new IllegalArgumentException("会话不存在。");
        }
    }

    public Optional<TurnRecord> findForUser(
            long userId,
            long conversationId,
            int turnId,
            String type) {
        return Optional.ofNullable(mapper.selectForUser(userId, conversationId, turnId, type))
                .map(this::toRecord);
    }

    public List<TurnRecord> findCompletedBefore(
            long userId,
            long conversationId,
            int currentTurnId,
            int limit) {
        return mapper.selectCompletedBefore(userId, conversationId, currentTurnId, limit * 2).stream()
                .map(this::toRecord)
                .toList();
    }

    public List<TurnRecord> findIncompleteTurns() {
        return mapper.selectIncompleteTurns().stream()
                .map(this::toRecord)
                .toList();
    }

    public List<IncompleteTurnRecord> findIncompleteTurnContexts() {
        return mapper.selectIncompleteTurnContexts().stream()
                .map(context -> new IncompleteTurnRecord(
                        context.userId(),
                        context.conversationId(),
                        context.turnId(),
                        context.content(),
                        context.traceId()))
                .toList();
    }

    public List<TurnRecord> findTurnsForUser(long userId, long conversationId) {
        return mapper.selectTurnsForUser(userId, conversationId).stream()
                .map(this::toRecord)
                .toList();
    }

    public boolean assistantTurnExists(long conversationId, int turnId) {
        return mapper.countAssistant(conversationId, turnId) > 0;
    }

    private TurnRecord toRecord(ConversationTurnEntity turn) {
        return new TurnRecord(
                turn.getId(),
                turn.getConversationId(),
                turn.getTurnId(),
                turn.getType(),
                turn.getContent(),
                Boolean.TRUE.equals(turn.getHidden()),
                turn.getErrorMessage(),
                turn.getTraceId(),
                turn.getAgentKey(),
                turn.getModelProviderKey(),
                turn.getModelName(),
                turn.getProtocolType(),
                turn.getCreatedAt(),
                turn.getUpdatedAt());
    }

    public record TurnRecord(
            long id,
            long conversationId,
            int turnId,
            String type,
            String content,
            boolean hidden,
            String errorMessage,
            String traceId,
            String agentKey,
            String modelProviderKey,
            String modelName,
            String protocolType,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record IncompleteTurnRecord(
            long userId,
            long conversationId,
            int turnId,
            String content,
            String traceId) {
    }
}
