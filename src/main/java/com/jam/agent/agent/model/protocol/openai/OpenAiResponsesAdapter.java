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
import java.util.Base64;
import java.util.ArrayList;
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
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 调用 OpenAI Responses 兼容接口，并把 typed Items 转换为 AgentLoop 的统一消息。
 *
 * <p>应用自行持久化上下文，因此请求固定使用 store=false；同一 Turn 的原始 output Items
 * 会暂存在 AssistantMessage 元数据中，以便工具结果回填时保留 reasoning/function_call。
 */
@Component
public class OpenAiResponsesAdapter implements ModelProtocolAdapter {

    static final String RAW_OUTPUT_ITEMS = "responses.rawOutputItems";

    private static final String DEFAULT_ENDPOINT_PATH = "/responses";
    private static final String FUNCTION_CALL = "function_call";
    private static final String FUNCTION_CALL_OUTPUT = "function_call_output";

    private final ObjectMapper objectMapper;
    private final Map<ConnectionKey, RestClient> clients = new ConcurrentHashMap<>();

    public OpenAiResponsesAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String protocolType() {
        return ModelProtocol.OPENAI_RESPONSES;
    }

    @Override
    public ModelCallResult call(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context) {
        AgentModelConfig config = context.modelConfig();
        String apiKey = ModelCredentials.resolve(config.apiKey());
        if (!ModelCredentials.isUsable(apiKey)) {
            return new ModelCallResult(new AssistantMessage(
                    "[mock] 当前 Agent 未配置有效的模型 API Key。你的问题是："
                            + context.currentQuery()));
        }

        ConnectionKey connection = new ConnectionKey(
                normalizeBaseUrl(config.baseUrl()),
                normalizeEndpointPath(config.endpointPath()),
                apiKey);
        RestClient client = clients.computeIfAbsent(connection, this::createClient);
        JsonNode response = client.post()
                .uri(connection.endpointUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequest(
                        messages,
                        tools,
                        config.modelName(),
                        context.budgetConfig().maxOutputTokens()))
                .retrieve()
                .body(JsonNode.class);
        return extractResult(response);
    }

    ObjectNode buildRequest(
            List<Message> messages,
            List<ToolCallback> tools,
            String modelName) {
        return buildRequest(messages, tools, modelName, 4096);
    }

    ObjectNode buildRequest(
            List<Message> messages,
            List<ToolCallback> tools,
            String modelName,
            int maxOutputTokens) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", modelName);
        request.put("max_output_tokens", maxOutputTokens);
        request.put("store", false);
        request.put("parallel_tool_calls", true);

        ArrayNode input = request.putArray("input");
        StringBuilder instructions = new StringBuilder();
        for (Message message : messages) {
            if (message instanceof SystemMessage system) {
                appendInstruction(instructions, system.getText());
            } else if (message instanceof UserMessage user) {
                addUser(input, user);
            } else if (message instanceof AssistantMessage assistant) {
                addAssistant(input, assistant);
            } else if (message instanceof ToolResponseMessage toolResponse) {
                addToolResponses(input, toolResponse);
            }
        }
        if (!instructions.isEmpty()) {
            request.put("instructions", instructions.toString());
        }

