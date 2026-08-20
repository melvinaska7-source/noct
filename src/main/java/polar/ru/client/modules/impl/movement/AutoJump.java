package polar.ru.client.modules.impl.movement;

import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.combat.Aura;

public class AutoJump
extends Module {
    public static AutoJump INSTANCE = new AutoJump();

    public AutoJump() {
        super("AutoJump", "Прыгает автоматически при ауре", Module.ModuleCategory.MOVEMENT);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (AutoJump.mc.player == null || AutoJump.mc.world == null) {
            return;
        }
        Aura aura = ModuleClass.aura;
        if (aura == null || !aura.isEnable()) {
            return;
        }
        if (aura.getTarget() != null && AutoJump.mc.player.isOnGround()) {
            AutoJump.mc.player.jump();
        }
    }
}

