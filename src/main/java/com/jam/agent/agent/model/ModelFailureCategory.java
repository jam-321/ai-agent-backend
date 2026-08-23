package com.jam.agent.agent.model;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/** 模型调用失败分类；只有临时性故障才允许 OuterLoop 切换模型。 */
public enum ModelFailureCategory {
    TIMEOUT(true),
    RATE_LIMIT(true),
    OVERLOADED(true),
    MODEL_NOT_FOUND(true),
    AUTH(false),
    FORMAT(false),
    BILLING(false),
    CONTEXT_EXCEEDED(false),
    UNKNOWN(false);

    private final boolean failoverEligible;

    ModelFailureCategory(boolean failoverEligible) {
        this.failoverEligible = failoverEligible;
    }

    public boolean failoverEligible() {
        return failoverEligible;
    }

    public static ModelFailureCategory classify(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ResourceAccessException) {
                return TIMEOUT;
            }
            if (cause instanceof RestClientResponseException response) {
                return fromStatus(response.getStatusCode().value());
            }
        }

        String message = messageOf(failure).toLowerCase();
        if (containsAny(message, "timeout", "timed out", "超时", "connection refused", "connection reset")) {
            return TIMEOUT;
        }
        if (containsAny(message, "rate limit", "too many requests", "限流", "频率过高")) {
            return RATE_LIMIT;
        }
        if (containsAny(message, "overloaded", "temporarily unavailable", "service unavailable", "过载")) {
            return OVERLOADED;
        }
        if (containsAny(message, "model not found", "unknown model", "模型不存在", "模型未找到")) {
            return MODEL_NOT_FOUND;
        }
        if (containsAny(message, "unauthorized", "forbidden", "invalid api key", "authentication", "鉴权", "api key")) {
            return AUTH;
        }
        if (containsAny(message, "context length", "context window", "上下文超限", "token limit")) {
            return CONTEXT_EXCEEDED;
        }
        if (containsAny(message, "billing", "quota", "payment", "余额", "计费")) {
            return BILLING;
        }
        if (containsAny(message, "invalid request", "bad request", "格式错误", "参数错误")) {
            return FORMAT;
        }
        return UNKNOWN;
    }

    private static ModelFailureCategory fromStatus(int status) {
        return switch (status) {
            case 401, 403 -> AUTH;
            case 404 -> MODEL_NOT_FOUND;
            case 408, 504 -> TIMEOUT;
            case 409, 429 -> RATE_LIMIT;
            case 402 -> BILLING;
            case 413 -> CONTEXT_EXCEEDED;
            case 400, 422 -> FORMAT;
            case 500, 502, 503, 529 -> OVERLOADED;
            default -> status >= 500 ? OVERLOADED : UNKNOWN;
        };
    }

    private static String messageOf(Throwable failure) {
        return failure == null || failure.getMessage() == null ? "" : failure.getMessage();
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
