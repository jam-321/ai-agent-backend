package com.jam.agent.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationRepository {
    private final JdbcTemplate jdbc;
    public ConversationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long insert(long userId, String title) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO conversation (user_id, title) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId); statement.setString(2, title); return statement;
        }, keys);
        if (keys.getKey() == null) throw new IllegalStateException("创建会话失败。");
        return keys.getKey().longValue();
    }

    public Optional<ConversationRecord> findForUser(long userId, long conversationId) {
        return jdbc.query("SELECT id, user_id, title, source, created_at, updated_at FROM conversation "
                        + "WHERE id = ? AND user_id = ? AND is_deleted = 0",
                (rs, n) -> new ConversationRecord(rs.getLong("id"), rs.getLong("user_id"), rs.getString("title"),
                        rs.getString("source"), rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime()),
                conversationId, userId).stream().findFirst();
    }

    public List<ConversationRecord> listForUser(long userId) {
        return jdbc.query("SELECT id, user_id, title, source, created_at, updated_at FROM conversation "
                        + "WHERE user_id = ? AND is_deleted = 0 ORDER BY updated_at DESC, id DESC",
                (rs, n) -> new ConversationRecord(rs.getLong("id"), rs.getLong("user_id"), rs.getString("title"),
                        rs.getString("source"), rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime()), userId);
    }

    public void updateTitleIfEmpty(long userId, long conversationId, String title) {
        jdbc.update("UPDATE conversation SET title = ? WHERE id = ? AND user_id = ? AND is_deleted = 0 "
                + "AND (title IS NULL OR title = '')", title, conversationId, userId);
    }

    public void touch(long userId, long conversationId) {
        jdbc.update("UPDATE conversation SET updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ? AND is_deleted = 0", conversationId, userId);
    }

    public void lockForUpdate(long userId, long conversationId) {
        List<Long> ids = jdbc.query("SELECT id FROM conversation WHERE id=? AND user_id=? AND is_deleted=0 FOR UPDATE", (rs,n)->rs.getLong(1), conversationId, userId);
        if (ids.isEmpty()) throw new IllegalArgumentException("会话不存在。");
    }

    public void softDelete(long userId, long conversationId) {
        jdbc.update("UPDATE conversation SET is_deleted = 1 WHERE id = ? AND user_id = ? AND is_deleted = 0", conversationId, userId);
    }

    public record ConversationRecord(long id, long userId, String title, String source,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {}
}
