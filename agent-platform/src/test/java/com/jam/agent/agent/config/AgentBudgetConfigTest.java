package com.jam.agent.agent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AgentBudgetConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolvesAgentBudgetAndMemoryWithoutExceedingGlobalLimits() {
        AgentProperties.Budget budgetDefaults = new AgentProperties.Budget();
        AgentProperties.Memory memoryDefaults = new AgentProperties.Memory();
        String magicParams = """
                {
                  "budget":{"maxTokensPerTurn":10000,"maxContextTokens":12000,"maxOutputTokens":1024},
                  "memory":{"compactionTriggerTokens":6000,"keepRecentTokens":3000,"maxToolResultTokens":1000}
                }
                """;

        AgentBudgetConfig budget = AgentBudgetConfig.resolve(
                budgetDefaults, magicParams, objectMapper);
        AgentMemoryConfig memory = AgentMemoryConfig.resolve(
                memoryDefaults, magicParams, objectMapper);

        assertEquals(10000, budget.maxTokensPerTurn());
        assertEquals(12000, budget.maxContextTokens());
        assertEquals(1024, budget.maxOutputTokens());
        assertEquals(6000, memory.compactionTriggerTokens());
        assertEquals(3000, memory.keepRecentTokens());
        assertEquals(1000, memory.maxToolResultTokens());
    }

    @Test
    void capsValuesAtYamlSafetyLimits() {
        AgentProperties.Budget defaults = new AgentProperties.Budget();
        AgentBudgetConfig actual = AgentBudgetConfig.resolve(
                defaults,
                "{\"budget\":{\"maxTokensPerTurn\":999999999,\"maxContextTokens\":999999}}",
                objectMapper);

        assertEquals(defaults.getMaxTokensPerTurn(), actual.maxTokensPerTurn());
        assertEquals(defaults.getMaxContextTokens(), actual.maxContextTokens());
    }
}
