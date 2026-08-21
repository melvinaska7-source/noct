package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import zov.alphadlc.event.list.EventKeyInput;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BindSetting;

@ModuleInformation(moduleName = "Cords Dropper", moduleDesc = "Скидывает координаты в чат", moduleCategory = ModuleCategory.PLAYER)
public class CordsDropper extends Module {

    private final BindSetting bind = new BindSetting("Key",-1);

    @Subscribe
    private void onKey(EventKeyInput e) {
        if (e.getAction() == 0) return;
        if (e.getKey() == bind.getValue()) {
            if (mc.player != null) {
                String message = String.format("! %.0f %.0f !!!", mc.player.getX(), mc.player.getZ());
                mc.player.networkHandler.sendChatMessage(message);
            }
        }
    }
}