package com.jam.agent.agent.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

class ModelRegistryTest {

    @Test
    void appliesPerAgentOptionsOnTheDefaultConnection() {
        ChatModel defaultModel = org.mockito.Mockito.mock(ChatModel.class);
        ModelRegistry registry = new ModelRegistry(
                defaultModel,
                "https://api.deepseek.com/",
                "test-key",
                "deepseek-chat",
                0.7);

        ModelRegistry.ResolvedModel resolved = registry.resolve(new AgentModelConfig(
                "deepseek",
                "DeepSeek",
                "OPENAI_CHAT_COMPLETIONS",
                "https://api.deepseek.com",
                "/v1/chat/completions",
                "test-key",
                "deepseek-reasoner",
                0.2));

        assertSame(defaultModel, resolved.model());
        assertEquals("deepseek-reasoner", resolved.modelName());
        assertEquals(0.2, resolved.temperature());
        assertTrue(resolved.enabled());
    }

    @Test
    void createsAndCachesClientsForAnotherConnection() {
        ChatModel defaultModel = org.mockito.Mockito.mock(ChatModel.class);
        ModelRegistry registry = new ModelRegistry(
                defaultModel,
                "https://api.deepseek.com",
                "default-key",
                "deepseek-chat",
                0.7);
        AgentModelConfig custom = new AgentModelConfig(
                "custom",
                "Custom",
                "OPENAI_CHAT_COMPLETIONS",
                "https://example.com/v1",
                "/chat/completions",
                "custom-key",
                "game-model",
                0.5);

        ModelRegistry.ResolvedModel first = registry.resolve(custom);
        ModelRegistry.ResolvedModel second = registry.resolve(custom);

        assertNotSame(defaultModel, first.model());
        assertSame(first.model(), second.model());
        assertTrue(first.enabled());
    }

    @Test
    void usesMockModeWhenApiKeyIsUnavailable() {
        ChatModel defaultModel = org.mockito.Mockito.mock(ChatModel.class);
        ModelRegistry registry = new ModelRegistry(
                defaultModel,
                "https://api.deepseek.com",
                "sk-dummy-not-configured",
                "deepseek-chat",
                0.7);

        ModelRegistry.ResolvedModel resolved = registry.resolve(
                new AgentModelConfig(null, null, null, null, null, null, null, null));

        assertFalse(resolved.enabled());
    }
}
