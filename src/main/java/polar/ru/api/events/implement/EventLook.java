package polar.ru.api.events.implement;

import polar.ru.api.events.Event;

public class EventLook
extends Event {
    private double yaw;
    private double pitch;
    public double getYaw() {
        return this.yaw;
    }
    public double getPitch() {
        return this.pitch;
    }
    public void setYaw(double yaw) {
        this.yaw = yaw;
    }
    public void setPitch(double pitch) {
        this.pitch = pitch;
    }
    public EventLook(double yaw, double pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
}

