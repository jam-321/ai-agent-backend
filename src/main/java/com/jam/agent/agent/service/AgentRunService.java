package com.jam.agent.agent.service;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.ConversationLock;
import com.jam.agent.agent.runtime.TurnFinalizer;
import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.config.AgentLoopConfig;
import com.jam.agent.agent.config.AgentProperties;
import com.jam.agent.agent.dto.ChatRequest;
import com.jam.agent.agent.dto.ChatResponse;
import com.jam.agent.conversation.persistence.repository.ConversationRepository;
import com.jam.agent.conversation.persistence.repository.ConversationTurnRepository;
import com.jam.agent.agent.persistence.repository.AgentConfigRepository;
import com.jam.agent.agent.model.AgentModelConfig;
import com.jam.agent.agent.model.persistence.repository.ModelProviderConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jam.agent.workflow.runtime.WorkflowConfig;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 接收一次聊天请求，落库用户消息后交给 Agent 线程池异步执行。
 *
 * <p>分配 turn_id 前先获取 Redis 会话锁；线程池成功接收任务后锁的所有权才移交给 Worker，
 * 更早阶段的任何失败都由本服务释放锁。
 */
@Service
public class AgentRunService {

    private static final Logger log = LoggerFactory.getLogger(AgentRunService.class);
    private static final int MAX_TITLE_CODE_POINTS = 50;

    private final ConversationRepository conversations;
    private final ConversationTurnRepository turns;
    private final ConversationLock lock;
    private final AgentRunWorker worker;
    private final TurnFinalizer finalizer;
    private final AgentProperties properties;
    private final TransactionTemplate transactions;
    private final Executor executor;
    private final AgentConfigRepository agentConfigs;
    private final ModelProviderConfigRepository modelProviders;
    private final ObjectMapper objectMapper;

    public AgentRunService(
            ConversationRepository conversations,
            ConversationTurnRepository turns,
            ConversationLock lock,
            AgentRunWorker worker,
            TurnFinalizer finalizer,
            AgentProperties properties,
            TransactionTemplate transactions,
            @Qualifier("agentRunExecutor") Executor executor,
            AgentConfigRepository agentConfigs,
            ModelProviderConfigRepository modelProviders,
            ObjectMapper objectMapper) {
        this.conversations = conversations;
        this.turns = turns;
        this.lock = lock;
        this.worker = worker;
        this.finalizer = finalizer;
        this.properties = properties;
        this.transactions = transactions;
        this.executor = executor;
        this.agentConfigs = agentConfigs;
        this.modelProviders = modelProviders;
        this.objectMapper = objectMapper;
    }

