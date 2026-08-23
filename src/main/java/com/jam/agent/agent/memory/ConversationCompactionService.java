package com.jam.agent.agent.memory;

import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.loop.ModelAdapter;
import com.jam.agent.agent.model.ModelCallScope;
import com.jam.agent.agent.model.protocol.ModelCallResult;
import com.jam.agent.agent.persistence.repository.ConversationMemorySummaryRepository;
import com.jam.agent.agent.persistence.repository.ConversationMemorySummaryRepository.SummaryRecord;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.conversation.persistence.repository.ConversationTurnRepository;
import com.jam.agent.conversation.persistence.repository.ConversationTurnRepository.TurnRecord;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

/** 在 Turn 边界把较早完整对话压缩成可持久化、可版本追踪的会话摘要。 */
@Service
public class ConversationCompactionService {

    private final ConversationTurnRepository turns;
    private final ConversationMemorySummaryRepository summaries;
    private final TokenEstimator tokens;
    private final ModelAdapter model;
    private final Dispatcher events;

    public ConversationCompactionService(
            ConversationTurnRepository turns,
            ConversationMemorySummaryRepository summaries,
            TokenEstimator tokens,
            ModelAdapter model,
            Dispatcher events) {
        this.turns = turns;
        this.summaries = summaries;
        this.tokens = tokens;
        this.model = model;
        this.events = events;
    }

    public void compactIfNeeded(AgentExecutionContext context) {
        if (!context.memoryConfig().compactionEnabled() || context.turnId() <= 2) return;

        SummaryRecord previous = summaries.latest(context.userId(), context.conversationId())
                .orElse(null);
        int coveredUntil = previous == null ? 0 : previous.coveredUntilTurnId();
        List<TurnRecord> records = turns.findCompletedRange(
                context.userId(), context.conversationId(), coveredUntil, context.turnId());
        Map<Integer, List<TurnRecord>> byTurn = group(records);
        int totalTokens = tokens.estimateText(previous == null ? null : previous.content())
                + records.stream().mapToInt(turn -> tokens.estimateText(turn.content())).sum();
        if (totalTokens <= context.memoryConfig().compactionTriggerTokens()) return;

        List<Integer> turnIds = new ArrayList<>(byTurn.keySet());
        int keepTokens = 0;
        int firstKeptIndex = turnIds.size();
        for (int index = turnIds.size() - 1; index >= 0; index--) {
            int turnTokens = byTurn.get(turnIds.get(index)).stream()
                    .mapToInt(turn -> tokens.estimateText(turn.content())).sum();
            if (keepTokens + turnTokens > context.memoryConfig().keepRecentTokens()) break;
            keepTokens += turnTokens;
            firstKeptIndex = index;
        }
        List<Integer> candidates = turnIds.subList(0, firstKeptIndex);
        if (candidates.isEmpty()) return;

        int maxSourceTokens = Math.max(1024,
                context.budgetConfig().maxContextTokens()
                        - context.budgetConfig().maxOutputTokens()
                        - context.budgetConfig().safetyMarginTokens()
                        - 2000);
        List<Integer> selected = selectContiguousPrefix(candidates, byTurn, previous, maxSourceTokens);
        if (selected.isEmpty()) return;

        try {
            String prompt = buildPrompt(previous, selected, byTurn);
            ModelCallResult result = model.call(
                    List.<Message>of(new UserMessage(prompt)),
                    List.of(),
                    context,
                    ModelCallScope.conversationCompaction());
            String summary = result.message().getText();
            if (summary == null || summary.isBlank() || summary.startsWith("[mock]")) {
                events.lifecycle(context, 1, 0, "conversation_compaction_skipped");
                return;
            }

            int until = selected.get(selected.size() - 1);
            summaries.insert(
                    context.conversationId(),
                    previous == null ? selected.get(0) : previous.coveredFromTurnId(),
                    until,
                    summary,
                    context.modelConfig().providerKey(),
                    context.modelConfig().modelName(),
                    result);
            events.lifecycle(context, 1, 0,
                    "conversation_compaction_success:coveredUntilTurn=" + until);
        } catch (RuntimeException exception) {
            // 压缩失败时回退到现有历史裁剪，不能阻断用户当前 Turn。
            events.lifecycle(context, 1, 0,
                    "conversation_compaction_failed:" + safeMessage(exception));
        }
    }

    private Map<Integer, List<TurnRecord>> group(List<TurnRecord> records) {
        Map<Integer, List<TurnRecord>> value = new LinkedHashMap<>();
        for (TurnRecord record : records) {
            value.computeIfAbsent(record.turnId(), ignored -> new ArrayList<>()).add(record);
        }
        return value;
    }

    private List<Integer> selectContiguousPrefix(
            List<Integer> candidates,
            Map<Integer, List<TurnRecord>> byTurn,
            SummaryRecord previous,
            int maxTokens) {
        int used = tokens.estimateText(previous == null ? null : previous.content());
        List<Integer> selected = new ArrayList<>();
        for (Integer turnId : candidates) {
            int cost = byTurn.get(turnId).stream()
                    .mapToInt(turn -> tokens.estimateText(turn.content())).sum();
            if (!selected.isEmpty() && used + cost > maxTokens) break;
            selected.add(turnId);
            used += cost;
        }
        return selected;
    }

    private String buildPrompt(
            SummaryRecord previous,
            List<Integer> selected,
            Map<Integer, List<TurnRecord>> byTurn) {
        StringBuilder source = new StringBuilder();
        if (previous != null) {
            source.append("【已有历史摘要】\n").append(previous.content()).append("\n\n");
        }
        for (Integer turnId : selected) {
            source.append("【Turn ").append(turnId).append("】\n");
            for (TurnRecord record : byTurn.get(turnId)) {
                source.append(record.type()).append(": ").append(record.content()).append('\n');
            }
        }
        return """
                请把下面的较早会话压缩成一份结构化中文记忆，供后续模型继续对话。
                必须保留：用户目标、已确认事实、关键决定、重要数据或标识、已完成事项、未解决问题和下一步。
                不要虚构，不要把已被推翻的猜测写成事实；涉及工具数据时说明数据可能过时，需要时应重新查询。
                只输出摘要正文，不要输出解释或 JSON。

                %s
                """.formatted(source);
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return exception.getClass().getSimpleName();
        return message.length() <= 300 ? message : message.substring(0, 300);
    }
}
