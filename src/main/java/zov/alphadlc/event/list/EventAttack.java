package zov.alphadlc.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;
import zov.alphadlc.event.Event;

@Getter
@AllArgsConstructor
public class EventAttack extends Event {
    private final Entity entity;
}