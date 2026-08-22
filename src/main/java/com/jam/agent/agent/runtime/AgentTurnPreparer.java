package com.jam.agent.agent.runtime;

import com.jam.agent.agent.event.AgentTurnContext;
import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.memory.ConversationContextManager;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/** Builds the immutable message baseline shared by every attempt in one Turn. */
@Component
public class AgentTurnPreparer {

    private final ConversationContextManager contextManager;
    private final Dispatcher events;

    public AgentTurnPreparer(
            ConversationContextManager contextManager,
            Dispatcher events) {
        this.contextManager = contextManager;
        this.events = events;
    }

    public List<Message> prepare(AgentExecutionContext context) {
        List<Message> messages = new ArrayList<>(contextManager.rebuild(
                context.userId(),
                context.conversationId(),
                context.turnId()));
        messages.add(new UserMessage(context.currentQuery()));

        // Turn 插件只执行一次；每个 Attempt 再复制此处形成的消息基线。
        AgentTurnContext turn = new AgentTurnContext(context, messages);
        events.turnStart(turn);
        return List.copyOf(turn.messages());
    }
}
