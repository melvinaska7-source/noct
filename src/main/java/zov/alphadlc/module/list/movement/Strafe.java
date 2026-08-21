package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.util.math.MathHelper;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.player.move.MoveUtil;
import zov.alphadlc.util.rotation.Rotation;
import zov.alphadlc.util.rotation.RotationComponent;

@ModuleInformation(moduleName = "Strafe", moduleDesc = "Горизонтальные стрейфы с матрицей скорости", moduleCategory = ModuleCategory.MOVEMENT)
public class Strafe extends Module {

    private final ModeSetting mode = new ModeSetting("Режим", "Матрикс", "Матрикс", "Грим");
    private final SliderSetting speed = new SliderSetting("Скорость", 0.42, 0.0, 1.0, 0.01);

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        boolean moving = MoveUtil.hasPlayerMovement();

        if (mode.is("Матрикс")) {
            MoveUtil.setMotion(moving ? speed.getValue() * 1.5 : 0.0);
        } else if (mode.is("Грим")) {
            if (!moving) return;
            MoveUtil.setMotion(speed.getValue() * 1.5);

            float forward = mc.player.input.movementForward;
            float strafe = mc.player.input.movementSideways;
            float moveYaw = (float) Math.toDegrees(RotationComponent.direction(mc.player.getYaw(), forward, strafe));
            moveYaw = MathHelper.wrapDegrees(moveYaw);

            RotationComponent.update(new Rotation(moveYaw, mc.player.getPitch()), 360F, 360F, 2, 0);
        }
    }
}
