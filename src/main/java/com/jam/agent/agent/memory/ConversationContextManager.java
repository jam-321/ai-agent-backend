package com.jam.agent.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.config.AgentProperties;
import com.jam.agent.repository.ConversationNodeRepository;
import com.jam.agent.repository.ConversationTurnRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

@Component
public class ConversationContextManager {
    private final ConversationTurnRepository turns;
    private final ConversationNodeRepository nodes;
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;
    public ConversationContextManager(ConversationTurnRepository turns, ConversationNodeRepository nodes, AgentProperties properties, ObjectMapper objectMapper) {
        this.turns = turns; this.nodes = nodes; this.properties = properties; this.objectMapper = objectMapper;
    }

    public List<Message> rebuild(long userId, long conversationId, int currentTurnId) {
        List<ConversationTurnRepository.TurnRecord> records = turns.findCompletedBefore(userId, conversationId, currentTurnId, properties.getMemory().getMaxHistoryTurns());
        Map<Integer, List<ConversationTurnRepository.TurnRecord>> grouped = new LinkedHashMap<>();
        records.forEach(r -> grouped.computeIfAbsent(r.turnId(), ignored -> new ArrayList<>()).add(r));
        List<ConversationNodeRepository.NodeRecord> allTools = nodes.findHistoryTools(userId, conversationId, currentTurnId);
        Map<Integer, List<ConversationNodeRepository.NodeRecord>> toolsByTurn = allTools.stream().collect(java.util.stream.Collectors.groupingBy(
                ConversationNodeRepository.NodeRecord::turnId, LinkedHashMap::new, java.util.stream.Collectors.toList()));
        int budget = properties.getMemory().getMaxHistoryTokens() * 4;
        List<Integer> candidateIds = grouped.keySet().stream().sorted(Comparator.reverseOrder()).limit(properties.getMemory().getMaxHistoryTurns()).toList();
        List<Integer> selectedIds = new ArrayList<>();
        int used = 0;
        for (Integer id : candidateIds) {
            int cost = grouped.get(id).stream().mapToInt(r -> r.content() == null ? 0 : r.content().length()).sum()
                    + toolsByTurn.getOrDefault(id, List.of()).stream().mapToInt(r -> r.content() == null ? 0 : r.content().length()).sum();
            if (!selectedIds.isEmpty() && used + cost > budget) break;
            selectedIds.add(id); used += cost;
        }
        List<Integer> ids = selectedIds.stream().sorted().toList();
        List<Message> result = new ArrayList<>();
        for (Integer turnId : ids) {
            List<ConversationTurnRepository.TurnRecord> pair = grouped.get(turnId);
            var user = pair.stream().filter(r -> r.type().equals("user")).findFirst();
            var assistant = pair.stream().filter(r -> r.type().equals("assistant")).findFirst();
            if (user.isEmpty() || assistant.isEmpty()) continue;
            result.add(new UserMessage(user.get().content()));
            appendToolMessages(result, toolsByTurn.getOrDefault(turnId, List.of()));
            result.add(new AssistantMessage(assistant.get().content()));
        }
        return result;
    }

    private void appendToolMessages(List<Message> result, List<ConversationNodeRepository.NodeRecord> records) {
        Map<String, List<ConversationNodeRepository.NodeRecord>> byCall = new LinkedHashMap<>();
        records.stream().filter(n -> n.aggrKey() != null).forEach(n -> byCall.computeIfAbsent(n.aggrKey(), ignored -> new ArrayList<>()).add(n));
        List<CallPair> pairs = byCall.values().stream().map(this::pair).filter(p -> p != null)
                .sorted(Comparator.comparingInt(CallPair::round).thenComparingInt(CallPair::index)).toList();
        int keep = properties.getMemory().getMaxToolPairsPerTurn();
        if (pairs.size() > keep) pairs = pairs.subList(pairs.size() - keep, pairs.size());
        Map<Integer, List<CallPair>> byRound = new LinkedHashMap<>();
        pairs.forEach(p -> byRound.computeIfAbsent(p.round(), ignored -> new ArrayList<>()).add(p));
        byRound.values().forEach(round -> {
            round.sort(Comparator.comparingInt(CallPair::index));
            List<AssistantMessage.ToolCall> calls = round.stream().map(p -> new AssistantMessage.ToolCall(p.id(), "function", p.name(), safeJson(p.start().content(), p.id(), properties.getMemory().getMaxToolArgsPreviewChars()))).toList();
            result.add(AssistantMessage.builder().content("").toolCalls(calls).build());
            result.add(ToolResponseMessage.builder().responses(round.stream().map(p -> new ToolResponseMessage.ToolResponse(p.id(), p.name(), safeJson(p.end().content(), p.id(), properties.getMemory().getMaxToolResultPreviewChars()))).toList()).build());
        });
    }
    private CallPair pair(List<ConversationNodeRepository.NodeRecord> records) {
        var start = records.stream().filter(n -> n.status().equals("START")).findFirst().orElse(null);
        var end = records.stream().filter(n -> n.status().equals("SUCCESS") || n.status().equals("ERROR"))
                .max(Comparator.comparingLong(ConversationNodeRepository.NodeRecord::id)).orElse(null);
        if (start == null || end == null || start.roundNo() == null || start.callIndex() == null) return null;
        return new CallPair(start.aggrKey(), start.nodeName(), start.roundNo(), start.callIndex(), start, end);
    }
    private String safeJson(String content, String id, int maxChars) {
        if (content == null || content.isBlank()) return placeholder(id, "");
        if (content.length() <= maxChars) return content;
        return placeholder(id, content.substring(0, maxChars));
    }
    private String placeholder(String id, String preview) {
        try { return objectMapper.writeValueAsString(Map.of("_truncated", true, "preview", preview, "toolCallId", id,
                "lookupHint", "Use query_conversation_node for full content")); }
        catch (Exception ignored) { return "{\"_truncated\":true,\"toolCallId\":\"" + id + "\"}"; }
    }
    private record CallPair(String id, String name, int round, int index, ConversationNodeRepository.NodeRecord start, ConversationNodeRepository.NodeRecord end) {}
}
