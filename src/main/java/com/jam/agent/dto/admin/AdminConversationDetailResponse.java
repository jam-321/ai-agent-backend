package com.jam.agent.dto.admin;

import java.util.List;

public record AdminConversationDetailResponse(
        AdminConversationSummaryResponse conversation,
        List<AdminTurnResponse> turns,
        List<AdminNodeResponse> nodes) {
}
