package com.jam.agent.service;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.AttemptRunner;
import com.jam.agent.agent.runtime.ConversationLock;
import com.jam.agent.agent.runtime.TurnFinalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AgentRunWorker {
    private static final Logger log = LoggerFactory.getLogger(AgentRunWorker.class);
    private final AttemptRunner attempts; private final TurnFinalizer finalizer; private final ConversationLock lock;
    public AgentRunWorker(AttemptRunner attempts, TurnFinalizer finalizer, ConversationLock lock) { this.attempts=attempts;this.finalizer=finalizer;this.lock=lock; }
    public void run(AgentExecutionContext context) {
        int attempt=1;
        try { var result=attempts.run(context); attempt=result.attemptNo(); finalizer.complete(context, attempt, result.answer()); }
        catch(Throwable ex) { log.error("Agent run failed traceId={}", context.traceId(), ex); finalizer.fail(context, attempt, ex); }
        finally { lock.unlock(context.conversationId(), context.traceId()); }
    }
}
