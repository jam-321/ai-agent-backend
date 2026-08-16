package com.jam.agent.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationNodeRepository {
    private final JdbcTemplate jdbc;
    public ConversationNodeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long insert(long conversationId, int turnId, String traceId, int attemptNo, Integer roundNo, Integer callIndex,
                       String nodeId, String nodeName, String aggrKey, String type, String status, String content) {
        jdbc.update("INSERT INTO conversation_node (conversation_id, turn_id, trace_id, attempt_no, round_no, call_index, "
                        + "node_id, node_name, aggr_key, type, status, content) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                conversationId, turnId, traceId, attemptNo, roundNo, callIndex, nodeId, nodeName, aggrKey, type, status, content);
        return jdbc.queryForObject("SELECT id FROM conversation_node WHERE conversation_id=? AND turn_id=? ORDER BY id DESC LIMIT 1",
                Long.class, conversationId, turnId);
    }

    public List<NodeRecord> findByTurn(long userId, long conversationId, int turnId) {
        return jdbc.query("SELECT n.id,n.conversation_id,n.turn_id,n.trace_id,n.attempt_no,n.round_no,n.call_index,n.node_id,n.node_name,n.aggr_key,n.type,n.status,n.content,n.created_at,n.updated_at "
                        + "FROM conversation_node n JOIN conversation c ON c.id=n.conversation_id WHERE n.conversation_id=? AND n.turn_id=? AND c.user_id=? AND c.is_deleted=0 ORDER BY n.id",
                (rs,n)->map(rs), conversationId, turnId, userId);
    }

    public List<NodeRecord> findByTurnUnscoped(long conversationId, int turnId) {
        return jdbc.query("SELECT id,conversation_id,turn_id,trace_id,attempt_no,round_no,call_index,node_id,node_name,aggr_key,type,status,content,created_at,updated_at FROM conversation_node WHERE conversation_id=? AND turn_id=? ORDER BY id",
                (rs,n)->map(rs), conversationId, turnId);
    }

    public List<NodeRecord> findHistoryTools(long userId, long conversationId, int currentTurnId) {
        return jdbc.query("SELECT n.id,n.conversation_id,n.turn_id,n.trace_id,n.attempt_no,n.round_no,n.call_index,n.node_id,n.node_name,n.aggr_key,n.type,n.status,n.content,n.created_at,n.updated_at "
                        + "FROM conversation_node n JOIN conversation c ON c.id=n.conversation_id WHERE n.conversation_id=? AND n.turn_id<? AND c.user_id=? AND c.is_deleted=0 AND n.type='TOOL_CALL' ORDER BY n.turn_id,n.round_no,n.call_index,n.id",
                (rs,n)->map(rs), conversationId, currentTurnId, userId);
    }

    public List<NodeRecord> findToolNodes(long userId, long conversationId, int targetTurnId, String aggrKey) {
        return jdbc.query("SELECT n.id,n.conversation_id,n.turn_id,n.trace_id,n.attempt_no,n.round_no,n.call_index,n.node_id,n.node_name,n.aggr_key,n.type,n.status,n.content,n.created_at,n.updated_at "
                        + "FROM conversation_node n JOIN conversation c ON c.id=n.conversation_id WHERE n.conversation_id=? AND n.turn_id=? AND n.aggr_key=? AND c.user_id=? AND c.is_deleted=0 ORDER BY n.id",
                (rs,n)->map(rs), conversationId, targetTurnId, aggrKey, userId);
    }

    private NodeRecord map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new NodeRecord(rs.getLong("id"),rs.getLong("conversation_id"),rs.getInt("turn_id"),rs.getString("trace_id"),rs.getInt("attempt_no"),
                (Integer) rs.getObject("round_no"),(Integer) rs.getObject("call_index"),rs.getString("node_id"),rs.getString("node_name"),rs.getString("aggr_key"),
                rs.getString("type"),rs.getString("status"),rs.getString("content"),rs.getTimestamp("created_at").toLocalDateTime(),rs.getTimestamp("updated_at").toLocalDateTime());
    }

    public record NodeRecord(long id,long conversationId,int turnId,String traceId,int attemptNo,Integer roundNo,Integer callIndex,
                             String nodeId,String nodeName,String aggrKey,String type,String status,String content,
                             LocalDateTime createdAt,LocalDateTime updatedAt) {}
}
