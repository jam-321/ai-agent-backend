package com.jam.agent.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProgressNodeResponse(String structureType, Long dbId, String aggrKey, String nodeId, String nodeName,
                                   String nodeType, String nodeStatus, String content, Boolean truncated,
                                   LocalDateTime createdTime, LocalDateTime updatedTime,
                                   List<ProgressNodeResponse> nodeList) {}
