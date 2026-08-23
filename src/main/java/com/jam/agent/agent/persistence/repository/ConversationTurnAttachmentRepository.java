package com.jam.agent.agent.persistence.repository;

import com.jam.agent.agent.persistence.entity.ConversationTurnAttachmentEntity;
import com.jam.agent.agent.persistence.mapper.ConversationTurnAttachmentMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationTurnAttachmentRepository {
    private final ConversationTurnAttachmentMapper mapper;

    public ConversationTurnAttachmentRepository(ConversationTurnAttachmentMapper mapper) { this.mapper = mapper; }

    public void bind(long conversationId, int turnId, List<Long> assetIds) {
        for (int index = 0; index < assetIds.size(); index++) {
            ConversationTurnAttachmentEntity entity = new ConversationTurnAttachmentEntity();
            entity.setConversationId(conversationId);
            entity.setTurnId(turnId);
            entity.setAssetId(assetIds.get(index));
            entity.setSortOrder(index);
            mapper.insert(entity);
        }
    }

    public List<Long> findAssetIds(long userId, long conversationId, int turnId) {
        return mapper.selectForTurn(userId, conversationId, turnId).stream()
                .map(ConversationTurnAttachmentEntity::getAssetId).toList();
    }

    public List<AttachmentRecord> findHistory(long userId, long conversationId, int currentTurnId) {
        return mapper.selectHistory(userId, conversationId, currentTurnId).stream()
                .map(row -> new AttachmentRecord(row.getTurnId(), row.getAssetId()))
                .toList();
    }

    public record AttachmentRecord(int turnId, long assetId) {}
}
