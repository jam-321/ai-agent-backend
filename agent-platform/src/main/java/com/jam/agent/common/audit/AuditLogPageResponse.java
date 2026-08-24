package com.jam.agent.common.audit;

import java.util.List;

public record AuditLogPageResponse(List<AuditLogResponse> items, int page, int size, long total) {
}
