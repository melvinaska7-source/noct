package polar.ru.api.events.implement;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import polar.ru.api.events.Event;

public class EventThorns
extends Event {
    private final LivingEntity user;
    private final Entity attacker;
    private final int level;
    private float damage;

    public EventThorns(LivingEntity user, Entity attacker, int level, float damage) {
        this.user = user;
        this.attacker = attacker;
        this.level = level;
        this.damage = damage;
    }
    public LivingEntity getUser() {
        return this.user;
    }
    public Entity getAttacker() {
        return this.attacker;
    }
    public int getLevel() {
        return this.level;
    }
    public float getDamage() {
        return this.damage;
    }
    public void setDamage(float damage) {
        this.damage = damage;
    }
}

