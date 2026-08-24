package com.jam.agent.agent.model.protocol;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModelProtocolRegistryTest {

    @Test
    void routesLegacyChatNameAndResponsesProtocol() {
        ModelProtocolAdapter chat = mock(ModelProtocolAdapter.class);
        ModelProtocolAdapter responses = mock(ModelProtocolAdapter.class);
        when(chat.protocolType()).thenReturn(ModelProtocol.OPENAI_CHAT_COMPLETIONS);
        when(responses.protocolType()).thenReturn(ModelProtocol.OPENAI_RESPONSES);
        ModelProtocolRegistry registry = new ModelProtocolRegistry(List.of(chat, responses));

        assertSame(chat, registry.require("OPENAI_COMPATIBLE"));
        assertSame(responses, registry.require("OPENAI_RESPONSES"));
        assertTrue(registry.supports("OPENAI_RESPONSES"));
    }
}
