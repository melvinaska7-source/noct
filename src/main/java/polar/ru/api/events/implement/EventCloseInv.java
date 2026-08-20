package polar.ru.api.events.implement;

import polar.ru.api.events.Event;

public class EventCloseInv
extends Event {
    public int windowId;
    public EventCloseInv(int windowId) {
        this.windowId = windowId;
    }
}

