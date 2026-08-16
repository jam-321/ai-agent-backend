package com.jam.agent.service;

import com.jam.agent.dto.ConversationResponse;
import com.jam.agent.dto.TurnResponse;
import com.jam.agent.repository.ConversationRepository;
import com.jam.agent.repository.ConversationTurnRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private final ConversationRepository conversations;
    private final ConversationTurnRepository turns;

    public ConversationService(
            ConversationRepository conversations,
            ConversationTurnRepository turns) {
        this.conversations = conversations;
        this.turns = turns;
    }

    public List<ConversationResponse> list(long userId) {
        return conversations.listForUser(userId).stream()
                .map(conversation -> new ConversationResponse(
                        conversation.id(),
                        conversation.title(),
                        conversation.createdAt(),
                        conversation.updatedAt()))
                .toList();
    }

    public List<TurnResponse> turns(long userId, long conversationId) {
        requireConversation(userId, conversationId);
        return turns.findTurnsForUser(userId, conversationId).stream()
                .map(turn -> new TurnResponse(
                        turn.turnId(),
                        turn.type(),
                        turn.content(),
                        turn.errorMessage(),
                        turn.traceId(),
                        turn.createdAt(),
                        turn.updatedAt()))
                .toList();
    }

    public void delete(long userId, long conversationId) {
        requireConversation(userId, conversationId);
        conversations.softDelete(userId, conversationId);
    }

    private void requireConversation(long userId, long conversationId) {
        if (conversations.findForUser(userId, conversationId).isEmpty()) {
            throw new AgentRunService.NotFoundException();
        }
    }
}
