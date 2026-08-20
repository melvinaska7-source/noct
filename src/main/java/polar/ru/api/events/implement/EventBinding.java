package polar.ru.api.events.implement;

import polar.ru.api.events.Event;

public class EventBinding
extends Event {
    private final int key;
    private final BindType bindType;

    public boolean isKeyDown(int button) {
        return this.key == button;
    }
    public EventBinding(int key, BindType bindType) {
        this.key = key;
        this.bindType = bindType;
    }
    public int getKey() {
        return this.key;
    }
    public BindType getBindType() {
        return this.bindType;
    }

    public static enum BindType {
        KEYBOARD,
        MOUSE;

    }
}

