package zov.alphadlc.module.list.render;

import org.lwjgl.glfw.GLFW;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.ui.ClickGuiFrame;

@ModuleInformation(moduleName = "Click Gui", moduleDesc = "Графическое меню настройки модулей", moduleCategory = ModuleCategory.RENDER, moduleKeybind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGui extends Module {

    public final SliderSetting size = new SliderSetting("Размер", 1.0, 0.7, 1.4, 0.05);

    private ClickGuiFrame clickGuiFrame;

    @Override
    public void onEnable() {
        if (clickGuiFrame == null) clickGuiFrame = new ClickGuiFrame();
        mc.setScreen(clickGuiFrame);
        clickGuiFrame.playOpenAnimation();
        toggle();
    }
}
