package com.jam.agent.controller;

import com.jam.agent.service.AuthService;
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
}
