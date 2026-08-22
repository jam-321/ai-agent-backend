package com.jam.agent.common.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audits")
public class AuditController {

    private final AuditLogService service;

    public AuditController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public AuditLogPageResponse page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.page(page, size);
    }
}
