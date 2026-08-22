package com.jam.agent.agent.config;

import com.jam.agent.agent.persistence.repository.AgentConfigRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Seeds a small set of editable Agent recipes on a new database. */
@Component
@Order(Integer.MAX_VALUE)
public class AgentConfigInitializer implements ApplicationRunner {

    private static final String ALL_BUILTIN_TOOLS =
            "[\"current_time\",\"calculate\",\"query_conversation_node\",\"query_image_summary\"]";

    private static final String GENERAL_PROMPT = """
            你是一个友好、专业的中文 AI Agent。
            需要准确时间时调用 current_time，需要精确算术时调用 calculate，需要查询历史工具完整数据时调用 query_conversation_node；当用户询问历史图片时调用 query_image_summary。不要编造工具结果。
            """;

    private final AgentConfigRepository repository;

    public AgentConfigInitializer(AgentConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.findByKey("general").isEmpty()) {
            repository.insert("general", GENERAL_PROMPT, "[]", ALL_BUILTIN_TOOLS, "{}");
            repository.findEntityByKey("general").ifPresent(entity -> {
                entity.setImageHistoryMode("FULL_IMAGE_HISTORY");
                repository.save(entity);
            });
        }
        if (repository.findByKey("with_time").isEmpty()) {
            repository.insert("with_time", GENERAL_PROMPT, "[\"time_inject\"]", ALL_BUILTIN_TOOLS, "{}");
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
    }
}
