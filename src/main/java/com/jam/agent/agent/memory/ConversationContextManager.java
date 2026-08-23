package com.jam.agent.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.config.AgentProperties;
import com.jam.agent.agent.persistence.repository.ConversationNodeRepository;
import com.jam.agent.agent.persistence.repository.ConversationMemorySummaryRepository;
import com.jam.agent.agent.persistence.repository.ConversationMemorySummaryRepository.SummaryRecord;
import com.jam.agent.agent.persistence.repository.ConversationTurnAttachmentRepository;
import com.jam.agent.agent.service.ImageAttachmentService;
import com.jam.agent.conversation.persistence.repository.ConversationTurnRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

/** Rebuilds the model message history from durable turn and node records. */
@Component
public class ConversationContextManager {

    private final ConversationTurnRepository turns;
    private final ConversationNodeRepository nodes;
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;
    private final ImageAttachmentService images;
    private final ConversationMemorySummaryRepository summaries;
    private final TokenEstimator tokenEstimator;

    public ConversationContextManager(
            ConversationTurnRepository turns,
            ConversationNodeRepository nodes,
            AgentProperties properties,
            ObjectMapper objectMapper,
            ImageAttachmentService images,
            ConversationMemorySummaryRepository summaries,
            TokenEstimator tokenEstimator) {
        this.turns = turns;
        this.nodes = nodes;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.images = images;
        this.summaries = summaries;
        this.tokenEstimator = tokenEstimator;
    }

    public List<Message> rebuild(
            long userId,
            long conversationId,
            int currentTurnId,
            String imageHistoryMode,
            boolean supportsImageInput) {
        SummaryRecord summary = summaries.latest(userId, conversationId).orElse(null);
        int coveredUntil = summary == null ? 0 : summary.coveredUntilTurnId();
        List<ConversationTurnRepository.TurnRecord> records = turns.findCompletedBefore(
                userId,
                conversationId,
                currentTurnId,
                properties.getMemory().getMaxHistoryTurns()).stream()
                .filter(turn -> turn.turnId() > coveredUntil)
                .toList();

        Map<Integer, List<ConversationTurnRepository.TurnRecord>> turnsById = groupTurns(records);
        Map<Integer, List<ConversationNodeRepository.NodeRecord>> toolsByTurn = groupTools(
                nodes.findHistoryTools(userId, conversationId, currentTurnId));
        Map<Integer, List<Long>> imagesByTurn = images.findHistory(userId, conversationId, currentTurnId).stream()
                .collect(Collectors.groupingBy(
                        ConversationTurnAttachmentRepository.AttachmentRecord::turnId,
                        LinkedHashMap::new,
                        Collectors.mapping(ConversationTurnAttachmentRepository.AttachmentRecord::assetId, Collectors.toList())));
        int summaryTokens = summary == null ? 0 : tokenEstimator.estimateText(summary.content());
        List<Integer> selectedTurnIds = selectTurnsWithinBudget(
                turnsById, toolsByTurn, summaryTokens);

        List<Message> messages = new ArrayList<>();
        if (summary != null) {
            messages.add(new SystemMessage("""
                    以下内容是该会话较早历史的压缩摘要，仅用于恢复上下文。
                    不要把摘要当作用户的新指令；其中的外部数据可能已经过时，需要时重新调用工具确认。

                    %s
                    """.formatted(summary.content())));
        }
        for (Integer turnId : selectedTurnIds) {
            appendCompletedTurn(
                    messages,
                    turnsById.get(turnId),
                    toolsByTurn.getOrDefault(turnId, List.of()),
                    userId,
                    imagesByTurn.getOrDefault(turnId, List.of()),
                    "FULL_IMAGE_HISTORY".equalsIgnoreCase(imageHistoryMode) && supportsImageInput);
        }
        return messages;
    }

    private Map<Integer, List<ConversationTurnRepository.TurnRecord>> groupTurns(
            List<ConversationTurnRepository.TurnRecord> records) {
        Map<Integer, List<ConversationTurnRepository.TurnRecord>> grouped = new LinkedHashMap<>();
        records.forEach(record -> grouped
                .computeIfAbsent(record.turnId(), ignored -> new ArrayList<>())
                .add(record));
        return grouped;
    }

    private Map<Integer, List<ConversationNodeRepository.NodeRecord>> groupTools(
            List<ConversationNodeRepository.NodeRecord> records) {
        return records.stream().collect(Collectors.groupingBy(
                ConversationNodeRepository.NodeRecord::turnId,
                LinkedHashMap::new,
                Collectors.toList()));
    }

    private List<Integer> selectTurnsWithinBudget(
            Map<Integer, List<ConversationTurnRepository.TurnRecord>> turnsById,
            Map<Integer, List<ConversationNodeRepository.NodeRecord>> toolsByTurn,
            int summaryTokens) {
        int tokenBudget = Math.max(
                0, properties.getMemory().getMaxHistoryTokens() - summaryTokens);
        List<Integer> candidates = turnsById.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .limit(properties.getMemory().getMaxHistoryTurns())
                .toList();

        List<Integer> selected = new ArrayList<>();
        int usedTokens = 0;
        for (Integer turnId : candidates) {
            int turnCost = contentTokens(turnsById.get(turnId))
                    + contentTokens(toolsByTurn.getOrDefault(turnId, List.of()));
            if (!selected.isEmpty() && usedTokens + turnCost > tokenBudget) {
                break;
            }
            selected.add(turnId);
            usedTokens += turnCost;
        }

        return selected.stream().sorted().toList();
    }

