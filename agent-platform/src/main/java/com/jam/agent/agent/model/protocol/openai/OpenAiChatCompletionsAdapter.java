package com.jam.agent.agent.model.protocol.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jam.agent.agent.model.AgentModelConfig;
import com.jam.agent.agent.model.protocol.ModelCallResult;
import com.jam.agent.agent.model.protocol.ModelCredentials;
import com.jam.agent.agent.model.protocol.ModelProtocol;
import com.jam.agent.agent.model.protocol.ModelProtocolAdapter;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 直接调用 OpenAI Chat Completions 兼容接口。
 *
 * <p>保留原始 usage JSON，避免 Spring AI 在通用映射时丢弃 DeepSeek 的
 * prompt_cache_hit_tokens / prompt_cache_miss_tokens 等供应商扩展字段。
 */
@Component
public class OpenAiChatCompletionsAdapter implements ModelProtocolAdapter {

    private static final String DEFAULT_ENDPOINT_PATH = "/v1/chat/completions";

    private final ObjectMapper objectMapper;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final Map<ConnectionKey, RestClient> clients = new ConcurrentHashMap<>();

    @Autowired
    public OpenAiChatCompletionsAdapter(
            ObjectMapper objectMapper,
            @Value("${agent.model.connect-timeout:10s}") Duration connectTimeout,
            @Value("${agent.model.read-timeout:45s}") Duration readTimeout) {
        this.objectMapper = objectMapper;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public OpenAiChatCompletionsAdapter(ObjectMapper objectMapper) {
        this(objectMapper, Duration.ofSeconds(10), Duration.ofSeconds(45));
    }

    @Override
    public String protocolType() {
        return ModelProtocol.OPENAI_CHAT_COMPLETIONS;
    }

    @Override
    public ModelCallResult call(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context) {
        AgentModelConfig config = context.modelConfig();
        String apiKey = ModelCredentials.resolve(config.apiKey());
        if (!ModelCredentials.isUsable(apiKey)) {
            return mockResponse(messages, context);
        }

        ConnectionKey connection = new ConnectionKey(
                normalizeBaseUrl(config.baseUrl()),
                normalizeEndpointPath(config.endpointPath()),
                apiKey);
        RestClient client = clients.computeIfAbsent(connection, this::createClient);
        JsonNode response = client.post()
                .uri(connection.endpointUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequest(messages, tools, context))
                .retrieve()
                .body(JsonNode.class);
        return extractResult(response);
    }

    ObjectNode buildRequest(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", context.modelConfig().modelName());
        request.put("temperature", context.modelConfig().temperature() == null
                ? 0.7 : context.modelConfig().temperature());
        request.put("max_tokens", context.budgetConfig().maxOutputTokens());
        request.put("parallel_tool_calls", true);

        ArrayNode requestMessages = request.putArray("messages");
        for (Message message : messages) {
            if (message instanceof SystemMessage system) {
                addTextMessage(requestMessages, "system", system.getText());
            } else if (message instanceof UserMessage user) {
                addUserMessage(requestMessages, user);
            } else if (message instanceof AssistantMessage assistant) {
                addAssistantMessage(requestMessages, assistant);
            } else if (message instanceof ToolResponseMessage toolResponse) {
                addToolResponses(requestMessages, toolResponse);
            }
        }

        if (!tools.isEmpty()) {
            ArrayNode toolList = request.putArray("tools");
            for (ToolCallback callback : tools) {
                ObjectNode tool = toolList.addObject();
                tool.put("type", "function");
                ObjectNode function = tool.putObject("function");
                function.put("name", callback.getToolDefinition().name());
                function.put("description", callback.getToolDefinition().description());
                function.set("parameters", parseSchema(callback.getToolDefinition().inputSchema()));
            }
            request.put("tool_choice", "auto");
        }
        return request;
    }

    ModelCallResult extractResult(JsonNode response) {
        if (response == null || !response.isObject()) {
            throw new IllegalStateException("Chat Completions 接口未返回有效响应。");
        }
        if (response.hasNonNull("error")) {
            throw new IllegalStateException("Chat Completions 接口返回错误："
                    + response.path("error").path("message").asText("未知错误"));
        }

        JsonNode message = response.path("choices").path(0).path("message");
        if (!message.isObject()) {
            throw new IllegalStateException("Chat Completions 接口缺少 assistant message。");
        }
        List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
        JsonNode rawToolCalls = message.path("tool_calls");
        if (rawToolCalls.isArray()) {
            for (JsonNode call : rawToolCalls) {
                JsonNode function = call.path("function");
                toolCalls.add(new AssistantMessage.ToolCall(
                        call.path("id").asText(),
                        call.path("type").asText("function"),
                        function.path("name").asText(),
                        function.path("arguments").asText("{}")));
            }
        }

        Map<String, Object> properties = new HashMap<>();
        putIfText(properties, "responseId", response.path("id"));
        putIfText(properties, "returnedModel", response.path("model"));
        AssistantMessage assistant = AssistantMessage.builder()
                .content(readContent(message.get("content")))
                .properties(properties)
                .toolCalls(toolCalls)
                .build();

        JsonNode usage = response.path("usage");
        Long inputTokens = nullableLong(usage.get("prompt_tokens"));
        Long outputTokens = nullableLong(usage.get("completion_tokens"));
        Long cachedInputTokens = firstLong(
                usage.get("prompt_cache_hit_tokens"),
                usage.path("prompt_tokens_details").get("cached_tokens"),
                usage.path("input_tokens_details").get("cached_tokens"),
                usage.get("cache_read_input_tokens"));
        Long cacheMissInputTokens = firstLong(usage.get("prompt_cache_miss_tokens"));
        if (cacheMissInputTokens == null && inputTokens != null && cachedInputTokens != null) {
            cacheMissInputTokens = Math.max(0, inputTokens - cachedInputTokens);
        }
        Long cacheWriteInputTokens = firstLong(usage.get("cache_creation_input_tokens"));
        Long reasoningTokens = firstLong(
                usage.path("completion_tokens_details").get("reasoning_tokens"),
                usage.path("output_tokens_details").get("reasoning_tokens"));
        Long totalTokens = nullableLong(usage.get("total_tokens"));
        if (totalTokens == null) totalTokens = sum(inputTokens, outputTokens);

        return new ModelCallResult(
                assistant,
                response.path("id").asText(null),
                response.path("model").asText(null),
                inputTokens,
                outputTokens,
                cachedInputTokens,
                cacheMissInputTokens,
                cacheWriteInputTokens,
                reasoningTokens,
                totalTokens);
    }

    private void addUserMessage(ArrayNode messages, UserMessage user) {
        if (user.getMedia() == null || user.getMedia().isEmpty()) {
            addTextMessage(messages, "user", user.getText());
            return;
        }
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        if (user.getText() != null && !user.getText().isBlank()) {
            content.addObject().put("type", "text").put("text", user.getText());
        }
        for (Media media : user.getMedia()) {
            ObjectNode image = content.addObject();
            image.put("type", "image_url");
            image.putObject("image_url").put("url", dataUrl(media));
        }
    }

    private void addAssistantMessage(ArrayNode messages, AssistantMessage assistant) {
        ObjectNode message = messages.addObject();
        message.put("role", "assistant");
        if (assistant.getText() != null && !assistant.getText().isBlank()) {
            message.put("content", assistant.getText());
        } else {
            message.putNull("content");
        }
        if (!assistant.getToolCalls().isEmpty()) {
            ArrayNode calls = message.putArray("tool_calls");
            for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
                ObjectNode value = calls.addObject();
                value.put("id", call.id());
                value.put("type", "function");
                ObjectNode function = value.putObject("function");
                function.put("name", call.name());
                function.put("arguments", call.arguments() == null ? "{}" : call.arguments());
            }
        }
    }

