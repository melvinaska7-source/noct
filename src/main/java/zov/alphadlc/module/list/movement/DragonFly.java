package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.player.move.MoveUtil;

@ModuleInformation(moduleName = "Dragon Fly", moduleDesc = "Ускоряет уже активный полёт", moduleCategory = ModuleCategory.MOVEMENT)
public class DragonFly extends Module {

    private final SliderSetting xzSpeed = new SliderSetting("Скорость по X/Z", 1.0, 0.0, 2.0, 0.1);
    private final SliderSetting ySpeed = new SliderSetting("Скорость по Y", 1.0, 0.0, 2.0, 0.1);

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null || !mc.player.getAbilities().flying) return;

        MoveUtil.setMotion(xzSpeed.getValue());

        if (mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().x, ySpeed.getValue(), mc.player.getVelocity().z);
        }

        if (mc.options.sneakKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().x, -ySpeed.getValue(), mc.player.getVelocity().z);
        }
    }
}
