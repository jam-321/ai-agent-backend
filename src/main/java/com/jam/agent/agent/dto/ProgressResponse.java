package com.jam.agent.agent.dto;

import java.util.List;

public record ProgressResponse(Long conversationId, Integer turnId, String traceId, String turnStatus,
                               String finalAnswer, String errorMessage, List<ProgressNodeResponse> nodeList) {}
