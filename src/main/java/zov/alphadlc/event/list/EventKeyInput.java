package zov.alphadlc.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import zov.alphadlc.event.Event;

@Getter
@AllArgsConstructor
public class EventKeyInput extends Event {
    private final int key, action;
}