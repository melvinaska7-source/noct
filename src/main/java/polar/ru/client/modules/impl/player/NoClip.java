package polar.ru.client.modules.impl.player;

import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;

public class NoClip
extends Module {
    public static NoClip INSTANCE = new NoClip();

    public NoClip() {
        super("NoClip", "Позволяте проходить через блоки", Module.ModuleCategory.PLAYER);
    }

    @EventLink
    public void onUpdate(EventUpdate ignored) {
        if (NoClip.mc.player == null) {
            return;
        }
        if (NoClip.mc.player.age % 35 == 0) {
            NoClip.mc.player.networkHandler.sendChatMessage("/gmsp");
        } else if (NoClip.mc.player.age % 35 == 2) {
            NoClip.mc.player.networkHandler.sendChatMessage("/gms");
        }
    }
}

