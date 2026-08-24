package com.jam.agent.agent.controller;

import com.jam.agent.agent.dto.ChatRequest;
import com.jam.agent.agent.dto.ChatResponse;
import com.jam.agent.agent.dto.ProgressResponse;
import com.jam.agent.auth.security.AuthenticatedUser;
import com.jam.agent.agent.service.AgentRunService;
import com.jam.agent.agent.service.ProgressQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final AgentRunService runService;
    private final ProgressQueryService progressService;

    public ChatController(AgentRunService runService, ProgressQueryService progressService) {
        this.runService = runService; this.progressService = progressService;
    }

    @PostMapping(value = "/chat", consumes = "multipart/form-data")
    public ResponseEntity<ChatResponse> chat(
            @RequestParam("message") String message,
            @RequestParam(value = "conversationId", required = false) Long conversationId,
            @RequestParam(value = "agentKey", required = false) String agentKey,
            @RequestParam(value = "modelProviderKey", required = false) String modelProviderKey,
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ChatRequest request = new ChatRequest(
                conversationId, message, agentKey, modelProviderKey, modelName);
        return ResponseEntity.accepted().body(runService.submit(user.id(), request, images));
    }

    @GetMapping("/chat/progress")
    public ProgressResponse progress(@RequestParam long conversationId, @RequestParam int turnId,
                                     @AuthenticationPrincipal AuthenticatedUser user) {
        return progressService.get(user.id(), conversationId, turnId);
    }
}
