package com.jam.agent.controller;

import com.jam.agent.dto.admin.AdminConversationDetailResponse;
import com.jam.agent.dto.admin.AdminConversationPageResponse;
import com.jam.agent.dto.admin.AdminOverviewResponse;
import com.jam.agent.dto.admin.AdminToolStatisticsResponse;
import com.jam.agent.service.AdminMonitorService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/monitor")
public class AdminMonitorController {

    private final AdminMonitorService service;

    public AdminMonitorController(AdminMonitorService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview() {
        return service.overview();
    }

    @GetMapping("/conversations")
    public AdminConversationPageResponse conversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return service.conversations(page, size, search);
    }

    @GetMapping("/conversations/{conversationId}")
    public AdminConversationDetailResponse conversation(@PathVariable long conversationId) {
        return service.conversation(conversationId);
    }

    @GetMapping("/tools")
    public List<AdminToolStatisticsResponse> tools() {
        return service.tools();
    }
}
