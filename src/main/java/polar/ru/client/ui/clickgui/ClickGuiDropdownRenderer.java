package polar.ru.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;

public class ClickGuiDropdownRenderer {
    private final ClickGuiState state = new ClickGuiState();
    private final ClickGuiSettingRenderer settingRenderer = new ClickGuiSettingRenderer();
    private final ClickGuiThemeSelector themeSelector = new ClickGuiThemeSelector();
    private final ClickGuiFiguraPanel figuraPanel = new ClickGuiFiguraPanel();
    private final ClickGuiRenderer renderer =
            new ClickGuiRenderer(state, settingRenderer, themeSelector, figuraPanel, null);
    private final ClickGuiInputHandler input =
            new ClickGuiInputHandler(state, themeSelector, figuraPanel, renderer);

    public void render(DrawContext context, int mouseX, int mouseY, Window window) {
        renderer.render(context, mouseX, mouseY, window, 1.0f);
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        input.mouseClicked(mouseX, mouseY, button, null);
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        input.mouseReleased(button);
    }

    public void mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        input.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        input.keyPressed(keyCode, modifiers);
    }

    public void charTyped(char chr, int modifiers) {
        input.charTyped(chr);
    }
}
