package polar.ru.client.modules.impl.render;

import net.minecraft.client.gui.screen.Screen;
import polar.ru.api.QClient;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.client.ui.clickgui.ClickGuiScreen;    // ← импорт
import polar.ru.client.ui.clickgui.ClickGuiType;

public class ClickGui
extends Module {
    public static ClickGui INSTANCE = new ClickGui();
    private final ModeSetting guiType = new ModeSetting("GUI Type", ClickGuiType.DROPDOWN.getDisplayName(), ClickGuiType.DROPDOWN.getDisplayName());

    public ClickGui() {
        super("ClickGui", "Click GUI Module", Module.ModuleCategory.RENDER);
        this.setKey(344);
        this.addSettings(this.guiType);
    }

    @Override
    public void onEnable() {
        if (mc != null && ClickGui.mc.player != null) {
            QClient.mc.setScreen(new ClickGuiScreen());   // ← замена
        }
        this.toggle();
    }

    public ClickGuiType getGuiType() {
        return ClickGuiType.DROPDOWN;
    }
    @Override
public void onEnable() {
    System.out.println(">>> ClickGui.onEnable() ВЫЗВАН!");
    if (mc != null && ClickGui.mc.player != null) {
        QClient.mc.setScreen(new ClickGuiScreen());
    }
    this.toggle();
    }
}