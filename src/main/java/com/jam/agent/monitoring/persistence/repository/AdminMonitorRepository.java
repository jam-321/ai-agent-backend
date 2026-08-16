package com.jam.agent.monitoring.persistence.repository;

import com.jam.agent.monitoring.dto.AdminConversationSummaryResponse;
import com.jam.agent.monitoring.dto.AdminNodeResponse;
import com.jam.agent.monitoring.dto.AdminOverviewResponse;
import com.jam.agent.monitoring.dto.AdminToolStatisticsResponse;
import com.jam.agent.monitoring.dto.AdminTurnResponse;
import com.jam.agent.monitoring.persistence.mapper.AdminMonitorMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Converts custom monitoring query rows into the API's immutable response records. */
@Repository
public class AdminMonitorRepository {

    private final AdminMonitorMapper mapper;

    public AdminMonitorRepository(AdminMonitorMapper mapper) {
        this.mapper = mapper;
    }

    public AdminOverviewResponse overview() {
        Map<String, Object> row = mapper.selectOverview();
        return new AdminOverviewResponse(
                asLong(row, "user_count"),
                asLong(row, "enabled_user_count"),
                asLong(row, "conversation_count"),
                asLong(row, "turn_count"),
                asLong(row, "node_count"),
                asLong(row, "completed_run_count"),
                asLong(row, "failed_run_count"),
                asLong(row, "reasoning_run_count"),
                asLong(row, "tool_call_count"),
                asLong(row, "tool_success_count"),
                asLong(row, "tool_error_count"));
    }

    public long countConversations(String search) {
        return mapper.countConversations(normalizeSearch(search));
    }

    public List<AdminConversationSummaryResponse> conversations(
            String search,
            int offset,
            int size) {
        return mapper.selectConversations(normalizeSearch(search), offset, size).stream()
                .map(this::toConversationSummary)
                .toList();
    }

    public Optional<AdminConversationSummaryResponse> conversation(long conversationId) {
        return Optional.ofNullable(mapper.selectConversation(conversationId))
                .map(this::toConversationSummary);
    }

    public List<AdminTurnResponse> turns(long conversationId) {
        return mapper.selectTurns(conversationId).stream()
                .map(row -> new AdminTurnResponse(
                        asLong(row, "id"),
                        asInt(row, "turn_id"),
                        asString(row, "type"),
                        asString(row, "content"),
                        asBoolean(row, "is_hidden"),
                        asString(row, "error_message"),
                        asString(row, "trace_id"),
                        asLocalDateTime(row, "created_at"),
                        asLocalDateTime(row, "updated_at")))
                .toList();
    }

    public List<AdminNodeResponse> nodes(long conversationId) {
        return mapper.selectNodes(conversationId).stream()
                .map(row -> new AdminNodeResponse(
                        asLong(row, "id"),
                        asInt(row, "turn_id"),
                        asString(row, "trace_id"),
                        asInt(row, "attempt_no"),
                        asNullableInt(row, "round_no"),
                        asNullableInt(row, "call_index"),
                        asString(row, "node_id"),
                        asString(row, "node_name"),
                        asString(row, "aggr_key"),
                        asString(row, "type"),
                        asString(row, "status"),
                        asString(row, "content"),
                        asLocalDateTime(row, "created_at"),
                        asLocalDateTime(row, "updated_at")))
                .toList();
    }

    public List<AdminToolStatisticsResponse> toolStatistics() {
        return mapper.selectToolStatistics().stream()
                .map(row -> new AdminToolStatisticsResponse(
                        asString(row, "tool_name"),
                        asLong(row, "call_count"),
                        asLong(row, "success_count"),
                        asLong(row, "error_count"),
                        asLong(row, "running_count"),
                        asNullableLong(row, "average_duration_ms")))
                .toList();
    }

    private AdminConversationSummaryResponse toConversationSummary(Map<String, Object> row) {
        return new AdminConversationSummaryResponse(
                asLong(row, "id"),
                asLong(row, "user_id"),
                asString(row, "username"),
                asString(row, "title"),
                asLong(row, "turn_count"),
                asLong(row, "node_count"),
                asString(row, "latest_status"),
                asLocalDateTime(row, "created_at"),
                asLocalDateTime(row, "updated_at"));
    }

    private String normalizeSearch(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }

    private long asLong(Map<String, Object> row, String key) {
        Number value = (Number) value(row, key);
        return value == null ? 0 : value.longValue();
    }

    private Long asNullableLong(Map<String, Object> row, String key) {
        Number value = (Number) value(row, key);
        return value == null ? null : value.longValue();
    }

    private int asInt(Map<String, Object> row, String key) {
        Number value = (Number) value(row, key);
        return value == null ? 0 : value.intValue();
    }

    private Integer asNullableInt(Map<String, Object> row, String key) {
        Number value = (Number) value(row, key);
        return value == null ? null : value.intValue();
    }

    private boolean asBoolean(Map<String, Object> row, String key) {
        Object value = value(row, key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value instanceof Number number && number.intValue() != 0;
    }

    private String asString(Map<String, Object> row, String key) {
        Object value = value(row, key);
        return value == null ? null : value.toString();
    }

    private LocalDateTime asLocalDateTime(Map<String, Object> row, String key) {
        Object value = value(row, key);
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    private Object value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value != null || row.containsKey(key)) {
            return value;
        }

        String uppercase = key.toUpperCase();
        value = row.get(uppercase);
        if (value != null || row.containsKey(uppercase)) {
            return value;
        }

        return row.get(toCamelCase(key));
    }

    private String toCamelCase(String value) {
        StringBuilder result = new StringBuilder();
        boolean uppercaseNext = false;
        for (char character : value.toCharArray()) {
            if (character == '_') {
                uppercaseNext = true;
            } else if (uppercaseNext) {
                result.append(Character.toUpperCase(character));
                uppercaseNext = false;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }
}
