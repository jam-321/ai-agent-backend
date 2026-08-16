package com.jam.agent.service;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.ConversationLock;
import com.jam.agent.agent.runtime.TurnFinalizer;
import com.jam.agent.config.AgentProperties;
import com.jam.agent.dto.ChatRequest;
import com.jam.agent.dto.ChatResponse;
import com.jam.agent.repository.ConversationRepository;
import com.jam.agent.repository.ConversationTurnRepository;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class AgentRunService {
    private final ConversationRepository conversations; private final ConversationTurnRepository turns;
    private final ConversationLock lock; private final AgentRunWorker worker; private final TurnFinalizer finalizer;
    private final AgentProperties properties; private final TransactionTemplate transactions; private final Executor executor;
    public AgentRunService(ConversationRepository conversations, ConversationTurnRepository turns, ConversationLock lock,
                           AgentRunWorker worker, TurnFinalizer finalizer, AgentProperties properties, TransactionTemplate transactions,
                           @Qualifier("agentRunExecutor") Executor agentRunExecutor) {
        this.conversations=conversations;this.turns=turns;this.lock=lock;this.worker=worker;this.finalizer=finalizer;this.properties=properties;this.transactions=transactions;this.executor=agentRunExecutor;
    }
    public ChatResponse submit(long userId, ChatRequest request) {
        if(request == null || request.message()==null || request.message().isBlank()) throw new IllegalArgumentException("消息不能为空。");
        String query=request.message().trim(); long conversationId;
        if(request.conversationId()==null) conversationId=conversations.insert(userId, null);
        else { conversationId=request.conversationId(); if(conversations.findForUser(userId, conversationId).isEmpty()) throw new NotFoundException(); }
        String traceId=UUID.randomUUID().toString();
        if(!lock.tryLock(conversationId, traceId, properties.getLock().getTtl())) throw new ConversationBusyException();
        int turnId;
        try {
            turnId=transactions.execute(status -> { conversations.lockForUpdate(userId, conversationId); int next=turns.nextTurnId(userId, conversationId); turns.insert(userId, conversationId, next, "user", query, traceId, null); conversations.updateTitleIfEmpty(userId, conversationId, title(query)); return next; });
            AgentExecutionContext context=new AgentExecutionContext(userId, conversationId, turnId, traceId, query, properties.getLoop().getMaxAttempts(), properties.getLoop().getMaxToolRounds(), properties.getLoop().getMaxToolsPerRound(), properties.getLoop().getMaxDegenerateRetries(), properties.getLoop().getMaxSameToolSignature(), AgentExecutionContext.deadline(properties.getLoop().getMaxRunDuration()));
            try { executor.execute(() -> worker.run(context)); }
            catch(RejectedExecutionException ex) { finalizer.fail(context, 1, ex); lock.unlock(conversationId, traceId); throw new TaskRejectedException(); }
            return new ChatResponse(conversationId, turnId, traceId, "REASONING");
        } catch(RuntimeException ex) { lock.unlock(conversationId, traceId); throw ex; }
    }
    private String title(String query) { String cleaned=query.replaceAll("\\s+", " ").trim(); return cleaned.codePoints().limit(50).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString(); }
    public static class ConversationBusyException extends RuntimeException {}
    public static class TaskRejectedException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
}
