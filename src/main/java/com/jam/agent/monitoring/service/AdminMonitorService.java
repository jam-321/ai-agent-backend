package com.jam.agent.monitoring.service;

import com.jam.agent.agent.service.AgentRunService;
import com.jam.agent.agent.persistence.repository.ConversationTurnAttachmentRepository;
import com.jam.agent.monitoring.dto.AdminConversationDetailResponse;
import com.jam.agent.monitoring.dto.AdminConversationPageResponse;
import com.jam.agent.monitoring.dto.AdminConversationSummaryResponse;
import com.jam.agent.monitoring.dto.AdminOverviewResponse;
import com.jam.agent.monitoring.dto.AdminToolStatisticsResponse;
import com.jam.agent.monitoring.dto.AdminNodeResponse;
import com.jam.agent.monitoring.dto.AdminToolCallResponse;
import com.jam.agent.monitoring.dto.AdminTurnResponse;
import com.jam.agent.monitoring.dto.AdminTurnTreeResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import com.jam.agent.monitoring.persistence.repository.AdminMonitorRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminMonitorService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminMonitorRepository repository;
    private final ConversationTurnAttachmentRepository attachments;

    public AdminMonitorService(
            AdminMonitorRepository repository,
            ConversationTurnAttachmentRepository attachments) {
        this.repository = repository;
        this.attachments = attachments;
    }

    public AdminOverviewResponse overview() {
        return repository.overview();
    }

    public AdminConversationPageResponse conversations(
            int requestedPage,
            int requestedSize,
            String search) {
        int page = Math.max(requestedPage, 1);
        int size = Math.min(Math.max(requestedSize, 1), MAX_PAGE_SIZE);
        int offset = (page - 1) * size;

        return new AdminConversationPageResponse(
                repository.conversations(search, offset, size),
                page,
                size,
                repository.countConversations(search));
    }

    public AdminConversationDetailResponse conversation(long conversationId) {
        AdminConversationSummaryResponse conversation = repository.conversation(conversationId)
                .orElseThrow(AgentRunService.NotFoundException::new);
        List<AdminTurnResponse> turns = repository.turns(conversationId).stream()
                .map(turn -> turn.withAttachmentIds(
                        attachments.findAssetIds(conversation.userId(), conversationId, turn.turnId())))
                .toList();
        List<AdminNodeResponse> nodes = repository.nodes(conversationId);
        return new AdminConversationDetailResponse(
                conversation,
                turns,
                nodes,
                buildTree(turns, nodes));
    }

    /**
     * 查询管理员 Agent 使用的会话片段。
     *
     * <p>会话 ID 查询整段会话；追加 Turn ID 或 trace ID 后只返回对应执行范围。
     */
    public AdminConversationDetailResponse detail(
            Long conversationId,
            Integer turnId,
            String traceId) {
        String normalizedTraceId = traceId == null || traceId.isBlank()
                ? null
                : traceId.trim();
        if (conversationId == null && normalizedTraceId == null) {
            throw new IllegalArgumentException("conversationId、turnId 或 traceId 至少提供一种定位条件。");
        }
        if (turnId != null && conversationId == null) {
            throw new IllegalArgumentException("查询 turnId 时必须同时提供 conversationId。");
        }

        long resolvedConversationId = conversationId == null
                ? repository.conversationIdByTrace(normalizedTraceId)
                        .orElseThrow(AgentRunService.NotFoundException::new)
                : conversationId;
        AdminConversationDetailResponse full = conversation(resolvedConversationId);

        java.util.Set<Integer> selectedTurnIds = new java.util.LinkedHashSet<>();
        if (turnId != null) {
            selectedTurnIds.add(turnId);
        } else if (normalizedTraceId != null) {
            full.turns().stream()
                    .filter(turn -> normalizedTraceId.equals(turn.traceId()))
                    .map(AdminTurnResponse::turnId)
                    .forEach(selectedTurnIds::add);
            full.nodes().stream()
                    .filter(node -> normalizedTraceId.equals(node.traceId()))
                    .map(AdminNodeResponse::turnId)
                    .forEach(selectedTurnIds::add);
        }

        if (!selectedTurnIds.isEmpty()) {
            boolean found = full.turns().stream().anyMatch(turn -> selectedTurnIds.contains(turn.turnId())
                    && (normalizedTraceId == null || normalizedTraceId.equals(turn.traceId())))
                    || full.nodes().stream().anyMatch(node -> selectedTurnIds.contains(node.turnId())
                    && (normalizedTraceId == null || normalizedTraceId.equals(node.traceId())));
            if (!found) {
                throw new AgentRunService.NotFoundException();
            }
            return filterTurns(full, selectedTurnIds, normalizedTraceId);
        }
        if (normalizedTraceId != null) {
            throw new AgentRunService.NotFoundException();
        }
        return full;
    }

    public List<AdminToolStatisticsResponse> tools() {
        return repository.toolStatistics();
    }

    private AdminConversationDetailResponse filterTurns(
            AdminConversationDetailResponse full,
            java.util.Set<Integer> turnIds,
            String traceId) {
        List<AdminTurnResponse> turns = full.turns().stream()
                .filter(turn -> turnIds.contains(turn.turnId())
                        && (traceId == null || traceId.equals(turn.traceId())))
                .toList();
        List<AdminNodeResponse> nodes = full.nodes().stream()
                .filter(node -> turnIds.contains(node.turnId())
                        && (traceId == null || traceId.equals(node.traceId())))
                .toList();
        return new AdminConversationDetailResponse(
                full.conversation(),
                turns,
                nodes,
                buildTree(turns, nodes));
    }

    private List<AdminTurnTreeResponse> buildTree(
            List<AdminTurnResponse> turns,
            List<AdminNodeResponse> nodes) {
        Map<Integer, List<AdminTurnResponse>> turnsById = new LinkedHashMap<>();
        for (AdminTurnResponse turn : turns) {
            turnsById.computeIfAbsent(turn.turnId(), ignored -> new java.util.ArrayList<>()).add(turn);
        }
        Map<Integer, List<AdminNodeResponse>> nodesByTurn = new LinkedHashMap<>();
        for (AdminNodeResponse node : nodes) {
            if ("TOOL_CALL".equals(node.type())) {
                nodesByTurn.computeIfAbsent(node.turnId(), ignored -> new java.util.ArrayList<>()).add(node);
            }
        }
        return turnsById.entrySet().stream().map(entry -> {
            AdminTurnResponse user = entry.getValue().stream()
                    .filter(turn -> "user".equals(turn.type())).findFirst().orElse(null);
            AdminTurnResponse assistant = entry.getValue().stream()
                    .filter(turn -> "assistant".equals(turn.type())).findFirst().orElse(null);
            Map<String, List<AdminNodeResponse>> grouped = new LinkedHashMap<>();
            for (AdminNodeResponse node : nodesByTurn.getOrDefault(entry.getKey(), List.of())) {
                grouped.computeIfAbsent(
                        node.aggrKey() == null ? String.valueOf(node.id()) : node.aggrKey(),
                        ignored -> new java.util.ArrayList<>()).add(node);
            }
            List<AdminToolCallResponse> calls = grouped.entrySet().stream().map(group -> {
                List<AdminNodeResponse> events = group.getValue();
                AdminNodeResponse start = events.stream()
                        .filter(node -> "START".equals(node.status())).findFirst().orElse(events.get(0));
                AdminNodeResponse end = events.stream()
                        .filter(node -> "SUCCESS".equals(node.status()) || "ERROR".equals(node.status()))
                        .reduce((first, second) -> second).orElse(null);
                return new AdminToolCallResponse(
                        group.getKey(), start.nodeName(), end == null ? start.status() : end.status(),
                        start.content(), end == null ? null : end.content(), events);
            }).toList();
            return new AdminTurnTreeResponse(entry.getKey(), user, assistant, calls);
        }).toList();
    }
}
