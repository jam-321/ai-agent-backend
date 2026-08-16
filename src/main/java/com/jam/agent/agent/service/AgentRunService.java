package com.jam.agent.agent.service;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.ConversationLock;
import com.jam.agent.agent.runtime.TurnFinalizer;
import com.jam.agent.agent.config.AgentProperties;
import com.jam.agent.agent.dto.ChatRequest;
import com.jam.agent.agent.dto.ChatResponse;
import com.jam.agent.conversation.persistence.repository.ConversationRepository;
import com.jam.agent.conversation.persistence.repository.ConversationTurnRepository;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Accepts a chat turn, persists its user message, and hands execution to the Agent pool.
 *
 * <p>The Redis lock is acquired before allocating turn_id. Ownership moves to the worker
 * only after the task is accepted by the executor; every earlier failure releases it here.
 */
@Service
public class AgentRunService {

    private static final int MAX_TITLE_CODE_POINTS = 50;

    private final ConversationRepository conversations;
    private final ConversationTurnRepository turns;
    private final ConversationLock lock;
    private final AgentRunWorker worker;
    private final TurnFinalizer finalizer;
    private final AgentProperties properties;
    private final TransactionTemplate transactions;
    private final Executor executor;

    public AgentRunService(
            ConversationRepository conversations,
            ConversationTurnRepository turns,
            ConversationLock lock,
            AgentRunWorker worker,
            TurnFinalizer finalizer,
            AgentProperties properties,
            TransactionTemplate transactions,
            @Qualifier("agentRunExecutor") Executor executor) {
        this.conversations = conversations;
        this.turns = turns;
        this.lock = lock;
        this.worker = worker;
        this.finalizer = finalizer;
        this.properties = properties;
        this.transactions = transactions;
        this.executor = executor;
    }

    public ChatResponse submit(long userId, ChatRequest request) {
        String query = validateAndNormalizeQuery(request);
        long conversationId = resolveConversation(userId, request.conversationId());
        String traceId = UUID.randomUUID().toString();

        if (!lock.tryLock(conversationId, traceId, properties.getLock().getTtl())) {
            throw new ConversationBusyException();
        }

        boolean workerOwnsLock = false;
        try {
            int turnId = createUserTurn(userId, conversationId, traceId, query);
            AgentExecutionContext context = buildContext(
                    userId,
                    conversationId,
                    turnId,
                    traceId,
                    query);

            submitWorker(context);
            workerOwnsLock = true;
            return new ChatResponse(conversationId, turnId, traceId, "REASONING");
        } finally {
            if (!workerOwnsLock) {
                lock.unlock(conversationId, traceId);
            }
        }
    }

    private String validateAndNormalizeQuery(ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("消息不能为空。");
        }
        return request.message().trim();
    }

    private long resolveConversation(long userId, Long requestedConversationId) {
        if (requestedConversationId == null) {
            return conversations.insert(userId, null);
        }
        if (conversations.findForUser(userId, requestedConversationId).isEmpty()) {
            throw new NotFoundException();
        }
        return requestedConversationId;
    }

    private int createUserTurn(
            long userId,
            long conversationId,
            String traceId,
            String query) {
        return Objects.requireNonNull(transactions.execute(status -> {
            conversations.lockForUpdate(userId, conversationId);
            int turnId = turns.nextTurnId(userId, conversationId);
            turns.insert(
                    userId,
                    conversationId,
                    turnId,
                    "user",
                    query,
                    traceId,
                    null);
            conversations.updateTitleIfEmpty(userId, conversationId, title(query));
            return turnId;
        }));
    }

    private AgentExecutionContext buildContext(
            long userId,
            long conversationId,
            int turnId,
            String traceId,
            String query) {
        AgentProperties.Loop loop = properties.getLoop();
        return new AgentExecutionContext(
                userId,
                conversationId,
                turnId,
                traceId,
                query,
                loop.getMaxAttempts(),
                loop.getMaxToolRounds(),
                loop.getMaxToolsPerRound(),
                loop.getMaxDegenerateRetries(),
                loop.getMaxSameToolSignature(),
                AgentExecutionContext.deadline(loop.getMaxRunDuration()));
    }

    private void submitWorker(AgentExecutionContext context) {
        try {
            executor.execute(() -> worker.run(context));
        } catch (RejectedExecutionException exception) {
            finalizer.fail(context, 1, exception);
            throw new TaskRejectedException();
        }
    }

    private String title(String query) {
        String cleaned = query.replaceAll("\\s+", " ").trim();
        return cleaned.codePoints()
                .limit(MAX_TITLE_CODE_POINTS)
                .collect(
                        StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString();
    }

    public static class ConversationBusyException extends RuntimeException {
    }

    public static class TaskRejectedException extends RuntimeException {
    }

    public static class NotFoundException extends RuntimeException {
    }
}
