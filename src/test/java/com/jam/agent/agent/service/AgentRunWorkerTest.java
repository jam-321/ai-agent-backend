package com.jam.agent.agent.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.AgentExecutor;
import com.jam.agent.agent.runtime.AgentExecutorRegistry;
import com.jam.agent.agent.runtime.AgentRunResult;
import com.jam.agent.agent.runtime.AgentTurnPreparer;
import com.jam.agent.agent.runtime.ConversationLock;
import com.jam.agent.agent.runtime.TurnFinalizer;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

class AgentRunWorkerTest {

    @Test
    void preparesTurnBeforeStartingExecutionAttempts() {
        AgentExecutorRegistry executors = mock(AgentExecutorRegistry.class);
        AgentTurnPreparer turnPreparer = mock(AgentTurnPreparer.class);
        TurnFinalizer finalizer = mock(TurnFinalizer.class);
        ConversationLock lock = mock(ConversationLock.class);
        AgentExecutor executor = mock(AgentExecutor.class);
        AgentExecutionContext context = context();
        List<Message> turnMessages = List.of(new UserMessage("query"));

        when(turnPreparer.prepare(context)).thenReturn(turnMessages);
        when(executors.resolve("LOOP")).thenReturn(executor);
        when(executor.execute(context, turnMessages))
                .thenReturn(new AgentRunResult(1, "answer"));

        new AgentRunWorker(executors, turnPreparer, finalizer, lock).run(context);

        InOrder order = inOrder(turnPreparer, executors, executor);
        order.verify(turnPreparer).prepare(context);
        order.verify(executors).resolve("LOOP");
        order.verify(executor).execute(context, turnMessages);
        verify(finalizer).complete(context, 1, "answer");
        verify(lock).unlock(2L, "trace");
    }

    private AgentExecutionContext context() {
        return new AgentExecutionContext(
                1L,
                2L,
                1,
                "trace",
                "query",
                new AgentConfigSnapshot(
                        "general",
                        "prompt",
                        Set.of(),
                        Set.of(),
                        "{}",
                        "LOOP",
                        null),
                new com.jam.agent.agent.model.AgentModelConfig(
                        null, null, null, null, null, null, null, null),
                1,
                2,
                1,
                0,
                2,
                4,
                Instant.now().plusSeconds(30));
    }
}
