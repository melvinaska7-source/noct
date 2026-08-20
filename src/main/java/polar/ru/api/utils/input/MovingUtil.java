package polar.ru.api.utils.input;

import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.events.implement.EventMoveInput;
import polar.ru.api.storages.implement.FreeLookStorage;

public final class MovingUtil
implements QClient {
    public static boolean hasPlayerMovement() {
        return MovingUtil.mc.player.input.movementForward != 0.0f || MovingUtil.mc.player.input.movementSideways != 0.0f;
    }

    public static double[] calculateDirection(double distance) {
        float forward = MovingUtil.mc.player.input.movementForward;
        float sideways = MovingUtil.mc.player.input.movementSideways;
        float yaw = MovingUtil.mc.player.getYaw();
        if (forward != 0.0f) {
            if (sideways > 0.0f) {
                yaw += forward > 0.0f ? -45.0f : 45.0f;
            } else if (sideways < 0.0f) {
                yaw += forward > 0.0f ? 45.0f : -45.0f;
            }
            sideways = 0.0f;
            forward = forward > 0.0f ? 1.0f : -1.0f;
        }
        double sinYaw = Math.sin(Math.toRadians(yaw + 90.0f));
        double cosYaw = Math.cos(Math.toRadians(yaw + 90.0f));
        double xMovement = (double)forward * distance * cosYaw + (double)sideways * distance * sinYaw;
        double zMovement = (double)forward * distance * sinYaw - (double)sideways * distance * cosYaw;
        return new double[]{xMovement, zMovement};
    }

    public static double getSpeedSqrt(Entity entity) {
        double dx = entity.getX() - entity.prevX;
        double dy = entity.getY() - entity.prevY;
        double dz = entity.getZ() - entity.prevZ;
        return Math.sqrt(dx * dx + dz * dz + dy * dy);
    }

    public static void setVelocity(double velocity) {
        double[] direction = MovingUtil.calculateDirection(velocity);
        Objects.requireNonNull(MovingUtil.mc.player).setVelocity(direction[0], MovingUtil.mc.player.getVelocity().getY(), direction[1]);
    }

    public static void setVelocity(double velocity, double y2) {
        double[] direction = MovingUtil.calculateDirection(velocity);
        Objects.requireNonNull(MovingUtil.mc.player).setVelocity(direction[0], y2, direction[1]);
    }

    public static double getDegreesRelativeToView(Vec3d positionRelativeToPlayer, float yaw) {
        float optimalYaw = (float)Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
        double currentYaw = Math.toRadians(MathHelper.wrapDegrees((float)yaw));
        return Math.toDegrees(MathHelper.wrapDegrees((double)((double)optimalYaw - currentYaw)));
    }

    public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
        boolean forwards = input.forward();
        boolean backwards = input.backward();
        boolean left = input.left();
        boolean right = input.right();
        if (dgs >= (double)(-90.0f + deadAngle) && dgs <= (double)(90.0f - deadAngle)) {
            forwards = true;
        } else if (dgs < (double)(-90.0f - deadAngle) || dgs > (double)(90.0f + deadAngle)) {
            backwards = true;
        }
        if (dgs >= (double)(0.0f + deadAngle) && dgs <= (double)(180.0f - deadAngle)) {
            right = true;
        } else if (dgs >= (double)(-180.0f + deadAngle) && dgs <= (double)(0.0f - deadAngle)) {
            left = true;
        }
        return new PlayerInput(forwards, backwards, left, right, input.jump(), input.sneak(), input.sprint());
    }

    public static void fixMovementFocus(EventMoveInput event, float yaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();
        if (forward != 0.0f || strafe != 0.0f) {
            double targetAngle = MathHelper.wrapDegrees((double)Math.toDegrees(MovingUtil.direction(yaw, forward, strafe)));
            float bestForward = 0.0f;
            float bestStrafe = 0.0f;
            float smallestDifference = Float.MAX_VALUE;
            for (float testForward = -1.0f; testForward <= 1.0f; testForward += 1.0f) {
                for (float testStrafe = -1.0f; testStrafe <= 1.0f; testStrafe += 1.0f) {
                    double testAngle;
                    float difference;
                    if (testForward == 0.0f && testStrafe == 0.0f || !((difference = Math.abs(MathHelper.wrapDegrees((float)((float)(targetAngle - (testAngle = MathHelper.wrapDegrees((double)Math.toDegrees(MovingUtil.direction(yaw, testForward, testStrafe))))))))) < smallestDifference)) continue;
                    smallestDifference = difference;
                    bestForward = testForward;
                    bestStrafe = testStrafe;
                }
            }
            event.setForward(bestForward);
            event.setStrafe(bestStrafe);
        }
    }

    public static void fixMovementFree(EventMoveInput event) {
        float forward = event.getForward();
        float strafe = event.getStrafe();
        double angle = MathHelper.wrapDegrees((double)Math.toDegrees(MovingUtil.direction(MovingUtil.mc.player.isGliding() ? MovingUtil.mc.player.getYaw() : FreeLookStorage.getFreeYaw(), forward, strafe)));
        if (forward != 0.0f || strafe != 0.0f) {
            float closestForward = 0.0f;
            float closestStrafe = 0.0f;
            float closestDifference = Float.MAX_VALUE;
            for (float predictedForward = -1.0f; predictedForward <= 1.0f; predictedForward += 1.0f) {
                for (float predictedStrafe = -1.0f; predictedStrafe <= 1.0f; predictedStrafe += 1.0f) {
                    double predictedAngle;
                    double difference;
                    if (predictedStrafe == 0.0f && predictedForward == 0.0f || !((difference = Math.abs(angle - (predictedAngle = MathHelper.wrapDegrees((double)Math.toDegrees(MovingUtil.direction(MovingUtil.mc.player.getYaw(), predictedForward, predictedStrafe)))))) < (double)closestDifference)) continue;
                    closestDifference = (float)difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
            event.setForward(closestForward);
            event.setStrafe(closestStrafe);
        }
    }

    public static double direction(float rotationYaw, float moveForward, float moveStrafing) {
        if (moveForward < 0.0f) {
            rotationYaw += 180.0f;
        }
        float forward = 1.0f;
        if (moveForward < 0.0f) {
            forward = -0.5f;
        }
        if (moveForward > 0.0f) {
            forward = 0.5f;
        }
        if (moveStrafing > 0.0f) {
            rotationYaw -= 90.0f * forward;
        }
        if (moveStrafing < 0.0f) {
            rotationYaw += 90.0f * forward;
        }
        return Math.toRadians(rotationYaw);
    }

    public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs) {
        return MovingUtil.getDirectionalInputForDegrees(input, dgs, 20.0f);
    }
    private MovingUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

