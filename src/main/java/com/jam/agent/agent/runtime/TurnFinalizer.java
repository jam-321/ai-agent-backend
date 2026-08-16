package com.jam.agent.agent.runtime;

import com.jam.agent.agent.event.EventPublisher;
import com.jam.agent.repository.ConversationRepository;
import com.jam.agent.repository.ConversationTurnRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Writes the canonical terminal state of a turn.
 *
 * <p>The assistant turn and its GENERATE node are committed in one transaction. This
 * prevents the progress API from exposing COMPLETE/ERROR before the final answer exists.
 */
@Component
public class TurnFinalizer {

    private static final String FAILURE_MESSAGE = "本次处理未能完成，请稍后重试。";
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final TransactionTemplate transactions;
    private final ConversationTurnRepository turns;
    private final ConversationRepository conversations;
    private final EventPublisher events;

    public TurnFinalizer(
            TransactionTemplate transactions,
            ConversationTurnRepository turns,
            ConversationRepository conversations,
            EventPublisher events) {
        this.transactions = transactions;
        this.turns = turns;
        this.conversations = conversations;
        this.events = events;
    }

    public void complete(AgentExecutionContext context, int attemptNo, String answer) {
        String finalAnswer = normalizeAnswer(answer);
        transactions.executeWithoutResult(status -> {
            turns.insert(
                    context.userId(),
                    context.conversationId(),
                    context.turnId(),
                    "assistant",
                    finalAnswer,
                    context.traceId(),
                    null);
            events.generate(context, attemptNo, finalAnswer, false);
            conversations.touch(context.userId(), context.conversationId());
        });
    }

    public void fail(AgentExecutionContext context, int attemptNo, Throwable failure) {
        String errorDetail = shorten(extractErrorMessage(failure), MAX_ERROR_MESSAGE_LENGTH);
        transactions.executeWithoutResult(status -> {
            if (assistantTurnExists(context)) {
                return;
            }

            turns.insert(
                    context.userId(),
                    context.conversationId(),
                    context.turnId(),
                    "assistant",
                    FAILURE_MESSAGE,
                    context.traceId(),
                    errorDetail);
            events.generate(context, attemptNo, errorDetail, true);
            conversations.touch(context.userId(), context.conversationId());
        });
    }

    private boolean assistantTurnExists(AgentExecutionContext context) {
        return turns.assistantTurnExists(context.conversationId(), context.turnId());
    }

    private String normalizeAnswer(String answer) {
        return answer == null || answer.isBlank()
                ? "抱歉，我没有生成有效回答。"
                : answer;
    }

    private String extractErrorMessage(Throwable failure) {
        if (failure == null) {
            return "未知错误";
        }
        return failure.getMessage() == null
                ? failure.getClass().getSimpleName()
                : failure.getMessage();
    }

    private String shorten(String value, int maxLength) {
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }
}
