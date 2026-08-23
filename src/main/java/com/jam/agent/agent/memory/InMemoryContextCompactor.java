package com.jam.agent.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/** 每次模型调用前压缩已完成的旧工具结果，同时保持 Tool Call/Result 协议配对。 */
@Component
public class InMemoryContextCompactor {

    private final TokenEstimator tokens;
    private final ObjectMapper objectMapper;
    private final Dispatcher events;

    public InMemoryContextCompactor(
            TokenEstimator tokens,
            ObjectMapper objectMapper,
            Dispatcher events) {
        this.tokens = tokens;
        this.objectMapper = objectMapper;
        this.events = events;
    }

    public void compactIfNeeded(
            List<Message> messages,
            List<ToolCallback> tools,
            AgentExecutionContext context,
            int attemptNo,
            int roundNo) {
        int allowedInput = context.budgetConfig().maxContextTokens()
                - context.budgetConfig().maxOutputTokens()
                - context.budgetConfig().safetyMarginTokens();
        int before = tokens.estimate(messages, tools);
        if (!context.memoryConfig().compactionEnabled() || before <= allowedInput) return;

        int compacted = 0;
        for (int index = 0; index < messages.size() && tokens.estimate(messages, tools) > allowedInput; index++) {
            if (!(messages.get(index) instanceof ToolResponseMessage toolMessage)) continue;
            List<ToolResponseMessage.ToolResponse> responses = toolMessage.getResponses().stream()
                    .map(response -> compact(response, context))
                    .toList();
            messages.set(index, ToolResponseMessage.builder().responses(responses).build());
            compacted++;
        }
        int after = tokens.estimate(messages, tools);
        if (compacted > 0) {
            events.lifecycle(context, attemptNo, roundNo,
                    "context_compacted:messages=" + compacted
                            + ",tokensBefore=" + before + ",tokensAfter=" + after);
        }
    }

    private ToolResponseMessage.ToolResponse compact(
            ToolResponseMessage.ToolResponse response,
            AgentExecutionContext context) {
        String content = response.responseData();
        if (content == null || content.contains("\"_compacted\":true")) return response;
        int previewLength = Math.min(
                content.length(), context.memoryConfig().compactedToolPreviewChars());
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("_compacted", true);
        value.put("turnId", context.turnId());
        value.put("handle", response.id());
        value.put("preview", content.substring(0, previewLength));
        value.put("lookupHint", "Use query_conversation_node with turnId and handle for the full node content");
        try {
            content = objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            content = "{\"_compacted\":true,\"turnId\":" + context.turnId()
                    + ",\"handle\":\"" + response.id() + "\"}";
        }
        return new ToolResponseMessage.ToolResponse(response.id(), response.name(), content);
    }
}
