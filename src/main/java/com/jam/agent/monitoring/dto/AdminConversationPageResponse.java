package com.jam.agent.monitoring.dto;

import java.util.List;

public record AdminConversationPageResponse(
        List<AdminConversationSummaryResponse> items,
        int page,
        int size,
        long total) {
}
