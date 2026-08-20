package polar.ru.api.utils.player;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.utils.input.MovingUtil;

public class MoveUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void setMotion(double motion) {
        if (MoveUtils.mc.player == null) {
            return;
        }
        double forward = MoveUtils.mc.player.input.movementForward;
        double strafe = MoveUtils.mc.player.input.movementSideways;
        float yaw = MoveUtils.mc.player.getYaw();
        if (forward == 0.0 && strafe == 0.0) {
            MoveUtils.mc.player.setVelocity(0.0, MoveUtils.mc.player.getVelocity().y, 0.0);
        } else {
            if (forward != 0.0) {
                if (strafe > 0.0) {
                    yaw += (float)(forward > 0.0 ? -45 : 45);
                } else if (strafe < 0.0) {
                    yaw += (float)(forward > 0.0 ? 45 : -45);
                }
                strafe = 0.0;
                if (forward > 0.0) {
                    forward = 1.0;
                } else if (forward < 0.0) {
                    forward = -1.0;
                }
            }
            double motionX = forward * motion * (double)MathHelper.cos((float)((float)Math.toRadians(yaw + 90.0f))) + strafe * motion * (double)MathHelper.sin((float)((float)Math.toRadians(yaw + 90.0f)));
            double motionZ = forward * motion * (double)MathHelper.sin((float)((float)Math.toRadians(yaw + 90.0f))) - strafe * motion * (double)MathHelper.cos((float)((float)Math.toRadians(yaw + 90.0f)));
            MoveUtils.mc.player.setVelocity(motionX, MoveUtils.mc.player.getVelocity().y, motionZ);
        }
    }

    public static double getSpeed() {
        if (MoveUtils.mc.player == null) {
            return 0.0;
        }
        Vec3d velocity = MoveUtils.mc.player.getVelocity();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    public static void setVelocity(double velocity) {
        double[] direction = MovingUtil.calculateDirection(velocity);
        Objects.requireNonNull(MoveUtils.mc.player).setVelocity(direction[0], MoveUtils.mc.player.getVelocity().getY(), direction[1]);
    }

    public static void setVelocity(double velocity, double y2) {
        double[] direction = MovingUtil.calculateDirection(velocity);
        Objects.requireNonNull(MoveUtils.mc.player).setVelocity(direction[0], y2, direction[1]);
    }

    public static void strafe() {
        MoveUtils.strafe(MoveUtils.getSpeed());
    }

    public static void strafe(double speed) {
        if (MoveUtils.mc.player == null) {
            return;
        }
        float yaw = MoveUtils.mc.player.getYaw();
        double forward = MoveUtils.mc.player.input.movementForward;
        double strafe = MoveUtils.mc.player.input.movementSideways;
        if (forward == 0.0 && strafe == 0.0) {
            MoveUtils.mc.player.setVelocity(0.0, MoveUtils.mc.player.getVelocity().y, 0.0);
            return;
        }
        if (forward != 0.0) {
            if (strafe > 0.0) {
                yaw += forward > 0.0 ? -45.0f : 45.0f;
            } else if (strafe < 0.0) {
                yaw += forward > 0.0 ? 45.0f : -45.0f;
            }
            strafe = 0.0;
            forward = forward > 0.0 ? 1.0 : -1.0;
        }
        double rad = Math.toRadians(yaw + 90.0f);
        double motionX = forward * speed * Math.cos(rad) + strafe * speed * Math.sin(rad);
        double motionZ = forward * speed * Math.sin(rad) - strafe * speed * Math.cos(rad);
        MoveUtils.mc.player.setVelocity(motionX, MoveUtils.mc.player.getVelocity().y, motionZ);
    }

    public static boolean isMoving() {
        if (MoveUtils.mc.player == null) {
            return false;
        }
        return MoveUtils.mc.player.input.movementForward != 0.0f || MoveUtils.mc.player.input.movementSideways != 0.0f;
    }
}

