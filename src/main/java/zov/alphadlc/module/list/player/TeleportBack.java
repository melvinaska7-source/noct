package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.util.math.StopWatch;
import zov.alphadlc.util.player.other.SlownessManager;

@ModuleInformation(moduleName = "Teleport Back", moduleDesc = "Телепортация на предыдущую позицию", moduleCategory = ModuleCategory.PLAYER)
public class TeleportBack extends Module {

    private final StopWatch stopWatch = new StopWatch();
    private boolean dead;

    @Subscribe
    private void onUpdate(EventTick e) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isAlive() && !dead && stopWatch.isReached(500)) {
            mc.player.networkHandler.sendChatCommand("sethome gavno");
            SlownessManager.addTimeTask(new SlownessManager.TimeTask(100, () -> {
                if (mc.player == null) return;
                mc.player.requestRespawn();
                mc.player.networkHandler.sendChatCommand("home gavno");
                dead = false;
            }, true));
            stopWatch.reset();
        }
    }
}