package polar.ru.client.modules.impl.combat.components.rotations;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.FreeLookStorage;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;

public class FunTimeRotation
extends RotationsSystem
implements QClient {
    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    private BodyPart currentBodyPart = BodyPart.BODY;
    private Vec3d targetPoint;
    private float targetYaw;
    private float targetPitch;
    private float currentYaw;
    private float currentPitch;
    private float rotationSpeed = 1.0f;
    private long lastSpeedChange = 0L;
    private long lastAttackTime = -1L;
    private boolean isPreparingAttack = false;
    private long attackPreparationStartMs = -1L;
    private boolean initialized = false;
    private int tick = 0;
    private boolean isAttacking = false;
    private int attackCooldown = 0;
    private float distanceToHitbox = 999.0f;
    private boolean nearHitbox = false;

    public void reset() {
        this.targetPoint = null;
        this.initialized = false;
        this.tick = 0;
        this.isAttacking = false;
        this.attackCooldown = 0;
        this.currentBodyPart = BodyPart.BODY;
        this.distanceToHitbox = 999.0f;
        this.nearHitbox = false;
        this.lastAttackTime = -1L;
        this.isPreparingAttack = false;
        this.attackPreparationStartMs = -1L;
    }

    public void onAttack() {
        this.changeBodyPart();
        this.randomizeSpeed();
        this.isAttacking = true;
        this.attackCooldown = 10 + this.rnd.nextInt(5);
        this.lastAttackTime = System.currentTimeMillis();
        this.isPreparingAttack = false;
        this.attackPreparationStartMs = -1L;
    }

    public void prepareAttack() {
        if (!this.isPreparingAttack) {
            this.isPreparingAttack = true;
            this.attackPreparationStartMs = System.currentTimeMillis();
        }
    }

    @Override
    public void updateRotations(LivingEntity target) {
        float newPitch;
        float newYaw;
        long prepTime;
        if (FunTimeRotation.mc.player == null || target == null) {
            this.isAttacking = false;
            this.isPreparingAttack = false;
            return;
        }
        ++this.tick;
        if (this.attackCooldown > 0) {
            --this.attackCooldown;
            if (this.attackCooldown == 0) {
                this.isAttacking = false;
            }
        }
        if (this.isPreparingAttack && this.attackPreparationStartMs > 0L && (prepTime = System.currentTimeMillis() - this.attackPreparationStartMs) > 300L) {
            this.isPreparingAttack = false;
        }
        Vec3d eyePos = FunTimeRotation.mc.player.getCameraPosVec(1.0f);
        if (target.getBoundingBox().contains(eyePos)) {
            Aura.adjYaw = 0.0f;
            Aura.adjPitch = 0.0f;
            RotationStorage.update(new Rotation(FunTimeRotation.mc.player.getYaw(), FunTimeRotation.mc.player.getPitch()), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, Aura.clientLook.isState());
            return;
        }
        this.updateTargetPoint(target, eyePos);
        this.calculateDistanceToHitbox(target, eyePos);
        if (!this.initialized) {
            this.currentYaw = FunTimeRotation.mc.player.getYaw();
            this.currentPitch = FunTimeRotation.mc.player.getPitch();
            this.initialized = true;
        }
        this.updateRotationSpeed();
        Vec2f targetAngles = RotationUtils.getRotations(this.targetPoint);
        this.targetYaw = targetAngles.x;
        this.targetPitch = targetAngles.y;
        if (this.isPreparingAttack && this.attackPreparationStartMs > 0L) {
            long prepTime2 = System.currentTimeMillis() - this.attackPreparationStartMs;
            float prepProgress = MathHelper.clamp((float)((float)prepTime2 / 250.0f), (float)0.0f, (float)1.0f);
            float yawDiff = MathHelper.wrapDegrees((float)(this.targetYaw - this.currentYaw));
            float pitchDiff = this.targetPitch - this.currentPitch;
            float smoothSpeed = prepProgress * prepProgress * 0.3f;
            this.currentYaw += yawDiff * smoothSpeed;
            this.currentPitch += pitchDiff * smoothSpeed;
            newYaw = this.currentYaw;
            newPitch = this.currentPitch;
        } else if (this.isAttacking) {
            float yawDiff = MathHelper.wrapDegrees((float)(this.targetYaw - this.currentYaw));
            float pitchDiff = this.targetPitch - this.currentPitch;
            float totalDelta = (float)Math.hypot(yawDiff, pitchDiff);
            float yawLimit = Math.abs(yawDiff / totalDelta) * 130.0f;
            float pitchLimit = Math.abs(pitchDiff / totalDelta) * 130.0f;
            this.currentYaw = MathHelper.lerp((float)0.85f, (float)this.currentYaw, (float)(this.currentYaw + MathHelper.clamp((float)yawDiff, (float)(-yawLimit), (float)yawLimit)));
            this.currentPitch = MathHelper.lerp((float)0.85f, (float)this.currentPitch, (float)(this.currentPitch + MathHelper.clamp((float)pitchDiff, (float)(-pitchLimit), (float)pitchLimit)));
            newYaw = this.currentYaw;
            newPitch = this.currentPitch;
        } else {
            float targetFreeYaw = FreeLookStorage.isActive() ? FreeLookStorage.getFreeYaw() : FunTimeRotation.mc.player.getYaw();
            float targetFreePitch = FreeLookStorage.isActive() ? FreeLookStorage.getFreePitch() : FunTimeRotation.mc.player.getPitch();
            newYaw = targetFreeYaw;
            newPitch = targetFreePitch;
            this.currentYaw = newYaw;
            this.currentPitch = newPitch;
        }
        float gcd = GCDUtil.getGCDValue();
        if (gcd > 0.0f) {
            float baseYaw = FunTimeRotation.mc.player.getYaw();
            float basePitch = FunTimeRotation.mc.player.getPitch();
            newYaw = baseYaw + (float)Math.round((newYaw - baseYaw) / gcd) * gcd;
            newPitch = basePitch + (float)Math.round((newPitch - basePitch) / gcd) * gcd;
        }
        newPitch = MathHelper.clamp((float)newPitch, (float)-89.0f, (float)89.0f);
        this.updateAdj(target, eyePos);
        boolean useClientLook = !this.isAttacking && !this.isPreparingAttack;
        RotationStorage.update(new Rotation(newYaw, newPitch), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, useClientLook && Aura.clientLook.isState());
    }

    private void calculateDistanceToHitbox(LivingEntity target, Vec3d eyePos) {
        Box box = target.getBoundingBox();
        Vec3d closest = new Vec3d(MathHelper.clamp((double)eyePos.x, (double)box.minX, (double)box.maxX), MathHelper.clamp((double)eyePos.y, (double)box.minY, (double)box.maxY), MathHelper.clamp((double)eyePos.z, (double)box.minZ, (double)box.maxZ));
        this.distanceToHitbox = (float)eyePos.distanceTo(closest);
        this.nearHitbox = this.distanceToHitbox < 0.5f;
    }

    private float getSmoothFactorByDistance() {
        if (this.distanceToHitbox < 0.2f) {
            return 0.08f + this.rnd.nextFloat() * 0.04f;
        }
        if (this.distanceToHitbox < 0.4f) {
            return 0.12f + this.rnd.nextFloat() * 0.06f;
        }
        if (this.distanceToHitbox < 0.6f) {
            return 0.18f + this.rnd.nextFloat() * 0.08f;
        }
        return 0.25f + this.rnd.nextFloat() * 0.15f;
    }

    private void changeBodyPart() {
        BodyPart[] parts = BodyPart.values();
        this.currentBodyPart = parts[this.rnd.nextInt(parts.length)];
    }

    private void randomizeSpeed() {
        this.rotationSpeed = 0.15f + this.rnd.nextFloat() * 0.65f;
    }

    private void updateRotationSpeed() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastSpeedChange > (long)(200 + this.rnd.nextInt(200))) {
            this.randomizeSpeed();
            this.lastSpeedChange = currentTime;
        }
    }

    private void updateTargetPoint(LivingEntity target, Vec3d eyePos) {
        Box box = target.getBoundingBox();
        Vec3d point = switch (this.currentBodyPart.ordinal()) {
            case 0 -> new Vec3d(box.minX + (box.maxX - box.minX) * (0.4 + this.rnd.nextDouble() * 0.2), box.maxY - 0.1 - this.rnd.nextDouble() * 0.15, box.minZ + (box.maxZ - box.minZ) * (0.4 + this.rnd.nextDouble() * 0.2));
            case 3 -> new Vec3d(box.minX + (box.maxX - box.minX) * (0.3 + this.rnd.nextDouble() * 0.4), box.minY + (box.maxY - box.minY) * (0.6 + this.rnd.nextDouble() * 0.2), box.minZ + (box.maxZ - box.minZ) * (0.3 + this.rnd.nextDouble() * 0.4));
            case 1 -> new Vec3d(box.minX + (box.maxX - box.minX) * (0.3 + this.rnd.nextDouble() * 0.4), box.minY + (box.maxY - box.minY) * (0.4 + this.rnd.nextDouble() * 0.2), box.minZ + (box.maxZ - box.minZ) * (0.3 + this.rnd.nextDouble() * 0.4));
            case 2 -> new Vec3d(box.minX + (box.maxX - box.minX) * (0.3 + this.rnd.nextDouble() * 0.4), box.minY + (box.maxY - box.minY) * (0.1 + this.rnd.nextDouble() * 0.2), box.minZ + (box.maxZ - box.minZ) * (0.3 + this.rnd.nextDouble() * 0.4));
            default -> box.getCenter();
        };
        if (this.shouldUseElytraPredict(target)) {
            point = this.getPredictedPoint(target, point);
        }
        this.targetPoint = point;
    }

    private void updateAdj(LivingEntity target, Vec3d eyePos) {
        Vec3d lookVec = FunTimeRotation.mc.player.getRotationVec(1.0f);
        Vec3d reachVec = eyePos.add(lookVec.multiply(999.0));
        Box box = this.getPredictedBox(target);
        Optional hit = box.raycast(eyePos, reachVec);
        boolean inside = box.contains(eyePos);
        if (hit.isPresent() || inside) {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw - this.rnd.nextFloat(0.01f, 0.03f)), (float)0.0f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch - this.rnd.nextFloat(0.01f, 0.03f)), (float)0.0f, (float)1.0f);
        } else {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw + this.rnd.nextFloat(0.003f, 0.018f)), (float)0.0f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch + this.rnd.nextFloat(0.003f, 0.018f)), (float)0.0f, (float)1.0f);
        }
    }

    private static enum BodyPart {
        HEAD,
        BODY,
        LEGS,
        CHEST;

    }
}

