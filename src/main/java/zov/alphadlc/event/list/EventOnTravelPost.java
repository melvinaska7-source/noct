package zov.alphadlc.event.list;

import net.minecraft.util.math.Vec3d;
import zov.alphadlc.event.Event;

public class EventOnTravelPost extends Event {
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
