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

public class WellMineRotation
extends RotationsSystem
implements QClient {
    private LivingEntity currentTarget;
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private float acceleration = 0.0f;
    private boolean isBack = false;
    private double randomOffsetX = 0.0;
    private double randomOffsetY = 0.0;
    private double randomOffsetZ = 0.0;

    public void reset() {
        this.currentTarget = null;
        this.acceleration = 0.0f;
        this.isBack = false;
        this.randomOffsetX = 0.0;
        this.randomOffsetY = 0.0;
        this.randomOffsetZ = 0.0;
        if (WellMineRotation.mc.player != null) {
            this.lastYaw = WellMineRotation.mc.player.getYaw();
            this.lastPitch = WellMineRotation.mc.player.getPitch();
        } else {
            this.lastYaw = 0.0f;
            this.lastPitch = 0.0f;
        }
    }

    private float getGCDValue() {
        float sensitivity = (float)((Double)WellMineRotation.mc.options.getMouseSensitivity().getValue() * (double)0.6f + (double)0.2f);
        return sensitivity * sensitivity * sensitivity * 1.2f;
    }

    private void updateRandomOffset(LivingEntity target) {
        Box box = target.getBoundingBox();
        double boxWidth = box.maxX - box.minX;
        double boxHeight = box.maxY - box.minY;
        double boxDepth = box.maxZ - box.minZ;
        this.randomOffsetX = (Math.random() - 0.5) * boxWidth * 0.15;
        this.randomOffsetY = (Math.random() - 0.5) * boxHeight * 0.15;
        this.randomOffsetZ = (Math.random() - 0.5) * boxDepth * 0.15;
    }

    @Override
    public void updateRotations(LivingEntity target) {
        if (WellMineRotation.mc.player == null || target == null) {
            return;
        }
        if (this.currentTarget != target) {
            this.currentTarget = target;
            this.acceleration = 0.0f;
            this.isBack = false;
            this.lastYaw = WellMineRotation.mc.player.getYaw();
            this.lastPitch = WellMineRotation.mc.player.getPitch();
            this.updateRandomOffset(target);
        }
        Box box = this.getPredictedBox(target);
        Vec3d eyePos = WellMineRotation.mc.player.getEyePos();
        Vec3d centerPoint = box.getCenter().add(this.randomOffsetX, this.randomOffsetY, this.randomOffsetZ);
        Vec3d toTarget = centerPoint.subtract(eyePos);
        float centerYaw = (float)MathHelper.wrapDegrees((double)(Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0));
        float centerPitch = (float)(-Math.toDegrees(Math.atan2(toTarget.y, Math.hypot(toTarget.x, toTarget.z))));
        boolean bothGliding = WellMineRotation.mc.player.isGliding() && target.isGliding();
        Vec3d lookVec = WellMineRotation.mc.player.getRotationVec(1.0f);
        Vec3d endVec = eyePos.add(lookVec.multiply(bothGliding ? 1488.0 : 999.0));
        Box shrunkBox = box.expand(bothGliding ? 0.0 : -0.5);
        boolean inBox = shrunkBox.raycast(eyePos, endVec).isPresent();
        if (bothGliding) {
            if (this.isBack) {
                if (this.acceleration >= -0.02f) {
                    this.acceleration -= Math.abs(MathHelper.wrapDegrees((float)(centerYaw - this.lastYaw))) > 80.0f ? 0.15f : 0.02f;
                }
                if (this.acceleration <= -0.02f) {
                    this.isBack = false;
                    this.updateRandomOffset(target);
                }
            } else {
                this.acceleration += 0.0105f;
                if (this.acceleration >= 0.305f || inBox) {
                    this.isBack = true;
                }
            }
        } else if (this.isBack) {
            if (this.acceleration >= -0.15f) {
                float slowdownSpeed = Math.abs(MathHelper.wrapDegrees((float)(centerYaw - this.lastYaw))) > 80.0f ? 0.1f : 0.01f;
                this.acceleration -= (slowdownSpeed *= 0.9f + (float)Math.random() * 0.2f);
            }
            if (this.acceleration <= -0.15f) {
                this.isBack = false;
                this.updateRandomOffset(target);
            }
        } else {
            float accelSpeed = 0.0082f + ((float)Math.random() * 0.002f - 0.001f);
            this.acceleration += accelSpeed;
            float threshold = 0.184f + ((float)Math.random() * 0.03f - 0.015f);
            if (this.acceleration >= threshold || inBox) {
                this.isBack = true;
            }
        }
        float deltaYaw = MathHelper.wrapDegrees((float)(centerYaw - this.lastYaw));
        float deltaPitch = centerPitch - this.lastPitch;
        float smooth = Math.max(this.acceleration, 0.0f);
        float humanYawOffset = (float)(Math.sin((double)System.currentTimeMillis() * 0.001) * 0.04);
        float humanPitchOffset = (float)(Math.cos((double)System.currentTimeMillis() * 0.0015) * 0.025);
        if (Math.abs(deltaYaw) > 1.0f || Math.abs(deltaPitch) > 1.0f) {
            humanYawOffset += ((float)Math.random() - 0.5f) * 0.035f;
            humanPitchOffset += ((float)Math.random() - 0.5f) * 0.02f;
        }
        float newYaw = this.lastYaw + deltaYaw * MathHelper.clamp((float)(smooth * 1.12f), (float)0.0f, (float)1.0f) + humanYawOffset;
        float newPitch = this.lastPitch + deltaPitch * MathHelper.clamp((float)(smooth / 1.88f), (float)0.0f, (float)1.0f) + humanPitchOffset;
        float gcd = this.getGCDValue();
        newYaw -= (newYaw - this.lastYaw) % gcd;
        if ((newPitch -= (newPitch - this.lastPitch) % gcd) > 89.0f) {
            newPitch = 89.0f;
        }
        if (newPitch < -89.0f) {
            newPitch = -89.0f;
        }
        this.lastYaw = newYaw;
        this.lastPitch = newPitch;
        RotationStorage.update(new Rotation(newYaw, newPitch), 360.0f, 45.0f, 45.0f, 45.0f, 0, 1, Aura.clientLook.isState());
    }
}

