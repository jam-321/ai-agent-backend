package com.jam.agent.agent.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jam.agent.agent.model.protocol.ModelCallResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

class TokenBudgetTrackerTest {

    @Test
    void accumulatesProviderUsageAcrossModelCalls() {
        TokenBudgetTracker tracker = new TokenBudgetTracker(1000);
        tracker.record(new ModelCallResult(
                new AssistantMessage("ok"), "r1", "m",
                120L, 30L, 80L, 40L, null, 10L, 150L));
        tracker.record(new ModelCallResult(
                new AssistantMessage("ok"), "r2", "m",
                200L, 50L, null, null, null, null, 250L));

        TokenBudgetTracker.Snapshot snapshot = tracker.snapshot();
        assertEquals(320, snapshot.inputTokens());
        assertEquals(80, snapshot.outputTokens());
        assertEquals(80, snapshot.cachedInputTokens());
        assertEquals(400, snapshot.usedTokens());
        assertEquals(2, snapshot.modelCalls());
    }

    @Test
    void rejectsNextCallWhenRemainingTurnBudgetIsInsufficient() {
        TokenBudgetTracker tracker = new TokenBudgetTracker(300);
        tracker.record(new ModelCallResult(new AssistantMessage("ok"), null, null, 180L, 20L));

        assertThrows(
                TokenBudgetTracker.TokenBudgetExceededException.class,
                () -> tracker.ensureCallAllowed(80, 40));
    }
}
