package com.jam.agent.agent.memory;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jam.agent.agent.config.AgentBudgetConfig;
import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.config.AgentMemoryConfig;
import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.loop.ModelAdapter;
import com.jam.agent.agent.model.AgentModelConfig;
import com.jam.agent.agent.model.ModelCallScope;
import com.jam.agent.agent.model.protocol.ModelCallResult;
import com.jam.agent.agent.persistence.repository.ConversationMemorySummaryRepository;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.TokenBudgetTracker;
import com.jam.agent.conversation.persistence.repository.ConversationTurnRepository;
import com.jam.agent.conversation.persistence.repository.ConversationTurnRepository.TurnRecord;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

class ConversationCompactionServiceTest {

    @Test
    void summarizesContiguousOldTurnsAndPersistsCoverage() {
        ConversationTurnRepository turns = mock(ConversationTurnRepository.class);
        ConversationMemorySummaryRepository summaries = mock(ConversationMemorySummaryRepository.class);
        ModelAdapter model = mock(ModelAdapter.class);
        Dispatcher events = mock(Dispatcher.class);
        AgentExecutionContext context = context();
        List<TurnRecord> history = List.of(
                turn(1, "user", "第一轮用户提出了一个很长的目标和约束条件"),
                turn(1, "assistant", "第一轮助手确认了目标并给出关键决定"),
                turn(2, "user", "第二轮继续补充很多重要的数据和未解决问题"),
                turn(2, "assistant", "第二轮助手记录了下一步和已经完成的事项"));
        ModelCallResult usage = new ModelCallResult(
                new AssistantMessage("结构化历史摘要"), "response", "model", 100L, 20L);
        when(summaries.latest(1, 2)).thenReturn(Optional.empty());
        when(turns.findCompletedRange(1, 2, 0, 3)).thenReturn(history);
        when(model.call(anyList(), anyList(), eq(context), eq(ModelCallScope.conversationCompaction())))
                .thenReturn(usage);

        new ConversationCompactionService(
                turns, summaries, new TokenEstimator(), model, events)
                .compactIfNeeded(context);

        verify(summaries).insert(
                2, 1, 2, "结构化历史摘要", "provider", "model", usage);
        verify(events).lifecycle(context, 1, 0,
                "conversation_compaction_success:coveredUntilTurn=2");
    }

    private TurnRecord turn(int turnId, String type, String content) {
        return new TurnRecord(
                turnId * 10L, 2, turnId, type, content, false, null,
                "trace-" + turnId, "general", "provider", "model", "TEST",
                LocalDateTime.now(), LocalDateTime.now());
    }

    private AgentExecutionContext context() {
        AgentBudgetConfig budget = new AgentBudgetConfig(10000, 8000, 1000, 500);
        return new AgentExecutionContext(
                1, 2, 3, "trace", "当前问题", List.of(),
                new AgentConfigSnapshot("general", "prompt", Set.of(), Set.of(), "{}"),
                new AgentModelConfig("provider", "Provider", "TEST", null,
                        null, null, "model", 0.7),
                1, 4, 4, 1, 3, 8, Instant.now().plusSeconds(30),
                budget,
                new AgentMemoryConfig(true, 10, 5, 100, 20),
                new TokenBudgetTracker(budget.maxTokensPerTurn()));
    }
}
