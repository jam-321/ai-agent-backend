package com.jam.agent.agent.config;

import com.jam.agent.agent.persistence.repository.AgentConfigRepository;
import com.jam.agent.agent.dto.AgentConfigResponse;
import com.jam.agent.auth.security.AuthenticatedUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lists the Agent recipes available to the authenticated frontend. */
@RestController
@RequestMapping("/api/agents")
public class AgentConfigController {

    private final AgentConfigRepository repository;

    public AgentConfigController(AgentConfigRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AgentConfigResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return repository.findAll().stream()
                .filter(agent -> !agent.adminOnly() || user.admin())
                .map(AgentConfigResponse::from)
                .toList();
    }
}
