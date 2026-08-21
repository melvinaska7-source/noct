package zov.alphadlc.module.list.combat;

final class AutoPotionThrowPlan {
    private static final float DOWNWARD_PITCH = 90.0f;

    private AutoPotionThrowPlan() {
    }

    static ServerRotation forYaw(float yaw) {
        return new ServerRotation(yaw, DOWNWARD_PITCH);
    }

    record ServerRotation(float yaw, float pitch) {
    }
}
