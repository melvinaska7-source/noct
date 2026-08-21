package zov.alphadlc.event;

import lombok.Data;
import zov.alphadlc.AlphaDLC;

@Data
public class Event {
    private boolean cancelled;

    public void post() {
        AlphaDLC.getInstance().getEventBus().post(this);
    }

    public void cancelEvent() {
        setCancelled(true);
    }
}