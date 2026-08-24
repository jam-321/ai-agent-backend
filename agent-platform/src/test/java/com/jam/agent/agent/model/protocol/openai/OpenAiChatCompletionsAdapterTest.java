package com.jam.agent.agent.model.protocol.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.model.AgentModelConfig;
import com.jam.agent.agent.model.protocol.ModelCallResult;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.MediaType;

class OpenAiChatCompletionsAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiChatCompletionsAdapter adapter =
            new OpenAiChatCompletionsAdapter(objectMapper);

    @Test
    void extractsDeepSeekCacheHitAndMissTokens() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "id":"chatcmpl-deepseek",
                  "model":"deepseek-v4-flash",
                  "choices":[{"message":{"role":"assistant","content":"完成"}}],
                  "usage":{
                    "prompt_tokens":1424,
                    "completion_tokens":83,
                    "total_tokens":1507,
                    "prompt_cache_hit_tokens":1024,
                    "prompt_cache_miss_tokens":400
                  }
                }
                """);

        ModelCallResult result = adapter.extractResult(response);

        assertEquals(1024L, result.cachedInputTokens());
        assertEquals(400L, result.cacheMissInputTokens());
        assertEquals(1507L, result.totalTokens());
    }

    @Test
    void extractsStandardCachedTokensAndDerivesMissTokens() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "id":"chatcmpl-openai",
                  "model":"gpt-test",
                  "choices":[{"message":{"role":"assistant","content":"完成"}}],
                  "usage":{
                    "prompt_tokens":1200,
                    "completion_tokens":80,
                    "prompt_tokens_details":{"cached_tokens":800}
                  }
                }
                """);

        ModelCallResult result = adapter.extractResult(response);

        assertEquals(800L, result.cachedInputTokens());
        assertEquals(400L, result.cacheMissInputTokens());
        assertEquals(1280L, result.totalTokens());
    }

    @Test
    void keepsCacheUsageUnknownWhenProviderDoesNotReportIt() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "choices":[{"message":{"role":"assistant","content":"完成"}}],
                  "usage":{"prompt_tokens":12,"completion_tokens":3,"total_tokens":15}
                }
                """);

        ModelCallResult result = adapter.extractResult(response);

        assertNull(result.cachedInputTokens());
        assertNull(result.cacheMissInputTokens());
    }

    @Test
    void extractsAnthropicCompatibleCacheReadAndWriteTokens() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "choices":[{"message":{"role":"assistant","content":"完成"}}],
                  "usage":{
                    "prompt_tokens":900,
                    "completion_tokens":40,
                    "cache_read_input_tokens":600,
                    "cache_creation_input_tokens":120
                  }
                }
                """);

        ModelCallResult result = adapter.extractResult(response);

        assertEquals(600L, result.cachedInputTokens());
        assertEquals(300L, result.cacheMissInputTokens());
        assertEquals(120L, result.cacheWriteInputTokens());
    }

    @Test
    void serializesImagesToolsAndToolResultsInOpenAiFormat() {
        Media image = Media.builder()
                .mimeType(MediaType.IMAGE_PNG)
                .data(new byte[]{1, 2, 3})
                .name("screenshot.png")
                .build();
        AssistantMessage assistant = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call-1", "function", "current_time", "{}")))
                .build();
        ToolResponseMessage toolResult = ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse(
                        "call-1", "current_time", "2026-08-23 17:00")))
                .build();

        JsonNode request = adapter.buildRequest(
                List.of(
                        UserMessage.builder().text("识别图片").media(image).build(),
                        assistant,
                        toolResult),
                List.of(tool("current_time")),
                context());

        JsonNode messages = request.path("messages");
        assertEquals("image_url", messages.get(0).path("content").get(1).path("type").asText());
        assertEquals("data:image/png;base64,AQID",
                messages.get(0).path("content").get(1).path("image_url").path("url").asText());
        assertEquals("call-1", messages.get(1).path("tool_calls").get(0).path("id").asText());
        assertEquals("call-1", messages.get(2).path("tool_call_id").asText());
        assertEquals("current_time", request.path("tools").get(0).path("function").path("name").asText());
        assertFalse(request.path("tools").isEmpty());
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

    private AgentExecutionContext context() {
        return new AgentExecutionContext(
                1, 2, 3, "trace", "识别图片",
                new AgentConfigSnapshot("general", "prompt", Set.of(), Set.of(), "{}"),
                new AgentModelConfig("deepseek", "DeepSeek", "OPENAI_CHAT_COMPLETIONS",
                        "https://api.deepseek.com", "/chat/completions", "test-key",
                        "deepseek-v4-flash", 0.7, true, true),
                1, 4, 4, 1, 3, 8, Instant.now().plusSeconds(30));
    }
}
