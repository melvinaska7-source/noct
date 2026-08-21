package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;

@ModuleInformation(moduleName = "No Jump Delay", moduleDesc = "Убирает задержку между прыжками", moduleCategory = ModuleCategory.MISC)
public class NoJumpDelay extends Module {

    @Subscribe
    private void onUpdate(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        mc.player.jumpingCooldown = 0;
    }
}