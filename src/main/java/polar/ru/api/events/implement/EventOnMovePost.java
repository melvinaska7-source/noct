package polar.ru.api.events.implement;

import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.Event;

public class EventOnMovePost
extends Event {
    private float speed;
    private Vec3d movementInput;
    public float getSpeed() {
        return this.speed;
    }
    public Vec3d getMovementInput() {
        return this.movementInput;
    }
    public void setSpeed(float speed) {
        this.speed = speed;
    }
    public void setMovementInput(Vec3d movementInput) {
        this.movementInput = movementInput;
    }
    public EventOnMovePost(float speed, Vec3d movementInput) {
        this.speed = speed;
        this.movementInput = movementInput;
    }
}

