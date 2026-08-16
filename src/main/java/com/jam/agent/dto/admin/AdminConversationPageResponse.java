package com.jam.agent.dto.admin;

import java.util.List;

public record AdminConversationPageResponse(
        List<AdminConversationSummaryResponse> items,
        int page,
        int size,
        long total) {
}
