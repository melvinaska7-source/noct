package polar.ru.api.events.implement;

import polar.ru.api.events.Event;

public class EventMoveInput
extends Event {
    private float forward;
    private float strafe;
    private boolean jump;
    private boolean sneak;
    public float getForward() {
        return this.forward;
    }
    public float getStrafe() {
        return this.strafe;
    }
    public boolean isJump() {
        return this.jump;
    }
    public boolean isSneak() {
        return this.sneak;
    }
    public void setForward(float forward) {
        this.forward = forward;
    }
    public void setStrafe(float strafe) {
        this.strafe = strafe;
    }
    public void setJump(boolean jump) {
        this.jump = jump;
    }
    public void setSneak(boolean sneak) {
        this.sneak = sneak;
    }
    public EventMoveInput(float forward, float strafe, boolean jump, boolean sneak) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
    }
}

