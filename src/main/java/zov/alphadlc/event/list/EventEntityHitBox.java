package zov.alphadlc.event.list;

import net.minecraft.entity.Entity;
import zov.alphadlc.event.Event;

public class EventEntityHitBox extends Event {
    private final Entity entity;
    private float size;

    public Entity getEntity() {
        return this.entity;
    }

    public float getSize() {
        return this.size;
    }

    public EventEntityHitBox(Entity entity, float size) {
        this.entity = entity;
        this.size = size;
    }

    public void setSize(float size) {
        this.size = size;
    }
}
