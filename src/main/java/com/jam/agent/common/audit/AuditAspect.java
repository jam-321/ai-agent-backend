package com.jam.agent.common.audit;

import com.jam.agent.auth.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Captures administrator mutations without putting audit code into every controller. */
@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditLogs;

    public AuditAspect(AuditLogService auditLogs) {
        this.auditLogs = auditLogs;
    }

    @Around("@annotation(action)")
    public Object record(ProceedingJoinPoint joinPoint, AuditAction action) throws Throwable {
        AuthenticatedUser user = currentUser();
        HttpServletRequest request = currentRequest();
        String targetId = request == null ? null : request.getRequestURI();
        try {
            Object result = joinPoint.proceed();
            writeSafely(user, action, targetId, request, "SUCCESS", null);
            return result;
        } catch (Throwable exception) {
            writeSafely(user, action, targetId, request, "FAILURE", exception.getMessage());
            throw exception;
        }
    }

    private void writeSafely(
            AuthenticatedUser user,
            AuditAction action,
            String targetId,
            HttpServletRequest request,
            String result,
            String detail) {
        try {
            auditLogs.record(
                    user,
                    action.action(),
                    action.targetType(),
                    targetId,
                    request == null ? null : request.getMethod(),
                    request == null ? null : request.getRequestURI(),
                    result,
                    detail == null ? null : truncate(detail));
        } catch (RuntimeException ignored) {
            // 审计写入失败不能反向破坏管理员原本的业务操作。
        }
    }

    private AuthenticatedUser currentUser() {
        Object principal = Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(authentication -> authentication.getPrincipal())
                .orElse(null);
        return principal instanceof AuthenticatedUser user ? user : null;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private String truncate(String value) {
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
