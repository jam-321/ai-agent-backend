package com.jam.agent.agent.memory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.config.AgentBudgetConfig;
import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.config.AgentMemoryConfig;
import com.jam.agent.agent.model.AgentModelConfig;
import com.jam.agent.agent.persistence.repository.ConversationNodeOutputRepository;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.TokenBudgetTracker;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ToolResultCompactionServiceTest {

    @Test
    void archivesLargeResultAndReturnsLookupPlaceholder() {
        ConversationNodeOutputRepository outputs = mock(ConversationNodeOutputRepository.class);
        ToolResultCompactionService service = new ToolResultCompactionService(
                new TokenEstimator(), outputs, new ObjectMapper());
        AgentExecutionContext context = context();
        String result = "这是需要归档的大工具结果，包含很多关键数据。";

        ToolResultCompactionService.CompactedResult actual = service.compact(
                context, "data_query", "call-1", result);

        assertTrue(actual.compacted());
        assertTrue(actual.modelContent().contains("\"_compacted\":true"));
        assertTrue(actual.modelContent().contains("\"handle\":\"call-1\""));
        verify(outputs).archive(2, 3, "data_query", "call-1", "trace",
                result, actual.originalTokens());
    }

    private AgentExecutionContext context() {
        AgentBudgetConfig budget = new AgentBudgetConfig(10000, 8000, 1000, 500);
        return new AgentExecutionContext(
                1, 2, 3, "trace", "问题", List.of(),
                new AgentConfigSnapshot("general", "prompt", Set.of(), Set.of(), "{}"),
                new AgentModelConfig(null, null, null, null, null, null, null, null),
                1, 4, 4, 1, 3, 8, Instant.now().plusSeconds(30),
                budget,
                new AgentMemoryConfig(true, 100, 50, 5, 10),
                new TokenBudgetTracker(budget.maxTokensPerTurn()));
    }
}
