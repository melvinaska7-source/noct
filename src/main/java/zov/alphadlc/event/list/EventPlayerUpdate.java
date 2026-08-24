package zov.alphadlc.event.list;

import zov.alphadlc.event.Event;

/**
 * Ивент, вызываемый при обновлении движения игрока.
 * Позволяет модулям корректировать направление движения.
 */
public class EventPlayerUpdate extends Event {

    private float forward;
    private float strafe;
    private boolean sneaking;

    public EventPlayerUpdate() {
        this(0f, 0f, false);
    }

    public EventPlayerUpdate(float forward, float strafe, boolean sneaking) {
        this.forward = forward;
        this.strafe = strafe;
        this.sneaking = sneaking;
    }

    public float getForward() {
        return forward;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    public float getStrafe() {
        return strafe;
    }

    public void setStrafe(float strafe) {
        this.strafe = strafe;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }
}
