package com.jam.agent.common.database;

import com.jam.agent.common.database.mapper.SchemaMigrationMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 为历史数据库补齐新增表和字段；全新数据库由 schema.sql 创建相同结构。
 *
 * <p>这里的检查必须保持幂等，确保不同历史版本都能重复启动。
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
        if (mapper.countAdminAuditTable() == 0) {
            mapper.createAdminAuditTable();
        }
        if (mapper.countConversationAgentKeyColumn() == 0) {
            mapper.addConversationAgentKeyColumn();
        }
        if (mapper.countModelProviderConfigTable() == 0) {
            mapper.createModelProviderConfigTable();
        }
        if (mapper.countModelProviderEndpointColumn() == 0) {
            if (mapper.countModelProviderLegacyPathColumn() > 0) {
                mapper.renameModelProviderEndpointColumn();
            } else {
                mapper.addModelProviderEndpointColumn();
            }
        }
        if (mapper.countModelProviderCatalogColumn() == 0) {
            mapper.addModelProviderCatalogColumn();
        }
        // 先创建内置供应商，旧 agent_config 增加非空外键列时才能安全迁移。
        mapper.insertBuiltInModelProviders();
        mapper.updateDefaultProviderCatalog();
        mapper.updateBuiltInProviderProtocols();
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
        if (mapper.countConversationModelProviderColumn() == 0) {
            mapper.addConversationModelColumns();
        }
        if (mapper.countConversationTurnModelColumn() == 0) {
            mapper.addConversationTurnModelColumns();
        }
    }
}
