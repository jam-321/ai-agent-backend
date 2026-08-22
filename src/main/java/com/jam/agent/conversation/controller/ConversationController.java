package com.jam.agent.conversation.controller;

import com.jam.agent.conversation.dto.ConversationResponse;
import com.jam.agent.conversation.dto.TurnResponse;
import com.jam.agent.auth.security.AuthenticatedUser;
import com.jam.agent.conversation.service.ConversationService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService service;

    public ConversationController(ConversationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConversationResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(user.id());
    }

    @GetMapping("/{conversationId}/turns")
    public List<TurnResponse> turns(@PathVariable long conversationId, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.turns(user.id(), conversationId);
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> delete(@PathVariable long conversationId, @AuthenticationPrincipal AuthenticatedUser user) {
        service.delete(user.id(), conversationId);
        return ResponseEntity.noContent().build();
    }
}
