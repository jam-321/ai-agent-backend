package com.jam.agent.agent.tool.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.auth.persistence.repository.UserRepository;
import com.jam.agent.monitoring.service.AdminMonitorService;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Tools reserved for administrator-only Agent recipes. */
@Component
public class AdminConversationTools implements AgentToolProvider {

    private final AdminMonitorService monitor;
    private final UserRepository users;
    private final ObjectMapper objectMapper;

    public AdminConversationTools(
            AdminMonitorService monitor,
            UserRepository users,
            ObjectMapper objectMapper) {
        this.monitor = monitor;
        this.users = users;
        this.objectMapper = objectMapper;
    }

    @Tool(
            name = "query_admin_session_detail",
            description = "仅管理员 Agent 可用的只读会话执行分析工具。提供 conversationId 查询整段会话；提供 conversationId 和 turnId 查询指定轮次；提供 traceId 查询一次执行。至少提供一种定位条件，不要编造 ID。")
    public String queryAdminSessionDetail(
            @ToolParam(description = "会话 ID；查询整段会话或与 turnId 一起查询指定轮次", required = false)
            Long conversationId,
            @ToolParam(description = "会话内轮次 ID；必须和 conversationId 一起提供", required = false)
            Integer turnId,
            @ToolParam(description = "一次 Agent 执行的 traceId；可以单独提供", required = false)
            String traceId,
            ToolContext toolContext) throws Exception {
        AgentExecutionContext context = requireAdminContext(toolContext);
        if (conversationId == null && turnId == null && (traceId == null || traceId.isBlank())) {
            throw new IllegalArgumentException("conversationId、turnId 或 traceId 至少提供一种定位条件。");
        }
        return objectMapper.writeValueAsString(monitor.detail(conversationId, turnId, traceId));
    }

    private AgentExecutionContext requireAdminContext(ToolContext toolContext) {
        Object value = toolContext == null
                ? null
                : toolContext.getContext().get("executionContext");
        if (!(value instanceof AgentExecutionContext context)) {
            throw new IllegalArgumentException("工具上下文无效。");
        }
        boolean admin = users.findById(context.userId())
                .map(UserRepository.UserRecord::admin)
                .orElse(false);
        if (!admin || !context.agentConfig().adminOnly()) {
            throw new IllegalArgumentException("当前 Agent 没有管理员会话分析权限。");
        }
        return context;
    }
}
