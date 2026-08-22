package com.jam.agent.common.database;

import com.jam.agent.common.database.mapper.SchemaMigrationMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Applies the one compatibility migration needed by databases created before admin support.
 * New installations receive the same column from schema.sql.
 */
@Component
@Order(Integer.MIN_VALUE)
public class DatabaseSchemaMigration implements ApplicationRunner {

    private final SchemaMigrationMapper mapper;

    public DatabaseSchemaMigration(SchemaMigrationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (mapper.countAdminColumn() == 0) {
            mapper.addAdminColumn();
        }
        if (mapper.countConversationAgentKeyColumn() == 0) {
            mapper.addConversationAgentKeyColumn();
        }
        if (mapper.countModelProviderConfigTable() == 0) {
            mapper.createModelProviderConfigTable();
        }
        // 先创建默认供应商，旧 agent_config 增加非空外键列时才能安全迁移。
        mapper.insertDefaultModelProvider();
        if (mapper.countAgentConfigTable() == 0) {
            mapper.createAgentConfigTable();
        }
        if (mapper.countAgentConfigEnabledToolsColumn() == 0) {
            mapper.addAgentConfigEnabledToolsColumn();
        }
        if (mapper.countAgentConfigExecutionTypeColumn() == 0) {
            mapper.addAgentConfigExecutionColumns();
        }
        if (mapper.countAgentConfigModelProviderColumn() == 0) {
            mapper.addAgentConfigModelColumns();
        }
    }
}
