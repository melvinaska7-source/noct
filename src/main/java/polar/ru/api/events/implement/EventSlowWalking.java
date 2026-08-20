package polar.ru.api.events.implement;

import polar.ru.api.events.Event;

public class EventSlowWalking
extends Event {
    private boolean cancelled;

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}

