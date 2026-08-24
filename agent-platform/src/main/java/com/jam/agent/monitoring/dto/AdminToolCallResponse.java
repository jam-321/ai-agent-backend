package com.jam.agent.monitoring.dto;

import java.util.List;

public record AdminToolCallResponse(
        String aggrKey,
        String toolName,
        String status,
        String input,
        String output,
        List<AdminNodeResponse> events) {
}
