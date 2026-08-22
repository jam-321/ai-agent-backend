package com.jam.agent.agent.tool.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.persistence.repository.ConversationNodeRepository;
import com.jam.agent.agent.persistence.repository.ConversationTurnAttachmentRepository;
import com.jam.agent.agent.persistence.repository.MediaAssetRepository;
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
    private final ConversationTurnAttachmentRepository attachments;
    private final MediaAssetRepository assets;

    public ConversationTools(
            ConversationNodeRepository nodes,
            ObjectMapper objectMapper,
            ConversationTurnAttachmentRepository attachments,
            MediaAssetRepository assets) {
        this.nodes = nodes;
        this.objectMapper = objectMapper;
        this.attachments = attachments;
        this.assets = assets;
    }

    @Tool(
            name = "query_image_summary",
            description = "查询当前会话历史图片的摘要。当用户询问之前发送的截图、图片细节且上下文没有图片内容时调用。")
    public String queryImageSummary(
            @ToolParam(description = "要查询的历史轮次；不传则查询全部历史图片", required = false) Integer targetTurnId,
            ToolContext toolContext) throws Exception {
        AgentExecutionContext context = requireExecutionContext(toolContext);
        if (targetTurnId != null && (targetTurnId < 1 || targetTurnId >= context.turnId())) {
            throw new IllegalArgumentException("只能查询更早轮次的图片摘要。");
        }
        List<ConversationTurnAttachmentRepository.AttachmentRecord> rows = attachments.findHistory(
                context.userId(), context.conversationId(), targetTurnId == null ? context.turnId() : targetTurnId + 1);
        return objectMapper.writeValueAsString(rows.stream()
                .filter(row -> targetTurnId == null || row.turnId() == targetTurnId)
                .map(row -> assets.findOwned(context.userId(), row.assetId())
                        .map(asset -> {
                            Map<String, Object> value = new LinkedHashMap<>();
                            value.put("turnId", row.turnId());
                            value.put("attachmentId", row.assetId());
                            value.put("status", asset.getSummary() == null ? "PROCESSING" : "SUCCESS");
                            value.put("summary", asset.getSummary());
                            return value;
                        }).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList());
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
