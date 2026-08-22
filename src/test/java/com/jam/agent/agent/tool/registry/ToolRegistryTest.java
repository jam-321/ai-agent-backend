package com.jam.agent.agent.tool.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.tool.definition.CalculateTools;
import com.jam.agent.agent.tool.definition.TimeTools;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

    @Test
    void discoversToolsFromAllProviderBeans() {
        ObjectMapper objectMapper = new ObjectMapper();
        ToolRegistry registry = new ToolRegistry(List.of(
                new TimeTools(objectMapper),
                new CalculateTools(objectMapper)));

        assertEquals(2, registry.callbacks().size());
        assertTrue(registry.callbacks().containsKey("current_time"));
        assertTrue(registry.callbacks().containsKey("calculate"));
    }
}
