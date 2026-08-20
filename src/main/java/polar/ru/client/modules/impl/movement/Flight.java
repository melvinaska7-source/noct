package polar.ru.client.modules.impl.movement;

import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class Flight
extends Module {
    public static Flight INSTANCE = new Flight();
    private final FloatSetting speed = new FloatSetting("Скорость", 2.0f, 0.1f, 10.0f, 0.1f);

    public Flight() {
        super("Flight", "Полёт", Module.ModuleCategory.MOVEMENT);
        this.addSettings(this.speed);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (Flight.mc.player == null) {
            return;
        }
        double spd = this.speed.get();
        float yaw = (float)Math.toRadians(Flight.mc.player.getYaw());
        double motionX = 0.0;
        double motionY = 0.0;
        double motionZ = 0.0;
        double forward = 0.0;
        double strafe = 0.0;
        if (Flight.mc.options.forwardKey.isPressed()) {
            forward += 1.0;
        }
        if (Flight.mc.options.backKey.isPressed()) {
            forward -= 1.0;
        }
        if (Flight.mc.options.leftKey.isPressed()) {
            strafe += 1.0;
        }
        if (Flight.mc.options.rightKey.isPressed()) {
            strafe -= 1.0;
        }
        if (forward != 0.0 || strafe != 0.0) {
            double angle = Math.atan2(forward, strafe) - 1.5707963267948966;
            motionX = -Math.sin((double)yaw + angle) * spd;
            motionZ = Math.cos((double)yaw + angle) * spd;
        }
        if (Flight.mc.options.jumpKey.isPressed()) {
            motionY = spd;
        } else if (Flight.mc.options.sneakKey.isPressed()) {
            motionY = -spd;
        }
        Flight.mc.player.setVelocity(new Vec3d(motionX, motionY, motionZ));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (Flight.mc.player != null) {
            Flight.mc.player.setVelocity(Vec3d.ZERO);
        }
    }
}

