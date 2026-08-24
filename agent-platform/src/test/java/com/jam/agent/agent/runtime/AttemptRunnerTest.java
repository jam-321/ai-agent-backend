package com.jam.agent.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.loop.AgentLoop;
import com.jam.agent.agent.loop.ModelAdapter.RetryableModelException;
import com.jam.agent.agent.model.AgentModelConfig;
import com.jam.agent.agent.model.ModelFailureCategory;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

class AttemptRunnerTest {

    @Test
    void failoverAttemptUsesConfiguredFallbackModel() {
        AgentLoop loop = mock(AgentLoop.class);
        Dispatcher events = mock(Dispatcher.class);
        AgentModelConfig primary = model("primary", "primary-model");
        AgentModelConfig fallback = model("fallback", "fallback-model");
        AgentConfigSnapshot recipe = new AgentConfigSnapshot(
                "test",
                false,
                "prompt",
                Set.of(),
                Set.of(),
                "{}",
                "SUMMARY_TOOL",
                "LOOP",
                null,
                primary,
                fallback);
        AgentExecutionContext context = new AgentExecutionContext(
                1, 2, 1, "trace", "query", recipe, primary,
                2, 4, 4, 1, 3, 8, Instant.now().plusSeconds(30));
        List<Message> messages = List.of(new UserMessage("query"));

        doAnswer(new org.mockito.stubbing.Answer<>() {
            private int calls;

            @Override
            public String answer(org.mockito.invocation.InvocationOnMock invocation) {
                if (++calls == 1) {
                    throw new RetryableModelException(
                            "temporary",
                            new RuntimeException("timeout"),
                            ModelFailureCategory.TIMEOUT);
                }
                return "fallback answer";
            }
        }).when(loop).run(any(AgentExecutionContext.class), anyInt(), any());

        AgentRunResult result = new AttemptRunner(loop, events).execute(context, messages);

        assertEquals("fallback answer", result.answer());
        assertEquals("fallback", result.modelConfig().providerKey());
        ArgumentCaptor<AgentExecutionContext> captor = ArgumentCaptor.forClass(AgentExecutionContext.class);
        verify(loop, times(2)).run(captor.capture(), any(Integer.class), eq(messages));
        assertEquals("primary", captor.getAllValues().get(0).modelConfig().providerKey());
        assertEquals("fallback", captor.getAllValues().get(1).modelConfig().providerKey());
    }

    private AgentModelConfig model(String provider, String name) {
        return new AgentModelConfig(
                provider, provider, "TEST", "http://localhost", "/chat", "key", name, 0.2);
    }
}
