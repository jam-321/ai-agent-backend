package com.jam.agent.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.memory.ToolResultCompactionService;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.tool.registry.ToolRegistry;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/** Single execution boundary for tool lookup, context injection, errors, and events. */
@Component
public class ToolExecutor {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final ToolRegistry registry;
    private final Dispatcher events;
    private final ObjectMapper objectMapper;
    private final ToolResultCompactionService compaction;

    public ToolExecutor(
            ToolRegistry registry,
            Dispatcher events,
            ObjectMapper objectMapper,
            ToolResultCompactionService compaction) {
        this.registry = registry;
        this.events = events;
        this.objectMapper = objectMapper;
        this.compaction = compaction;
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
            ToolCallback callback = requireCallback(context, call.name());
            result = callback.call(normalizeArguments(call.arguments()), buildToolContext(context, attemptNo, roundNo, call));
            if (result == null || result.isBlank()) {
                result = "{\"success\":true,\"result\":null}";
            }
        } catch (Exception exception) {
            error = true;
            result = errorResult(exception);
        }

        String modelResult = result;
        if (!error) {
            try {
                modelResult = compaction.compact(context, call.name(), call.id(), result).modelContent();
            } catch (Exception exception) {
                // 压缩属于保护能力；归档失败时继续回填原始结果，不能把成功工具变成失败。
                modelResult = result;
            }
        }

        // Node 保存实际回填给模型的内容；超大原文由 conversation_node_output 单独归档。
        events.toolEnd(context, attemptNo, roundNo, callIndex, call.name(), call.id(), modelResult, error);
        return new ToolResult(call.id(), call.name(), modelResult, error);
    }

    private ToolCallback requireCallback(AgentExecutionContext context, String name) {
        if (!context.agentConfig().isToolEnabled(name)) {
            throw new IllegalArgumentException("当前 Agent 未启用工具：" + name);
        }
        return registry.require(name);
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
        return registry.callbacks();
    }

    public record ToolResult(String id, String name, String responseData, boolean error) {
    }
}
