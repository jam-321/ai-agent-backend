package com.jam.agent.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.persistence.repository.ConversationNodeOutputRepository;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 归档超大工具结果，并向模型返回可按句柄回查的紧凑占位。 */
@Service
public class ToolResultCompactionService {

    private final TokenEstimator tokens;
    private final ConversationNodeOutputRepository outputs;
    private final ObjectMapper objectMapper;

    public ToolResultCompactionService(
            TokenEstimator tokens,
            ConversationNodeOutputRepository outputs,
            ObjectMapper objectMapper) {
        this.tokens = tokens;
        this.outputs = outputs;
        this.objectMapper = objectMapper;
    }

    public CompactedResult compact(
            AgentExecutionContext context,
            String toolName,
            String toolCallId,
            String result) {
        int contentTokens = tokens.estimateText(result);
        if (!context.memoryConfig().compactionEnabled()
                || contentTokens < context.memoryConfig().maxToolResultTokens()) {
            return new CompactedResult(result, false, contentTokens);
        }

        outputs.archive(
                context.conversationId(),
                context.turnId(),
                toolName,
                toolCallId,
                context.traceId(),
                result,
                contentTokens);
        int previewLength = Math.min(
                result.length(), context.memoryConfig().compactedToolPreviewChars());
        Map<String, Object> placeholder = new LinkedHashMap<>();
        placeholder.put("_compacted", true);
        placeholder.put("tool", toolName);
        placeholder.put("turnId", context.turnId());
        placeholder.put("handle", toolCallId);
        placeholder.put("originalTokens", contentTokens);
        placeholder.put("preview", result.substring(0, previewLength));
        placeholder.put("lookupHint", "Use query_tool_output with turnId and handle for the archived result");
        try {
            return new CompactedResult(objectMapper.writeValueAsString(placeholder), true, contentTokens);
        } catch (Exception exception) {
            return new CompactedResult(
                    "{\"_compacted\":true,\"turnId\":" + context.turnId()
                            + ",\"handle\":\"" + toolCallId + "\"}",
                    true,
                    contentTokens);
        }
    }

    public record CompactedResult(String modelContent, boolean compacted, int originalTokens) {
    }
}
