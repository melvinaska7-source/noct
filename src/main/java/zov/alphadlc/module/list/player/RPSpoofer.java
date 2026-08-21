package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.util.packet.NetworkUtils;

@ModuleInformation(moduleName = "RP Spoofer", moduleDesc = "Изменяет отправку пакетов ресурспаков", moduleCategory = ModuleCategory.MISC)
public class RPSpoofer extends Module {

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof ResourcePackSendS2CPacket) {
            NetworkUtils.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
            NetworkUtils.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.DOWNLOADED));
            NetworkUtils.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            e.cancelEvent();
        }
    }
}