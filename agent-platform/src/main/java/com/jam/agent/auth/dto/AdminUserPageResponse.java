package com.jam.agent.auth.dto;

import java.util.List;

public record AdminUserPageResponse(List<AdminUserResponse> items, int page, int size, long total) {
}
