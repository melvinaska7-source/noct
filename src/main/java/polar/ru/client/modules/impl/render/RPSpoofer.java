package polar.ru.client.modules.impl.render;

import java.util.UUID;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.utils.bot.BotSessionManager;
import polar.ru.client.modules.Module;

public class RPSpoofer
extends Module {
    public static RPSpoofer INSTANCE = new RPSpoofer();

    public RPSpoofer() {
        super("RPSpoofer", "Убирает ресурс-пак сервера", Module.ModuleCategory.PLAYER);
    }

    @EventLink
    public void onReceivePacket(EventPacket e2) {
        Packet<?> var_2596_2 = e2.getPacket();
        if (var_2596_2 instanceof ResourcePackSendS2CPacket) {
            ResourcePackSendS2CPacket packet = (ResourcePackSendS2CPacket)var_2596_2;
            if (this.isEnable() || BotSessionManager.shouldBypassResourcePacks()) {
                UUID packId = packet.id();
                mc.getNetworkHandler().sendPacket((Packet)new ResourcePackStatusC2SPacket(packId, ResourcePackStatusC2SPacket.Status.ACCEPTED));
                mc.getNetworkHandler().sendPacket((Packet)new ResourcePackStatusC2SPacket(packId, ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
                e2.setCancelled(true);
            }
        }
    }
}

