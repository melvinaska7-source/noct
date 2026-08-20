package polar.ru.client.modules.impl.combat;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventAttackEntity;
import polar.ru.api.utils.combat.IdealHitUtils;
import polar.ru.client.modules.Module;

public class PacketCriticals
extends Module {
    public static PacketCriticals INSTANCE = new PacketCriticals();

    public PacketCriticals() {
        super("PacketCriticals", "Бьет критами под эффект плавного падения / в паутине", Module.ModuleCategory.COMBAT);
    }

    @EventLink
        public void onAttack(EventAttackEntity event) {
        if (PacketCriticals.mc.player == null || PacketCriticals.mc.world == null) {
            return;
        }
        boolean inWeb = IdealHitUtils.isInCobweb();
        double x2 = PacketCriticals.mc.player.getX();
        double y2 = PacketCriticals.mc.player.getY();
        double z2 = PacketCriticals.mc.player.getZ();
        if (inWeb) {
            PacketCriticals.mc.player.networkHandler.sendPacket((Packet)new PlayerMoveC2SPacket.PositionAndOnGround(x2, y2 + 0.003, z2, false, false));
            PacketCriticals.mc.player.networkHandler.sendPacket((Packet)new PlayerMoveC2SPacket.PositionAndOnGround(x2, y2, z2, false, false));
        }
    }
}

