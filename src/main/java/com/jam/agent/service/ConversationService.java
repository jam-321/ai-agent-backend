package com.jam.agent.service;

import com.jam.agent.dto.ConversationResponse;
import com.jam.agent.dto.TurnResponse;
import com.jam.agent.repository.ConversationRepository;
import com.jam.agent.repository.ConversationTurnRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {
    private final ConversationRepository conversations; private final ConversationTurnRepository turns;
    public ConversationService(ConversationRepository conversations, ConversationTurnRepository turns) { this.conversations=conversations;this.turns=turns; }
    public List<ConversationResponse> list(long userId) { return conversations.listForUser(userId).stream().map(c -> new ConversationResponse(c.id(), c.title(), c.createdAt(), c.updatedAt())).toList(); }
    public List<TurnResponse> turns(long userId, long conversationId) {
        require(userId, conversationId);
        return turns.findTurnsForUser(userId, conversationId).stream().map(t -> new TurnResponse(t.turnId(),t.type(),t.content(),t.errorMessage(),t.traceId(),t.createdAt(),t.updatedAt())).toList();
    }
    public void delete(long userId, long conversationId) { require(userId, conversationId); conversations.softDelete(userId, conversationId); }
    private void require(long userId,long id) { if(conversations.findForUser(userId,id).isEmpty()) throw new AgentRunService.NotFoundException(); }
}
