package zov.alphadlc.util.player.move;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.event.list.MoveInputEvent;

import java.util.Objects;

@UtilityClass
public class MoveUtil implements IMinecraft {

    public boolean hasPlayerMovement() {
        return mc.player.input.movementForward != 0f || mc.player.input.movementSideways != 0f;
    }

    public boolean isMoving() {
        return hasPlayerMovement();
    }

    public double getDirection() {
        return direction(mc.player.getYaw(), mc.player.input.movementForward, mc.player.input.movementSideways);
    }

    public double[] calculateDirection(final double distance) {
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward != 0.0f) {
            if (sideways > 0.0f) {
                yaw += (forward > 0.0f) ? -45 : 45;
            } else if (sideways < 0.0f) {
                yaw += (forward > 0.0f) ? 45 : -45;
            }
            sideways = 0.0f;
            forward = (forward > 0.0f) ? 1.0f : -1.0f;
        }

        double sinYaw = Math.sin(Math.toRadians(yaw + 90.0f));
        double cosYaw = Math.cos(Math.toRadians(yaw + 90.0f));
        double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
        double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;

        return new double[]{xMovement, zMovement};
    }

    public double getSpeedSqrt(Entity entity) {
        double dx = entity.getX() - entity.prevX;
        double dy = entity.getY() - entity.prevY;
        double dz = entity.getZ() - entity.prevZ;
        return Math.sqrt(dx * dx + dz * dz + dy * dy);
    }

    public void setMotion(double velocity) {
        final double[] direction = calculateDirection(velocity);
        Objects.requireNonNull(mc.player).setVelocity(direction[0], mc.player.getVelocity().getY(), direction[1]);
    }

    public void setMotion(double velocity, double y) {
        final double[] direction = calculateDirection(velocity);
        Objects.requireNonNull(mc.player).setVelocity(direction[0], y, direction[1]);
    }

    public double getDegreesRelativeToView(
            Vec3d positionRelativeToPlayer,
            float yaw) {

        float optimalYaw =
                (float) Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
        double currentYaw = Math.toRadians(MathHelper.wrapDegrees(yaw));

        return Math.toDegrees(MathHelper.wrapDegrees((optimalYaw - currentYaw)));
    }

    public PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
        boolean forwards = input.forward();
        boolean backwards = input.backward();
        boolean left = input.left();
        boolean right = input.right();

        if (dgs >= (-90.0F + deadAngle) && dgs <= (90.0F - deadAngle)) {
            forwards = true;
        } else if (dgs < (-90.0F - deadAngle) || dgs > (90.0F + deadAngle)) {
            backwards = true;
        }

        if (dgs >= (0.0F + deadAngle) && dgs <= (180.0F - deadAngle)) {
            right = true;
        } else if (dgs >= (-180.0F + deadAngle) && dgs <= (0.0F - deadAngle)) {
            left = true;
        }

        return new PlayerInput(forwards, backwards, left, right, input.jump(), input.sneak(), input.sprint());
    }

    public double direction(float rotationYaw, final float moveForward, final float moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;
        float forward = 1F;
        if (moveForward < 0F) forward = -0.5F;
        if (moveForward > 0F) forward = 0.5F;
        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;
        return Math.toRadians(rotationYaw);
    }

    public PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs) {
        return getDirectionalInputForDegrees(input, dgs, 20.0F);
    }

    /**
     * Корректирует движение игрока в направлении указанного yaw.
     * Используется в аурах для strafe к цели.
     * Теперь использует MoveInputEvent вместо EventPlayerUpdate.
     */
    public static void fixMovement(MoveInputEvent event, float yaw) {
        if (mc.player == null) return;

        float forward = event.getForward();
        float strafe = event.getStrafe();

        if (forward == 0 && strafe == 0) return;

        final double targetAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, forward, strafe)));

        float bestForward = 0, bestStrafe = 0;
        float smallestDifference = Float.MAX_VALUE;

        for (float testForward = -1F; testForward <= 1F; testForward++) {
            for (float testStrafe = -1F; testStrafe <= 1F; testStrafe++) {
                if (testForward == 0 && testStrafe == 0) continue;

                final double testAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, testForward, testStrafe)));
                final float difference = Math.abs(MathHelper.wrapDegrees((float)(targetAngle - testAngle)));

                if (difference < smallestDifference) {
                    smallestDifference = difference;
                    bestForward = testForward;
                    bestStrafe = testStrafe;
                }
            }
        }

        event.forward = bestForward;
        event.strafe = bestStrafe;
    }
}
