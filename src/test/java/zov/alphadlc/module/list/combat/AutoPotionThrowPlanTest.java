package zov.alphadlc.module.list.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoPotionThrowPlanTest {
    @Test
    void aimsServerRotationStraightDownWithoutChangingYaw() {
        var rotation = AutoPotionThrowPlan.forYaw(37.5f);

        assertEquals(37.5f, rotation.yaw());
        assertEquals(90.0f, rotation.pitch());
    }
}
