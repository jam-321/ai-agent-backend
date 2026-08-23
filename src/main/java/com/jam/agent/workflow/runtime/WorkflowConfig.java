package com.jam.agent.workflow.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.agent.config.AgentProperties;

/** Resolves the bounded workflow safety budget from an Agent recipe. */
public record WorkflowConfig(int maxSteps) {

    public static WorkflowConfig resolve(
            AgentProperties.Workflow defaults,
            String magicParams,
            ObjectMapper objectMapper) {
        JsonNode workflow = readWorkflowNode(magicParams, objectMapper);
        int configured = defaults.getMaxSteps();
        if (workflow != null
                && workflow.has("maxSteps")
                && workflow.get("maxSteps").canConvertToInt()) {
            configured = workflow.get("maxSteps").asInt();
        }
        return new WorkflowConfig(Math.max(1, Math.min(configured, defaults.getMaxSteps())));
    }

    private static JsonNode readWorkflowNode(String magicParams, ObjectMapper objectMapper) {
        if (magicParams == null || magicParams.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(magicParams);
            if (root == null || !root.isObject()) {
                return null;
            }
            JsonNode workflow = root.get("workflow");
            return workflow != null && workflow.isObject() ? workflow : null;
        } catch (Exception ignored) {
            // 工作流配置错误时回退到全局安全上限，不能阻塞其他 Agent。
            return null;
        }
    }
}