    public ChatResponse submit(long userId, ChatRequest request) {
        String query = validateAndNormalizeQuery(request);
        String requestedAgentKey = normalizeAgentKey(request.agentKey());
        if (requestedAgentKey != null && agentConfigs.findByKey(requestedAgentKey).isEmpty()) {
            log.warn("Ignoring unknown Agent recipe: {}", requestedAgentKey);
            requestedAgentKey = null;
        }

        ConversationRepository.ConversationRecord existingConversation = request.conversationId() == null
                ? null
                : conversations.findForUser(userId, request.conversationId())
                        .orElseThrow(NotFoundException::new);
        String selectedAgentKey = requestedAgentKey != null
                ? requestedAgentKey
                : existingConversation == null
                        ? "general"
                        : Optional.ofNullable(existingConversation.agentKey()).orElse("general");
        AgentConfigSnapshot agentConfig = agentConfigs.findByKey(selectedAgentKey)
                .orElseGet(() -> {
                    log.warn("Unknown Agent recipe {}, using general", selectedAgentKey);
                    return agentConfigs.findByKey("general")
                            .orElseGet(AgentConfigSnapshot::defaultConfig);
                });
        String effectiveAgentKey = agentConfig.agentKey();
        AgentModelConfig modelConfig = resolveModelConfig(
                userId,
                request,
                existingConversation,
                agentConfig,
                requestedAgentKey != null);
        long conversationId = existingConversation == null
                ? conversations.insert(
                        userId,
                        null,
                        effectiveAgentKey,
                        modelConfig.providerKey(),
                        modelConfig.modelName())
                : existingConversation.id();
        String traceId = UUID.randomUUID().toString();

        if (!lock.tryLock(conversationId, traceId, properties.getLock().getTtl())) {
            throw new ConversationBusyException();
        }

        boolean workerOwnsLock = false;
        try {
            int turnId = createUserTurn(
                    userId,
                    conversationId,
                    traceId,
                    query,
                    effectiveAgentKey,
                    modelConfig);
            AgentExecutionContext context = buildContext(
                    userId,
                    conversationId,
                    turnId,
                    traceId,
                    query,
                    agentConfig,
                    modelConfig);

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

    private AgentModelConfig resolveModelConfig(
            long userId,
            ChatRequest request,
            ConversationRepository.ConversationRecord conversation,
            AgentConfigSnapshot agentConfig,
            boolean agentExplicitlySelected) {
        String requestedProvider = normalizeModelValue(request.modelProviderKey());
        String requestedModel = normalizeModelValue(request.modelName());
        if ((requestedProvider == null) != (requestedModel == null)) {
            throw new IllegalArgumentException("模型供应商和模型名称必须同时提供。");
        }
        if (requestedProvider != null) {
            return modelProviders.requireModel(
                    userId,
                    requestedProvider,
                    requestedModel,
                    agentConfig.modelConfig().temperature());
        }
        // 只选择 Agent 时必须切到该 Agent 的默认模型，不能继续沿用旧会话模型。
        if (agentExplicitlySelected) {
            return agentConfig.modelConfig();
        }
        if (conversation != null
                && conversation.modelProviderKey() != null
                && conversation.modelName() != null) {
            return modelProviders.requireModel(
                    userId,
                    conversation.modelProviderKey(),
                    conversation.modelName(),
                    agentConfig.modelConfig().temperature());
        }
        return agentConfig.modelConfig();
    }

    private int createUserTurn(
            long userId,
            long conversationId,
            String traceId,
            String query,
            String agentKey,
            AgentModelConfig modelConfig) {
        return Objects.requireNonNull(transactions.execute(status -> {
            conversations.lockForUpdate(userId, conversationId);
            conversations.updateExecutionSelection(
                    userId,
                    conversationId,
                    agentKey,
                    modelConfig.providerKey(),
                    modelConfig.modelName());
            int turnId = turns.nextTurnId(userId, conversationId);
            turns.insert(
                    userId,
                    conversationId,
                    turnId,
                    "user",
                    query,
                    traceId,
                    agentKey,
                    modelConfig,
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
            String query,
            AgentConfigSnapshot agentConfig,
            AgentModelConfig modelConfig) {
        AgentLoopConfig loop = AgentLoopConfig.resolve(
                properties.getLoop(),
                agentConfig.magicParams(),
                objectMapper);
        WorkflowConfig workflow = WorkflowConfig.resolve(
                properties.getWorkflow(),
                agentConfig.magicParams(),
                objectMapper);
        return new AgentExecutionContext(
                userId,
                conversationId,
                turnId,
                traceId,
                query,
                agentConfig,
                modelConfig,
                loop.maxAttempts(),
                loop.maxToolRounds(),
                loop.maxToolsPerRound(),
                loop.maxDegenerateRetries(),
                loop.maxSameToolSignature(),
                workflow.maxSteps(),
                AgentExecutionContext.deadline(loop.maxRunDuration()));
    }

    private void submitWorker(AgentExecutionContext context) {
        try {
            executor.execute(() -> worker.run(context));
        } catch (RejectedExecutionException exception) {
            finalizer.fail(context, 1, exception);
            throw new TaskRejectedException();
        }
    }

    private String normalizeAgentKey(String agentKey) {
        return agentKey == null || agentKey.isBlank() ? null : agentKey.trim();
    }

    private String normalizeModelValue(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
