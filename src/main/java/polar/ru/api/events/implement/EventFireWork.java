package polar.ru.api.events.implement;

import net.minecraft.entity.projectile.FireworkRocketEntity;
import polar.ru.api.events.Event;

public class EventFireWork
extends Event {
    private final FireworkRocketEntity firework;
    public FireworkRocketEntity getFirework() {
        return this.firework;
    }
    public EventFireWork(FireworkRocketEntity firework) {
        this.firework = firework;
    }
}

