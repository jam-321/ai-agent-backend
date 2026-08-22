package com.jam.agent.agent.runtime;

import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.loop.AgentLoop;
import com.jam.agent.agent.loop.ModelAdapter.RetryableModelException;
import com.jam.agent.agent.memory.ConversationContextManager;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

/**
 * Outer retry boundary for one Agent run.
 *
 * <p>Only model failures explicitly marked retryable restart the inner loop. Tool and
 * persistence failures are not replayed.
 */
@Component
public class AttemptRunner implements AgentExecutor {

    private final AgentLoop loop;
    private final ConversationContextManager contextManager;
    private final Dispatcher events;

    public AttemptRunner(
            AgentLoop loop,
            ConversationContextManager contextManager,
            Dispatcher events) {
        this.loop = loop;
        this.contextManager = contextManager;
        this.events = events;
    }

    @Override
    public String executionType() {
        return "LOOP";
    }

    @Override
    public AgentRunResult execute(AgentExecutionContext context) {
        List<Message> history = contextManager.rebuild(
                context.userId(),
                context.conversationId(),
                context.turnId());
        RetryableModelException lastFailure = null;

        for (int attemptNo = 1; attemptNo <= context.maxAttempts(); attemptNo++) {
            context.checkDeadline();
            events.lifecycle(context, attemptNo, null, "attempt_start");

            try {
                return new AgentRunResult(attemptNo, loop.run(context, attemptNo, history));
            } catch (RetryableModelException exception) {
                lastFailure = exception;
                events.lifecycle(context, attemptNo, null, "attempt_retryable_error");
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AgentRunException("模型重试失败。", false);
    }

}
