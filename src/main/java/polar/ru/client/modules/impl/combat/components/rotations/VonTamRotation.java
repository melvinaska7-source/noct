package polar.ru.client.modules.impl.combat.components.rotations;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.FreeLookStorage;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.math.MathUtil;
import polar.ru.api.utils.math.TimerUtils;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;

public class VonTamRotation
extends RotationsSystem
implements QClient {
    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    private final TimerUtils attackTimer = new TimerUtils();
    private Vec3d aimPoint;
    private Vec3d[] offPoints;
    private int pointIndex;
    private int pointSwitchCd;
    private float snapBackYaw;
    private float snapBackPitch;
    private int snapBackTicks;
    private float flickYaw;
    private float flickPitch;
    private int flickTicks;
    private int flickCd;
    private float jitterYaw;
    private float jitterPitch;
    private float driftYaw;
    private float driftPitch;
    private float microYaw;
    private float microPitch;
    private float smoothYaw;
    private float smoothPitch;
    private float lastTargetYaw;
    private float lastTargetPitch;
    private int reactionDelay;
    private int microPauseTicks;
    private long lastMicroPause;
    private float baseSpeed;
    private float speedVariation;
    private int speedChangeCd;
    private boolean initialized;
    private int tick;
    private boolean snapPhase;
    private boolean isPreparingAttack;
    private long attackPreparationStartMs;
    private float currentYaw;
    private float currentPitch;
    private static final long HOLD_TIME = 360L;
    private static final float MAX_YAW = 38.0f;
    private static final float MAX_PITCH = 20.0f;
    private static final float MIN_SPEED = 0.28f;
    private static final float MAX_SPEED = 0.88f;
    private static final int REACTION_TIME_MIN = 2;
    private static final int REACTION_TIME_MAX = 7;
    private static final float MICRO_INTENSITY = 0.035f;

    public void reset() {
        this.aimPoint = null;
        this.offPoints = null;
        this.pointIndex = 0;
        this.pointSwitchCd = 0;
        this.snapBackYaw = 0.0f;
        this.snapBackPitch = 0.0f;
        this.snapBackTicks = 0;
        this.flickYaw = 0.0f;
        this.flickPitch = 0.0f;
        this.flickTicks = 0;
        this.flickCd = 0;
        this.jitterYaw = 0.0f;
        this.jitterPitch = 0.0f;
        this.driftYaw = 0.0f;
        this.driftPitch = 0.0f;
        this.microYaw = 0.0f;
        this.microPitch = 0.0f;
        this.smoothYaw = 0.0f;
        this.smoothPitch = 0.0f;
        this.lastTargetYaw = 0.0f;
        this.lastTargetPitch = 0.0f;
        this.reactionDelay = 0;
        this.microPauseTicks = 0;
        this.lastMicroPause = 0L;
        this.baseSpeed = 0.5f;
        this.speedVariation = 0.0f;
        this.speedChangeCd = 0;
        this.initialized = false;
        this.tick = 0;
        this.snapPhase = false;
        this.isPreparingAttack = false;
        this.attackPreparationStartMs = -1L;
        this.currentYaw = VonTamRotation.mc.player != null ? VonTamRotation.mc.player.getYaw() : 0.0f;
        this.currentPitch = VonTamRotation.mc.player != null ? VonTamRotation.mc.player.getPitch() : 0.0f;
    }

    public void prepareAttack() {
        if (!this.isPreparingAttack) {
            this.isPreparingAttack = true;
            this.attackPreparationStartMs = System.currentTimeMillis();
            this.reactionDelay = 2 + this.rnd.nextInt(5);
        }
    }

    public void onAttack() {
        this.attackTimer.reset();
        this.snapBackYaw = (float)MathUtil.getRandom(-3.8, -1.2);
        this.snapBackPitch = (float)MathUtil.getRandom(0.6, 2.8);
        this.snapBackTicks = 3 + this.rnd.nextInt(3);
        this.snapPhase = true;
        this.driftYaw *= 0.2f;
        this.driftPitch *= 0.24f;
        this.flickCd = Math.max(0, this.flickCd - 5);
        this.isPreparingAttack = false;
        this.attackPreparationStartMs = -1L;
        if (this.rnd.nextFloat() > 0.68f) {
            this.baseSpeed = (float)MathUtil.getRandom(0.45, 0.75);
            this.speedChangeCd = 10 + this.rnd.nextInt(15);
        }
    }

    @Override
    public void updateRotations(LivingEntity target) {
        float newPitch;
        float newYaw;
        long prepTime;
        long currentTime;
        if (VonTamRotation.mc.player == null || target == null) {
            this.reset();
            return;
        }
        ++this.tick;
        if (this.flickCd > 0) {
            --this.flickCd;
        }
        if (this.snapBackTicks > 0) {
            --this.snapBackTicks;
        }
        if (this.flickTicks > 0) {
            --this.flickTicks;
        }
        if (this.speedChangeCd > 0) {
            --this.speedChangeCd;
        }
        if (this.reactionDelay > 0) {
            --this.reactionDelay;
        }
        if (this.microPauseTicks > 0) {
            --this.microPauseTicks;
        }
        if ((currentTime = System.currentTimeMillis()) - this.lastMicroPause > (long)(900 + this.rnd.nextInt(1400)) && this.rnd.nextFloat() > 0.94f) {
            this.microPauseTicks = 1 + this.rnd.nextInt(3);
            this.lastMicroPause = currentTime;
        }
        if (this.isPreparingAttack && this.attackPreparationStartMs > 0L && (prepTime = System.currentTimeMillis() - this.attackPreparationStartMs) > 400L) {
            this.isPreparingAttack = false;
        }
        Vec3d eyePos = VonTamRotation.mc.player.getCameraPosVec(1.0f);
        if (target.getBoundingBox().contains(eyePos)) {
            Aura.adjYaw = 0.0f;
            Aura.adjPitch = 0.0f;
            RotationStorage.update(new Rotation(VonTamRotation.mc.player.getYaw(), VonTamRotation.mc.player.getPitch()), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, Aura.clientLook.isState());
            return;
        }
        if (this.offPoints == null || this.tick % (12 + this.rnd.nextInt(7)) == 0) {
            this.buildOffPoints(target);
        }
        if (this.pointSwitchCd > 0) {
            --this.pointSwitchCd;
        } else if (this.offPoints != null && this.offPoints.length > 0) {
            this.pointIndex = this.rnd.nextInt(this.offPoints.length);
            this.pointSwitchCd = 2 + this.rnd.nextInt(5);
        }
        this.aimPoint = this.offPoints != null && this.offPoints.length > 0 ? this.offPoints[this.pointIndex] : target.getBoundingBox().getCenter();
        if (this.shouldUseElytraPredict(target)) {
            this.aimPoint = this.getPredictedPoint(target, this.aimPoint);
        }
        Vec2f angle = RotationUtils.getRotations(this.aimPoint);
        boolean canAttack = Aura.INSTANCE.getAttackTimer().getElapsedTime() < 55L;
        boolean hold = !this.attackTimer.finished(360L);
        this.updateDrift(canAttack, hold);
        this.updateJitter(canAttack, hold);
        this.updateMicroMovement(canAttack, hold);
        this.updateFlick(canAttack, hold, target);
        this.updateAdaptiveSpeed(canAttack, hold);
        float targetYaw = angle.x + this.driftYaw + this.jitterYaw + this.microYaw;
        float targetPitch = MathHelper.clamp((float)(angle.y + this.driftPitch + this.jitterPitch + this.microPitch), (float)-89.0f, (float)89.0f);
        if (!this.initialized) {
            this.currentYaw = VonTamRotation.mc.player.getYaw();
            this.currentPitch = VonTamRotation.mc.player.getPitch();
            this.lastTargetYaw = targetYaw;
            this.lastTargetPitch = targetPitch;
            this.smoothYaw = this.currentYaw;
            this.smoothPitch = this.currentPitch;
            this.initialized = true;
        }
        if (this.isPreparingAttack && this.reactionDelay <= 0) {
            long prepTime2 = System.currentTimeMillis() - this.attackPreparationStartMs;
            float prepProgress = MathHelper.clamp((float)((float)prepTime2 / 300.0f), (float)0.0f, (float)1.0f);
            float yawDiff = MathHelper.wrapDegrees((float)(targetYaw - this.currentYaw));
            float pitchDiff = targetPitch - this.currentPitch;
            float smoothFactor = prepProgress * prepProgress * (3.0f - 2.0f * prepProgress) * 0.3f;
            this.currentYaw += yawDiff * smoothFactor;
            this.currentPitch += pitchDiff * smoothFactor;
            newYaw = this.currentYaw;
            newPitch = this.currentPitch;
        } else if ((canAttack || hold) && this.reactionDelay <= 0) {
            if (this.microPauseTicks > 0) {
                newYaw = this.currentYaw + this.microYaw * 0.15f;
                newPitch = this.currentPitch + this.microPitch * 0.15f;
            } else {
                float yawDiff = MathHelper.wrapDegrees((float)(targetYaw - this.currentYaw));
                float pitchDiff = targetPitch - this.currentPitch;
                float totalDelta = (float)Math.hypot(yawDiff, pitchDiff);
                float speed = this.resolveSpeed(canAttack, hold, yawDiff, pitchDiff);
                float yawLimit = totalDelta > 0.0f ? Math.abs(yawDiff / totalDelta) * 38.0f : 38.0f;
                float pitchLimit = totalDelta > 0.0f ? Math.abs(pitchDiff / totalDelta) * 20.0f : 20.0f;
                this.currentYaw = MathHelper.lerp((float)speed, (float)this.currentYaw, (float)(this.currentYaw + MathHelper.clamp((float)yawDiff, (float)(-yawLimit), (float)yawLimit)));
                this.currentPitch = MathHelper.lerp((float)(speed * 0.94f), (float)this.currentPitch, (float)(this.currentPitch + MathHelper.clamp((float)pitchDiff, (float)(-pitchLimit), (float)pitchLimit)));
                newYaw = this.currentYaw;
                newPitch = this.currentPitch;
            }
        } else {
            float targetFreeYaw = FreeLookStorage.isActive() ? FreeLookStorage.getFreeYaw() : VonTamRotation.mc.player.getYaw();
            float targetFreePitch = FreeLookStorage.isActive() ? FreeLookStorage.getFreePitch() : VonTamRotation.mc.player.getPitch();
            this.currentYaw = MathHelper.lerp((float)0.12f, (float)this.currentYaw, (float)targetFreeYaw);
            this.currentPitch = MathHelper.lerp((float)0.12f, (float)this.currentPitch, (float)targetFreePitch);
            newYaw = this.currentYaw;
            newPitch = this.currentPitch;
        }
        Aura.otvodkaYaw = (this.lastTargetYaw - targetYaw) * (this.snapPhase ? 0.36f : 0.2f);
        Aura.otvodkaPitch = (this.lastTargetPitch - targetPitch) * (this.snapPhase ? 0.3f : 0.16f);
        this.lastTargetYaw = targetYaw;
        this.lastTargetPitch = targetPitch;
        if (this.snapBackTicks <= 0) {
            this.snapPhase = false;
        }
        this.updateAdj(canAttack, hold, target);
        if (this.snapBackTicks > 0 && !this.isPreparingAttack) {
            float snapFactor = (float)this.snapBackTicks / 6.0f;
            newYaw += this.snapBackYaw * snapFactor;
            newPitch += this.snapBackPitch * snapFactor * 0.82f;
            this.snapBackYaw *= 0.74f;
            this.snapBackPitch *= 0.77f;
        }
        if (this.flickTicks > 0) {
            newYaw += this.flickYaw;
            newPitch += this.flickPitch;
            this.flickYaw *= 0.6f;
            this.flickPitch *= 0.64f;
        }
        newYaw += Aura.otvodkaYaw;
        newPitch += Aura.otvodkaPitch;
        float gcd = GCDUtil.getGCDValue();
        if (gcd > 0.0f) {
            float baseYaw = VonTamRotation.mc.player.getYaw();
            float basePitch = VonTamRotation.mc.player.getPitch();
            newYaw = baseYaw + (float)Math.round((newYaw - baseYaw) / gcd) * gcd;
            newPitch = basePitch + (float)Math.round((newPitch - basePitch) / gcd) * gcd;
        }
        newPitch = MathHelper.clamp((float)newPitch, (float)-89.0f, (float)89.0f);
        boolean useClientLook = !canAttack && !hold && !this.isPreparingAttack;
        RotationStorage.update(new Rotation(newYaw, newPitch), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, useClientLook && Aura.clientLook.isState());
    }

    private float resolveSpeed(boolean canAttack, boolean hold, float diffYaw, float diffPitch) {
        float rotDiff = (float)Math.hypot(Math.abs(diffYaw), Math.abs(diffPitch));
        if (canAttack || hold) {
            float baseSpeedCalc = 0.75f + rotDiff / 240.0f;
            float finalSpeed = MathHelper.clamp((float)baseSpeedCalc, (float)0.68f, (float)0.9f);
            return finalSpeed * (this.baseSpeed + this.speedVariation * 0.5f);
        }
        float baseSpeedCalc = 0.42f + rotDiff / 380.0f;
        return MathHelper.clamp((float)baseSpeedCalc, (float)0.32f, (float)0.65f) * (this.baseSpeed + this.speedVariation * 0.3f);
    }

    private void updateAdj(boolean canAttack, boolean hold, LivingEntity target) {
        boolean onTarget;
        Vec3d eye = VonTamRotation.mc.player.getCameraPosVec(1.0f);
        Vec3d look = VonTamRotation.mc.player.getRotationVec(1.0f);
        Box box = this.getPredictedBox(target);
        boolean bl = onTarget = box.raycast(eye, eye.add(look.multiply(999.0))).isPresent() || box.contains(eye);
        if (onTarget) {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw - (float)MathUtil.getRandom(0.003, 0.015)), (float)0.4f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch - (float)MathUtil.getRandom(0.003, 0.014)), (float)0.4f, (float)1.0f);
        } else {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw + (float)MathUtil.getRandom(0.005, 0.012)), (float)0.0f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch + (float)MathUtil.getRandom(0.004, 0.01)), (float)0.0f, (float)1.0f);
        }
        if (canAttack || hold) {
            Aura.adjYaw = MathHelper.lerp((float)0.2f, (float)Aura.adjYaw, (float)0.96f);
            Aura.adjPitch = MathHelper.lerp((float)0.2f, (float)Aura.adjPitch, (float)0.94f);
        }
    }

    private void updateDrift(boolean canAttack, boolean hold) {
        if (canAttack || hold) {
            this.driftYaw *= 0.84f;
            this.driftPitch *= 0.86f;
            return;
        }
        if (this.tick % (2 + this.rnd.nextInt(3)) != 0) {
            this.driftYaw *= 0.9f;
            this.driftPitch *= 0.92f;
            return;
        }
        this.driftYaw += (this.rnd.nextFloat() - 0.5f) * 0.18f;
        this.driftPitch += (this.rnd.nextFloat() - 0.5f) * 0.09f;
        this.driftYaw = MathHelper.clamp((float)this.driftYaw, (float)-0.48f, (float)0.48f);
        this.driftPitch = MathHelper.clamp((float)this.driftPitch, (float)-0.24f, (float)0.24f);
    }

    private void updateJitter(boolean canAttack, boolean hold) {
        if (canAttack || hold) {
            this.jitterYaw *= 0.72f;
            this.jitterPitch *= 0.74f;
            return;
        }
        this.jitterYaw = (float)(Math.sin((double)this.tick * 1.92) * 0.07 + Math.cos((double)this.tick * 2.38 + 0.8) * 0.055);
        this.jitterPitch = (float)(Math.sin((double)this.tick * 2.15 + 0.5) * 0.045 + Math.cos((double)this.tick * 2.72) * 0.038);
    }

    private void updateMicroMovement(boolean canAttack, boolean hold) {
        float tremor1 = (float)Math.sin((double)this.tick * 0.48 + this.rnd.nextDouble() * 0.2);
        float tremor2 = (float)Math.cos((double)this.tick * 0.56 + this.rnd.nextDouble() * 0.15);
        float tremor3 = (float)Math.sin((double)this.tick * 0.64 + this.rnd.nextDouble() * 0.18);
        this.microYaw = (tremor1 * 0.012f + tremor2 * 0.01f + tremor3 * 0.008f) * 0.035f;
        this.microPitch = (tremor2 * 0.01f + tremor3 * 0.008f) * 0.035f * 0.8f;
        if (canAttack || hold) {
            this.microYaw *= 1.15f;
            this.microPitch *= 1.15f;
        }
    }

    private void updateAdaptiveSpeed(boolean canAttack, boolean hold) {
        if (this.speedChangeCd > 0) {
            return;
        }
        if (this.rnd.nextFloat() > 0.92f) {
            this.speedVariation = (this.rnd.nextFloat() - 0.5f) * 0.15f;
            this.speedChangeCd = 15 + this.rnd.nextInt(20);
        }
        this.speedVariation *= 0.98f;
    }

    private void updateFlick(boolean canAttack, boolean hold, LivingEntity target) {
        if (canAttack || hold || this.flickCd > 0) {
            return;
        }
        if (this.rnd.nextFloat() > 0.97f) {
            float yawSign = this.rnd.nextBoolean() ? -1.0f : 1.0f;
            this.flickYaw = yawSign * (float)MathUtil.getRandom(14.0, 28.0);
            this.flickPitch = -((float)MathUtil.getRandom(6.0, 18.0));
            this.flickTicks = 2 + this.rnd.nextInt(2);
            this.flickCd = 30 + this.rnd.nextInt(35);
            return;
        }
        float yawDiff = Math.abs(MathHelper.wrapDegrees((float)(RotationUtils.getRotations((Vec3d)this.aimPoint).x - VonTamRotation.mc.player.getYaw())));
        if (yawDiff > 25.0f && this.rnd.nextFloat() > 0.9f) {
            this.flickYaw = Math.signum(MathHelper.wrapDegrees((float)(RotationUtils.getRotations((Vec3d)target.getBoundingBox().getCenter()).x - VonTamRotation.mc.player.getYaw()))) * (float)MathUtil.getRandom(5.0, 12.0);
            this.flickPitch = (this.rnd.nextFloat() - 0.6f) * (float)MathUtil.getRandom(3.0, 8.0);
            this.flickTicks = 2 + this.rnd.nextInt(3);
            this.flickCd = 20 + this.rnd.nextInt(18);
        }
    }

    private void buildOffPoints(LivingEntity target) {
        Box box = this.getPredictedBox(target);
        double w2 = box.maxX - box.minX;
        double h2 = box.maxY - box.minY;
        double d2 = box.maxZ - box.minZ;
        double cx = box.minX + w2 * 0.5;
        double cz = box.minZ + d2 * 0.5;
        this.offPoints = new Vec3d[12];
        this.offPoints[0] = new Vec3d(box.minX - w2 * this.rnd.nextDouble(0.06, 0.2), box.minY + h2 * this.rnd.nextDouble(0.8, 0.96), cz);
        this.offPoints[1] = new Vec3d(box.maxX + w2 * this.rnd.nextDouble(0.06, 0.18), box.minY + h2 * this.rnd.nextDouble(0.75, 0.92), cz);
        this.offPoints[2] = new Vec3d(cx, box.maxY + h2 * this.rnd.nextDouble(0.04, 0.16), cz);
        this.offPoints[3] = new Vec3d(box.minX + w2 * 0.12, box.minY + h2 * this.rnd.nextDouble(0.58, 0.72), cz);
        this.offPoints[4] = new Vec3d(box.maxX - w2 * 0.12, box.minY + h2 * this.rnd.nextDouble(0.55, 0.68), cz);
        this.offPoints[5] = new Vec3d(cx + w2 * this.rnd.nextDouble(-0.3, 0.3), box.minY + h2 * this.rnd.nextDouble(0.42, 0.68), cz + d2 * this.rnd.nextDouble(-0.3, 0.3));
        this.offPoints[6] = new Vec3d(cx + w2 * this.rnd.nextDouble(-0.25, 0.25), box.minY + h2 * this.rnd.nextDouble(0.48, 0.62), cz + d2 * this.rnd.nextDouble(-0.28, 0.28));
        this.offPoints[7] = new Vec3d(cx, box.minY + h2 * this.rnd.nextDouble(0.12, 0.32), cz);
        this.offPoints[8] = new Vec3d(box.minX - w2 * 0.1, box.minY + h2 * 0.85, box.maxZ + d2 * 0.15);
        this.offPoints[9] = new Vec3d(box.maxX + w2 * 0.12, box.minY + h2 * 0.5, box.minZ - d2 * 0.14);
        this.offPoints[10] = new Vec3d(cx, box.minY + h2 * this.rnd.nextDouble(0.85, 0.98), cz + d2 * this.rnd.nextDouble(-0.38, 0.38));
        this.offPoints[11] = new Vec3d(cx + w2 * this.rnd.nextDouble(-0.15, 0.15), box.minY + h2 * 0.6, cz);
    }
}

