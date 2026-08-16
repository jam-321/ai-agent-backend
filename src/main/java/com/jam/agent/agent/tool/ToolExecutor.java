package com.jam.agent.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.event.EventPublisher;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutor {
    private final Map<String, ToolCallback> callbacks = new LinkedHashMap<>();
    private final EventPublisher events;
    private final ObjectMapper objectMapper;
    public ToolExecutor(BuiltinTools builtinTools, EventPublisher events, ObjectMapper objectMapper) {
        this.events = events; this.objectMapper = objectMapper;
        for (ToolCallback callback : org.springframework.ai.tool.method.MethodToolCallbackProvider.builder().toolObjects(builtinTools).build().getToolCallbacks()) {
            callbacks.put(callback.getToolDefinition().name(), callback);
        }
    }
    public ToolResult execute(AgentExecutionContext context, int attemptNo, int roundNo, int callIndex, AssistantMessage.ToolCall call) {
        ToolCallback callback = callbacks.get(call.name());
        String result;
        boolean error = false;
        try {
            if (callback == null) throw new IllegalArgumentException("未知工具：" + call.name());
            Map<String,Object> values = Map.of("executionContext", context, "userId", context.userId(), "conversationId", context.conversationId(),
                    "turnId", context.turnId(), "traceId", context.traceId(), "attemptNo", attemptNo, "roundNo", roundNo, "toolCallId", call.id());
            result = callback.call(call.arguments() == null || call.arguments().isBlank() ? "{}" : call.arguments(), new ToolContext(values));
            if (result == null || result.isBlank()) result = "{\"success\":true,\"result\":null}";
        } catch (Exception ex) {
            error = true;
            try { result = objectMapper.writeValueAsString(Map.of("success", false, "is_error", true, "error", safeMessage(ex))); }
            catch (Exception ignored) { result = "{\"success\":false,\"is_error\":true,\"error\":\"工具执行失败\"}"; }
        }
        events.toolEnd(context, attemptNo, roundNo, callIndex, call.name(), call.id(), result, error);
        return new ToolResult(call.id(), call.name(), result, error);
    }
    private String safeMessage(Exception ex) { String message=ex.getMessage(); if(message==null || message.isBlank()) return "工具执行失败"; return message.length()>500 ? message.substring(0,500) : message; }
    public Map<String, ToolCallback> callbacks() { return Map.copyOf(callbacks); }
    public record ToolResult(String id, String name, String responseData, boolean error) {}
}
