package com.jam.agent.agent.runtime;

import com.jam.agent.agent.loop.AgentLoop;
import com.jam.agent.agent.loop.ModelAdapter.RetryableModelException;
import com.jam.agent.agent.event.EventPublisher;
import com.jam.agent.agent.memory.ConversationContextManager;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

@Component
public class AttemptRunner {
    private final AgentLoop loop;
    private final ConversationContextManager contextManager;
    private final EventPublisher events;
    public AttemptRunner(AgentLoop loop, ConversationContextManager contextManager, EventPublisher events) {
        this.loop=loop; this.contextManager=contextManager; this.events=events;
    }
    public RunResult run(AgentExecutionContext context) {
        List<Message> history=contextManager.rebuild(context.userId(), context.conversationId(), context.turnId());
        Throwable last=null;
        for(int attempt=1;attempt<=context.maxAttempts();attempt++) {
            context.checkDeadline(); events.lifecycle(context, attempt, null, "attempt_start");
            try { return new RunResult(attempt, loop.run(context, attempt, history)); }
            catch(RetryableModelException ex) { last=ex; events.lifecycle(context, attempt, null, "attempt_retryable_error"); if(attempt==context.maxAttempts()) break; }
        }
        if(last instanceof RuntimeException runtime) throw runtime;
        throw new AgentRunException("模型重试失败。", last, false);
    }
    public record RunResult(int attemptNo, String answer) {}
}