        if (!tools.isEmpty()) {
            ArrayNode toolList = request.putArray("tools");
            for (ToolCallback callback : tools) {
                ObjectNode tool = toolList.addObject();
                tool.put("type", "function");
                tool.put("name", callback.getToolDefinition().name());
                tool.put("description", callback.getToolDefinition().description());
                tool.set("parameters", parseSchema(callback.getToolDefinition().inputSchema()));
            }
        }
        return request;
    }

    ModelCallResult extractResult(JsonNode response) {
        if (response == null || !response.isObject()) {
            throw new IllegalStateException("Responses 接口未返回有效响应。");
        }
        if (response.hasNonNull("error")) {
            throw new IllegalStateException("Responses 接口返回错误："
                    + response.path("error").path("message").asText("未知错误"));
        }

        JsonNode output = response.path("output");
        if (!output.isArray()) {
            throw new IllegalStateException("Responses 接口缺少 output Items。");
        }

        StringBuilder text = new StringBuilder();
        List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
        for (JsonNode item : output) {
            String type = item.path("type").asText();
            if (FUNCTION_CALL.equals(type)) {
                toolCalls.add(new AssistantMessage.ToolCall(
                        item.path("call_id").asText(item.path("id").asText()),
                        "function",
                        item.path("name").asText(),
                        item.path("arguments").asText("{}")));
            } else if ("message".equals(type)) {
                appendOutputText(text, item.path("content"));
            }
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(RAW_OUTPUT_ITEMS, output.toString());
        putIfText(properties, "responseId", response.path("id"));
        putIfText(properties, "returnedModel", response.path("model"));
        AssistantMessage message = AssistantMessage.builder()
                .content(text.toString())
                .properties(properties)
                .toolCalls(toolCalls)
                .build();
        JsonNode usage = response.path("usage");
        Long inputTokens = nullableLong(usage.get("input_tokens"));
        Long outputTokens = nullableLong(usage.get("output_tokens"));
        Long cachedInputTokens = nullableLong(
                usage.path("input_tokens_details").get("cached_tokens"));
        return new ModelCallResult(
                message,
                response.path("id").asText(null),
                response.path("model").asText(null),
                inputTokens,
                outputTokens,
                cachedInputTokens,
                cacheMiss(inputTokens, cachedInputTokens),
                null,
                nullableLong(usage.path("output_tokens_details").get("reasoning_tokens")),
                nullableLong(usage.get("total_tokens")) == null
                        ? sum(inputTokens, outputTokens)
                        : nullableLong(usage.get("total_tokens")));
    }

    private RestClient createClient(ConnectionKey connection) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofMinutes(3));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> headers.setBearerAuth(connection.apiKey()))
                .build();
    }

    private void addAssistant(ArrayNode input, AssistantMessage assistant) {
        Object rawItems = assistant.getMetadata().get(RAW_OUTPUT_ITEMS);
        if (rawItems instanceof String rawJson) {
            JsonNode parsed = readJson(rawJson);
            if (parsed.isArray()) {
                parsed.forEach(item -> input.add(item.deepCopy()));
                return;
            }
        }

        addMessage(input, "assistant", assistant.getText());
        for (AssistantMessage.ToolCall call : assistant.getToolCalls()) {
            ObjectNode item = input.addObject();
            item.put("type", FUNCTION_CALL);
            item.put("call_id", call.id());
            item.put("name", call.name());
            item.put("arguments", call.arguments());
        }
    }

    private void addUser(ArrayNode input, UserMessage user) {
        if (user.getMedia() == null || user.getMedia().isEmpty()) {
            addMessage(input, "user", user.getText());
            return;
        }
        ObjectNode item = input.addObject();
        item.put("role", "user");
        ArrayNode content = item.putArray("content");
        if (user.getText() != null && !user.getText().isBlank()) {
            content.addObject().put("type", "input_text").put("text", user.getText());
        }
        for (Media media : user.getMedia()) {
            ObjectNode image = content.addObject();
            image.put("type", "input_image");
            image.put("image_url", dataUrl(media));
        }
    }

    private String dataUrl(Media media) {
        if (!(media.getData() instanceof byte[] bytes)) {
            throw new IllegalArgumentException("Responses 图片必须使用二进制媒体数据。");
        }
        return "data:" + media.getMimeType() + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private void addToolResponses(
            ArrayNode input,
            ToolResponseMessage message) {
        for (ToolResponseMessage.ToolResponse response : message.getResponses()) {
            ObjectNode item = input.addObject();
            item.put("type", FUNCTION_CALL_OUTPUT);
            item.put("call_id", response.id());
            item.put("output", response.responseData());
        }
    }

    private void addMessage(ArrayNode input, String role, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        ObjectNode item = input.addObject();
        item.put("role", role);
        item.put("content", content);
    }

    private void appendInstruction(StringBuilder target, String instruction) {
        if (instruction == null || instruction.isBlank()) {
            return;
        }
        if (!target.isEmpty()) {
            target.append("\n\n");
        }
        target.append(instruction);
    }

    private void appendOutputText(StringBuilder target, JsonNode content) {
        if (!content.isArray()) {
            return;
        }
        for (JsonNode part : content) {
            if (!"output_text".equals(part.path("type").asText())) {
                continue;
            }
            String value = part.path("text").asText();
            if (!value.isBlank()) {
                if (!target.isEmpty()) {
                    target.append('\n');
                }
                target.append(value);
            }
        }
    }

    private JsonNode parseSchema(String schema) {
        JsonNode parsed = readJson(schema);
        return parsed.isObject() ? parsed : objectMapper.createObjectNode();
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalArgumentException("模型协议 JSON 格式无效。", exception);
        }
    }

    private void putIfText(Map<String, Object> properties, String key, JsonNode value) {
        if (value != null && value.isTextual() && !value.asText().isBlank()) {
            properties.put(key, value.asText());
        }
    }

    private Long nullableLong(JsonNode value) {
        return value == null || !value.isNumber() ? null : value.longValue();
    }

    private Long sum(Long first, Long second) {
        return first == null && second == null
                ? null
                : (first == null ? 0 : first) + (second == null ? 0 : second);
    }

    private Long cacheMiss(Long input, Long cached) {
        return input == null || cached == null ? null : Math.max(0, input - cached);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Responses 供应商缺少 Base URL。");
        }
        return baseUrl.trim().replaceAll("/+$", "");
    }

    private String normalizeEndpointPath(String endpointPath) {
        String path = endpointPath == null || endpointPath.isBlank()
                ? DEFAULT_ENDPOINT_PATH
                : endpointPath.trim();
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
                    + ", endpointPath=" + endpointPath
                    + ", apiKey=***]";
        }
    }
}
