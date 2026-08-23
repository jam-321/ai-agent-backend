package com.jam.agent.auth.controller;

import com.jam.agent.auth.dto.AdminUserPageResponse;
import com.jam.agent.auth.dto.AdminUserUpdateRequest;
import com.jam.agent.auth.security.AuthenticatedUser;
import com.jam.agent.auth.service.AdminUserService;
import com.jam.agent.common.audit.AuditAction;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public AdminUserPageResponse page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.page(page, size);
    }

    @PatchMapping("/{userId}")
    @AuditAction(action = "UPDATE_USER", targetType = "USER")
    public void update(
            @PathVariable long userId,
            @RequestBody AdminUserUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser operator) {
        service.update(userId, request, operator);
    }
}
