package com.jam.agent.agent.persistence.repository;

import com.jam.agent.agent.persistence.entity.ConversationNodeOutputEntity;
import com.jam.agent.agent.persistence.mapper.ConversationNodeOutputMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationNodeOutputRepository {
    private final ConversationNodeOutputMapper mapper;

    public ConversationNodeOutputRepository(ConversationNodeOutputMapper mapper) {
        this.mapper = mapper;
    }

    public void archive(
            long conversationId,
            int turnId,
            String nodeName,
            String aggrKey,
            String traceId,
            String content,
            int contentTokens) {
        ConversationNodeOutputEntity entity = new ConversationNodeOutputEntity();
        entity.setConversationId(conversationId);
        entity.setTurnId(turnId);
        entity.setNodeName(nodeName);
        entity.setAggrKey(aggrKey);
        entity.setTraceId(traceId);
        entity.setType("TOOL_RESULT_ARCHIVE");
        entity.setContent(content);
        entity.setContentTokens(contentTokens);
        mapper.insert(entity);
    }

    public Optional<ConversationNodeOutputEntity> findOwned(
            long userId,
            long conversationId,
            int turnId,
            String aggrKey) {
        return Optional.ofNullable(mapper.selectOwned(userId, conversationId, turnId, aggrKey));
    }
}
