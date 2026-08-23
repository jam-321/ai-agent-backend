package com.jam.agent.agent.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.model.AgentModelConfig;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AgentConfigResponseTest {

    @Test
    void neverSerializesModelApiKey() throws Exception {
        AgentConfigSnapshot snapshot = new AgentConfigSnapshot(
                "custom",
                "prompt",
                Set.of(),
                Set.of(),
                "{}",
                "LOOP",
                null,
                new AgentModelConfig(
                        "custom-provider",
                        "Custom Provider",
                        "OPENAI_CHAT_COMPLETIONS",
                        "https://example.com/v1",
                        "/chat/completions",
                        "secret-key",
                        "game-model",
                        0.4));

        String json = new ObjectMapper().writeValueAsString(AgentConfigResponse.from(snapshot));

        assertFalse(json.contains("secret-key"));
        assertTrue(json.contains("\"modelApiKeyConfigured\":true"));
        assertTrue(json.contains("\"modelName\":\"game-model\""));
    }
}
