package com.jam.agent.agent.runtime;

import com.jam.agent.agent.event.EventPublisher;
import com.jam.agent.repository.ConversationNodeRepository;
import com.jam.agent.repository.ConversationRepository;
import com.jam.agent.repository.ConversationTurnRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TurnFinalizer {
    private final TransactionTemplate transactions;
    private final ConversationTurnRepository turns;
    private final ConversationRepository conversations;
    private final EventPublisher events;
    private final JdbcTemplate jdbc;
    public TurnFinalizer(TransactionTemplate transactions, ConversationTurnRepository turns, ConversationRepository conversations,
                         EventPublisher events, JdbcTemplate jdbc) {
        this.transactions = transactions; this.turns = turns; this.conversations = conversations; this.events = events; this.jdbc = jdbc;
    }
    public void complete(AgentExecutionContext c, int attemptNo, String answer) {
        transactions.executeWithoutResult(status -> {
            turns.insert(c.userId(), c.conversationId(), c.turnId(), "assistant", safe(answer), c.traceId(), null);
            events.generate(c, attemptNo, safe(answer), false);
            conversations.touch(c.userId(), c.conversationId());
        });
    }
    public void fail(AgentExecutionContext c, int attemptNo, Throwable failure) {
        String detail = shorten(failure == null ? "未知错误" : (failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage()), 1000);
        transactions.executeWithoutResult(status -> {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM conversation_turn WHERE conversation_id=? AND turn_id=? AND type='assistant'",
                    Integer.class, c.conversationId(), c.turnId());
            if (count == null || count == 0) {
                turns.insert(c.userId(), c.conversationId(), c.turnId(), "assistant", "本次处理未能完成，请稍后重试。", c.traceId(), detail);
                events.generate(c, attemptNo, detail, true);
                conversations.touch(c.userId(), c.conversationId());
            }
        });
    }
    private String safe(String value) { return value == null || value.isBlank() ? "抱歉，我没有生成有效回答。" : value; }
    private String shorten(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
}
