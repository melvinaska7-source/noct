package polar.ru.client.modules.impl.misc;

import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventBinding;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BindSetting;

public class Coords
extends Module {
    public static Coords INSTANCE = new Coords();
    private final BindSetting coordsBind = new BindSetting("Кнопка", -1);

    public Coords() {
        super("Coords", "Автоматически кидает координаты в чат", Module.ModuleCategory.MISC);
        this.addSettings(this.coordsBind);
    }

    @EventLink
    public void onBinding(EventBinding event) {
        if (Coords.mc.currentScreen != null) {
            return;
        }
        int key = event.getKey();
        if (key == this.coordsBind.getKey() && this.coordsBind.getKey() != -1) {
            this.sendCoordinates();
        }
    }

    private void sendCoordinates() {
        if (Coords.mc.player == null) {
            return;
        }
        int x2 = (int)Math.round(Coords.mc.player.getX());
        int y2 = (int)Math.round(Coords.mc.player.getY());
        int z2 = (int)Math.round(Coords.mc.player.getZ());
        Coords.mc.player.networkHandler.sendChatMessage("!" + x2 + " " + z2);
    }
}

