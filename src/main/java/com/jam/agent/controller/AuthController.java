package com.jam.agent.controller;

import com.jam.agent.dto.LoginRequest;
import com.jam.agent.dto.MessageResponse;
import com.jam.agent.dto.RegisterRequest;
import com.jam.agent.dto.UserResponse;
import com.jam.agent.security.AuthenticatedUser;
import com.jam.agent.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public MessageResponse csrf(CsrfToken csrfToken) {
        return new MessageResponse(csrfToken.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@RequestBody RegisterRequest request) {
        authService.register(request.username(), request.password());
        return new MessageResponse("注册成功，请登录。");
    }

    @PostMapping("/login")
    public UserResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthenticatedUser user = authService.login(request.username(), request.password(), httpRequest);
        return toResponse(user);
    }

    @PostMapping("/logout")
    public MessageResponse logout(HttpServletRequest request) {
        authService.logout(request);
        return new MessageResponse("已退出登录。");
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return toResponse(user);
    }

    private UserResponse toResponse(AuthenticatedUser user) {
        return new UserResponse(user.id(), user.username());
    }
}
