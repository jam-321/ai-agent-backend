package com.jam.agent.agent.config;

import com.jam.agent.agent.dto.AgentConfigResponse;
import com.jam.agent.common.audit.AuditAction;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/agents")
public class AgentConfigAdminController {

    private final AgentConfigAdminService service;

    public AgentConfigAdminController(AgentConfigAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<AgentConfigResponse> list() {
        return service.list().stream().map(AgentConfigResponse::from).toList();
    }

    @PostMapping
    @AuditAction(action = "CREATE_AGENT", targetType = "AGENT")
    public void create(@RequestBody AgentConfigAdminRequest request) {
        service.create(request);
    }

    @PutMapping("/{agentKey}")
    @AuditAction(action = "UPDATE_AGENT", targetType = "AGENT")
    public void update(@PathVariable String agentKey, @RequestBody AgentConfigAdminRequest request) {
        service.update(agentKey, request);
    }

    @DeleteMapping("/{agentKey}")
    @AuditAction(action = "DELETE_AGENT", targetType = "AGENT")
    public void delete(@PathVariable String agentKey) {
        service.delete(agentKey);
    }
}
