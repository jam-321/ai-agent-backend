package com.jam.agent.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserRecord> findByUsername(String username) {
        return jdbcTemplate.query(
                        "SELECT id, username, password_hash, status FROM app_user WHERE username = ?",
                        (resultSet, rowNum) -> new UserRecord(
                                resultSet.getLong("id"),
                                resultSet.getString("username"),
                                resultSet.getString("password_hash"),
                                resultSet.getBoolean("status")),
                        username)
                .stream()
                .findFirst();
    }

    public long insert(String username, String passwordHash) {
        jdbcTemplate.update(
                "INSERT INTO app_user (username, password_hash) VALUES (?, ?)",
                username,
                passwordHash);
        return jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, username);
    }

    public record UserRecord(Long id, String username, String passwordHash, boolean enabled) {
    }
}
