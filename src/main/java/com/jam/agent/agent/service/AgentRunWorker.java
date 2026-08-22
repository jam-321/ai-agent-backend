package com.jam.agent.agent.service;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.AgentExecutor;
import com.jam.agent.agent.runtime.AgentExecutorRegistry;
import com.jam.agent.agent.runtime.AgentRunResult;
import com.jam.agent.agent.runtime.AgentTurnPreparer;
import com.jam.agent.agent.runtime.ConversationLock;
import com.jam.agent.agent.runtime.TurnFinalizer;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Owns the complete asynchronous lifetime of one submitted turn. */
@Component
public class AgentRunWorker {

    private static final Logger log = LoggerFactory.getLogger(AgentRunWorker.class);

    private final AgentExecutorRegistry executors;
    private final AgentTurnPreparer turnPreparer;
    private final TurnFinalizer finalizer;
    private final ConversationLock lock;

    public AgentRunWorker(
            AgentExecutorRegistry executors,
            AgentTurnPreparer turnPreparer,
            TurnFinalizer finalizer,
            ConversationLock lock) {
        this.executors = executors;
        this.turnPreparer = turnPreparer;
        this.finalizer = finalizer;
        this.lock = lock;
    }

    public void run(AgentExecutionContext context) {
        int attemptNo = 1;
        try {
            // Turn 先于执行策略及其 Attempt；准备阶段的插件在整轮中只运行一次。
            List<Message> turnMessages = turnPreparer.prepare(context);
            AgentExecutor executor = executors.resolve(context.agentConfig().executionType());
            AgentRunResult result = executor.execute(context, turnMessages);
            attemptNo = result.attemptNo();
            finalizer.complete(context, attemptNo, result.answer());
        } catch (Throwable exception) {
            log.error("Agent run failed traceId={}", context.traceId(), exception);
            finalizer.fail(context, attemptNo, exception);
        } finally {
            // The conversation remains locked through terminal persistence.
            lock.unlock(context.conversationId(), context.traceId());
        }
    }
}
