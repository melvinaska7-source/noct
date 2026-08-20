package polar.ru.api.events.implement;

import polar.ru.api.events.Event;

public class EventRotation
extends Event {
    private float yaw;
    private float pitch;
    private float partialTicks;
    public float getYaw() {
        return this.yaw;
    }
    public float getPitch() {
        return this.pitch;
    }
    public float getPartialTicks() {
        return this.partialTicks;
    }
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
    public void setPartialTicks(float partialTicks) {
        this.partialTicks = partialTicks;
    }
    public EventRotation(float yaw, float pitch, float partialTicks) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.partialTicks = partialTicks;
    }
}

