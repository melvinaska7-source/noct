package polar.ru.api.events.implement;

import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.Event;

public class EventOnTravelPost
extends Event {
    private Vec3d oldVelocity;
    public Vec3d getOldVelocity() {
        return this.oldVelocity;
    }
    public void setOldVelocity(Vec3d oldVelocity) {
        this.oldVelocity = oldVelocity;
    }
    public EventOnTravelPost(Vec3d oldVelocity) {
        this.oldVelocity = oldVelocity;
    }
}

