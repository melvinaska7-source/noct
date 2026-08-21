package zov.alphadlc.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import zov.alphadlc.event.Event;

@Getter
@AllArgsConstructor
public class LookEvent extends Event {
    private double yaw, pitch;
}