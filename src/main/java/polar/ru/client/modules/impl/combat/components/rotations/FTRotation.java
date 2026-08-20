package polar.ru.client.modules.impl.combat.components.rotations;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.math.TimerUtils;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;

public class FTRotation
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
    private float lastTargetYaw;
    private float lastTargetPitch;
    private boolean initialized;
    private int tick;
    private boolean snapPhase;
    private static final long HOLD_TIME = 420L;
    private static final float MAX_YAW = 38.0f;
    private static final float MAX_PITCH = 18.0f;

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
        this.lastTargetYaw = 0.0f;
        this.lastTargetPitch = 0.0f;
        this.initialized = false;
        this.tick = 0;
        this.snapPhase = false;
    }

    public void onAttack() {
        this.attackTimer.reset();
        this.snapBackYaw = this.rnd.nextFloat(-6.5f, -2.8f);
        this.snapBackPitch = this.rnd.nextFloat(1.2f, 4.8f);
        this.snapBackTicks = 4 + this.rnd.nextInt(4);
        this.snapPhase = true;
        this.driftYaw *= 0.35f;
        this.driftPitch *= 0.35f;
        this.flickCd = Math.max(0, this.flickCd - 8);
    }

    @Override
    public void updateRotations(LivingEntity target) {
        float gcd;
        if (FTRotation.mc.player == null || target == null) {
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
        if (this.offPoints == null || this.tick % 14 == 0) {
            this.buildOffPoints(target);
        }
        if (this.pointSwitchCd > 0) {
            --this.pointSwitchCd;
        } else if (this.offPoints != null && this.offPoints.length > 0) {
            this.pointIndex = this.rnd.nextInt(this.offPoints.length);
            this.pointSwitchCd = 1 + this.rnd.nextInt(3);
        }
        this.aimPoint = this.offPoints != null && this.offPoints.length > 0 ? this.offPoints[this.pointIndex] : target.getBoundingBox().getCenter();
        if (this.shouldUseElytraPredict(target)) {
            this.aimPoint = this.getPredictedPoint(target, this.aimPoint);
        }
        Vec2f angle = RotationUtils.getRotations(this.aimPoint);
        boolean canAttack = Aura.INSTANCE.getAttackTimer().getElapsedTime() < 55L;
        boolean hold = !this.attackTimer.finished(420L);
        this.updateDrift(canAttack, hold);
        this.updateJitter(canAttack, hold);
        this.updateFlick(canAttack, hold, target);
        float targetYaw = angle.x + this.driftYaw + this.jitterYaw;
        float targetPitch = MathHelper.clamp((float)(angle.y + this.driftPitch + this.jitterPitch), (float)-89.0f, (float)89.0f);
        if (!this.initialized) {
            this.lastTargetYaw = targetYaw;
            this.lastTargetPitch = targetPitch;
            this.initialized = true;
        }
        Aura.otvodkaYaw = (this.lastTargetYaw - targetYaw) * (this.snapPhase ? 0.42f : 0.28f);
        Aura.otvodkaPitch = (this.lastTargetPitch - targetPitch) * (this.snapPhase ? 0.36f : 0.22f);
        this.lastTargetYaw = targetYaw;
        this.lastTargetPitch = targetPitch;
        if (this.snapBackTicks <= 0) {
            this.snapPhase = false;
        }
        float currentYaw = FTRotation.mc.player.getYaw();
        float currentPitch = FTRotation.mc.player.getPitch();
        float diffYaw = MathHelper.wrapDegrees((float)(targetYaw - currentYaw));
        float diffPitch = targetPitch - currentPitch;
        float speed = this.resolveSpeed(canAttack, hold, diffYaw, diffPitch);
        diffYaw = MathHelper.clamp((float)diffYaw, (float)-38.0f, (float)38.0f);
        diffPitch = MathHelper.clamp((float)diffPitch, (float)-18.0f, (float)18.0f);
        if (this.snapBackTicks > 0) {
            diffYaw *= 0.62f;
            diffPitch *= 0.58f;
        }
        this.updateAdj(canAttack, hold, target);
        float newYaw = currentYaw + diffYaw * speed * Aura.adjYaw + this.snapBackYaw * (this.snapBackTicks > 0 ? 1.0f : 0.0f) + this.flickYaw + Aura.otvodkaYaw;
        float newPitch = currentPitch + diffPitch * speed * Aura.adjPitch + this.snapBackPitch * (this.snapBackTicks > 0 ? 0.75f : 0.0f) + this.flickPitch + Aura.otvodkaPitch;
        if (this.snapBackTicks > 0) {
            this.snapBackYaw *= 0.68f;
            this.snapBackPitch *= 0.72f;
        }
        if (this.flickTicks > 0) {
            this.flickYaw *= 0.55f;
            this.flickPitch *= 0.58f;
        }
        if ((gcd = GCDUtil.getGCDValue()) > 0.0f) {
            newYaw = currentYaw + (float)Math.round((newYaw - currentYaw) / gcd) * gcd;
            newPitch = currentPitch + (float)Math.round((newPitch - currentPitch) / gcd) * gcd;
        }
        newPitch = MathHelper.clamp((float)newPitch, (float)-89.0f, (float)89.0f);
        RotationStorage.update(new Rotation(newYaw, newPitch), 360.0f, 360.0f, 42.0f, 36.0f, 1, 1, Aura.clientLook.isState());
    }

    private float resolveSpeed(boolean canAttack, boolean hold, float diffYaw, float diffPitch) {
        float rotDiff = (float)Math.hypot(Math.abs(diffYaw), Math.abs(diffPitch));
        if (canAttack || hold) {
            return MathHelper.clamp((float)(0.78f + rotDiff / 220.0f), (float)0.72f, (float)0.92f);
        }
        return MathHelper.clamp((float)(0.48f + rotDiff / 360.0f), (float)0.38f, (float)0.68f);
    }

    private void updateAdj(boolean canAttack, boolean hold, LivingEntity target) {
        boolean onTarget;
        Vec3d eye = FTRotation.mc.player.getCameraPosVec(1.0f);
        Vec3d look = FTRotation.mc.player.getRotationVec(1.0f);
        Box box = this.getPredictedBox(target);
        boolean bl = onTarget = box.raycast(eye, eye.add(look.multiply(999.0))).isPresent() || box.contains(eye);
        if (onTarget) {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw - this.rnd.nextFloat(0.004f, 0.018f)), (float)0.35f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch - this.rnd.nextFloat(0.004f, 0.016f)), (float)0.35f, (float)1.0f);
        } else {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw + this.rnd.nextFloat(0.006f, 0.014f)), (float)0.0f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch + this.rnd.nextFloat(0.005f, 0.012f)), (float)0.0f, (float)1.0f);
        }
        if (canAttack || hold) {
            Aura.adjYaw = MathHelper.lerp((float)0.22f, (float)Aura.adjYaw, (float)0.94f);
            Aura.adjPitch = MathHelper.lerp((float)0.22f, (float)Aura.adjPitch, (float)0.92f);
        }
    }

    private void updateDrift(boolean canAttack, boolean hold) {
        if (canAttack || hold) {
            this.driftYaw *= 0.82f;
            this.driftPitch *= 0.84f;
            return;
        }
        if (this.tick % (2 + this.rnd.nextInt(2)) != 0) {
            this.driftYaw *= 0.88f;
            this.driftPitch *= 0.9f;
            return;
        }
        this.driftYaw += (this.rnd.nextFloat() - 0.5f) * 0.22f;
        this.driftPitch += (this.rnd.nextFloat() - 0.5f) * 0.11f;
        this.driftYaw = MathHelper.clamp((float)this.driftYaw, (float)-0.55f, (float)0.55f);
        this.driftPitch = MathHelper.clamp((float)this.driftPitch, (float)-0.28f, (float)0.28f);
    }

    private void updateJitter(boolean canAttack, boolean hold) {
        if (canAttack || hold) {
            this.jitterYaw *= 0.7f;
            this.jitterPitch *= 0.72f;
            return;
        }
        this.jitterYaw = (float)(Math.sin((double)this.tick * 1.85) * 0.08 + Math.cos((double)this.tick * 2.45 + 1.2) * 0.06);
        this.jitterPitch = (float)(Math.sin((double)this.tick * 2.05 + 0.7) * 0.05 + Math.cos((double)this.tick * 2.65) * 0.04);
    }

    private void updateFlick(boolean canAttack, boolean hold, LivingEntity target) {
        if (canAttack || hold || this.flickCd > 0) {
            return;
        }
        if (this.rnd.nextFloat() > 0.965f) {
            float yawSign = this.rnd.nextBoolean() ? -1.0f : 1.0f;
            this.flickYaw = yawSign * this.rnd.nextFloat(18.0f, 34.0f);
            this.flickPitch = -this.rnd.nextFloat(8.0f, 22.0f);
            this.flickTicks = 2 + this.rnd.nextInt(2);
            this.flickCd = 28 + this.rnd.nextInt(28);
            return;
        }
        float yawDiff = Math.abs(MathHelper.wrapDegrees((float)(RotationUtils.getRotations((Vec3d)this.aimPoint).x - FTRotation.mc.player.getYaw())));
        if (yawDiff > 22.0f && this.rnd.nextFloat() > 0.88f) {
            this.flickYaw = Math.signum(MathHelper.wrapDegrees((float)(RotationUtils.getRotations((Vec3d)target.getBoundingBox().getCenter()).x - FTRotation.mc.player.getYaw()))) * this.rnd.nextFloat(6.0f, 14.0f);
            this.flickPitch = (this.rnd.nextFloat() - 0.65f) * this.rnd.nextFloat(4.0f, 9.0f);
            this.flickTicks = 2 + this.rnd.nextInt(3);
            this.flickCd = 18 + this.rnd.nextInt(16);
        }
    }

    private void buildOffPoints(LivingEntity target) {
        Box box = this.getPredictedBox(target);
        double w2 = box.maxX - box.minX;
        double h2 = box.maxY - box.minY;
        double d2 = box.maxZ - box.minZ;
        double cx = box.minX + w2 * 0.5;
        double cz = box.minZ + d2 * 0.5;
        this.offPoints = new Vec3d[10];
        this.offPoints[0] = new Vec3d(box.minX - w2 * this.rnd.nextDouble(0.08, 0.22), box.minY + h2 * this.rnd.nextDouble(0.78, 0.95), cz);
        this.offPoints[1] = new Vec3d(box.maxX + w2 * this.rnd.nextDouble(0.08, 0.2), box.minY + h2 * this.rnd.nextDouble(0.72, 0.9), cz);
        this.offPoints[2] = new Vec3d(cx, box.maxY + h2 * this.rnd.nextDouble(0.05, 0.18), cz);
        this.offPoints[3] = new Vec3d(cx, box.minY + h2 * this.rnd.nextDouble(0.15, 0.35), cz);
        this.offPoints[4] = new Vec3d(box.minX + w2 * 0.15, box.minY + h2 * 0.62, box.minZ - d2 * this.rnd.nextDouble(0.1, 0.28));
        this.offPoints[5] = new Vec3d(box.maxX - w2 * 0.15, box.minY + h2 * 0.58, box.maxZ + d2 * this.rnd.nextDouble(0.1, 0.25));
        this.offPoints[6] = new Vec3d(cx + w2 * this.rnd.nextDouble(-0.35, 0.35), box.minY + h2 * this.rnd.nextDouble(0.45, 0.7), cz + d2 * this.rnd.nextDouble(-0.35, 0.35));
        this.offPoints[7] = new Vec3d(box.minX - w2 * 0.12, box.minY + h2 * 0.82, box.maxZ + d2 * 0.18);
        this.offPoints[8] = new Vec3d(box.maxX + w2 * 0.14, box.minY + h2 * 0.48, box.minZ - d2 * 0.16);
        this.offPoints[9] = new Vec3d(cx, box.minY + h2 * this.rnd.nextDouble(0.82, 0.98), cz + d2 * this.rnd.nextDouble(-0.42, 0.42));
    }
}

