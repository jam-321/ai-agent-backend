package com.jam.agent.agent.service;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.ConversationLock;
import com.jam.agent.agent.runtime.TurnFinalizer;
import com.jam.agent.agent.config.AgentConfigSnapshot;
import com.jam.agent.agent.config.AgentProperties;
import com.jam.agent.conversation.persistence.repository.ConversationTurnRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AgentRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(AgentRecoveryService.class);

    private final ConversationTurnRepository turns;
    private final TurnFinalizer finalizer;
    private final ConversationLock lock;
    private final AgentProperties properties;

    public AgentRecoveryService(
            ConversationTurnRepository turns,
            TurnFinalizer finalizer,
            ConversationLock lock,
            AgentProperties properties) {
        this.turns = turns;
        this.finalizer = finalizer;
        this.lock = lock;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        List<ConversationTurnRepository.IncompleteTurnRecord> incompleteTurns = turns.findIncompleteTurnContexts();
        for (ConversationTurnRepository.IncompleteTurnRecord turn : incompleteTurns) {
            recover(turn);
        }

        if (!incompleteTurns.isEmpty()) {
            log.warn("Recovered {} incomplete agent turns", incompleteTurns.size());
        }
    }

    private void recover(ConversationTurnRepository.IncompleteTurnRecord turn) {
        AgentExecutionContext context = new AgentExecutionContext(
                turn.userId(),
                turn.conversationId(),
                turn.turnId(),
                turn.traceId(),
                turn.content(),
                AgentConfigSnapshot.defaultConfig(),
                properties.getLoop().getMaxAttempts(),
                properties.getLoop().getMaxToolRounds(),
                properties.getLoop().getMaxToolsPerRound(),
                properties.getLoop().getMaxDegenerateRetries(),
                properties.getLoop().getMaxSameToolSignature(),
                properties.getWorkflow().getMaxSteps(),
                AgentExecutionContext.deadline(properties.getLoop().getMaxRunDuration()));

        try {
            // A user turn without an assistant terminal would otherwise poll forever after restart.
            finalizer.fail(context, 1, new IllegalStateException("服务重启，未完成任务已终止。"));
            lock.unlock(turn.conversationId(), turn.traceId());
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to recover incomplete turn conversationId={} turnId={}",
                    turn.conversationId(),
                    turn.turnId(),
                    exception);
        }
    }
}
