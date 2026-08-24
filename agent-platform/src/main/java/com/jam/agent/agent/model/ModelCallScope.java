package com.jam.agent.agent.model;

/** 标识一次模型调用在执行轨迹中的位置和用途。 */
public record ModelCallScope(
        String purpose,
        int attemptNo,
        Integer roundNo,
        boolean countAgainstTurnBudget) {

    public static ModelCallScope agentRound(int attemptNo, int roundNo) {
        return new ModelCallScope("AGENT_ROUND", attemptNo, roundNo, true);
    }

    public static ModelCallScope workflowStep(int stepNo) {
        return new ModelCallScope("WORKFLOW_STEP", 1, stepNo, true);
    }

    public static ModelCallScope conversationCompaction() {
        return new ModelCallScope("CONVERSATION_COMPACTION", 1, 0, true);
    }

    public static ModelCallScope imageSummary() {
        return new ModelCallScope("IMAGE_SUMMARY", 1, null, false);
    }
}
