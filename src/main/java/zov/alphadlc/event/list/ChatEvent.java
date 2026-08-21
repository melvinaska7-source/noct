package zov.alphadlc.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import zov.alphadlc.event.Event;

@Getter
@AllArgsConstructor
public class ChatEvent extends Event {
    private final String message;
}