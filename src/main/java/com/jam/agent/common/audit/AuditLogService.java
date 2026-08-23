package com.jam.agent.common.audit;

import com.jam.agent.auth.security.AuthenticatedUser;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogMapper mapper;

    public AuditLogService(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    public void record(
            AuthenticatedUser user,
            String action,
            String targetType,
            String targetId,
            String method,
            String uri,
            String result,
            String detail) {
        if (user == null || !user.admin()) {
            return;
        }
        AuditLogEntity entity = new AuditLogEntity();
        entity.setUserId(user.id());
        entity.setUsername(user.username());
        entity.setAction(action);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setRequestMethod(method);
        entity.setRequestUri(uri);
        entity.setResult(result);
        entity.setDetail(detail);
        mapper.insert(entity);
    }

    public AuditLogPageResponse page(int requestedPage, int requestedSize) {
        int page = Math.max(requestedPage, 1);
        int size = Math.min(Math.max(requestedSize, 1), MAX_PAGE_SIZE);
        return new AuditLogPageResponse(
                mapper.selectPage((page - 1) * size, size).stream()
                        .map(AuditLogResponse::from).toList(),
                page,
                size,
                mapper.countAll());
    }
}
