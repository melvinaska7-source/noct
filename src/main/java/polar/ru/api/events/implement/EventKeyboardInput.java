package polar.ru.api.events.implement;

import net.minecraft.util.math.MathHelper;
import polar.ru.api.events.Event;

public class EventKeyboardInput
extends Event {
    private float movementForward;
    private float movementSideways;

    public void setYaw(float yaw, float yaw2) {
        float forward = this.getMovementForward();
        float sideways = this.getMovementSideways();
        double angle = MathHelper.wrapDegrees((double)Math.toDegrees(this.direction(yaw2, forward, sideways)));
        if (forward == 0.0f && sideways == 0.0f) {
            return;
        }
        float closestForward = 0.0f;
        float closestSideways = 0.0f;
        float closestDifference = Float.MAX_VALUE;
        for (float predictedForward = -1.0f; predictedForward <= 1.0f; predictedForward += 1.0f) {
            for (float predictedSideways = -1.0f; predictedSideways <= 1.0f; predictedSideways += 1.0f) {
                double predictedAngle;
                double difference;
                if (predictedSideways == 0.0f && predictedForward == 0.0f || !((difference = Math.abs(angle - (predictedAngle = MathHelper.wrapDegrees((double)Math.toDegrees(this.direction(yaw, predictedForward, predictedSideways)))))) < (double)closestDifference)) continue;
                closestDifference = (float)difference;
                closestForward = predictedForward;
                closestSideways = predictedSideways;
            }
        }
        this.setMovementForward(closestForward);
        this.setMovementSideways(closestSideways);
    }

    private double direction(float yaw, double movementForward, double movementSideways) {
        if (movementForward < 0.0) {
            yaw += 180.0f;
        }
        float forward = 1.0f;
        if (movementForward < 0.0) {
            forward = -0.5f;
        } else if (movementForward > 0.0) {
            forward = 0.5f;
        }
        if (movementSideways > 0.0) {
            yaw -= 90.0f * forward;
        }
        if (movementSideways < 0.0) {
            yaw += 90.0f * forward;
        }
        return Math.toRadians(yaw);
    }
    public EventKeyboardInput(float movementForward, float movementSideways) {
        this.movementForward = movementForward;
        this.movementSideways = movementSideways;
    }
    public float getMovementForward() {
        return this.movementForward;
    }
    public float getMovementSideways() {
        return this.movementSideways;
    }
    public void setMovementForward(float movementForward) {
        this.movementForward = movementForward;
    }
    public void setMovementSideways(float movementSideways) {
        this.movementSideways = movementSideways;
    }
}

