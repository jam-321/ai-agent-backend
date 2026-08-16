package com.jam.agent.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationTurnRepository {
    private final JdbcTemplate jdbc;
    public ConversationTurnRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public int nextTurnId(long userId, long conversationId) {
        Integer value = jdbc.queryForObject("SELECT COALESCE(MAX(t.turn_id), 0) FROM conversation_turn t "
                + "JOIN conversation c ON c.id = t.conversation_id WHERE c.id = ? AND c.user_id = ? AND c.is_deleted = 0",
                Integer.class, conversationId, userId);
        return value == null ? 1 : value + 1;
    }

    public void insert(long userId, long conversationId, int turnId, String type, String content,
                       String traceId, String errorMessage) {
        int count = jdbc.update("INSERT INTO conversation_turn (conversation_id, turn_id, type, content, error_message, trace_id) "
                + "SELECT c.id, ?, ?, ?, ?, ? FROM conversation c WHERE c.id = ? AND c.user_id = ? AND c.is_deleted = 0",
                turnId, type, content, errorMessage, traceId, conversationId, userId);
        if (count != 1) throw new IllegalArgumentException("会话不存在。");
    }

    public Optional<TurnRecord> findForUser(long userId, long conversationId, int turnId, String type) {
        return jdbc.query("SELECT t.id, t.conversation_id, t.turn_id, t.type, t.content, t.is_hidden, t.error_message, "
                        + "t.trace_id, t.created_at, t.updated_at FROM conversation_turn t JOIN conversation c ON c.id = t.conversation_id "
                        + "WHERE t.conversation_id = ? AND t.turn_id = ? AND t.type = ? AND c.user_id = ? AND c.is_deleted = 0",
                (rs, n) -> map(rs), conversationId, turnId, type, userId).stream().findFirst();
    }

    public List<TurnRecord> findCompletedBefore(long userId, long conversationId, int currentTurnId, int limit) {
        return jdbc.query("SELECT t.id, t.conversation_id, t.turn_id, t.type, t.content, t.is_hidden, t.error_message, "
                        + "t.trace_id, t.created_at, t.updated_at FROM conversation_turn t JOIN conversation c ON c.id = t.conversation_id "
                        + "WHERE t.conversation_id = ? AND c.user_id = ? AND c.is_deleted = 0 AND t.turn_id < ? "
                        + "AND t.is_hidden = 0 AND t.type IN ('user', 'assistant') "
                        + "AND EXISTS (SELECT 1 FROM conversation_turn a WHERE a.conversation_id=t.conversation_id AND a.turn_id=t.turn_id AND a.type='assistant') "
                        + "ORDER BY t.turn_id DESC, t.id DESC LIMIT ?", (rs, n) -> map(rs), conversationId, userId, currentTurnId, limit * 2);
    }

    public List<TurnRecord> findIncompleteTurns() {
        return jdbc.query("SELECT u.id, u.conversation_id, u.turn_id, u.type, u.content, u.is_hidden, u.error_message, "
                        + "u.trace_id, u.created_at, u.updated_at FROM conversation_turn u "
                        + "WHERE u.type='user' AND NOT EXISTS (SELECT 1 FROM conversation_turn a WHERE a.conversation_id=u.conversation_id AND a.turn_id=u.turn_id AND a.type='assistant')",
                (rs, n) -> map(rs));
    }

    public List<IncompleteTurnRecord> findIncompleteTurnContexts() {
        return jdbc.query("SELECT c.user_id,u.conversation_id,u.turn_id,u.content,u.trace_id FROM conversation_turn u "
                        + "JOIN conversation c ON c.id=u.conversation_id WHERE u.type='user' AND c.is_deleted=0 "
                        + "AND NOT EXISTS (SELECT 1 FROM conversation_turn a WHERE a.conversation_id=u.conversation_id AND a.turn_id=u.turn_id AND a.type='assistant')",
                (rs,n)->new IncompleteTurnRecord(rs.getLong("user_id"),rs.getLong("conversation_id"),rs.getInt("turn_id"),rs.getString("content"),rs.getString("trace_id")));
    }

    public List<TurnRecord> findTurnsForUser(long userId, long conversationId) {
        return jdbc.query("SELECT t.id, t.conversation_id, t.turn_id, t.type, t.content, t.is_hidden, t.error_message, "
                        + "t.trace_id, t.created_at, t.updated_at FROM conversation_turn t JOIN conversation c ON c.id=t.conversation_id "
                        + "WHERE t.conversation_id=? AND c.user_id=? AND c.is_deleted=0 AND t.type IN ('user','assistant') ORDER BY t.turn_id, t.id",
                (rs,n)->map(rs), conversationId, userId);
    }

    private TurnRecord map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new TurnRecord(rs.getLong("id"), rs.getLong("conversation_id"), rs.getInt("turn_id"), rs.getString("type"),
                rs.getString("content"), rs.getBoolean("is_hidden"), rs.getString("error_message"), rs.getString("trace_id"),
                rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());
    }

    public record TurnRecord(long id, long conversationId, int turnId, String type, String content, boolean hidden,
                             String errorMessage, String traceId, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record IncompleteTurnRecord(long userId,long conversationId,int turnId,String content,String traceId) {}
}
