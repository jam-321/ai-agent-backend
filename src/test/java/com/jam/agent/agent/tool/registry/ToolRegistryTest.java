package com.jam.agent.agent.tool.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.tool.definition.AdminConversationTools;
import com.jam.agent.agent.tool.definition.CalculateTools;
import com.jam.agent.agent.tool.definition.TimeTools;
import com.jam.agent.auth.persistence.repository.UserRepository;
import com.jam.agent.monitoring.service.AdminMonitorService;
import java.util.List;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

    @Test
    void discoversToolsFromAllProviderBeans() {
        ObjectMapper objectMapper = new ObjectMapper();
        ToolRegistry registry = new ToolRegistry(List.of(
                new TimeTools(objectMapper),
                new CalculateTools(objectMapper),
                new AdminConversationTools(
                        mock(AdminMonitorService.class),
                        mock(UserRepository.class),
                        objectMapper)));

        assertEquals(3, registry.callbacks().size());
        assertTrue(registry.callbacks().containsKey("current_time"));
        assertTrue(registry.callbacks().containsKey("calculate"));
        assertTrue(registry.callbacks().containsKey("query_admin_session_detail"));
    }
}
