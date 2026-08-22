package com.jam.agent.agent.runtime;

import com.jam.agent.agent.event.AgentTurnContext;
import com.jam.agent.agent.event.Dispatcher;
import com.jam.agent.agent.memory.ConversationContextManager;
import com.jam.agent.agent.service.ImageAttachmentService;
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
    private final ImageAttachmentService images;

    public AgentTurnPreparer(
            ConversationContextManager contextManager,
            Dispatcher events,
            ImageAttachmentService images) {
        this.contextManager = contextManager;
        this.events = events;
        this.images = images;
    }

    public List<Message> prepare(AgentExecutionContext context) {
        List<Message> messages = new ArrayList<>(contextManager.rebuild(
                context.userId(),
                context.conversationId(),
                context.turnId(),
                context.agentConfig().imageHistoryMode(),
                context.modelConfig().supportsImageInput()));
        if (context.modelConfig().supportsImageInput() && !context.attachmentIds().isEmpty()) {
            messages.add(UserMessage.builder()
                    .text(context.currentQuery())
                    .media(context.attachmentIds().stream()
                            .map(id -> images.toMedia(context.userId(), id))
                            .toList())
                    .build());
        } else {
            messages.add(new UserMessage(context.currentQuery()));
        }

        // Turn 插件只执行一次；每个 Attempt 再复制此处形成的消息基线。
        AgentTurnContext turn = new AgentTurnContext(context, messages);
        events.turnStart(turn);
        return List.copyOf(turn.messages());
    }
}
