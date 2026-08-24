package com.jam.agent.agent.service;

import com.jam.agent.agent.config.AgentProperties;
import com.jam.agent.agent.dto.ProgressNodeResponse;
import com.jam.agent.agent.dto.ProgressResponse;
import com.jam.agent.agent.persistence.repository.ConversationNodeRepository;
import com.jam.agent.conversation.persistence.repository.ConversationRepository;
import com.jam.agent.conversation.persistence.repository.ConversationTurnRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ProgressQueryService {

    private final ConversationRepository conversations;
    private final ConversationTurnRepository turns;
    private final ConversationNodeRepository nodes;
    private final AgentProperties properties;

    public ProgressQueryService(
            ConversationRepository conversations,
            ConversationTurnRepository turns,
            ConversationNodeRepository nodes,
            AgentProperties properties) {
        this.conversations = conversations;
        this.turns = turns;
        this.nodes = nodes;
        this.properties = properties;
    }

    public ProgressResponse get(long userId, long conversationId, int turnId) {
        if (conversations.findForUser(userId, conversationId).isEmpty()) {
            throw new AgentRunService.NotFoundException();
        }

        ConversationTurnRepository.TurnRecord userTurn = turns
                .findForUser(userId, conversationId, turnId, "user")
                .orElseThrow(AgentRunService.NotFoundException::new);
        ConversationTurnRepository.TurnRecord assistantTurn = turns
                .findForUser(userId, conversationId, turnId, "assistant")
                .orElse(null);
        List<ConversationNodeRepository.NodeRecord> records = nodes.findByTurn(
                userId,
                conversationId,
                turnId);

        boolean complete = hasGenerateStatus(records, "COMPLETE");
        boolean error = hasGenerateStatus(records, "ERROR")
                || (assistantTurn != null && assistantTurn.errorMessage() != null);
        String status = complete ? "COMPLETE" : error ? "ERROR" : "REASONING";
        String answer = complete && assistantTurn != null ? assistantTurn.content() : null;
        String errorMessage = error && assistantTurn != null ? assistantTurn.errorMessage() : null;

        return new ProgressResponse(
                conversationId,
                turnId,
                userTurn.traceId(),
                status,
                answer,
                errorMessage,
                aggregate(records));
    }

    private boolean hasGenerateStatus(
            List<ConversationNodeRepository.NodeRecord> records,
            String status) {
        return records.stream().anyMatch(node -> node.type().equals("GENERATE") && node.status().equals(status));
    }

    private List<ProgressNodeResponse> aggregate(List<ConversationNodeRepository.NodeRecord> records) {
        Map<String, List<ConversationNodeRepository.NodeRecord>> grouped = new LinkedHashMap<>();
        for (ConversationNodeRepository.NodeRecord node : records) {
            // aggr_key groups START and terminal rows for one tool call into one UI card.
            String key = node.aggrKey() == null ? "#single-" + node.id() : node.aggrKey();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(node);
        }

        return grouped.values().stream()
                .sorted(Comparator.comparingLong(group -> group.get(0).id()))
                .map(this::toResponse)
                .toList();
    }

    private ProgressNodeResponse toResponse(List<ConversationNodeRepository.NodeRecord> group) {
        ConversationNodeRepository.NodeRecord first = group.get(0);
        if (first.aggrKey() == null) {
            return node(first, "single", null, List.of());
        }

        ConversationNodeRepository.NodeRecord terminal = group.stream()
                .filter(node -> node.status().equals("ERROR") || node.status().equals("SUCCESS"))
                .max(Comparator.comparingLong(ConversationNodeRepository.NodeRecord::id))
                .orElse(null);
        String status = group.stream().anyMatch(node -> node.status().equals("ERROR"))
                ? "ERROR"
                : terminal == null ? "START" : "SUCCESS";

        return new ProgressNodeResponse(
                "multiple",
                first.id(),
                first.aggrKey(),
                first.nodeId(),
                first.nodeName(),
                first.type(),
                status,
                null,
                false,
                first.createdAt(),
                terminal == null ? first.updatedAt() : terminal.updatedAt(),
                group.stream().map(value -> node(value, "single", null, List.of())).toList());
    }

    private ProgressNodeResponse node(
            ConversationNodeRepository.NodeRecord node,
            String structure,
            String aggrKey,
            List<ProgressNodeResponse> children) {
        int limit = node.status().equals("START")
                ? properties.getProgress().getMaxToolArgsPreviewChars()
                : properties.getProgress().getMaxToolResultPreviewChars();
        String content = node.content();
        boolean truncated = content != null && content.length() > limit;
        if (truncated) {
            content = content.substring(0, limit);
        }

        return new ProgressNodeResponse(
                structure,
                node.id(),
                aggrKey,
                node.nodeId(),
                node.nodeName(),
                node.type(),
                node.status(),
                content,
                truncated,
                node.createdAt(),
                node.updatedAt(),
                children);
    }
}
