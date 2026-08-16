package com.jam.agent.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.config.AgentProperties;
import com.jam.agent.agent.persistence.repository.ConversationNodeRepository;
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
import org.springframework.stereotype.Component;

/** Rebuilds the model message history from durable turn and node records. */
@Component
public class ConversationContextManager {

    private final ConversationTurnRepository turns;
    private final ConversationNodeRepository nodes;
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;

    public ConversationContextManager(
            ConversationTurnRepository turns,
            ConversationNodeRepository nodes,
            AgentProperties properties,
            ObjectMapper objectMapper) {
        this.turns = turns;
        this.nodes = nodes;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<Message> rebuild(long userId, long conversationId, int currentTurnId) {
        List<ConversationTurnRepository.TurnRecord> records = turns.findCompletedBefore(
                userId,
                conversationId,
                currentTurnId,
                properties.getMemory().getMaxHistoryTurns());

        Map<Integer, List<ConversationTurnRepository.TurnRecord>> turnsById = groupTurns(records);
        Map<Integer, List<ConversationNodeRepository.NodeRecord>> toolsByTurn = groupTools(
                nodes.findHistoryTools(userId, conversationId, currentTurnId));
        List<Integer> selectedTurnIds = selectTurnsWithinBudget(turnsById, toolsByTurn);

        List<Message> messages = new ArrayList<>();
        for (Integer turnId : selectedTurnIds) {
            appendCompletedTurn(messages, turnsById.get(turnId), toolsByTurn.getOrDefault(turnId, List.of()));
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
            Map<Integer, List<ConversationNodeRepository.NodeRecord>> toolsByTurn) {
        // Four characters per token is deliberately conservative and avoids a tokenizer dependency here.
        int characterBudget = properties.getMemory().getMaxHistoryTokens() * 4;
        List<Integer> candidates = turnsById.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .limit(properties.getMemory().getMaxHistoryTurns())
                .toList();

        List<Integer> selected = new ArrayList<>();
        int usedCharacters = 0;
        for (Integer turnId : candidates) {
            int turnCost = contentLength(turnsById.get(turnId))
                    + contentLength(toolsByTurn.getOrDefault(turnId, List.of()));
            if (!selected.isEmpty() && usedCharacters + turnCost > characterBudget) {
                break;
            }
            selected.add(turnId);
            usedCharacters += turnCost;
        }

        return selected.stream().sorted().toList();
    }

    private int contentLength(List<?> records) {
        return records.stream().mapToInt(record -> {
            String content;
            if (record instanceof ConversationTurnRepository.TurnRecord turn) {
                content = turn.content();
            } else {
                content = ((ConversationNodeRepository.NodeRecord) record).content();
            }
            return content == null ? 0 : content.length();
        }).sum();
    }

    private void appendCompletedTurn(
            List<Message> messages,
            List<ConversationTurnRepository.TurnRecord> turnRecords,
            List<ConversationNodeRepository.NodeRecord> toolRecords) {
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

        messages.add(new UserMessage(user.content()));
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
