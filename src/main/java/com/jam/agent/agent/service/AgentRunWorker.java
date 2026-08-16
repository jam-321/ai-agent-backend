package com.jam.agent.agent.service;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.AttemptRunner;
import com.jam.agent.agent.runtime.ConversationLock;
import com.jam.agent.agent.runtime.TurnFinalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Owns the complete asynchronous lifetime of one submitted turn. */
@Component
public class AgentRunWorker {

    private static final Logger log = LoggerFactory.getLogger(AgentRunWorker.class);

    private final AttemptRunner attempts;
    private final TurnFinalizer finalizer;
    private final ConversationLock lock;

    public AgentRunWorker(
            AttemptRunner attempts,
            TurnFinalizer finalizer,
            ConversationLock lock) {
        this.attempts = attempts;
        this.finalizer = finalizer;
        this.lock = lock;
    }

    public void run(AgentExecutionContext context) {
        int attemptNo = 1;
        try {
            AttemptRunner.RunResult result = attempts.run(context);
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
