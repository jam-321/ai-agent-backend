package com.jam.agent.controller;

import com.jam.agent.dto.ChatRequest;
import com.jam.agent.dto.ChatResponse;
import com.jam.agent.dto.ProgressResponse;
import com.jam.agent.security.AuthenticatedUser;
import com.jam.agent.service.AgentRunService;
import com.jam.agent.service.ProgressQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final AgentRunService runService;
    private final ProgressQueryService progressService;

    public ChatController(AgentRunService runService, ProgressQueryService progressService) {
        this.runService = runService; this.progressService = progressService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.accepted().body(runService.submit(user.id(), request));
    }

    @GetMapping("/chat/progress")
    public ProgressResponse progress(@RequestParam long conversationId, @RequestParam int turnId,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return progressService.get(user.id(), conversationId, turnId);
    }
}
