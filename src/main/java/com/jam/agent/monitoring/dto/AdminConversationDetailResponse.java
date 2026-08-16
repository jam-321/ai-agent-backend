package com.jam.agent.monitoring.dto;

import java.util.List;

public record AdminConversationDetailResponse(
        AdminConversationSummaryResponse conversation,
        List<AdminTurnResponse> turns,
        List<AdminNodeResponse> nodes) {
}
