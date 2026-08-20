package polar.ru.client.modules.impl.combat.components;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.client.modules.impl.combat.ElytraTarget;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;

public abstract class RotationsSystem
implements QClient {
    public Vec2f rotate = Vec2f.ZERO;

    public abstract void updateRotations(LivingEntity var1);

    public static Vec2f correctRotation(float yaw, float pitch) {
        if (yaw == -90.0f && pitch == 90.0f || yaw == -180.0f) {
            return new Vec2f(RotationsSystem.mc.player.getYaw(), RotationsSystem.mc.player.getPitch());
        }
        float gcd = GCDUtil.getGCD();
        yaw -= yaw % gcd;
        pitch -= pitch % gcd;
        return new Vec2f(yaw, pitch);
    }

    protected boolean shouldUseElytraPredict(LivingEntity target) {
        ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
        return RotationsSystem.mc.player != null && target != null && RotationsSystem.mc.player.isGliding() && target.isGliding() && elytraTarget != null && elytraTarget.isPredictionActive();
    }

    protected int getElytraPredictTicks() {
        ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
        if (elytraTarget == null || !elytraTarget.isPredictionActive()) {
            return 0;
        }
        return Math.max(0, elytraTarget.getForwardTicks());
    }

    protected Vec3d getPredictedPoint(LivingEntity target, Vec3d point) {
        if (!this.shouldUseElytraPredict(target)) {
            return point;
        }
        ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
        return elytraTarget != null ? elytraTarget.getPredictedPoint(target, point) : point;
    }

    protected Box getPredictedBox(LivingEntity target) {
        Box box = target.getBoundingBox();
        if (!this.shouldUseElytraPredict(target)) {
            return box;
        }
        Vec3d currentCenter = box.getCenter();
        Vec3d predictedCenter = this.getPredictedPoint(target, currentCenter);
        return box.offset(predictedCenter.subtract(currentCenter));
    }
}

