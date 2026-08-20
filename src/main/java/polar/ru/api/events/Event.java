package polar.ru.api.events;

import java.lang.reflect.InvocationTargetException;
import polar.ru.api.events.EventInvoker;

public class Event {
    private boolean cancelled;

    public void cancel() {
        this.cancelled = true;
    }

    public void call() {
        EventInvoker.invoke(this);
    }
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    public boolean isCancelled() {
        return this.cancelled;
    }
}

