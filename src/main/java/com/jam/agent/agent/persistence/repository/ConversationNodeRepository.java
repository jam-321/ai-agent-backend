package com.jam.agent.agent.persistence.repository;

import com.jam.agent.agent.persistence.entity.ConversationNodeEntity;
import com.jam.agent.agent.persistence.mapper.ConversationNodeMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationNodeRepository {

    private final ConversationNodeMapper mapper;

    public ConversationNodeRepository(ConversationNodeMapper mapper) {
        this.mapper = mapper;
    }

    public long insert(
            long conversationId,
            int turnId,
            String traceId,
            int attemptNo,
            Integer roundNo,
            Integer callIndex,
            String nodeId,
            String nodeName,
            String aggrKey,
            String type,
            String status,
            String content) {
        ConversationNodeEntity node = new ConversationNodeEntity();
        node.setConversationId(conversationId);
        node.setTurnId(turnId);
        node.setTraceId(traceId);
        node.setAttemptNo(attemptNo);
        node.setRoundNo(roundNo);
        node.setCallIndex(callIndex);
        node.setNodeId(nodeId);
        node.setNodeName(nodeName);
        node.setAggrKey(aggrKey);
        node.setType(type);
        node.setStatus(status);
        node.setContent(content);
        mapper.insert(node);

        // MyBatis-Plus writes the auto-increment key back to the entity for this exact INSERT.
        if (node.getId() == null) {
            throw new IllegalStateException("写入执行节点失败。");
        }
        return node.getId();
    }

    public List<NodeRecord> findByTurn(long userId, long conversationId, int turnId) {
        return toRecords(mapper.selectByTurn(userId, conversationId, turnId));
    }

    public List<NodeRecord> findByTurnUnscoped(long conversationId, int turnId) {
        return toRecords(mapper.selectByTurnUnscoped(conversationId, turnId));
    }

    public List<NodeRecord> findHistoryTools(long userId, long conversationId, int currentTurnId) {
        return toRecords(mapper.selectHistoryTools(userId, conversationId, currentTurnId));
    }

    public List<NodeRecord> findToolNodes(
            long userId,
            long conversationId,
            int targetTurnId,
            String aggrKey) {
        return toRecords(mapper.selectToolNodes(userId, conversationId, targetTurnId, aggrKey));
    }

    private List<NodeRecord> toRecords(List<ConversationNodeEntity> nodes) {
        return nodes.stream().map(this::toRecord).toList();
    }

    private NodeRecord toRecord(ConversationNodeEntity node) {
        return new NodeRecord(
                node.getId(),
                node.getConversationId(),
                node.getTurnId(),
                node.getTraceId(),
                node.getAttemptNo(),
                node.getRoundNo(),
                node.getCallIndex(),
                node.getNodeId(),
                node.getNodeName(),
                node.getAggrKey(),
                node.getType(),
                node.getStatus(),
                node.getContent(),
                node.getCreatedAt(),
                node.getUpdatedAt());
    }

    public record NodeRecord(
            long id,
            long conversationId,
            int turnId,
            String traceId,
            int attemptNo,
            Integer roundNo,
            Integer callIndex,
            String nodeId,
            String nodeName,
            String aggrKey,
            String type,
            String status,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
