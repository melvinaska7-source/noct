package zov.alphadlc.module.list.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerBotCriticalsTest {
    @Test
    void waitsWhileARequestedCriticalIsPossibleButNotReady() {
        assertFalse(TriggerBotCriticals.shouldAttack(true, true, true, true, false));
    }

    @Test
    void attacksAtTheCorrectCriticalMoment() {
        assertTrue(TriggerBotCriticals.shouldAttack(true, true, true, true, true));
    }

    @Test
    void allowsOrdinaryAttackWhenCriticalIsPhysicallyImpossible() {
        assertTrue(TriggerBotCriticals.shouldAttack(true, true, true, false, false));
    }

    @Test
    void smartCriticalsAllowsOrdinaryAttackWhenPlayerIsNotTryingToJump() {
        assertTrue(TriggerBotCriticals.shouldAttack(true, true, false, true, false));
    }

    @Test
    void disabledOnlyCriticalsNeverWaitsForCriticalMoment() {
        assertTrue(TriggerBotCriticals.shouldAttack(false, true, true, true, false));
    }
}
