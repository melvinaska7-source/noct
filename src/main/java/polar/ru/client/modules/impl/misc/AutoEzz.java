package polar.ru.client.modules.impl.misc;

import net.minecraft.entity.LivingEntity;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.settings.implement.TextSetting;

public class AutoEzz
extends Module {
    public static final AutoEzz INSTANCE = new AutoEzz();
    private final TextSetting message = new TextSetting("Сообщение", "!%name% ты был унижен CrushG3n хочешь потяфкать пищи в дс crushg3n", 256);
    private LivingEntity prevTarget = null;

    public AutoEzz() {
        super("AutoEzz", "Унижение в чат после убийства чела", Module.ModuleCategory.PLAYER);
        this.addSettings(this.message);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (AutoEzz.mc.player == null || AutoEzz.mc.world == null) {
            return;
        }
        Aura aura = Aura.INSTANCE;
        if (aura == null) {
            return;
        }
        LivingEntity current = aura.getTarget();
        if (this.prevTarget != null && current == null && !this.prevTarget.isAlive()) {
            String name = this.prevTarget.getName().getString();
            String msg = this.message.get().replace("%name%", name);
            AutoEzz.mc.player.networkHandler.sendChatMessage(msg);
        }
        this.prevTarget = current;
    }

    @Override
    public void onDisable() {
        this.prevTarget = null;
        super.onDisable();
    }
}

