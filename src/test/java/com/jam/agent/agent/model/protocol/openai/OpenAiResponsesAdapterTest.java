package com.jam.agent.agent.model.protocol.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jam.agent.agent.model.protocol.ModelCallResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

class OpenAiResponsesAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiResponsesAdapter adapter = new OpenAiResponsesAdapter(objectMapper);

    @Test
    void replaysRawOutputItemsAndMatchesToolResultsByCallId() throws Exception {
        AssistantMessage assistant = AssistantMessage.builder()
                .content("")
                .properties(Map.of(
                        OpenAiResponsesAdapter.RAW_OUTPUT_ITEMS,
                        """
                        [
                          {"type":"reasoning","id":"reasoning-1"},
                          {"type":"function_call","call_id":"call-1","name":"current_time","arguments":"{}"}
                        ]
                        """))
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call-1", "function", "current_time", "{}")))
                .build();
        ToolResponseMessage toolResult = ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse(
                        "call-1", "current_time", "2026-08-22 20:00")))
                .build();

        ObjectNode request = adapter.buildRequest(
                List.of(
                        new SystemMessage("system prompt"),
                        new UserMessage("现在几点"),
                        assistant,
                        toolResult),
                List.of(tool("current_time")),
                "gpt-test");

        JsonNode input = request.path("input");
        assertEquals("system prompt", request.path("instructions").asText());
        assertEquals("reasoning", input.get(1).path("type").asText());
        assertEquals("function_call", input.get(2).path("type").asText());
        assertEquals("function_call_output", input.get(3).path("type").asText());
        assertEquals("call-1", input.get(3).path("call_id").asText());
        assertEquals("current_time", request.path("tools").get(0).path("name").asText());
        assertEquals(4096, request.path("max_output_tokens").asInt());
    }

    @Test
    void extractsTextToolCallsAndResponseMetadata() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "id":"response-1",
                  "model":"glm-5.3",
                  "usage":{
                    "input_tokens":12,
                    "output_tokens":8,
                    "total_tokens":20,
                    "input_tokens_details":{"cached_tokens":5},
                    "output_tokens_details":{"reasoning_tokens":3}
                  },
                  "output":[
                    {"type":"reasoning","id":"reasoning-1"},
                    {"type":"message","content":[{"type":"output_text","text":"先查询时间"}]},
                    {"type":"function_call","call_id":"call-1","name":"current_time","arguments":"{}"}
                  ]
                }
                """);

        ModelCallResult result = adapter.extractResult(response);

        assertEquals("response-1", result.responseId());
        assertEquals("glm-5.3", result.returnedModel());
        assertEquals(12L, result.inputTokens());
        assertEquals(8L, result.outputTokens());
        assertEquals(5L, result.cachedInputTokens());
        assertEquals(3L, result.reasoningTokens());
        assertEquals(20L, result.totalTokens());
        assertEquals("先查询时间", result.message().getText());
        assertEquals("call-1", result.message().getToolCalls().get(0).id());
        assertTrue(result.message().getMetadata()
                .containsKey(OpenAiResponsesAdapter.RAW_OUTPUT_ITEMS));
    }

    private ToolCallback tool(String name) {
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn(name);
        when(definition.description()).thenReturn("获取当前时间");
        when(definition.inputSchema()).thenReturn("{\"type\":\"object\",\"properties\":{}}");
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        return callback;
    }
}
