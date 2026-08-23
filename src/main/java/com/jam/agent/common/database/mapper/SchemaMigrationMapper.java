package com.jam.agent.common.database.mapper;

public interface SchemaMigrationMapper {

    int countAdminColumn();

    int countAdminAuditTable();

    void createAdminAuditTable();

    void addAdminColumn();

    int countConversationAgentKeyColumn();

    void addConversationAgentKeyColumn();

    int countAgentConfigTable();

    int countAgentConfigAdminOnlyColumn();

    void addAgentConfigAdminOnlyColumn();

    int countModelProviderConfigTable();

    void createModelProviderConfigTable();

    void insertBuiltInModelProviders();

    int countModelProviderCatalogColumn();

    int countModelProviderEndpointColumn();

    int countModelProviderLegacyPathColumn();

    void renameModelProviderEndpointColumn();

    void addModelProviderEndpointColumn();

    void addModelProviderCatalogColumn();

    void updateDefaultProviderCatalog();

    void updateBuiltInProviderProtocols();

    void createAgentConfigTable();

    int countAgentConfigEnabledToolsColumn();

    void addAgentConfigEnabledToolsColumn();

    int countAgentConfigExecutionTypeColumn();

    void addAgentConfigExecutionColumns();

    int countAgentConfigModelProviderColumn();

    void addAgentConfigModelColumns();

    int countConversationModelProviderColumn();

    void addConversationModelColumns();

    int countConversationTurnModelColumn();

    void addConversationTurnModelColumns();

    int countAgentConfigImageHistoryModeColumn();

    void addAgentConfigImageHistoryModeColumn();

    void updateDefaultAgentImageHistoryMode();

    int countConversationNodeAttachmentColumn();

    void addConversationNodeAttachmentColumn();

    int countMediaAssetTable();

    void createMediaAssetTable();

    int countTurnAttachmentTable();

    void createTurnAttachmentTable();

    void updateBuiltInAgentTools();

    void removeUserSessionDetailTool();

    void removeConversationNodeTool();

    int countConversationNodeOutputTable();

    void createConversationNodeOutputTable();

    int countConversationMemorySummaryTable();

    void createConversationMemorySummaryTable();

    void appendToolToBuiltInAgents(String toolName);
}
