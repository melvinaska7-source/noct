package zov.alphadlc.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import zov.alphadlc.event.Event;

@Getter
@AllArgsConstructor
public class EventPopTotem extends Event {
    private final PlayerEntity player;
}