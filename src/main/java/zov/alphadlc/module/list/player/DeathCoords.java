package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;

@ModuleInformation(moduleName = "Death Coords", moduleDesc = "Показывает координаты смерти", moduleCategory = ModuleCategory.PLAYER)
public class DeathCoords extends Module {

    private boolean send;

    @Subscribe
    private void onUpdate(EventTick e) {
        if (mc.player == null) return;
        if (mc.player.isDead()) {
            if (!send) {
                logDirect(String.format("Вы умерли на XYZ: %.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()).replace(",","."));
                send = true;
            }
        } else send = false;
    }
}