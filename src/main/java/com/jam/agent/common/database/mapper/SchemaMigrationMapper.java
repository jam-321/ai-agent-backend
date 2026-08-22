package com.jam.agent.common.database.mapper;

import org.apache.ibatis.annotations.Param;

public interface SchemaMigrationMapper {

    int countAdminColumn();

    void addAdminColumn();

    int countConversationAgentKeyColumn();

    void addConversationAgentKeyColumn();

    int countAgentConfigTable();

    int countModelProviderConfigTable();

    void createModelProviderConfigTable();

    void insertDefaultModelProvider();

    void createAgentConfigTable();

    int countAgentConfigEnabledToolsColumn();

    void addAgentConfigEnabledToolsColumn();

    int countAgentConfigExecutionTypeColumn();

    void addAgentConfigExecutionColumns();

    int countAgentConfigModelProviderColumn();

    void addAgentConfigModelColumns();
}
