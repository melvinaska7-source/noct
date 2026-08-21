package zov.alphadlc.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;

@ModuleInformation(moduleName = "Velocity", moduleDesc = "Убирает отбрасывание от ударов", moduleCategory = ModuleCategory.COMBAT)
public class Velocity extends Module {
    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() != mc.player.getId()) return;

            e.cancelEvent();
        }
    }
}