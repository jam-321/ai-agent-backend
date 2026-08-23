package com.jam.agent.agent.config;

import com.jam.agent.agent.persistence.repository.AgentConfigRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Seeds the built-in Agent recipes and keeps the reserved admin Agent protected. */
@Component
@Order(Integer.MAX_VALUE)
public class AgentConfigInitializer implements ApplicationRunner {

    private static final String BUILTIN_TOOLS =
            "[\"current_time\",\"calculate\",\"query_conversation_node\",\"query_tool_output\",\"query_image_summary\"]";
    private static final String ADMIN_TOOLS = "[\"query_admin_session_detail\"]";

    private static final String GENERAL_PROMPT = """
            你是一个友好、专业的中文 AI Agent。
            需要准确时间时调用 current_time，需要精确算术时调用 calculate，需要查询历史工具节点时调用 query_conversation_node，被压缩的大结果按提示调用 query_tool_output；当用户询问历史图片时调用 query_image_summary。不要编造工具结果。
            """;

    private static final String ADMIN_PROMPT = """
            你是系统管理 Agent，只为管理员服务。
            你可以分析系统中用户会话的执行详情。需要排查某个会话或某次执行时，调用 query_admin_session_detail，并根据用户提供的 conversationId、turnId 或 traceId 查询，不要编造数据库信息。
            """;

    private final AgentConfigRepository repository;

    public AgentConfigInitializer(AgentConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.findByKey("general").isEmpty()) {
            repository.insert("general", GENERAL_PROMPT, "[]", BUILTIN_TOOLS, "{}");
            repository.findEntityByKey("general").ifPresent(entity -> {
                entity.setImageHistoryMode("FULL_IMAGE_HISTORY");
                repository.save(entity);
            });
        }
        if (repository.findByKey("with_time").isEmpty()) {
            repository.insert("with_time", GENERAL_PROMPT, "[\"time_inject\"]", BUILTIN_TOOLS, "{}");
        }
        if (repository.findByKey("time_workflow").isEmpty()) {
            repository.insert(
                    "time_workflow",
                    GENERAL_PROMPT,
                    "[]",
                    "[\"current_time\"]",
                    "{\"workflow\":{\"maxSteps\":8}}",
                    "WORKFLOW",
                    "time_report");
        }
        ensureSystemAdminAgent();
    }

    private void ensureSystemAdminAgent() {
        if (repository.findByKey("system_admin").isEmpty()) {
            repository.insert(
                    "system_admin",
                    ADMIN_PROMPT,
                    "[]",
                    ADMIN_TOOLS,
                    "{}",
                    "LOOP",
                    null);
        }
        repository.findEntityByKey("system_admin").ifPresent(entity -> {
            // system_admin 是保留 Agent，不能因为配置历史或手工 SQL 被变成普通 Agent。
            if (!Boolean.TRUE.equals(entity.getAdminOnly())) {
                entity.setAdminOnly(true);
                repository.save(entity);
            }
        });
    }
}
