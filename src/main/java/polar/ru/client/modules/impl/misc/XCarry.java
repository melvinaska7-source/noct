package polar.ru.client.modules.impl.misc;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;

public class XCarry
extends Module {
    public static XCarry INSTANCE = new XCarry();
    public BooleanSetting autoDisable = new BooleanSetting("Авто выкл", true);
    private boolean wasInInventory = false;

    public XCarry() {
        super("XCarry", "Дополнительные слоты", Module.ModuleCategory.MISC);
        this.addSettings(this.autoDisable);
    }

    @EventLink
    public void onPacket(EventPacket event) {
        if (XCarry.mc.player == null || XCarry.mc.world == null) {
            return;
        }
        if (event.getPacket() instanceof CloseHandledScreenC2SPacket && XCarry.mc.currentScreen instanceof InventoryScreen) {
            event.cancel();
            this.wasInInventory = true;
        }
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (XCarry.mc.player == null || XCarry.mc.world == null) {
            return;
        }
        if (this.wasInInventory && XCarry.mc.currentScreen == null) {
            if (this.autoDisable.isState()) {
                this.toggle();
            }
            this.wasInInventory = false;
        }
    }
}

