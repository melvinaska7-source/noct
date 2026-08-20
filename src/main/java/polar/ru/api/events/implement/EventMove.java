package polar.ru.api.events.implement;

import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.Event;

public class EventMove
extends Event {
    private Vec3d movePos;
    public Vec3d getMovePos() {
        return this.movePos;
    }
    public void setMovePos(Vec3d movePos) {
        this.movePos = movePos;
    }
    public EventMove() {
    }
    public EventMove(Vec3d movePos) {
        this.movePos = movePos;
    }
}

