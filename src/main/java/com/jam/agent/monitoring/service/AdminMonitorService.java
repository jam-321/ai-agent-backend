package com.jam.agent.monitoring.service;

import com.jam.agent.agent.service.AgentRunService;
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

    public AdminMonitorService(AdminMonitorRepository repository) {
        this.repository = repository;
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
        return new AdminConversationDetailResponse(
                conversation,
                repository.turns(conversationId),
                repository.nodes(conversationId),
                buildTree(repository.turns(conversationId), repository.nodes(conversationId)));
    }

    public List<AdminToolStatisticsResponse> tools() {
        return repository.toolStatistics();
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
