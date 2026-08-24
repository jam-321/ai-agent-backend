package com.jam.agent.agent.runtime;

import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.loop.AgentLoop;
import com.jam.agent.agent.loop.ModelAdapter.RetryableModelException;
import com.jam.agent.agent.model.AgentModelConfig;
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
    private final Dispatcher events;

    public AttemptRunner(
            AgentLoop loop,
            Dispatcher events) {
        this.loop = loop;
        this.events = events;
    }

    @Override
    public String executionType() {
        return "LOOP";
    }

    @Override
    public AgentRunResult execute(
            AgentExecutionContext context,
            List<Message> turnMessages) {
        RetryableModelException lastFailure = null;

        for (int attemptNo = 1; attemptNo <= context.maxAttempts(); attemptNo++) {
            context.checkDeadline();
            events.lifecycle(context, attemptNo, null, "attempt_start");
            AgentModelConfig attemptModel = modelForAttempt(context, attemptNo);
            AgentExecutionContext attemptContext = context.forAttempt(attemptModel);

            try {
                return new AgentRunResult(
                        attemptNo,
                        loop.run(attemptContext, attemptNo, turnMessages),
                        attemptModel);
            } catch (RetryableModelException exception) {
                lastFailure = exception;
                if (!exception.category().failoverEligible()
                        || attemptNo >= context.maxAttempts()) {
                    throw exception;
                }
                events.lifecycle(
                        context,
                        attemptNo,
                        null,
                        "attempt_failover:category=" + exception.category()
                                + ",from=" + modelName(attemptModel)
                                + ",to=" + modelName(modelForAttempt(context, attemptNo + 1)));
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new AgentRunException("模型重试失败。", false);
    }

    private AgentModelConfig modelForAttempt(AgentExecutionContext context, int attemptNo) {
        if (attemptNo > 1 && context.agentConfig().fallbackModelConfig() != null) {
            return context.agentConfig().fallbackModelConfig();
        }
        return context.modelConfig();
    }

    private String modelName(AgentModelConfig model) {
        return model.providerKey() + "/" + model.modelName();
    }

}
