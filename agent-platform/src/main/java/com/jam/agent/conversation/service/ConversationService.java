package com.jam.agent.conversation.service;

import com.jam.agent.agent.service.AgentRunService;
import com.jam.agent.conversation.dto.ConversationResponse;
import com.jam.agent.conversation.dto.TurnResponse;
import com.jam.agent.conversation.persistence.repository.ConversationRepository;
import com.jam.agent.conversation.persistence.repository.ConversationTurnRepository;
import com.jam.agent.agent.persistence.repository.ConversationTurnAttachmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private final ConversationRepository conversations;
    private final ConversationTurnRepository turns;
    private final ConversationTurnAttachmentRepository attachments;

    public ConversationService(
            ConversationRepository conversations,
            ConversationTurnRepository turns,
            ConversationTurnAttachmentRepository attachments) {
        this.conversations = conversations;
        this.turns = turns;
        this.attachments = attachments;
    }

    public List<ConversationResponse> list(long userId) {
        return conversations.listForUser(userId).stream()
                .map(conversation -> new ConversationResponse(
                        conversation.id(),
                        conversation.title(),
                        conversation.agentKey(),
                        conversation.modelProviderKey(),
                        conversation.modelName(),
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
                        turn.agentKey(),
                        turn.modelProviderKey(),
                        turn.modelName(),
                        turn.protocolType(),
                        attachments.findAssetIds(userId, conversationId, turn.turnId()),
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
