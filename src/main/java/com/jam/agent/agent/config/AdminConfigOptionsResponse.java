package com.jam.agent.agent.config;

import java.util.List;

/** 管理端结构化配置表单所需的运行时选项和全局安全上限。 */
public record AdminConfigOptionsResponse(
        List<String> executionTypes,
        List<String> workflowKeys,
        List<String> protocolTypes,
        List<String> imageHistoryModes,
        List<ToolOption> tools,
        List<PluginOption> plugins,
        LoopDefaults loopDefaults,
        BudgetDefaults budgetDefaults,
        MemoryDefaults memoryDefaults,
        WorkflowDefaults workflowDefaults) {

    public record ToolOption(String name, String description) {
    }

    public record PluginOption(String id, boolean system) {
    }

    public record LoopDefaults(
            int maxAttempts,
            int maxToolRounds,
            int maxToolsPerRound,
            long maxRunDurationSeconds,
            int maxDegenerateRetries,
            int maxSameToolSignature) {
    }

    public record BudgetDefaults(
            long maxTokensPerTurn,
            int maxContextTokens,
            int maxOutputTokens,
            int safetyMarginTokens) {
    }

    public record MemoryDefaults(
            boolean compactionEnabled,
            int compactionTriggerTokens,
            int keepRecentTokens,
            int maxToolResultTokens,
            int compactedToolPreviewChars) {
    }

    public record WorkflowDefaults(int maxSteps) {
    }
}
