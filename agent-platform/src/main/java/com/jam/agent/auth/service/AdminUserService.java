package com.jam.agent.auth.service;

import com.jam.agent.auth.dto.AdminUserPageResponse;
import com.jam.agent.auth.dto.AdminUserResponse;
import com.jam.agent.auth.dto.AdminUserUpdateRequest;
import com.jam.agent.auth.persistence.repository.UserRepository;
import com.jam.agent.auth.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;
    private final UserRepository users;

    public AdminUserService(UserRepository users) {
        this.users = users;
    }

    public AdminUserPageResponse page(int requestedPage, int requestedSize) {
        int page = Math.max(requestedPage, 1);
        int size = Math.min(Math.max(requestedSize, 1), MAX_PAGE_SIZE);
        return new AdminUserPageResponse(
                users.findPage((page - 1) * size, size).stream().map(this::toResponse).toList(),
                page, size, users.countAll());
    }

    public void update(long targetId, AdminUserUpdateRequest request, AuthenticatedUser operator) {
        if (request == null || (request.admin() == null && request.enabled() == null)) {
            throw new IllegalArgumentException("至少提供一个要修改的用户属性。");
        }
        if (targetId == operator.id() && Boolean.FALSE.equals(request.admin())) {
            throw new IllegalArgumentException("不能取消当前登录账号的管理员身份。");
        }
        if (Boolean.FALSE.equals(request.admin()) && users.countAdmins() <= 1) {
            throw new IllegalArgumentException("系统至少需要保留一个启用的管理员。");
        }
        if (request.admin() != null) {
            users.updateAdmin(targetId, request.admin());
        }
        if (request.enabled() != null) {
            if (targetId == operator.id() && !request.enabled()) {
                throw new IllegalArgumentException("不能禁用当前登录账号。");
            }
            users.updateEnabled(targetId, request.enabled());
        }
    }

    private AdminUserResponse toResponse(UserRepository.UserRecord user) {
        return new AdminUserResponse(
                user.id(), user.username(), user.enabled(), user.admin(), user.createdAt(), user.updatedAt());
    }
}
