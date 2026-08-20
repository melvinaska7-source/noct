package polar.ru.client.modules.impl.combat.components.rotations;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.FreeLookStorage;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.math.MathUtils;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;
import polar.ru.client.modules.impl.combat.components.rotations.neuro.NeuroAI;
import polar.ru.client.modules.impl.combat.components.rotations.neuro.NeuroPatternRecorder;

public class NeuroRotation
extends RotationsSystem
implements QClient {
    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    public static final NeuroAI neuroAI = new NeuroAI();
    public static final NeuroPatternRecorder recorder = new NeuroPatternRecorder();
    private float currentYaw;
    private float currentPitch;
    private boolean initialized = false;
    private Queue<float[]> recentMovements = new ArrayDeque<float[]>();
    private static final int MOVEMENT_HISTORY_SIZE = 10;
    private boolean isAttacking = false;
    private int attackCooldown = 0;
    private float distanceToHitbox = 999.0f;
    private boolean nearHitbox = false;
    private float jitterYaw = 0.0f;
    private float jitterPitch = 0.0f;
    private float jitterYawVelocity = 0.0f;
    private float jitterPitchVelocity = 0.0f;
    private long lastJitterYawTime = 0L;
    private long lastJitterPitchTime = 0L;
    private boolean snapBackActive = false;
    private float snapBackYaw = 0.0f;
    private float snapBackPitch = 0.0f;
    private int snapBackDuration = 0;
    private Vec3d targetPoint;

    public void reset() {
        this.initialized = false;
        this.isAttacking = false;
        this.attackCooldown = 0;
        this.distanceToHitbox = 999.0f;
        this.nearHitbox = false;
        this.jitterYaw = 0.0f;
        this.jitterPitch = 0.0f;
        this.jitterYawVelocity = 0.0f;
        this.jitterPitchVelocity = 0.0f;
        this.snapBackActive = false;
        this.snapBackDuration = 0;
        this.recentMovements.clear();
        this.targetPoint = null;
    }

    public void onAttack() {
        this.isAttacking = true;
        this.attackCooldown = 10 + this.rnd.nextInt(5);
        if (this.rnd.nextFloat() < 0.3f) {
            this.generateSnapBack();
        }
    }

    @Override
    public void updateRotations(LivingEntity target) {
        float gcd;
        if (NeuroRotation.mc.player == null || target == null) {
            return;
        }
        if (recorder.isRecording()) {
            recorder.update(target);
        }
        if (this.attackCooldown > 0) {
            --this.attackCooldown;
            if (this.attackCooldown == 0) {
                this.isAttacking = false;
            }
        }
        Vec3d eyePos = NeuroRotation.mc.player.getCameraPosVec(1.0f);
        if (target.getBoundingBox().contains(eyePos)) {
            Aura.adjYaw = 0.0f;
            Aura.adjPitch = 0.0f;
            RotationStorage.update(new Rotation(NeuroRotation.mc.player.getYaw(), NeuroRotation.mc.player.getPitch()), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, Aura.clientLook.isState());
            return;
        }
        if (!neuroAI.hasPattern()) {
            RotationStorage.update(new Rotation(FreeLookStorage.getFreeYaw(), FreeLookStorage.getFreePitch()), MathUtils.random(100.0f, 170.0f), MathUtils.random(100.0f, 170.0f), MathUtils.random(100.0f, 170.0f), MathUtils.random(100.0f, 170.0f), 1, 6, false);
            return;
        }
        if (!this.initialized) {
            this.currentYaw = NeuroRotation.mc.player.getYaw();
            this.currentPitch = NeuroRotation.mc.player.getPitch();
            this.initialized = true;
        }
        this.updateTargetPoint(target, eyePos);
        this.calculateDistanceToHitbox(target, eyePos);
        this.updateYawJitter();
        this.updatePitchJitter();
        this.updateSnapBack();
        float[] aiRotation = neuroAI.getNextRotation(target, this.currentYaw, this.currentPitch, this.isAttacking, this.distanceToHitbox);
        float newYaw = aiRotation[0];
        float newPitch = aiRotation[1];
        float deltaYaw = MathHelper.wrapDegrees((float)(newYaw - this.currentYaw));
        float deltaPitch = newPitch - this.currentPitch;
        this.addMovementToHistory(deltaYaw, deltaPitch);
        this.currentYaw = newYaw;
        this.currentPitch = newPitch;
        newYaw += this.jitterYaw;
        newPitch += this.jitterPitch;
        if (this.snapBackActive) {
            newYaw += this.snapBackYaw;
            newPitch += this.snapBackPitch;
        }
        if ((gcd = GCDUtil.getGCDValue()) > 0.0f) {
            float baseYaw = NeuroRotation.mc.player.getYaw();
            float basePitch = NeuroRotation.mc.player.getPitch();
            newYaw = baseYaw + (float)Math.round((newYaw - baseYaw) / gcd) * gcd;
            newPitch = basePitch + (float)Math.round((newPitch - basePitch) / gcd) * gcd;
        }
        newPitch = MathHelper.clamp((float)newPitch, (float)-89.0f, (float)89.0f);
        this.updateAdj(target, eyePos);
        RotationStorage.update(new Rotation(newYaw, newPitch), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, Aura.clientLook.isState());
    }

    private void addMovementToHistory(float deltaYaw, float deltaPitch) {
        if (this.recentMovements.size() >= 10) {
            this.recentMovements.poll();
        }
        this.recentMovements.offer(new float[]{deltaYaw, deltaPitch});
    }

    private void updateTargetPoint(LivingEntity target, Vec3d eyePos) {
        Vec3d point;
        Box box = target.getBoundingBox();
        this.targetPoint = point = new Vec3d(box.minX + (box.maxX - box.minX) * (0.3 + this.rnd.nextDouble() * 0.4), box.minY + (box.maxY - box.minY) * (0.4 + this.rnd.nextDouble() * 0.3), box.minZ + (box.maxZ - box.minZ) * (0.3 + this.rnd.nextDouble() * 0.4));
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

    private void updateYawJitter() {
        int interval;
        long currentTime = System.currentTimeMillis();
        int n2 = interval = this.nearHitbox ? 100 : 70;
        if (currentTime - this.lastJitterYawTime > (long)(interval + this.rnd.nextInt(70))) {
            float intensity = this.nearHitbox ? 0.3f : 0.6f + this.rnd.nextFloat() * 0.4f;
            this.jitterYawVelocity = (this.rnd.nextFloat() - 0.5f) * intensity * 3.5f;
            this.lastJitterYawTime = currentTime;
        }
        this.jitterYaw += this.jitterYawVelocity;
        this.jitterYaw = MathHelper.clamp((float)this.jitterYaw, (float)-2.5f, (float)2.5f);
        this.jitterYaw *= 0.88f;
        this.jitterYawVelocity *= 0.85f;
    }

    private void updatePitchJitter() {
        int interval;
        long currentTime = System.currentTimeMillis();
        int n2 = interval = this.nearHitbox ? 120 : 80;
        if (currentTime - this.lastJitterPitchTime > (long)(interval + this.rnd.nextInt(70))) {
            float intensity = this.nearHitbox ? 0.25f : 0.5f + this.rnd.nextFloat() * 0.3f;
            this.jitterPitchVelocity = (this.rnd.nextFloat() - 0.5f) * intensity * 2.2f;
            this.lastJitterPitchTime = currentTime;
        }
        this.jitterPitch += this.jitterPitchVelocity;
        this.jitterPitch = MathHelper.clamp((float)this.jitterPitch, (float)-1.8f, (float)1.8f);
        this.jitterPitch *= 0.9f;
        this.jitterPitchVelocity *= 0.87f;
    }

    private void generateSnapBack() {
        if (!this.isAttacking) {
            return;
        }
        float snapBackStrength = 5.0f + this.rnd.nextFloat() * 10.0f;
        this.snapBackYaw = (this.rnd.nextFloat() - 0.7f) * snapBackStrength;
        this.snapBackPitch = (this.rnd.nextFloat() - 0.6f) * snapBackStrength * 0.6f;
        this.snapBackDuration = 2 + this.rnd.nextInt(3);
        this.snapBackActive = true;
    }

    private void updateSnapBack() {
        if (this.snapBackActive) {
            --this.snapBackDuration;
            if (this.snapBackDuration <= 0) {
                this.snapBackActive = false;
                this.snapBackYaw = 0.0f;
                this.snapBackPitch = 0.0f;
            } else {
                this.snapBackYaw *= 0.7f;
                this.snapBackPitch *= 0.7f;
            }
        }
    }

    private void updateAdj(LivingEntity target, Vec3d eyePos) {
        Vec3d lookVec = NeuroRotation.mc.player.getRotationVec(1.0f);
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
}

