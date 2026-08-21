package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.event.list.EventNoSlow;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.list.combat.KillAura;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.util.player.simulate.SimulatedPlayer;

@ModuleInformation(moduleName = "No Slow", moduleDesc = "Убирает замедление при использовании", moduleCategory = ModuleCategory.MOVEMENT)
public class NoSlow extends Module {

    private final ModeSetting mode = new ModeSetting("Мод", "Vanilla", "Vanilla", "Grim");

    @Subscribe
    private void onNoSlow(EventNoSlow e) {
        switch (mode.getValue()) {
            case "Vanilla" -> {
                if (!(AlphaDLC.getInstance().getModuleStorage().get(KillAura.class).getTarget() != null && SimulatedPlayer.simulateLocalPlayer(1).fallDistance > 0)) mc.player.setSprinting(true);
                e.cancelEvent();
            }
            case "Grim" -> {
                mc.player.setSprinting(mc.player.getItemUseTime() > 4 && !(AlphaDLC.getInstance().getModuleStorage().get(KillAura.class).getTarget() != null && SimulatedPlayer.simulateLocalPlayer(1).fallDistance > 0) && AlphaDLC.getInstance().getServerManager().getSprintingChangeTicks() > 0);
                if (mc.player.getItemUseTime() % 2 == 0) e.cancelEvent();
            }
        }
    }
}