package polar.ru.client.modules.impl.combat.components.rotations;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;
import polar.ru.client.modules.impl.combat.components.interpolation.BestPoint;

public class SpaceTimesRotation
extends RotationsSystem
implements QClient {
    private static final float YAW_ROT_SPEED = 53.2f;
    private static final float PITCH_ROT_RATIO = 0.51f;
    private final Aura aura;
    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    private LivingEntity trackedTarget;
    private float lastYaw;
    private float lastPitch;
    private float speedAcceleration;
    private boolean back;
    private boolean initialized;
    private float jerkYawTarget;
    private float jerkPitchTarget;
    private float jerkYawSmooth;
    private float jerkPitchSmooth;
    private int jerkCd;
    private float aimOffYaw;
    private float aimOffPitch;
    private float pitchDamp;
    private float pitchStoreSpeed;
    private int pitchStoreTicks;
    private Vec3d stableAim;
    private int aimRefreshTicks;
    private int postHitTicks;
    private int tick;
    private float shakeAmplitude;
    private int shakeTicks;
    private boolean wasInHitbox;
    private int hitboxExitTicks;

    public SpaceTimesRotation(Aura aura) {
        this.aura = aura;
    }

    public void reset() {
        this.trackedTarget = null;
        this.speedAcceleration = 0.0f;
        this.back = false;
        this.jerkYawTarget = 0.0f;
        this.jerkPitchTarget = 0.0f;
        this.jerkYawSmooth = 0.0f;
        this.jerkPitchSmooth = 0.0f;
        this.jerkCd = 0;
        this.aimOffYaw = 0.0f;
        this.aimOffPitch = 0.0f;
        this.pitchDamp = 0.28f;
        this.pitchStoreSpeed = 0.0f;
        this.pitchStoreTicks = 0;
        this.stableAim = null;
        this.aimRefreshTicks = 0;
        this.postHitTicks = 0;
        this.tick = 0;
        this.shakeAmplitude = 0.0f;
        this.shakeTicks = 0;
        this.wasInHitbox = false;
        this.hitboxExitTicks = 0;
        boolean bl = this.initialized = SpaceTimesRotation.mc.player != null;
        if (SpaceTimesRotation.mc.player != null) {
            this.lastYaw = SpaceTimesRotation.mc.player.getYaw();
            this.lastPitch = SpaceTimesRotation.mc.player.getPitch();
        } else {
            this.lastYaw = 0.0f;
            this.lastPitch = 0.0f;
        }
    }

    public void onAttack() {
        this.speedAcceleration *= 0.42f;
        this.jerkCd = 0;
        this.jerkYawTarget = 0.0f;
        this.jerkPitchTarget = 0.0f;
        this.aimOffYaw *= 0.32f;
        this.aimOffPitch *= 0.32f;
        this.postHitTicks = 14 + this.rnd.nextInt(6);
        this.shakeAmplitude = 0.15f + this.rnd.nextFloat() * 0.2f;
        this.shakeTicks = 3 + this.rnd.nextInt(3);
    }

    @Override
    public void updateRotations(LivingEntity target) {
        float yawLimit = 0.0f;
        double dist;
        if (SpaceTimesRotation.mc.player == null || target == null) {
            return;
        }
        if (!this.initialized) {
            this.lastYaw = SpaceTimesRotation.mc.player.getYaw();
            this.lastPitch = SpaceTimesRotation.mc.player.getPitch();
            this.initialized = true;
        }
        if (this.trackedTarget != target) {
            this.trackedTarget = target;
            this.speedAcceleration = 0.0f;
            this.back = false;
            this.jerkYawTarget = 0.0f;
            this.jerkPitchTarget = 0.0f;
            this.jerkYawSmooth = 0.0f;
            this.jerkPitchSmooth = 0.0f;
            this.jerkCd = 0;
            this.stableAim = null;
            this.aimOffYaw = 0.0f;
            this.aimOffPitch = 0.0f;
            this.aimRefreshTicks = 0;
            this.postHitTicks = 0;
            this.tick = 0;
            this.wasInHitbox = false;
            this.hitboxExitTicks = 0;
        }
        ++this.tick;
        if (this.postHitTicks > 0) {
            --this.postHitTicks;
        }
        if (this.shakeTicks > 0) {
            --this.shakeTicks;
        }
        boolean close = (dist = SpaceTimesRotation.mc.player.getEyePos().distanceTo(target.getPos())) < 2.8;
        boolean ready = SpaceTimesRotation.mc.player.getAttackCooldownProgress(1.0f) > 0.85f && this.aura.getWhiteRiseTicksToAttack() <= 1;
        boolean inHitbox = SpaceTimesRotation.mc.player.getBoundingBox().intersects(target.getBoundingBox());
        if (!inHitbox && this.wasInHitbox) {
            this.hitboxExitTicks = 25;
        }
        if (this.hitboxExitTicks > 0) {
            --this.hitboxExitTicks;
        }
        this.wasInHitbox = inHitbox;
        boolean nearHitbox = dist < 2.0 || inHitbox || this.hitboxExitTicks > 0;
        this.updateAimOffset(ready, close);
        Vec3d point = this.resolveAimPoint(target, dist, close, ready);
        if (this.shouldUseElytraPredict(target)) {
            point = this.getPredictedPoint(target, point);
        }
        Vec2f angle = RotationUtils.getRotations(point);
        float targetYaw = angle.x + this.aimOffYaw;
        float targetPitch = MathHelper.clamp((float)(angle.y + this.aimOffPitch), (float)-89.0f, (float)89.0f);
        if (this.shakeTicks > 0) {
            targetYaw += (this.rnd.nextFloat() - 0.5f) * this.shakeAmplitude;
            targetPitch += (this.rnd.nextFloat() - 0.5f) * this.shakeAmplitude;
        }
        float yawDiff = Math.abs(MathHelper.wrapDegrees((float)(targetYaw - this.lastYaw)));
        float pitchDiff = Math.abs(targetPitch - this.lastPitch);
        if (!this.back) {
            float gain = 0.0082f;
            gain = yawDiff > 50.0f ? (gain += 0.032f) : (yawDiff > 20.0f ? (gain += 0.016f) : (gain += 0.007f));
            if (ready) {
                gain += 0.025f;
            }
            if (this.postHitTicks > 0) {
                gain *= 0.82f;
            }
            this.speedAcceleration += gain * 1.85f;
            if (this.speedAcceleration >= 0.28f) {
                this.back = true;
            }
        } else {
            float loss = ready ? 0.06f : 0.015f;
            this.speedAcceleration -= loss * 2.2f;
            if (this.speedAcceleration <= -0.04f) {
                this.back = false;
            }
        }
        float smooth = MathHelper.clamp((float)this.speedAcceleration, (float)0.0f, (float)(SpaceTimesRotation.mc.player.isGliding() ? 0.45f : 0.35f));
        if (ready) {
            smooth = Math.min(smooth + 0.14f, SpaceTimesRotation.mc.player.isGliding() ? 0.55f : 0.42f);
        }
        if (close && ready) {
            smooth = Math.min(smooth + 0.05f, 0.4f);
        }
        if (this.postHitTicks > 0) {
            smooth = Math.min(smooth, 0.3f);
        }
        this.updateJerk(yawDiff, pitchDiff, close, nearHitbox);
        float jerkSmoothSpeed = nearHitbox ? 0.12f : 0.24f;
        float jerkPitchSmoothSpeed = nearHitbox ? 0.1f : 0.18f;
        this.jerkYawSmooth = MathHelper.lerp((float)jerkSmoothSpeed, (float)this.jerkYawSmooth, (float)this.jerkYawTarget);
        this.jerkPitchSmooth = MathHelper.lerp((float)jerkPitchSmoothSpeed, (float)this.jerkPitchSmooth, (float)this.jerkPitchTarget);
        float deltaYaw = MathHelper.wrapDegrees((float)(targetYaw - this.lastYaw));
        float deltaPitch = targetPitch - this.lastPitch;
        float distMultiplier = this.getDistanceSpeedMultiplier(dist, inHitbox, yawDiff);
        float f2 = SpaceTimesRotation.mc.player.isGliding() ? 52.0f : (yawLimit = ready ? 36.0f : 28.0f);
        float pitchLimit = SpaceTimesRotation.mc.player.isGliding() ? 22.0f : (ready ? 12.0f : 8.0f);
        deltaYaw = MathHelper.clamp((float)deltaYaw, (float)(-(yawLimit *= distMultiplier)), (float)yawLimit);
        deltaPitch = MathHelper.clamp((float)deltaPitch, (float)(-(pitchLimit *= distMultiplier)), (float)pitchLimit);
        this.pitchDamp = MathHelper.lerp((float)0.14f, (float)this.pitchDamp, (float)(0.22f + this.rnd.nextFloat() * 0.06f));
        float pitchMult = this.pitchDamp * (ready ? 0.55f : 0.38f) * 0.55f;
        float newYaw = this.lastYaw + deltaYaw * smooth + this.jerkYawSmooth;
        float newPitch = this.lastPitch + deltaPitch * (smooth * pitchMult) + this.jerkPitchSmooth * 0.55f;
        float gcd = GCDUtil.getGCDValue();
        if (gcd > 0.0f) {
            newYaw = this.lastYaw + (float)Math.round((newYaw - this.lastYaw) / gcd) * gcd;
            newPitch = this.lastPitch + (float)Math.round((newPitch - this.lastPitch) / gcd) * gcd;
        }
        newPitch = MathHelper.clamp((float)newPitch, (float)-89.0f, (float)89.0f);
        Rotation finalRot = new Rotation(newYaw, newPitch);
        float yawSpeed = SpaceTimesRotation.mc.player.isGliding() && target.isGliding() ? 360.0f : 53.2f;
        float pitchSpeed = this.getPitchStoreSpeed(yawSpeed);
        RotationStorage.update(finalRot, yawSpeed, pitchSpeed, yawSpeed, pitchSpeed, 0, 1, Aura.clientLook.isState());
        this.rotate = new Vec2f(finalRot.getYaw(), finalRot.getPitch());
        this.lastYaw = finalRot.getYaw();
        this.lastPitch = finalRot.getPitch();
    }

    private Vec3d resolveAimPoint(LivingEntity target, double dist, boolean close, boolean ready) {
        float blend;
        Vec3d best;
        this.aimRefreshTicks = this.aimRefreshTicks <= 0 ? (ready ? 1 + this.rnd.nextInt(2) : 3 + this.rnd.nextInt(2)) : --this.aimRefreshTicks;
        Vec3d center = this.getPredictedBox(target).getCenter();
        Vec3d raw = BestPoint.getMultipoint((Entity)target, dist + 1.6);
        if (raw == null) {
            raw = BestPoint.getPoint((Entity)target);
        }
        if (raw == null) {
            raw = center;
        }
        if (ready && (best = BestPoint.getPoint((Entity)target)) != null) {
            raw = raw.lerp(best, close ? (double)0.42f : (double)0.32f);
        }
        if (close) {
            raw = raw.lerp(center, ready ? (double)0.15f : 0.25);
        }
        if (this.stableAim == null) {
            this.stableAim = raw;
            return this.stableAim;
        }
        float f2 = blend = ready ? 0.36f : 0.24f;
        if (close && ready) {
            blend = 0.45f;
        } else if (close) {
            blend = 0.28f;
        }
        if (this.postHitTicks > 0) {
            blend *= 0.82f;
        }
        this.stableAim = this.stableAim.lerp(raw, (double)blend);
        return this.stableAim;
    }

    private void updateAimOffset(boolean ready, boolean close) {
        float spread = 0.0f;
        if (this.tick % (4 + this.rnd.nextInt(3)) != 0) {
            this.aimOffYaw *= ready ? 0.85f : 0.9f;
            this.aimOffPitch *= ready ? 0.88f : 0.91f;
            return;
        }
        float f2 = ready ? 0.08f : (spread = close ? 0.14f : 0.22f);
        if (this.postHitTicks > 0) {
            spread *= 0.65f;
        }
        this.aimOffYaw += (this.rnd.nextFloat() - 0.5f) * spread;
        this.aimOffPitch += (this.rnd.nextFloat() - 0.5f) * spread * 0.4f;
        float max = ready ? 0.12f : (close ? 0.2f : 0.3f);
        this.aimOffYaw = MathHelper.clamp((float)this.aimOffYaw, (float)(-max), (float)max);
        this.aimOffPitch = MathHelper.clamp((float)this.aimOffPitch, (float)(-max * 0.45f), (float)(max * 0.45f));
    }

    private float getPitchStoreSpeed(float yawSpeed) {
        --this.pitchStoreTicks;
        if (this.pitchStoreTicks <= 0 || this.pitchStoreSpeed <= 0.0f) {
            this.pitchStoreSpeed = yawSpeed * (0.51f + this.rnd.nextFloat() * 0.08f);
            this.pitchStoreTicks = 8 + this.rnd.nextInt(8);
        }
        return this.postHitTicks > 0 ? this.pitchStoreSpeed * 0.85f : this.pitchStoreSpeed;
    }

    private void updateJerk(float yawDiff, float pitchDiff, boolean close, boolean nearHitbox) {
        float mul;
        if (this.jerkCd > 0) {
            --this.jerkCd;
            this.jerkYawTarget *= nearHitbox ? 0.8f : 0.72f;
            this.jerkPitchTarget *= nearHitbox ? 0.82f : 0.75f;
            return;
        }
        float f2 = mul = close ? 0.7f : 1.1f;
        if (nearHitbox) {
            mul *= 0.6f;
        }
        if (yawDiff > 35.0f && this.rnd.nextFloat() > 0.58f) {
            this.jerkYawTarget = (this.rnd.nextFloat() - 0.5f) * 3.2f * mul;
            this.jerkCd = nearHitbox ? 8 + this.rnd.nextInt(6) : 6 + this.rnd.nextInt(5);
        } else if (yawDiff > 8.0f && this.rnd.nextFloat() > 0.52f) {
            float gcd = GCDUtil.getGCDValue();
            float amp = (gcd > 0.0f ? gcd * this.rnd.nextFloat(1.4f, 2.5f) : 1.1f) * mul;
            float sign = Math.signum(MathHelper.wrapDegrees((float)(RotationUtils.getRotations((Vec3d)this.trackedTarget.getBoundingBox().getCenter()).x - this.lastYaw)));
            if (sign == 0.0f) {
                sign = this.rnd.nextBoolean() ? 1.0f : -1.0f;
            }
            this.jerkYawTarget += sign * amp;
            int n2 = this.jerkCd = nearHitbox ? 6 + this.rnd.nextInt(5) : 4 + this.rnd.nextInt(4);
        }
        if (pitchDiff > 8.0f && this.rnd.nextFloat() > 0.68f) {
            this.jerkPitchTarget = (this.rnd.nextFloat() - 0.5f) * 1.2f * mul;
            this.jerkCd = Math.max(this.jerkCd, nearHitbox ? 8 + this.rnd.nextInt(5) : 6 + this.rnd.nextInt(4));
        }
    }

    private float getDistanceSpeedMultiplier(double dist, boolean inHitbox, float yawDiff) {
        if (inHitbox) {
            return 0.08f;
        }
        if (this.hitboxExitTicks > 0 && dist < 1.8) {
            return 0.18f;
        }
        if (yawDiff > 60.0f && dist < 3.0) {
            return 0.25f;
        }
        if (yawDiff > 35.0f && dist < 2.5) {
            return 0.35f;
        }
        if (dist <= 0.8) {
            return 0.3f;
        }
        if (dist <= 1.2) {
            return 0.4f;
        }
        if (dist <= 1.8) {
            return 0.5f;
        }
        if (dist <= 2.5) {
            return 0.65f;
        }
        return 1.0f;
    }
}

