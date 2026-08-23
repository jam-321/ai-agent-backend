package com.jam.agent.agent.config;

import com.jam.agent.agent.config.AdminConfigOptionsResponse.BudgetDefaults;
import com.jam.agent.agent.config.AdminConfigOptionsResponse.LoopDefaults;
import com.jam.agent.agent.config.AdminConfigOptionsResponse.MemoryDefaults;
import com.jam.agent.agent.config.AdminConfigOptionsResponse.PluginOption;
import com.jam.agent.agent.config.AdminConfigOptionsResponse.ToolOption;
import com.jam.agent.agent.config.AdminConfigOptionsResponse.WorkflowDefaults;
import com.jam.agent.agent.event.EventRegistry;
import com.jam.agent.agent.model.protocol.ModelProtocolRegistry;
import com.jam.agent.agent.runtime.AgentExecutorRegistry;
import com.jam.agent.agent.tool.registry.ToolRegistry;
import com.jam.agent.workflow.registry.WorkflowRegistry;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 为管理员配置页提供后端实际支持的选项，避免前端维护易漂移的硬编码列表。 */
@RestController
@RequestMapping("/api/admin/config-options")
public class AdminConfigOptionsController {

    private final AgentExecutorRegistry executors;
    private final WorkflowRegistry workflows;
    private final ModelProtocolRegistry protocols;
    private final ToolRegistry tools;
    private final EventRegistry plugins;
    private final AgentProperties properties;

    public AdminConfigOptionsController(
            AgentExecutorRegistry executors,
            WorkflowRegistry workflows,
            ModelProtocolRegistry protocols,
            ToolRegistry tools,
            EventRegistry plugins,
            AgentProperties properties) {
        this.executors = executors;
        this.workflows = workflows;
        this.protocols = protocols;
        this.tools = tools;
        this.plugins = plugins;
        this.properties = properties;
    }

    @GetMapping
    public AdminConfigOptionsResponse options() {
        AgentProperties.Loop loop = properties.getLoop();
        AgentProperties.Budget budget = properties.getBudget();
        AgentProperties.Memory memory = properties.getMemory();
        AgentProperties.Workflow workflow = properties.getWorkflow();
        return new AdminConfigOptionsResponse(
                executors.executionTypes(),
                workflows.keys(),
                protocols.protocolTypes(),
                List.of("SUMMARY_TOOL", "FULL_IMAGE_HISTORY"),
                tools.callbacks().values().stream()
                        .map(callback -> new ToolOption(
                                callback.getToolDefinition().name(),
                                callback.getToolDefinition().description()))
                        .sorted(java.util.Comparator.comparing(ToolOption::name))
                        .toList(),
                plugins.allPlugins().stream()
                        .map(entry -> new PluginOption(entry.id(), entry.system()))
                        .toList(),
                new LoopDefaults(
                        loop.getMaxAttempts(), loop.getMaxToolRounds(), loop.getMaxToolsPerRound(),
                        loop.getMaxRunDuration().toSeconds(), loop.getMaxDegenerateRetries(),
                        loop.getMaxSameToolSignature()),
                new BudgetDefaults(
                        budget.getMaxTokensPerTurn(), budget.getMaxContextTokens(),
                        budget.getMaxOutputTokens(), budget.getSafetyMarginTokens(),
                        budget.getMaxUserInputTokens()),
                new MemoryDefaults(
                        memory.isCompactionEnabled(), memory.getCompactionTriggerTokens(),
                        memory.getKeepRecentTokens(), memory.getMaxToolResultTokens(),
                        memory.getCompactedToolPreviewChars(), memory.getMaxToolPairsPerTurn()),
                new WorkflowDefaults(workflow.getMaxSteps()));
    }
}
