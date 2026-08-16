package com.jam.agent.repository;

import com.jam.agent.entity.ConversationEntity;
import com.jam.agent.mapper.ConversationMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationRepository {

    private final ConversationMapper mapper;

    public ConversationRepository(ConversationMapper mapper) {
        this.mapper = mapper;
    }

    public long insert(long userId, String title) {
        ConversationEntity conversation = new ConversationEntity();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        mapper.insert(conversation);

        if (conversation.getId() == null) {
            throw new IllegalStateException("创建会话失败。");
        }
        return conversation.getId();
    }

    public Optional<ConversationRecord> findForUser(long userId, long conversationId) {
        return Optional.ofNullable(mapper.selectForUser(userId, conversationId))
                .map(this::toRecord);
    }

    public List<ConversationRecord> listForUser(long userId) {
        return mapper.selectListForUser(userId).stream()
                .map(this::toRecord)
                .toList();
    }

    public void updateTitleIfEmpty(long userId, long conversationId, String title) {
        mapper.updateTitleIfEmpty(userId, conversationId, title);
    }

    public void touch(long userId, long conversationId) {
        mapper.touch(userId, conversationId);
    }

    public void lockForUpdate(long userId, long conversationId) {
        if (mapper.lockForUpdate(userId, conversationId).isEmpty()) {
            throw new IllegalArgumentException("会话不存在。");
        }
    }

    public void softDelete(long userId, long conversationId) {
        mapper.softDelete(userId, conversationId);
    }

    private ConversationRecord toRecord(ConversationEntity conversation) {
        return new ConversationRecord(
                conversation.getId(),
                conversation.getUserId(),
                conversation.getTitle(),
                conversation.getSource(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }

    public record ConversationRecord(
            long id,
            long userId,
            String title,
            String source,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
