package com.jam.agent.common.web;

import com.jam.agent.auth.service.AuthService;
import com.jam.agent.agent.service.AgentRunService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthService.UsernameAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> usernameAlreadyExists() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "用户名已存在。"));
    }

    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户名或密码错误。"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(AgentRunService.NotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "资源不存在。")); }

    @ExceptionHandler(AgentRunService.AgentAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> agentAccessDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "当前用户无权使用该 Agent。"));
    }

    @ExceptionHandler(AgentRunService.ConversationBusyException.class)
    public ResponseEntity<Map<String, String>> busy() { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "当前会话正在处理中，请稍后再试。")); }

    @ExceptionHandler(AgentRunService.TaskRejectedException.class)
    public ResponseEntity<Map<String, String>> rejected() { return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "系统繁忙，请稍后重试。")); }
}
