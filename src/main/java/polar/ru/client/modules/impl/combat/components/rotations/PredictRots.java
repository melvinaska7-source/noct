package polar.ru.client.modules.impl.combat.components.rotations;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;

public class PredictRots
extends RotationsSystem
implements QClient {
    public Vec2f rotating(Vec2f rotation, LivingEntity target) {
        Vec3d vec = this.calcPointed(target);
        float rawYaw = (float)MathHelper.wrapDegrees((double)(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0));
        float rawPitch = (float)MathHelper.wrapDegrees((double)Math.toDegrees(-Math.atan2(vec.y, Math.hypot(vec.x, vec.z))));
        float yawDelta = MathHelper.wrapDegrees((float)(rawYaw - rotation.x));
        float pitchDelta = MathHelper.wrapDegrees((float)(rawPitch - rotation.y));
        if (Math.abs(yawDelta) > 180.0f) {
            yawDelta -= Math.signum(yawDelta) * 360.0f;
        }
        float additionYaw = MathHelper.clamp((float)yawDelta, (float)-180.0f, (float)180.0f);
        float additionPitch = MathHelper.clamp((float)pitchDelta, (float)-90.0f, (float)90.0f);
        float yaw = rotation.x + additionYaw;
        float pitch = rotation.y + additionPitch;
        float yawFinal = GCDUtil.getFixedRotation(yaw);
        float pitchFinal = GCDUtil.getFixedRotation(pitch);
        return new Vec2f(yawFinal, pitchFinal);
    }

    private Vec3d calcPointed(LivingEntity target) {
        if (target != null) {
            Vec3d vecPosition = this.getPredictedPoint(target, target.getBoundingBox().getCenter());
            return new Vec3d(vecPosition.getX() - PredictRots.mc.player.getX(), vecPosition.getY() - PredictRots.mc.player.getY(), vecPosition.getZ() - PredictRots.mc.player.getZ());
        }
        return Vec3d.ZERO;
    }

    @Override
    public void updateRotations(LivingEntity entity) {
    }
}