    private void addToolResponses(ArrayNode messages, ToolResponseMessage toolResponse) {
        for (ToolResponseMessage.ToolResponse response : toolResponse.getResponses()) {
            ObjectNode message = messages.addObject();
            message.put("role", "tool");
            message.put("tool_call_id", response.id());
            message.put("name", response.name());
            message.put("content", response.responseData());
        }
    }

    private void addTextMessage(ArrayNode messages, String role, String content) {
        ObjectNode message = messages.addObject();
        message.put("role", role);
        message.put("content", content == null ? "" : content);
    }

    private String readContent(JsonNode content) {
        if (content == null || content.isNull()) return "";
        if (content.isTextual()) return content.asText();
        if (!content.isArray()) return content.asText("");
        StringBuilder text = new StringBuilder();
        for (JsonNode part : content) {
            String value = part.path("text").asText("");
            if (!value.isBlank()) {
                if (!text.isEmpty()) text.append('\n');
                text.append(value);
            }
        }
        return text.toString();
    }

    private JsonNode parseSchema(String schema) {
        try {
            JsonNode parsed = objectMapper.readTree(schema);
            return parsed != null && parsed.isObject()
                    ? parsed : objectMapper.createObjectNode();
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String dataUrl(Media media) {
        if (!(media.getData() instanceof byte[] bytes)) {
            throw new IllegalArgumentException("Chat Completions 图片必须使用二进制媒体数据。");
        }
        return "data:" + media.getMimeType() + ";base64,"
                + Base64.getEncoder().encodeToString(bytes);
    }

    private RestClient createClient(ConnectionKey connection) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> headers.setBearerAuth(connection.apiKey()))
                .build();
    }

    private ModelCallResult mockResponse(
            List<Message> messages,
            AgentExecutionContext context) {
        String query = context.currentQuery();
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (messages.get(index) instanceof UserMessage user && user.getText() != null) {
                query = user.getText();
                break;
            }
        }
        return new ModelCallResult(new AssistantMessage(
                "[mock] 当前 Agent 未配置有效的模型 API Key。你的问题是：" + query));
    }

    private void putIfText(Map<String, Object> properties, String key, JsonNode value) {
        if (value != null && value.isTextual() && !value.asText().isBlank()) {
            properties.put(key, value.asText());
        }
    }

    private Long firstLong(JsonNode... values) {
        for (JsonNode value : values) {
            Long parsed = nullableLong(value);
            if (parsed != null) return parsed;
        }
        return null;
    }

    private Long nullableLong(JsonNode value) {
        return value == null || !value.isNumber() ? null : value.longValue();
    }

    private Long sum(Long first, Long second) {
        return first == null && second == null
                ? null
                : (first == null ? 0 : first) + (second == null ? 0 : second);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Chat Completions 供应商缺少 Base URL。");
        }
        return baseUrl.trim().replaceAll("/+$", "");
    }

    private String normalizeEndpointPath(String endpointPath) {
        String path = endpointPath == null || endpointPath.isBlank()
                ? DEFAULT_ENDPOINT_PATH : endpointPath.trim();
        return path.startsWith("/") ? path : "/" + path;
    }

    /** API Key 只参与客户端缓存标识，不进入日志或异常信息。 */
    private record ConnectionKey(String baseUrl, String endpointPath, String apiKey) {
        private String endpointUrl() {
            return baseUrl + endpointPath;
        }

        @Override
        public String toString() {
            return "ConnectionKey[baseUrl=" + baseUrl
                    + ", endpointPath=" + endpointPath + ", apiKey=***]";
        }
    }
}
