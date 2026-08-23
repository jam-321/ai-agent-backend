package com.jam.agent.agent.memory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

class ConversationCompactionServiceTest {

    @Test
    void compactsOldMessagesIntoUserCheckpointAndPersistsItAfterTurnCompletes() {
        ModelAdapter model = mock(ModelAdapter.class);
        Dispatcher events = mock(Dispatcher.class);
        ConversationMemorySummaryRepository summaries = mock(ConversationMemorySummaryRepository.class);
        AgentExecutionContext context = context();
        ModelCallResult usage = new ModelCallResult(
                new AssistantMessage("结构化历史摘要"), "response", "model", 100L, 20L);
        when(model.call(anyList(), anyList(), eq(context), eq(ModelCallScope.conversationCompaction())))
                .thenReturn(usage);

        List<Message> messages = new ArrayList<>(List.of(
                new UserMessage("较早的用户目标和约束条件"),
                new AssistantMessage("较早的关键决定和已完成事项"),
                new UserMessage("最近一轮仍需保留的内容")));

        new ConversationCompactionService(
                new TokenEstimator(), model, events, summaries)
                .compactIfNeeded(messages, List.of(), context, 1, 1, 20L);

        assertTrue(messages.get(0).getText().contains("[CONTEXT_SUMMARY]"));
        verify(summaries, org.mockito.Mockito.never()).upsertCheckpoint(
                eq(2L), eq(3), anyString(), eq("provider"), eq("model"), eq(usage));
        new ConversationCompactionService(
                new TokenEstimator(), model, events, summaries)
                .captureFinalMessages(context, messages);
        verify(summaries).upsertCheckpoint(
                eq(2L), eq(3), anyString(), eq("provider"), eq("model"), eq(usage));
        verify(events).lifecycle(
                eq(context), eq(1), eq(1), anyString());
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
