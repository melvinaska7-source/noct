package polar.ru.client.modules.impl.combat.components.rotations;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;

public class SlothRotation
extends RotationsSystem
implements QClient {
    private LivingEntity trackedTarget;
    private float currentYaw;
    private float currentPitch;
    private float velocityYaw;
    private float velocityPitch;
    private double aimPointX;
    private double aimPointY;
    private double aimPointZ;
    private float noiseAngle;
    private final float noiseAmplitude = 1.8f;
    private int hitPhase;
    private int hitTimer;
    private float pitchBeforeHit;
    private long firstSeenTime;
    private int reactionMs;
    private boolean reactionComplete;
    private float lastSentYaw;
    private float lastSentPitch;
    private float smoothYaw;
    private float smoothPitch;

    public void reset() {
        this.trackedTarget = null;
        this.velocityPitch = 0.0f;
        this.velocityYaw = 0.0f;
        this.aimPointZ = 0.0;
        this.aimPointY = 0.0;
        this.aimPointX = 0.0;
        this.noiseAngle = 0.0f;
        this.hitTimer = 0;
        this.hitPhase = 0;
        this.firstSeenTime = 0L;
        this.reactionComplete = false;
        this.reactionMs = 0;
        if (SlothRotation.mc.player != null) {
            this.currentYaw = SlothRotation.mc.player.getYaw();
            this.currentPitch = SlothRotation.mc.player.getPitch();
            this.lastSentYaw = this.currentYaw;
            this.lastSentPitch = this.currentPitch;
            this.smoothYaw = this.currentYaw;
            this.smoothPitch = this.currentPitch;
        } else {
            this.currentPitch = 0.0f;
            this.currentYaw = 0.0f;
            this.lastSentPitch = 0.0f;
            this.lastSentYaw = 0.0f;
            this.smoothPitch = 0.0f;
            this.smoothYaw = 0.0f;
        }
    }

    private float calcGcd() {
        double s2 = (Double)SlothRotation.mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        return (float)(s2 * s2 * s2 * 1.2);
    }

    private void pickAimPoint(LivingEntity e2) {
        Box bb2 = e2.getBoundingBox();
        double w2 = bb2.maxX - bb2.minX;
        double h2 = bb2.maxY - bb2.minY;
        double d2 = bb2.maxZ - bb2.minZ;
        this.aimPointX = (Math.random() - 0.5) * w2 * 0.12;
        this.aimPointY = (Math.random() - 0.5) * h2 * 0.11;
        this.aimPointZ = (Math.random() - 0.5) * d2 * 0.12;
    }

    public void onAttack() {
        this.hitPhase = 1;
        this.hitTimer = 0;
        this.pitchBeforeHit = this.currentPitch;
    }

    private float measureAngle(LivingEntity e2) {
        if (SlothRotation.mc.player == null) {
            return 0.0f;
        }
        Vec3d eyes = SlothRotation.mc.player.getEyePos();
        Vec3d mid = e2.getBoundingBox().getCenter();
        Vec3d delta = mid.subtract(eyes);
        float needYaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0f;
        float needPitch = (float)(-Math.toDegrees(Math.atan2(delta.y, delta.horizontalLength())));
        float dYaw = Math.abs(MathHelper.wrapDegrees((float)(needYaw - SlothRotation.mc.player.getYaw())));
        float dPitch = Math.abs(needPitch - SlothRotation.mc.player.getPitch());
        return dYaw + dPitch;
    }

    private int computeReaction(float angle) {
        if (angle > 130.0f) {
            return 140 + (int)(Math.random() * 90.0);
        }
        if (angle > 70.0f) {
            return 90 + (int)(Math.random() * 60.0);
        }
        if (angle > 30.0f) {
            return 45 + (int)(Math.random() * 35.0);
        }
        return 12 + (int)(Math.random() * 20.0);
    }

    private boolean isMovingForward() {
        if (SlothRotation.mc.player == null) {
            return false;
        }
        return SlothRotation.mc.options.forwardKey.isPressed();
    }

    private boolean isOvertakingTarget(LivingEntity target) {
        if (SlothRotation.mc.player == null || target == null) {
            return false;
        }
        Vec3d playerPos = SlothRotation.mc.player.getPos();
        Vec3d targetPos = target.getPos();
        Vec3d playerVel = new Vec3d(SlothRotation.mc.player.getX() - SlothRotation.mc.player.prevX, SlothRotation.mc.player.getY() - SlothRotation.mc.player.prevY, SlothRotation.mc.player.getZ() - SlothRotation.mc.player.prevZ);
        Vec3d targetVel = new Vec3d(target.getX() - target.prevX, target.getY() - target.prevY, target.getZ() - target.prevZ);
        Vec3d toTarget = targetPos.subtract(playerPos).normalize();
        double playerSpeedToTarget = playerVel.dotProduct(toTarget);
        double targetSpeedToPlayer = targetVel.dotProduct(toTarget.multiply(-1.0));
        double relativeSpeed = playerSpeedToTarget + targetSpeedToPlayer;
        double distance = Math.sqrt(Math.pow(playerPos.x - targetPos.x, 2.0) + Math.pow(playerPos.z - targetPos.z, 2.0));
        return relativeSpeed > 0.05 && distance < 4.0;
    }

    private float[] generateNoise(float dist) {
        this.noiseAngle += 0.042f + (float)(Math.random() * (double)0.018f);
        float scale = MathHelper.clamp((float)(dist / 4.5f), (float)0.25f, (float)1.0f);
        float amp = 1.8f * scale;
        float n1 = (float)Math.sin((double)this.noiseAngle * 0.87) * 0.38f;
        float n2 = (float)Math.sin((double)this.noiseAngle * 1.43 + 0.75) * 0.28f;
        float n3 = (float)Math.cos((double)this.noiseAngle * 1.18 + 0.35) * 0.32f;
        float n4 = (float)Math.cos((double)this.noiseAngle * 1.76 + 1.42) * 0.23f;
        float yawNoise = (n1 + n2) * amp;
        float pitchNoise = (n3 + n4) * amp * 0.52f;
        return new float[]{yawNoise += ((float)Math.random() - 0.5f) * amp * 0.13f, pitchNoise += ((float)Math.random() - 0.5f) * amp * 0.09f};
    }

    private float smoothStep(float x2) {
        x2 = MathHelper.clamp((float)x2, (float)0.0f, (float)1.0f);
        return x2 * x2 * (3.0f - 2.0f * x2);
    }

    private float accelCurve(float x2) {
        x2 = MathHelper.clamp((float)x2, (float)0.0f, (float)1.0f);
        return 1.0f - (1.0f - x2) * (1.0f - x2);
    }

    private float springInterp(float current, float target, float vel, float stiffness, float damping) {
        float diff = target - current;
        float acc = diff * stiffness - vel * damping;
        return vel + acc;
    }

    private float smoothLerp(float from, float to, float alpha) {
        alpha = MathHelper.clamp((float)alpha, (float)0.0f, (float)1.0f);
        float delta = MathHelper.wrapDegrees((float)(to - from));
        return from + delta * alpha;
    }

    private float calculateCurrentAngle(float targetYaw, float targetPitch) {
        float dYaw = Math.abs(MathHelper.wrapDegrees((float)(targetYaw - this.currentYaw)));
        float dPitch = Math.abs(targetPitch - this.currentPitch);
        return dYaw + dPitch;
    }

    @Override
    public void updateRotations(LivingEntity target) {
        if (SlothRotation.mc.player == null || target == null) {
            return;
        }
        boolean playerFlying = SlothRotation.mc.player.isGliding();
        if (this.trackedTarget != target) {
            this.trackedTarget = target;
            this.currentYaw = SlothRotation.mc.player.getYaw();
            this.currentPitch = SlothRotation.mc.player.getPitch();
            this.lastSentYaw = this.currentYaw;
            this.lastSentPitch = this.currentPitch;
            this.smoothYaw = this.currentYaw;
            this.smoothPitch = this.currentPitch;
            this.velocityPitch = 0.0f;
            this.velocityYaw = 0.0f;
            this.pickAimPoint(target);
            this.hitTimer = 0;
            this.hitPhase = 0;
            this.noiseAngle = (float)(Math.random() * Math.PI * 2.0);
            float angleDiff = this.measureAngle(target);
            this.reactionMs = this.computeReaction(angleDiff);
            this.firstSeenTime = System.currentTimeMillis();
            this.reactionComplete = false;
        }
        Vec3d eyePos = SlothRotation.mc.player.getEyePos();
        Vec3d targetCenter = this.getPredictedPoint(target, target.getBoundingBox().getCenter());
        float distance = (float)eyePos.distanceTo(targetCenter);
        float gcd = this.calcGcd();
        if (!this.reactionComplete) {
            long elapsed = System.currentTimeMillis() - this.firstSeenTime;
            if (elapsed < (long)this.reactionMs) {
                float jitterY = ((float)Math.random() - 0.5f) * 0.22f;
                float jitterP = ((float)Math.random() - 0.5f) * 0.14f;
                float outY = this.lastSentYaw + jitterY;
                float outP = MathHelper.clamp((float)(this.lastSentPitch + jitterP), (float)-89.0f, (float)89.0f);
                outY -= (outY - this.lastSentYaw) % gcd;
                outP -= (outP - this.lastSentPitch) % gcd;
                this.lastSentYaw = outY;
                this.lastSentPitch = outP;
                RotationStorage.update(new Rotation(outY, outP), 360.0f, 45.0f, 45.0f, 45.0f, 0, 1, Aura.clientLook.isState());
                return;
            }
            this.reactionComplete = true;
        }
        float[] noise = this.generateNoise(distance);
        if (this.hitPhase > 0) {
            ++this.hitTimer;
            int upDuration = 25;
            int downDuration = 20;
            float targetPitchUp = -89.0f;
            if (this.hitPhase == 1) {
                float t2 = (float)this.hitTimer / (float)upDuration;
                t2 = MathHelper.clamp((float)t2, (float)0.0f, (float)1.0f);
                float curved = this.accelCurve(t2);
                this.currentPitch = MathHelper.lerp((float)curved, (float)this.pitchBeforeHit, (float)targetPitchUp);
                if (this.hitTimer >= upDuration) {
                    this.hitPhase = 2;
                    this.hitTimer = 0;
                }
            } else if (this.hitPhase == 2) {
                float goal = this.pitchBeforeHit;
                float t3 = (float)this.hitTimer / (float)downDuration;
                t3 = MathHelper.clamp((float)t3, (float)0.0f, (float)1.0f);
                float curved = this.smoothStep(t3);
                this.currentPitch = MathHelper.lerp((float)curved, (float)targetPitchUp, (float)goal);
                if (this.hitTimer >= downDuration) {
                    this.hitPhase = 0;
                    this.hitTimer = 0;
                }
            }
            float outY = this.currentYaw + noise[0];
            float outP = MathHelper.clamp((float)(this.currentPitch + noise[1]), (float)-89.0f, (float)89.0f);
            outY -= (outY - this.lastSentYaw) % gcd;
            outP -= (outP - this.lastSentPitch) % gcd;
            this.lastSentYaw = outY;
            this.lastSentPitch = outP;
            RotationStorage.update(new Rotation(outY, outP), 360.0f, 45.0f, 45.0f, 45.0f, 0, 1, Aura.clientLook.isState());
            return;
        }
        if (Math.random() < 0.015) {
            this.pickAimPoint(target);
        }
        Vec3d targetVel = new Vec3d(target.getX() - target.prevX, target.getY() - target.prevY, target.getZ() - target.prevZ);
        int predictTicks = this.shouldUseElytraPredict(target) ? 0 : 2;
        Vec3d predictedCenter = targetCenter.add(targetVel.multiply((double)predictTicks));
        Vec3d aimPos = predictedCenter.add(this.aimPointX, this.aimPointY, this.aimPointZ);
        Vec3d direction = aimPos.subtract(eyePos);
        float wantYaw = (float)MathHelper.wrapDegrees((double)(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0));
        float wantPitch = (float)(-Math.toDegrees(Math.atan2(direction.y, direction.horizontalLength())));
        float diffYaw = MathHelper.wrapDegrees((float)(wantYaw - this.currentYaw));
        float diffPitch = wantPitch - this.currentPitch;
        float speedMultiplier = 1.0f;
        if (playerFlying) {
            float currentAngle = this.calculateCurrentAngle(wantYaw, wantPitch);
            if (currentAngle > 120.0f) {
                speedMultiplier = 0.18f;
            } else if (currentAngle > 80.0f) {
                float t = (currentAngle - 80.0f) / 40.0f;
                speedMultiplier = MathHelper.lerp((float)this.smoothStep(t), (float)0.35f, (float)0.18f);
            } else if (currentAngle > 25.0f) {
                float t = (currentAngle - 25.0f) / 55.0f;
                speedMultiplier = MathHelper.lerp((float)this.smoothStep(t), (float)0.65f, (float)0.35f);
            } else {
                speedMultiplier = 0.65f + 0.35f * (1.0f - currentAngle / 25.0f);
            }
        } else {
            boolean movingForward = this.isMovingForward();
            boolean overtaking = this.isOvertakingTarget(target);
            if (movingForward || overtaking) {
                speedMultiplier = 0.5f;
            }
        }
        float stiffness = (0.038f + (float)Math.random() * 0.009f) * speedMultiplier;
        float damping = 0.68f + 0.12f * (1.0f - speedMultiplier);
        float totalDiff = (float)Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
        if (totalDiff > 32.0f) {
            stiffness += 0.018f * speedMultiplier;
        } else if (totalDiff < 4.2f) {
            stiffness *= 0.48f;
        }
        this.velocityYaw = this.springInterp(this.currentYaw, this.currentYaw + diffYaw, this.velocityYaw, stiffness += MathHelper.clamp((float)((distance - 1.6f) / 7.5f), (float)0.0f, (float)0.045f) * speedMultiplier, damping);
        this.velocityPitch = this.springInterp(this.currentPitch, wantPitch, this.velocityPitch, stiffness * 0.87f, damping);
        float maxVelYaw = 7.5f * speedMultiplier;
        float maxVelPitch = 5.8f * speedMultiplier;
        this.velocityYaw = MathHelper.clamp((float)this.velocityYaw, (float)(-maxVelYaw), (float)maxVelYaw);
        this.velocityPitch = MathHelper.clamp((float)this.velocityPitch, (float)(-maxVelPitch), (float)maxVelPitch);
        this.currentYaw += this.velocityYaw;
        this.currentPitch += this.velocityPitch;
        this.currentPitch = MathHelper.clamp((float)this.currentPitch, (float)-89.0f, (float)89.0f);
        float smoothFactor = playerFlying ? 0.3f + speedMultiplier * 0.4f : 0.85f;
        this.smoothYaw = this.smoothLerp(this.smoothYaw, this.currentYaw, smoothFactor);
        this.smoothPitch = this.smoothLerp(this.smoothPitch, this.currentPitch, smoothFactor * 0.95f);
        float outY = this.smoothYaw + noise[0];
        float outP = this.smoothPitch + noise[1];
        outP = MathHelper.clamp((float)outP, (float)-89.0f, (float)89.0f);
        outY -= (outY - this.lastSentYaw) % gcd;
        outP -= (outP - this.lastSentPitch) % gcd;
        this.lastSentYaw = outY;
        this.lastSentPitch = outP;
        RotationStorage.update(new Rotation(outY, outP), 360.0f, 45.0f, 45.0f, 45.0f, 0, 1, Aura.clientLook.isState());
    }
}

