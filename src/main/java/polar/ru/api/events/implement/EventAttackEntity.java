package polar.ru.api.events.implement;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import polar.ru.api.events.Event;

public class EventAttackEntity
extends Event {
    private final PlayerEntity player;
    private final Entity target;
    public EventAttackEntity(PlayerEntity player, Entity target) {
        this.player = player;
        this.target = target;
    }
    public PlayerEntity getPlayer() {
        return this.player;
    }
    public Entity getTarget() {
        return this.target;
    }
}

