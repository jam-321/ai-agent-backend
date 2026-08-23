package com.jam.agent.agent.persistence.repository;

import com.jam.agent.agent.model.protocol.ModelCallResult;
import com.jam.agent.agent.persistence.entity.ConversationMemorySummaryEntity;
import com.jam.agent.agent.persistence.mapper.ConversationMemorySummaryMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationMemorySummaryRepository {
    private final ConversationMemorySummaryMapper mapper;

    public ConversationMemorySummaryRepository(ConversationMemorySummaryMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<SummaryRecord> latest(long userId, long conversationId) {
        return Optional.ofNullable(mapper.selectLatestOwned(userId, conversationId))
                .map(this::toRecord);
    }

    public void insert(
            long conversationId,
            int coveredFromTurnId,
            int coveredUntilTurnId,
            String content,
            String providerKey,
            String modelName,
            ModelCallResult usage) {
        ConversationMemorySummaryEntity entity = new ConversationMemorySummaryEntity();
        entity.setConversationId(conversationId);
        entity.setCoveredFromTurnId(coveredFromTurnId);
        entity.setCoveredUntilTurnId(coveredUntilTurnId);
        entity.setContent(content);
        entity.setModelProviderKey(providerKey);
        entity.setModelName(modelName);
        entity.setInputTokens(usage.inputTokens());
        entity.setOutputTokens(usage.outputTokens());
        mapper.insert(entity);
    }

    private SummaryRecord toRecord(ConversationMemorySummaryEntity entity) {
        return new SummaryRecord(
                entity.getId(),
                entity.getConversationId(),
                entity.getCoveredFromTurnId(),
                entity.getCoveredUntilTurnId(),
                entity.getContent(),
                entity.getModelProviderKey(),
                entity.getModelName(),
                entity.getInputTokens(),
                entity.getOutputTokens());
    }

    public record SummaryRecord(
            long id,
            long conversationId,
            int coveredFromTurnId,
            int coveredUntilTurnId,
            String content,
            String providerKey,
            String modelName,
            Long inputTokens,
            Long outputTokens) {
    }
}
