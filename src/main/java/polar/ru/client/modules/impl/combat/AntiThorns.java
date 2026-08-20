package polar.ru.client.modules.impl.combat;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.world.World;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;

public class AntiThorns
extends Module {
    public static final AntiThorns INSTANCE = new AntiThorns();
    private static final int ELYTRA_THORNS_VELOCITY_TICKS = 8;
    private static final byte THORNS_STATUS = 33;
    private int elytraThornsVelocityTicks = 0;
    private boolean thornsHit = false;

    public AntiThorns() {
        super("AntiThorns", "Отменяет урон от шипов", Module.ModuleCategory.COMBAT);
    }

    @EventLink
        public void onPacket(EventPacket event) {
        EntityStatusS2CPacket packet;
        if (AntiThorns.mc.player == null || AntiThorns.mc.world == null) {
            this.elytraThornsVelocityTicks = 0;
            this.thornsHit = false;
            return;
        }
        if (event.getType() != EventPacket.Type.RECEIVE) {
            return;
        }
        Packet<?> var_2596_2 = event.getPacket();
        if (var_2596_2 instanceof EntityStatusS2CPacket && (packet = (EntityStatusS2CPacket)var_2596_2).getStatus() == 33 && packet.getEntity((World)AntiThorns.mc.world) == AntiThorns.mc.player) {
            this.thornsHit = true;
            if (AntiThorns.mc.player.isGliding()) {
                this.elytraThornsVelocityTicks = 8;
            }
            event.cancel();
            return;
        }
        var_2596_2 = event.getPacket();
        if (var_2596_2 instanceof EntityVelocityUpdateS2CPacket velPacket && velPacket.getEntityId() == AntiThorns.mc.player.getId()) {
            if (this.thornsHit) {
                event.cancel();
                this.thornsHit = false;
                return;
            }
            if (this.shouldCancelElytraThornsVelocity()) {
                event.cancel();
                this.elytraThornsVelocityTicks = 0;
            }
        }
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (AntiThorns.mc.player == null) {
            this.elytraThornsVelocityTicks = 0;
            this.thornsHit = false;
            return;
        }
        if (!AntiThorns.mc.player.isGliding()) {
            this.elytraThornsVelocityTicks = 0;
        }
        if (this.elytraThornsVelocityTicks > 0) {
            --this.elytraThornsVelocityTicks;
        }
    }

    @Override
    public void onEnable() {
        this.elytraThornsVelocityTicks = 0;
        this.thornsHit = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.elytraThornsVelocityTicks = 0;
        this.thornsHit = false;
        super.onDisable();
    }

    private boolean shouldCancelElytraThornsVelocity() {
        return this.elytraThornsVelocityTicks > 0 && AntiThorns.mc.player.isGliding();
    }
}

