package polar.ru.api.events.implement;

import net.minecraft.entity.player.PlayerEntity;
import polar.ru.api.events.Event;

public class EventPopTotem
extends Event {
    private final PlayerEntity player;
    public EventPopTotem(PlayerEntity player) {
        this.player = player;
    }
    public PlayerEntity getPlayer() {
        return this.player;
    }
}

