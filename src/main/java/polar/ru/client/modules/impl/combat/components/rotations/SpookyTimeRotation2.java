package polar.ru.client.modules.impl.combat.components.rotations;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
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

public class SpookyTimeRotation2
extends RotationsSystem
implements QClient {
    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    private Vec3d stableAim;
    private boolean initialized;
    private float jerkYaw;
    private float jerkPitch;
    private int jerkCd;
    private float aimOffYaw;
    private float aimOffPitch;
    private int tick;
    private int aimRefreshTicks;
    private float lastTargetYaw;
    private float lastTargetPitch;
    private float pitchDamp;
    private int postHitTicks;
    private float shakeAmplitude;
    private int shakeTicks;
    private float lastYawPos = 0.0f;
    private float lastPitchPos = 0.0f;
    private static final float MAX_YAW_SPEED = 90.0f;
    private static final float MAX_PITCH_SPEED = 40.0f;

    public void reset() {
        this.stableAim = null;
        this.jerkYaw = 0.0f;
        this.jerkPitch = 0.0f;
        this.jerkCd = 0;
        this.aimOffYaw = 0.0f;
        this.aimOffPitch = 0.0f;
        this.tick = 0;
        this.aimRefreshTicks = 0;
        this.initialized = false;
        this.lastTargetYaw = 0.0f;
        this.lastTargetPitch = 0.0f;
        this.pitchDamp = 0.28f;
        this.postHitTicks = 0;
        this.shakeAmplitude = 0.0f;
        this.shakeTicks = 0;
    }

    public void onAttack() {
        this.jerkCd = 0;
        this.jerkYaw = 0.0f;
        this.jerkPitch = 0.0f;
        this.aimOffYaw *= 0.32f;
        this.aimOffPitch *= 0.32f;
        this.postHitTicks = 8 + this.rnd.nextInt(5);
        this.shakeAmplitude = 0.15f + this.rnd.nextFloat() * 0.2f;
        this.shakeTicks = 3 + this.rnd.nextInt(3);
    }

    @Override
    public void updateRotations(LivingEntity target) {
        double dist;
        boolean close;
        if (SpookyTimeRotation2.mc.player == null || target == null) {
            return;
        }
        this.lastYawPos = SpookyTimeRotation2.mc.player.getYaw();
        this.lastPitchPos = SpookyTimeRotation2.mc.player.getPitch();
        Vec3d eyePos = SpookyTimeRotation2.mc.player.getCameraPosVec(1.0f);
        if (target.getBoundingBox().contains(eyePos)) {
            Aura.adjYaw = 0.0f;
            Aura.adjPitch = 0.0f;
            RotationStorage.update(new Rotation(SpookyTimeRotation2.mc.player.getYaw(), SpookyTimeRotation2.mc.player.getPitch()), 360.0f, 360.0f, 40.0f, 35.0f, 1, 1, Aura.clientLook.isState());
            return;
        }
        ++this.tick;
        Vec3d lookVec = SpookyTimeRotation2.mc.player.getRotationVec(1.0f);
        Vec3d reachVec = eyePos.add(lookVec.multiply(999.0));
        Box box = this.getPredictedBox(target);
        Optional hit = box.raycast(eyePos, reachVec);
        boolean inside = box.contains(eyePos);
        if (hit.isPresent() || inside) {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw - this.rnd.nextFloat(0.005f, 0.02f)), (float)0.0f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch - this.rnd.nextFloat(0.005f, 0.02f)), (float)0.0f, (float)1.0f);
        } else {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw + this.rnd.nextFloat(9.0E-5f, 0.009f)), (float)0.0f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch + this.rnd.nextFloat(9.0E-4f, 0.009f)), (float)0.0f, (float)1.0f);
        }
        if (this.postHitTicks > 0) {
            --this.postHitTicks;
            Aura.adjYaw *= 0.82f;
            Aura.adjPitch *= 0.82f;
        }
        if (this.shakeTicks > 0) {
            --this.shakeTicks;
        }
        boolean bl = close = (dist = eyePos.distanceTo(target.getPos())) < 2.8;
        if (this.aimRefreshTicks > 0) {
            --this.aimRefreshTicks;
        } else {
            Vec3d center = this.getPredictedBox(target).getCenter();
            Vec3d raw = BestPoint.getMultipoint((Entity)target, dist + 1.6);
            if (raw == null) {
                raw = BestPoint.getPoint((Entity)target);
            }
            if (raw == null) {
                raw = MultipointUtils.getClosestPoint((Entity)target);
            }
            if (raw == null) {
                raw = center;
            }
            if (close) {
                raw = raw.lerp(center, 0.25);
            }
            if (this.stableAim == null) {
                this.stableAim = raw;
            } else {
                float blend;
                if (dist <= 1.2) {
                    blend = this.rnd.nextFloat(0.08f, 0.18f);
                    this.aimRefreshTicks = 2 + this.rnd.nextInt(3);
                } else if (dist <= 2.2) {
                    blend = this.rnd.nextFloat(0.45f, 0.75f);
                    this.aimRefreshTicks = 1;
                } else {
                    blend = this.rnd.nextFloat(0.12f, 0.25f);
                    this.aimRefreshTicks = 1 + this.rnd.nextInt(2);
                }
                this.stableAim = this.stableAim.lerp(raw, (double)blend);
            }
        }
        if (this.shouldUseElytraPredict(target)) {
            this.stableAim = this.getPredictedPoint(target, this.stableAim);
        }
        this.updateAimOffset();
        this.updateJerk(target);
        Vec2f angle = RotationUtils.getRotations(this.stableAim);
        float jitterYaw = (float)(Math.sin((double)this.tick * 1.7) * 0.04 + Math.cos((double)this.tick * 2.3) * 0.03);
        float jitterPitch = (float)(Math.sin((double)this.tick * 1.9 + 1.0) * 0.03 + Math.cos((double)this.tick * 2.7 + 2.0) * 0.02);
        float rawTargetYaw = angle.x + this.aimOffYaw + jitterYaw;
        float rawTargetPitch = MathHelper.clamp((float)(angle.y + this.aimOffPitch + jitterPitch), (float)-89.0f, (float)89.0f);
        if (!this.initialized) {
            this.lastTargetYaw = rawTargetYaw;
            this.lastTargetPitch = rawTargetPitch;
            this.initialized = true;
        }
        Aura.otvodkaYaw = (this.lastTargetYaw - rawTargetYaw) * 0.3f;
        Aura.otvodkaPitch = (this.lastTargetPitch - rawTargetPitch) * 0.25f;
        this.lastTargetYaw = rawTargetYaw;
        this.lastTargetPitch = rawTargetPitch;
        float currentYaw = SpookyTimeRotation2.mc.player.getYaw();
        float currentPitch = SpookyTimeRotation2.mc.player.getPitch();
        float diffYaw = MathHelper.wrapDegrees((float)(rawTargetYaw - currentYaw));
        float diffPitch = rawTargetPitch - currentPitch;
        float maxYawSpeed = hit.isPresent() || inside ? 45.0f : 90.0f;
        float maxPitchSpeed = hit.isPresent() || inside ? 20.0f : 40.0f;
        diffYaw = MathHelper.clamp((float)diffYaw, (float)(-maxYawSpeed), (float)maxYawSpeed);
        diffPitch = MathHelper.clamp((float)diffPitch, (float)(-maxPitchSpeed), (float)maxPitchSpeed);
        if (this.postHitTicks > 0) {
            diffYaw *= 0.7f;
            diffPitch *= 0.7f;
        }
        this.pitchDamp = MathHelper.lerp((float)0.14f, (float)this.pitchDamp, (float)(0.22f + this.rnd.nextFloat() * 0.06f));
        float shakeYaw = this.shakeTicks > 0 ? (this.rnd.nextFloat() - 0.5f) * this.shakeAmplitude : 0.0f;
        float shakePitch = this.shakeTicks > 0 ? (this.rnd.nextFloat() - 0.5f) * this.shakeAmplitude : 0.0f;
        float newYaw = currentYaw + diffYaw * Aura.adjYaw + this.jerkYaw + Aura.otvodkaYaw + shakeYaw;
        float newPitch = currentPitch + diffPitch * Aura.adjPitch * this.pitchDamp + this.jerkPitch * 0.55f + Aura.otvodkaPitch + shakePitch;
        float gcd = GCDUtil.getGCDValue();
        if (gcd > 0.0f) {
            newYaw = currentYaw + (float)Math.round((newYaw - currentYaw) / gcd) * gcd;
            newPitch = currentPitch + (float)Math.round((newPitch - currentPitch) / gcd) * gcd;
        }
        newPitch = MathHelper.clamp((float)newPitch, (float)-89.0f, (float)89.0f);
        RotationStorage.update(new Rotation(newYaw, newPitch), 360.0f, 360.0f, 40.0f, 35.0f, 1, 1, Aura.clientLook.isState());
    }

    private void updateAimOffset() {
        if (this.tick % (3 + this.rnd.nextInt(2)) != 0) {
            this.aimOffYaw *= 0.85f;
            this.aimOffPitch *= 0.87f;
            return;
        }
        float spread = this.rnd.nextFloat(0.12f, 0.28f);
        this.aimOffYaw += (this.rnd.nextFloat() - 0.5f) * spread;
        this.aimOffPitch += (this.rnd.nextFloat() - 0.5f) * spread * 0.4f;
        this.aimOffYaw = MathHelper.clamp((float)this.aimOffYaw, (float)-0.3f, (float)0.3f);
        this.aimOffPitch = MathHelper.clamp((float)this.aimOffPitch, (float)-0.15f, (float)0.15f);
    }

    private void updateJerk(LivingEntity target) {
        float mul;
        if (this.jerkCd > 0) {
            --this.jerkCd;
            this.jerkYaw *= 0.72f;
            this.jerkPitch *= 0.75f;
            return;
        }
        float yawDiff = Math.abs(MathHelper.wrapDegrees((float)(RotationUtils.getRotations((Vec3d)this.stableAim).x - SpookyTimeRotation2.mc.player.getYaw())));
        float pitchDiff = Math.abs(RotationUtils.getRotations((Vec3d)this.stableAim).y - SpookyTimeRotation2.mc.player.getPitch());
        double dist = SpookyTimeRotation2.mc.player.getEyePos().distanceTo(target.getPos());
        float f2 = mul = dist < 2.8 ? 0.7f : 1.1f;
        if (yawDiff > 30.0f && this.rnd.nextFloat() > 0.5f) {
            this.jerkYaw = (this.rnd.nextFloat() - 0.5f) * 3.5f * mul;
            this.jerkCd = 5 + this.rnd.nextInt(4);
        } else if (yawDiff > 6.0f && this.rnd.nextFloat() > 0.45f) {
            float gcd = GCDUtil.getGCDValue();
            float amp = (gcd > 0.0f ? gcd * this.rnd.nextFloat(1.5f, 2.8f) : 1.3f) * mul;
            float sign = Math.signum(MathHelper.wrapDegrees((float)(RotationUtils.getRotations((Vec3d)target.getBoundingBox().getCenter()).x - SpookyTimeRotation2.mc.player.getYaw())));
            if (sign == 0.0f) {
                sign = this.rnd.nextBoolean() ? 1.0f : -1.0f;
            }
            this.jerkYaw += sign * amp;
            this.jerkCd = 3 + this.rnd.nextInt(3);
        }
        if (pitchDiff > 6.0f && this.rnd.nextFloat() > 0.6f) {
            this.jerkPitch = (this.rnd.nextFloat() - 0.5f) * 1.4f * mul;
            this.jerkCd = Math.max(this.jerkCd, 5 + this.rnd.nextInt(3));
        }
    }
}

