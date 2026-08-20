package polar.ru.client.modules.impl.combat.components.rotations;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.rotate.MultipointUtils;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;
import polar.ru.client.modules.impl.combat.components.interpolation.BestPoint;

public class HvHRotation
extends RotationsSystem
implements QClient {
    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    private Vec3d targetPoint;
    private boolean initialized;
    private float jerkYaw;
    private float jerkPitch;
    private int jerkCooldown;
    private float aimOffsetYaw;
    private float aimOffsetPitch;
    private int tick;
    private static final float MAX_YAW_SPEED = 360.0f;
    private static final float MAX_PITCH_SPEED = 90.0f;
    private static final float JERK_AMPLITUDE = 4.0f;
    private static final int JERK_DURATION = 3;
    private static final float JERK_TRIGGER_YAW = 10.0f;
    private static final float JERK_TRIGGER_PITCH = 8.0f;
    private float jitterPhase;

    public void reset() {
        this.targetPoint = null;
        this.initialized = false;
        this.jerkYaw = 0.0f;
        this.jerkPitch = 0.0f;
        this.jerkCooldown = 0;
        this.aimOffsetYaw = 0.0f;
        this.aimOffsetPitch = 0.0f;
        this.tick = 0;
        this.jitterPhase = 0.0f;
    }

    public void onAttack() {
        this.jerkYaw = (this.rnd.nextFloat() - 0.5f) * 4.0f * 1.5f;
        this.jerkPitch = (this.rnd.nextFloat() - 0.5f) * 4.0f * 1.0f;
        this.jerkCooldown = 3 + this.rnd.nextInt(2);
        this.aimOffsetYaw *= 0.2f;
        this.aimOffsetPitch *= 0.2f;
    }

    @Override
    public void updateRotations(LivingEntity target) {
        if (HvHRotation.mc.player == null || target == null) {
            return;
        }
        ++this.tick;
        this.jitterPhase = (float)((double)this.jitterPhase + (0.12 + this.rnd.nextDouble() * 0.08));
        Vec3d eyePos = HvHRotation.mc.player.getCameraPosVec(1.0f);
        if (target.getBoundingBox().contains(eyePos)) {
            Aura.adjYaw = 1.0f;
            Aura.adjPitch = 1.0f;
            RotationStorage.update(new Rotation(HvHRotation.mc.player.getYaw(), HvHRotation.mc.player.getPitch()), 360.0f, 360.0f, 40.0f, 35.0f, 1, 1, Aura.clientLook.isState());
            return;
        }
        Vec3d bestPoint = this.getBestPoint(target);
        if (bestPoint == null) {
            bestPoint = target.getBoundingBox().getCenter();
        }
        this.targetPoint = bestPoint;
        Vec3d velocity = target.getVelocity();
        if (velocity.lengthSquared() > 0.01) {
            this.targetPoint = this.targetPoint.add(velocity.multiply(0.1));
        }
        Vec2f rawAngles = RotationUtils.getRotations(this.targetPoint);
        float jitterYaw = (float)Math.sin(this.jitterPhase) * 0.02f;
        float jitterPitch = (float)Math.cos((double)this.jitterPhase * 1.3 + 1.2) * 0.015f;
        float targetYaw = rawAngles.x + this.aimOffsetYaw + jitterYaw;
        float targetPitch = MathHelper.clamp((float)(rawAngles.y + this.aimOffsetPitch + jitterPitch), (float)-89.0f, (float)89.0f);
        float currentYaw = HvHRotation.mc.player.getYaw();
        float currentPitch = HvHRotation.mc.player.getPitch();
        float diffYaw = MathHelper.wrapDegrees((float)(targetYaw - currentYaw));
        float diffPitch = targetPitch - currentPitch;
        diffYaw = MathHelper.clamp((float)diffYaw, (float)-360.0f, (float)360.0f);
        diffPitch = MathHelper.clamp((float)diffPitch, (float)-90.0f, (float)90.0f);
        this.updateJerk(diffYaw, diffPitch);
        float finalYaw = currentYaw + diffYaw * Aura.adjYaw + this.jerkYaw;
        float finalPitch = currentPitch + diffPitch * Aura.adjPitch + this.jerkPitch * 0.7f;
        float gcd = GCDUtil.getGCDValue();
        if (gcd > 0.0f) {
            finalYaw = currentYaw + (float)Math.round((finalYaw - currentYaw) / gcd) * gcd;
            finalPitch = currentPitch + (float)Math.round((finalPitch - currentPitch) / gcd) * gcd;
        }
        finalPitch = MathHelper.clamp((float)finalPitch, (float)-89.0f, (float)89.0f);
        Aura.adjYaw = 1.0f;
        Aura.adjPitch = 1.0f;
        Aura.otvodkaYaw = 0.0f;
        Aura.otvodkaPitch = 0.0f;
        RotationStorage.update(new Rotation(finalYaw, finalPitch), 360.0f, 360.0f, 40.0f, 35.0f, 1, 1, Aura.clientLook.isState());
    }

    private Vec3d getBestPoint(LivingEntity target) {
        Vec3d point = BestPoint.getPoint((Entity)target);
        if (point != null) {
            return point;
        }
        point = MultipointUtils.getClosestPoint((Entity)target);
        if (point != null) {
            return point;
        }
        return target.getBoundingBox().getCenter();
    }

    private void updateJerk(float diffYaw, float diffPitch) {
        if (this.jerkCooldown > 0) {
            --this.jerkCooldown;
            this.jerkYaw *= 0.7f;
            this.jerkPitch *= 0.7f;
            return;
        }
        if (Math.abs(diffYaw) > 10.0f || Math.abs(diffPitch) > 8.0f) {
            float yawJerk = (this.rnd.nextFloat() - 0.5f) * 4.0f * 1.2f;
            float pitchJerk = (this.rnd.nextFloat() - 0.5f) * 4.0f * 0.8f;
            float signYaw = Math.signum(diffYaw);
            float signPitch = Math.signum(diffPitch);
            if (this.rnd.nextFloat() > 0.3f) {
                yawJerk += signYaw * 4.0f * 0.5f;
                pitchJerk += signPitch * 4.0f * 0.3f;
            }
            this.jerkYaw = yawJerk;
            this.jerkPitch = pitchJerk;
            this.jerkCooldown = 3 + this.rnd.nextInt(3);
        } else {
            this.jerkYaw *= 0.95f;
            this.jerkPitch *= 0.95f;
            if ((double)Math.abs(this.jerkYaw) < 0.01) {
                this.jerkYaw = 0.0f;
            }
            if ((double)Math.abs(this.jerkPitch) < 0.01) {
                this.jerkPitch = 0.0f;
            }
        }
    }

    public void forceTargetSwitch() {
        this.jerkYaw = (this.rnd.nextFloat() - 0.5f) * 4.0f * 2.0f;
        this.jerkPitch = (this.rnd.nextFloat() - 0.5f) * 4.0f * 1.5f;
        this.jerkCooldown = 4;
    }
}

