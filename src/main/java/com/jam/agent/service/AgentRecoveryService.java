package com.jam.agent.service;

import com.jam.agent.agent.runtime.AgentExecutionContext;
import com.jam.agent.agent.runtime.ConversationLock;
import com.jam.agent.agent.runtime.TurnFinalizer;
import com.jam.agent.config.AgentProperties;
import com.jam.agent.repository.ConversationTurnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AgentRecoveryService {
    private static final Logger log=LoggerFactory.getLogger(AgentRecoveryService.class);
    private final ConversationTurnRepository turns; private final TurnFinalizer finalizer; private final ConversationLock lock; private final AgentProperties properties;
    public AgentRecoveryService(ConversationTurnRepository turns,TurnFinalizer finalizer,ConversationLock lock,AgentProperties properties) { this.turns=turns;this.finalizer=finalizer;this.lock=lock;this.properties=properties; }
    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        var incomplete=turns.findIncompleteTurnContexts();
        for(var turn:incomplete) {
            var context=new AgentExecutionContext(turn.userId(),turn.conversationId(),turn.turnId(),turn.traceId(),turn.content(),properties.getLoop().getMaxAttempts(),properties.getLoop().getMaxToolRounds(),properties.getLoop().getMaxToolsPerRound(),properties.getLoop().getMaxDegenerateRetries(),properties.getLoop().getMaxSameToolSignature(),AgentExecutionContext.deadline(properties.getLoop().getMaxRunDuration()));
            try { finalizer.fail(context,1,new IllegalStateException("服务重启，未完成任务已终止。")); lock.unlock(turn.conversationId(),turn.traceId()); }
            catch(RuntimeException ex) { log.error("Failed to recover incomplete turn conversationId={} turnId={}",turn.conversationId(),turn.turnId(),ex); }
        }
        if(!incomplete.isEmpty()) log.warn("Recovered {} incomplete agent turns",incomplete.size());
    }
}
