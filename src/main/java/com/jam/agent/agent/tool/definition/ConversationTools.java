package com.jam.agent.agent.tool.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.persistence.repository.ConversationNodeRepository;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ConversationTools implements AgentToolProvider {

    private final ConversationNodeRepository nodes;
    private final ObjectMapper objectMapper;

    public ConversationTools(
            ConversationNodeRepository nodes,
            ObjectMapper objectMapper) {
        this.nodes = nodes;
        this.objectMapper = objectMapper;
    }

    @Tool(
            name = "query_conversation_node",
            description = "查询当前会话历史某轮工具调用的完整入参和结果。只能查询当前会话且目标轮次必须早于当前轮次。")
    public String queryConversationNode(
            @ToolParam(description = "目标历史轮次") Integer targetTurnId,
            @ToolParam(description = "工具调用 ID") String aggrKey,
            ToolContext toolContext) throws Exception {
        AgentExecutionContext context = requireExecutionContext(toolContext);
        validateHistoryLookup(context, targetTurnId, aggrKey);

        List<ConversationNodeRepository.NodeRecord> records = nodes.findToolNodes(
                context.userId(),
                context.conversationId(),
                targetTurnId,
                aggrKey);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("未找到对应的历史工具调用。");
        }

        return objectMapper.writeValueAsString(records.stream().map(node -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("status", node.status());
            value.put("tool", node.nodeName());
            value.put("content", node.content());
            return value;
        }).toList());
    }

    private AgentExecutionContext requireExecutionContext(ToolContext toolContext) {
        Object value = toolContext == null
                ? null
                : toolContext.getContext().get("executionContext");
        if (!(value instanceof AgentExecutionContext context)) {
            throw new IllegalArgumentException("工具上下文无效。");
        }
        return context;
    }

    private void validateHistoryLookup(
            AgentExecutionContext context,
            Integer targetTurnId,
            String aggrKey) {
        if (targetTurnId == null
                || targetTurnId < 1
                || targetTurnId >= context.turnId()
                || aggrKey == null
                || aggrKey.isBlank()) {
            throw new IllegalArgumentException("只能查询当前会话中更早轮次的有效工具调用。");
        }
    }
}
