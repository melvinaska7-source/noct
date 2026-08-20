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

public class HolyWorldRots
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
    private float microJitterPhase;
    private float macroJitterPhase;
    private float reactionDelay;
    private int reactionCounter;
    private Vec3d lastTargetPos;
    private float smoothError;
    private float noiseSeed;
    private int noiseChangeTimer;
    private static final float MAX_YAW_SPEED = 45.0f;
    private static final float MAX_PITCH_SPEED = 20.0f;

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
        this.microJitterPhase = 0.0f;
        this.macroJitterPhase = 0.0f;
        this.reactionDelay = 0.0f;
        this.reactionCounter = 0;
        this.lastTargetPos = null;
        this.smoothError = 0.0f;
        this.noiseSeed = this.rnd.nextFloat() * 10.0f;
        this.noiseChangeTimer = 0;
    }

    public void onAttack() {
        this.jerkCd = 0;
        this.jerkYaw = 0.0f;
        this.jerkPitch = 0.0f;
        this.aimOffYaw *= 0.32f;
        this.aimOffPitch *= 0.32f;
        this.postHitTicks = 10 + this.rnd.nextInt(7);
        this.shakeAmplitude = 0.25f + this.rnd.nextFloat() * 0.3f;
        this.shakeTicks = 4 + this.rnd.nextInt(4);
        this.noiseSeed = this.rnd.nextFloat() * 10.0f;
    }

    @Override
    public void updateRotations(LivingEntity target) {
        double dist;
        boolean close;
        if (HolyWorldRots.mc.player == null || target == null) {
            return;
        }
        this.lastYawPos = HolyWorldRots.mc.player.getYaw();
        this.lastPitchPos = HolyWorldRots.mc.player.getPitch();
        Vec3d eyePos = HolyWorldRots.mc.player.getCameraPosVec(1.0f);
        if (target.getBoundingBox().contains(eyePos)) {
            Aura.adjYaw = 0.0f;
            Aura.adjPitch = 0.0f;
            RotationStorage.update(new Rotation(HolyWorldRots.mc.player.getYaw(), HolyWorldRots.mc.player.getPitch()), 360.0f, 360.0f, 40.0f, 35.0f, 1, 1, Aura.clientLook.isState());
            return;
        }
        ++this.tick;
        this.microJitterPhase = (float)((double)this.microJitterPhase + (0.07 + this.rnd.nextDouble() * 0.03));
        this.macroJitterPhase = (float)((double)this.macroJitterPhase + (0.02 + this.rnd.nextDouble() * 0.015));
        if (this.reactionCounter > 0) {
            --this.reactionCounter;
        } else if (this.rnd.nextFloat() < 0.08f) {
            this.reactionDelay = 1 + this.rnd.nextInt(3);
            this.reactionCounter = (int)this.reactionDelay;
        }
        ++this.noiseChangeTimer;
        if (this.noiseChangeTimer > 200 + this.rnd.nextInt(300)) {
            this.noiseSeed = this.rnd.nextFloat() * 10.0f;
            this.noiseChangeTimer = 0;
        }
        Vec3d lookVec = HolyWorldRots.mc.player.getRotationVec(1.0f);
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
            if (this.lastTargetPos != null) {
                Vec3d velocity = target.getPos().subtract(this.lastTargetPos);
                double speed = velocity.length();
                double errorFactor = Math.min(speed * 0.15, 1.0);
                if (speed > 0.05) {
                    Vec3d perp = new Vec3d(-velocity.z, 0.0, velocity.x).normalize().multiply(errorFactor * 0.2);
                    raw = raw.add(perp);
                }
            }
            this.lastTargetPos = target.getPos();
            this.smoothError = (float)((double)this.smoothError * 0.9 + ((double)this.rnd.nextFloat() - 0.5) * 0.03);
            raw = raw.add(new Vec3d((double)this.smoothError * 0.1, (double)this.smoothError * 0.05, 0.0));
            if (close) {
                raw = raw.lerp(center, 0.25);
            }
            if (this.stableAim == null) {
                this.stableAim = raw;
            } else {
                float blend;
                if (dist <= 1.2) {
                    blend = this.rnd.nextFloat(0.05f, 0.15f);
                    this.aimRefreshTicks = 3 + this.rnd.nextInt(4);
                } else if (dist <= 2.2) {
                    blend = this.rnd.nextFloat(0.4f, 0.7f);
                    this.aimRefreshTicks = 2 + this.rnd.nextInt(2);
                } else {
                    blend = this.rnd.nextFloat(0.1f, 0.2f);
                    this.aimRefreshTicks = 2 + this.rnd.nextInt(3);
                }
                if (this.rnd.nextFloat() < 0.03f) {
                    blend = 0.9f + this.rnd.nextFloat() * 0.1f;
                    this.stableAim = this.stableAim.lerp(raw, (double)blend);
                } else {
                    this.stableAim = this.stableAim.lerp(raw, (double)blend);
                }
            }
        }
        if (this.shouldUseElytraPredict(target)) {
            this.stableAim = this.getPredictedPoint(target, this.stableAim);
        }
        this.updateAimOffset();
        this.updateJerk(target);
        Vec2f angle = RotationUtils.getRotations(this.stableAim);
        float jitterYaw = (float)(Math.sin(this.microJitterPhase) * 0.04 + Math.cos((double)this.macroJitterPhase * 1.3) * 0.03 + Math.sin((double)this.tick * 0.7 + (double)this.noiseSeed) * 0.02);
        float jitterPitch = (float)(Math.sin((double)this.microJitterPhase * 0.7 + 1.2) * 0.03 + Math.cos((double)this.macroJitterPhase * 0.9 + 2.4) * 0.025 + Math.sin((double)this.tick * 0.5 + (double)this.noiseSeed * 0.5) * 0.015);
        float distNoise = (float)Math.min(dist * 0.02, 0.1);
        float rawTargetYaw = angle.x + this.aimOffYaw + (jitterYaw += (this.rnd.nextFloat() - 0.5f) * distNoise);
        float rawTargetPitch = MathHelper.clamp((float)(angle.y + this.aimOffPitch + (jitterPitch += (this.rnd.nextFloat() - 0.5f) * distNoise * 0.6f)), (float)-89.0f, (float)89.0f);
        if (this.reactionCounter > 0) {
            rawTargetYaw = this.lastTargetYaw + (rawTargetYaw - this.lastTargetYaw) * 0.1f;
            rawTargetPitch = this.lastTargetPitch + (rawTargetPitch - this.lastTargetPitch) * 0.1f;
        }
        if (!this.initialized) {
            this.lastTargetYaw = rawTargetYaw;
            this.lastTargetPitch = rawTargetPitch;
            this.initialized = true;
        }
        float otvodkaBlend = 0.3f + this.rnd.nextFloat() * 0.2f;
        Aura.otvodkaYaw = (this.lastTargetYaw - rawTargetYaw) * otvodkaBlend;
        Aura.otvodkaPitch = (this.lastTargetPitch - rawTargetPitch) * (otvodkaBlend * 0.8f);
        this.lastTargetYaw = rawTargetYaw;
        this.lastTargetPitch = rawTargetPitch;
        float currentYaw = HolyWorldRots.mc.player.getYaw();
        float currentPitch = HolyWorldRots.mc.player.getPitch();
        float diffYaw = MathHelper.wrapDegrees((float)(rawTargetYaw - currentYaw));
        float diffPitch = rawTargetPitch - currentPitch;
        float speedMulYaw = 0.8f + this.rnd.nextFloat() * 0.4f;
        float speedMulPitch = 0.7f + this.rnd.nextFloat() * 0.4f;
        if (dist > 5.0) {
            speedMulYaw *= 0.7f;
            speedMulPitch *= 0.7f;
        } else if (dist < 2.0) {
            speedMulYaw *= 1.2f;
            speedMulPitch *= 1.2f;
        }
        diffYaw = MathHelper.clamp((float)diffYaw, (float)(-45.0f * speedMulYaw), (float)(45.0f * speedMulYaw));
        diffPitch = MathHelper.clamp((float)diffPitch, (float)(-20.0f * speedMulPitch), (float)(20.0f * speedMulPitch));
        if (this.postHitTicks > 0) {
            diffYaw *= 0.6f + this.rnd.nextFloat() * 0.2f;
            diffPitch *= 0.6f + this.rnd.nextFloat() * 0.2f;
        }
        this.pitchDamp = MathHelper.lerp((float)(0.1f + this.rnd.nextFloat() * 0.08f), (float)this.pitchDamp, (float)(0.22f + this.rnd.nextFloat() * 0.1f));
        float shakeYaw = this.shakeTicks > 0 ? (this.rnd.nextFloat() - 0.5f) * this.shakeAmplitude * 1.5f : 0.0f;
        float shakePitch = this.shakeTicks > 0 ? (this.rnd.nextFloat() - 0.5f) * this.shakeAmplitude * 1.5f : 0.0f;
        float suddenJerk = 0.0f;
        if (this.rnd.nextFloat() < 0.002f) {
            suddenJerk = (this.rnd.nextFloat() - 0.5f) * 4.0f;
        }
        float newYaw = currentYaw + diffYaw * Aura.adjYaw + this.jerkYaw + Aura.otvodkaYaw + shakeYaw + suddenJerk;
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
        if (this.tick % (2 + this.rnd.nextInt(3)) != 0) {
            this.aimOffYaw *= 0.8f + this.rnd.nextFloat() * 0.1f;
            this.aimOffPitch *= 0.82f + this.rnd.nextFloat() * 0.1f;
            return;
        }
        float spread = this.rnd.nextFloat(0.15f, 0.35f);
        this.aimOffYaw += (this.rnd.nextFloat() - 0.5f) * spread;
        this.aimOffPitch += (this.rnd.nextFloat() - 0.5f) * spread * 0.5f;
        this.aimOffYaw = MathHelper.clamp((float)this.aimOffYaw, (float)-0.5f, (float)0.5f);
        this.aimOffPitch = MathHelper.clamp((float)this.aimOffPitch, (float)-0.25f, (float)0.25f);
    }

    private void updateJerk(LivingEntity target) {
        float mul;
        if (this.jerkCd > 0) {
            --this.jerkCd;
            this.jerkYaw *= 0.7f + this.rnd.nextFloat() * 0.1f;
            this.jerkPitch *= 0.73f + this.rnd.nextFloat() * 0.1f;
            return;
        }
        float yawDiff = Math.abs(MathHelper.wrapDegrees((float)(RotationUtils.getRotations((Vec3d)this.stableAim).x - HolyWorldRots.mc.player.getYaw())));
        float pitchDiff = Math.abs(RotationUtils.getRotations((Vec3d)this.stableAim).y - HolyWorldRots.mc.player.getPitch());
        double dist = HolyWorldRots.mc.player.getEyePos().distanceTo(target.getPos());
        float f2 = mul = dist < 2.8 ? 0.8f : 1.2f;
        if (yawDiff > 25.0f && this.rnd.nextFloat() > 0.4f) {
            this.jerkYaw = (this.rnd.nextFloat() - 0.5f) * 2.8f * mul;
            this.jerkCd = 4 + this.rnd.nextInt(5);
        } else if (yawDiff > 5.0f && this.rnd.nextFloat() > 0.35f) {
            float gcd = GCDUtil.getGCDValue();
            float amp = (gcd > 0.0f ? gcd * this.rnd.nextFloat(1.2f, 2.5f) : 1.0f) * mul;
            float sign = Math.signum(MathHelper.wrapDegrees((float)(RotationUtils.getRotations((Vec3d)target.getBoundingBox().getCenter()).x - HolyWorldRots.mc.player.getYaw())));
            if (sign == 0.0f) {
                sign = this.rnd.nextBoolean() ? 1.0f : -1.0f;
            }
            this.jerkYaw += sign * amp;
            this.jerkCd = 2 + this.rnd.nextInt(4);
        }
        if (pitchDiff > 5.0f && this.rnd.nextFloat() > 0.5f) {
            this.jerkPitch = (this.rnd.nextFloat() - 0.5f) * 1.2f * mul;
            this.jerkCd = Math.max(this.jerkCd, 4 + this.rnd.nextInt(4));
        }
        if (this.rnd.nextFloat() < 0.01f) {
            this.jerkYaw += (this.rnd.nextFloat() - 0.5f) * 1.5f;
            this.jerkCd = Math.max(this.jerkCd, 2 + this.rnd.nextInt(3));
        }
    }
}

