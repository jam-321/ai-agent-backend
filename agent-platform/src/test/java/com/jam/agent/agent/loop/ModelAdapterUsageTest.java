package com.jam.agent.agent.loop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.memory.TokenEstimator;
import com.jam.agent.agent.model.AgentModelConfig;
import com.jam.agent.agent.model.ModelCallScope;
import com.jam.agent.agent.model.protocol.ModelCallResult;
import com.jam.agent.agent.model.protocol.ModelProtocolAdapter;
import com.jam.agent.agent.model.protocol.ModelProtocolRegistry;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

class ModelAdapterUsageTest {

    @Test
    void publishesUsageAndUpdatesTurnBudget() {
        ModelProtocolRegistry protocols = mock(ModelProtocolRegistry.class);
        ModelProtocolAdapter protocol = mock(ModelProtocolAdapter.class);
        Dispatcher events = mock(Dispatcher.class);
        AgentExecutionContext context = context();
        when(protocols.require("TEST")).thenReturn(protocol);
        when(protocol.call(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyList(), eq(context)))
                .thenReturn(new ModelCallResult(
                        new AssistantMessage("完成"), "response-1", "returned-model",
                        120L, 30L, 50L, 70L, null, 10L, 150L));
        ModelAdapter adapter = new ModelAdapter(
                protocols, events, new ObjectMapper(), new TokenEstimator());

        adapter.call(
                List.of(new UserMessage("问题")),
                List.of(),
                context,
                ModelCallScope.agentRound(1, 1));

        assertEquals(150, context.tokenBudget().snapshot().usedTokens());
        verify(events).modelCallStart(eq(context), eq(1), eq(1), anyString(),
                contains("estimatedInputTokens"));
        verify(events).modelCallEnd(eq(context), eq(1), eq(1), anyString(),
                contains("\"inputTokens\":120"), eq(false));
    }

    private AgentExecutionContext context() {
        return new AgentExecutionContext(
                1, 2, 3, "trace", "问题",
                new AgentConfigSnapshot("general", "prompt", Set.of(), Set.of(), "{}"),
                new AgentModelConfig("provider", "Provider", "TEST", null,
                        null, null, "model", 0.7),
                1, 4, 4, 1, 3, 8, Instant.now().plusSeconds(30));
    }
}
