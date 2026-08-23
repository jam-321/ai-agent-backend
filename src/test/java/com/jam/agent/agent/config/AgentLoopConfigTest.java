package com.jam.agent.agent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AgentLoopConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolvesPerAgentValuesFromMagicParams() {
        AgentProperties.Loop defaults = new AgentProperties.Loop();

        AgentLoopConfig actual = AgentLoopConfig.resolve(
                defaults,
                "{\"loop\":{\"maxToolRounds\":6,\"maxToolsPerRound\":2,\"maxRunDurationSeconds\":30}}",
                objectMapper);

        assertEquals(6, actual.maxToolRounds());
        assertEquals(2, actual.maxToolsPerRound());
        assertEquals(Duration.ofSeconds(30), actual.maxRunDuration());
    }

    @Test
    void neverExceedsGlobalSafetyLimits() {
        AgentProperties.Loop defaults = new AgentProperties.Loop();

        AgentLoopConfig actual = AgentLoopConfig.resolve(
                defaults,
                "{\"loop\":{\"maxToolRounds\":999,\"maxToolsPerRound\":999,\"maxRunDurationSeconds\":999999}}",
                objectMapper);

        assertEquals(defaults.getMaxToolRounds(), actual.maxToolRounds());
        assertEquals(defaults.getMaxToolsPerRound(), actual.maxToolsPerRound());
        assertEquals(defaults.getMaxRunDuration(), actual.maxRunDuration());
    }
}
