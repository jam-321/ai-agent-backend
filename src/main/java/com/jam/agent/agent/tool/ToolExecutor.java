package com.jam.agent.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.event.EventPublisher;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

/** Single execution boundary for tool lookup, context injection, errors, and events. */
@Component
public class ToolExecutor {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final Map<String, ToolCallback> callbacks = new LinkedHashMap<>();
    private final EventPublisher events;
    private final ObjectMapper objectMapper;

    public ToolExecutor(BuiltinTools builtinTools, EventPublisher events, ObjectMapper objectMapper) {
        this.events = events;
        this.objectMapper = objectMapper;

        ToolCallback[] builtins = MethodToolCallbackProvider.builder()
                .toolObjects(builtinTools)
                .build()
                .getToolCallbacks();
        for (ToolCallback callback : builtins) {
            callbacks.put(callback.getToolDefinition().name(), callback);
        }
    }

    public ToolResult execute(
            AgentExecutionContext context,
            int attemptNo,
            int roundNo,
            int callIndex,
            AssistantMessage.ToolCall call) {
        String result;
        boolean error = false;

        try {
            ToolCallback callback = requireCallback(call.name());
            result = callback.call(normalizeArguments(call.arguments()), buildToolContext(context, attemptNo, roundNo, call));
            if (result == null || result.isBlank()) {
                result = "{\"success\":true,\"result\":null}";
            }
        } catch (Exception exception) {
            error = true;
            result = errorResult(exception);
        }

        // Persist the terminal event on this synchronous path before the result returns to the loop.
        events.toolEnd(context, attemptNo, roundNo, callIndex, call.name(), call.id(), result, error);
        return new ToolResult(call.id(), call.name(), result, error);
    }

    private ToolCallback requireCallback(String name) {
        ToolCallback callback = callbacks.get(name);
        if (callback == null) {
            throw new IllegalArgumentException("未知工具：" + name);
        }
        return callback;
    }

    private String normalizeArguments(String arguments) {
        return arguments == null || arguments.isBlank() ? "{}" : arguments;
    }

    private ToolContext buildToolContext(
            AgentExecutionContext context,
            int attemptNo,
            int roundNo,
            AssistantMessage.ToolCall call) {
        Map<String, Object> values = Map.of(
                "executionContext", context,
                "userId", context.userId(),
                "conversationId", context.conversationId(),
                "turnId", context.turnId(),
                "traceId", context.traceId(),
                "attemptNo", attemptNo,
                "roundNo", roundNo,
                "toolCallId", call.id());
        return new ToolContext(values);
    }

    private String errorResult(Exception exception) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "success", false,
                    "is_error", true,
                    "error", safeMessage(exception)));
        } catch (Exception ignored) {
            return "{\"success\":false,\"is_error\":true,\"error\":\"工具执行失败\"}";
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "工具执行失败";
        }
        if (message.length() > MAX_ERROR_MESSAGE_LENGTH) {
            return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
        }
        return message;
    }

    public Map<String, ToolCallback> callbacks() {
        return Map.copyOf(callbacks);
    }

    public record ToolResult(String id, String name, String responseData, boolean error) {
    }
}
