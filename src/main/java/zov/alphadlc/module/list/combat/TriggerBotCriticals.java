package zov.alphadlc.module.list.combat;

final class TriggerBotCriticals {
    private TriggerBotCriticals() {
    }

    static boolean shouldAttack(
            boolean onlyCriticals,
            boolean smartCriticals,
            boolean jumpPressed,
            boolean criticalPhysicallyPossible,
            boolean criticalNow
    ) {
        if (!onlyCriticals || !criticalPhysicallyPossible) return true;
        if (smartCriticals && !jumpPressed) return true;
        return criticalNow;
    }
}
