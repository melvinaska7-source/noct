package zov.alphadlc.util.player.combat;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.util.IMinecraft;

@UtilityClass
public class PredictUtils implements IMinecraft {
    public static Vec3d predict(LivingEntity target, double ticksAhead) {
        double i2;
        if (target.isGliding()) {
            Vec3d vel = target.getVelocity();
            double speed = vel.length();
            if (speed < 0.01) {
                return target.getPos();
            }
            return target.getPos().add(vel.multiply(ticksAhead * 1.25));
        }
        if (Math.hypot(target.prevX - target.getX(), target.prevZ - target.getZ()) * 20.0 <= 1.0 && target.prevY - target.getY() <= 1.0) {
            return target.getPos();
        }
        Vec3d forward = Vec3d.fromPolar((float)(target.getPitch() + (target.getPitch() - target.prevPitch)), (float)(target.getYaw() + (target.getYaw() - target.prevYaw))).multiply(new Vec3d(target.getX() - target.prevX, target.getY() - target.prevY, target.getZ() - target.prevZ).length() * ticksAhead);
        Vec3d vec3d = target.getRotationVector(target.getPitch() + (target.getPitch() - target.prevPitch), target.getYaw() + (target.getYaw() - target.prevYaw));
        float f2 = target.getPitch() * ((float)Math.PI / 180);
        double d2 = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        double e2 = forward.horizontalLength();
        boolean bl = target.getVelocity().y <= 0.0;
        double g2 = bl && target.hasStatusEffect(StatusEffects.SLOW_FALLING) ? Math.min(target.getFinalGravity(), 0.01) : target.getFinalGravity();
        double h2 = MathHelper.square((double)Math.cos(f2));
        forward = forward.add(0.0, g2 * (-1.0 + h2 * 0.75), 0.0);
        if (forward.y < 0.0 && d2 > 0.0) {
            i2 = forward.y * -0.1 * h2;
            forward = forward.add(vec3d.x * i2 / d2, i2, vec3d.z * i2 / d2);
        }
        if (f2 < 0.0f && d2 > 0.0) {
            i2 = e2 * (double)(-MathHelper.sin((float)f2)) * 0.04;
            forward = forward.add(-vec3d.x * i2 / d2, i2 * (double)2.2f, -vec3d.z * i2 / d2);
        }
        if (d2 > 0.0) {
            forward = forward.add((vec3d.x / d2 * e2 - forward.x) * 0.1, 0.0, (vec3d.z / d2 * e2 - forward.z) * 0.1);
        }
        return target.getPos().add(forward);
    }
}