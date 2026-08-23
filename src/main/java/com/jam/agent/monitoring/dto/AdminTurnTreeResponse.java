package com.jam.agent.monitoring.dto;

import java.util.List;

public record AdminTurnTreeResponse(
        int turnId,
        AdminTurnResponse userTurn,
        AdminTurnResponse assistantTurn,
        List<AdminToolCallResponse> toolCalls,
        AdminTokenUsageResponse tokenUsage) {
}