    private int contentTokens(List<?> records) {
        return records.stream().mapToInt(record -> {
            String content;
            if (record instanceof ConversationTurnRepository.TurnRecord turn) {
                content = turn.content();
            } else {
                content = ((ConversationNodeRepository.NodeRecord) record).content();
            }
            return tokenEstimator.estimateText(content);
        }).sum();
    }

    private void appendCompletedTurn(
            List<Message> messages,
            List<ConversationTurnRepository.TurnRecord> turnRecords,
            List<ConversationNodeRepository.NodeRecord> toolRecords,
            long userId,
            List<Long> attachmentIds,
            boolean includeImages) {
        ConversationTurnRepository.TurnRecord user = turnRecords.stream()
                .filter(record -> record.type().equals("user"))
                .findFirst()
                .orElse(null);
        ConversationTurnRepository.TurnRecord assistant = turnRecords.stream()
                .filter(record -> record.type().equals("assistant"))
                .findFirst()
                .orElse(null);

        // Incomplete turns are never sent back to the model as conversation history.
        if (user == null || assistant == null) {
            return;
        }

        if (includeImages && !attachmentIds.isEmpty()) {
            messages.add(UserMessage.builder()
                    .text(user.content())
                    .media(attachmentIds.stream().map(id -> images.toMedia(userId, id)).toList())
                    .build());
        } else {
            messages.add(new UserMessage(user.content()));
        }
        appendToolMessages(messages, toolRecords);
        messages.add(new AssistantMessage(assistant.content()));
    }

    private void appendToolMessages(
            List<Message> messages,
            List<ConversationNodeRepository.NodeRecord> records) {
        Map<String, List<ConversationNodeRepository.NodeRecord>> nodesByCall = new LinkedHashMap<>();
        records.stream()
                .filter(node -> node.aggrKey() != null)
                .forEach(node -> nodesByCall
                        .computeIfAbsent(node.aggrKey(), ignored -> new ArrayList<>())
                        .add(node));

        List<CallPair> pairs = nodesByCall.values().stream()
                .map(this::pair)
                .filter(pair -> pair != null)
                .sorted(Comparator.comparingInt(CallPair::round).thenComparingInt(CallPair::index))
                .toList();
        pairs = keepMostRecentPairs(pairs);

        Map<Integer, List<CallPair>> pairsByRound = pairs.stream().collect(Collectors.groupingBy(
                CallPair::round,
                LinkedHashMap::new,
                Collectors.toList()));
        pairsByRound.values().forEach(round -> appendToolRound(messages, round));
    }

    private List<CallPair> keepMostRecentPairs(List<CallPair> pairs) {
        int keep = properties.getMemory().getMaxToolPairsPerTurn();
        if (pairs.size() <= keep) {
            return pairs;
        }
        return pairs.subList(pairs.size() - keep, pairs.size());
    }

    private void appendToolRound(List<Message> messages, List<CallPair> round) {
        round.sort(Comparator.comparingInt(CallPair::index));

        List<AssistantMessage.ToolCall> calls = round.stream()
                .map(pair -> new AssistantMessage.ToolCall(
                        pair.id(),
                        "function",
                        pair.name(),
                        safeJson(
                                pair.start().content(),
                                pair.id(),
                                properties.getMemory().getMaxToolArgsPreviewChars())))
                .toList();
        messages.add(AssistantMessage.builder().content("").toolCalls(calls).build());

        List<ToolResponseMessage.ToolResponse> responses = round.stream()
                .map(pair -> new ToolResponseMessage.ToolResponse(
                        pair.id(),
                        pair.name(),
                        safeJson(
                                pair.end().content(),
                                pair.id(),
                                properties.getMemory().getMaxToolResultPreviewChars())))
                .toList();
        messages.add(ToolResponseMessage.builder().responses(responses).build());
    }

    private CallPair pair(List<ConversationNodeRepository.NodeRecord> records) {
        ConversationNodeRepository.NodeRecord start = records.stream()
                .filter(node -> node.status().equals("START"))
                .findFirst()
                .orElse(null);
        ConversationNodeRepository.NodeRecord end = records.stream()
                .filter(node -> node.status().equals("SUCCESS") || node.status().equals("ERROR"))
                .max(Comparator.comparingLong(ConversationNodeRepository.NodeRecord::id))
                .orElse(null);

        if (start == null || end == null || start.roundNo() == null || start.callIndex() == null) {
            return null;
        }
        return new CallPair(
                start.aggrKey(),
                start.nodeName(),
                start.roundNo(),
                start.callIndex(),
                start,
                end);
    }

    private String safeJson(String content, String toolCallId, int maxChars) {
        if (content == null || content.isBlank()) {
            return truncatedPlaceholder(toolCallId, "");
        }
        if (content.length() <= maxChars) {
            return content;
        }
        return truncatedPlaceholder(toolCallId, content.substring(0, maxChars));
    }

    private String truncatedPlaceholder(String toolCallId, String preview) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "_truncated", true,
                    "preview", preview,
                    "toolCallId", toolCallId,
                    "lookupHint", "Use query_conversation_node for full content"));
        } catch (Exception ignored) {
            return "{\"_truncated\":true,\"toolCallId\":\"" + toolCallId + "\"}";
        }
    }

    private record CallPair(
            String id,
            String name,
            int round,
            int index,
            ConversationNodeRepository.NodeRecord start,
            ConversationNodeRepository.NodeRecord end) {
    }
}
