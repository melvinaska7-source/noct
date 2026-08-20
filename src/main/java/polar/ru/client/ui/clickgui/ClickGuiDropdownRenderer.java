package polar.ru.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiDropdownRenderer {

    private final List<ClickGuiDropdownPanel> panels = new ArrayList<>();

    public ClickGuiDropdownRenderer() {
        for (Module.ModuleCategory category : Module.ModuleCategory.values()) {
            List<Module> modules = ModuleClass.INSTANCE.getObject().stream()
                    .filter(m -> m.getCategory() == category)
                    .toList();
            if (!modules.isEmpty()) {
                panels.add(new ClickGuiDropdownPanel(category, modules));
            }
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, Window window) {
        int screenWidth = window.getScaledWidth();
        int screenHeight = window.getScaledHeight();

        float gap = 10f;
        float panelWidth = 105f;
        float totalWidth = panels.size() * panelWidth + (panels.size() - 1) * gap;
        float startX = (screenWidth - totalWidth) / 2f;
        float startY = (screenHeight - 220f) / 2f;

        float x = startX;
        for (ClickGuiDropdownPanel panel : panels) {
            panel.setPosition(x, startY);
            panel.render(context, mouseX, mouseY);
            x += panelWidth + gap;
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        for (ClickGuiDropdownPanel panel : panels) {
            panel.mouseClicked(mouseX, mouseY, button);
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (ClickGuiDropdownPanel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        for (ClickGuiDropdownPanel panel : panels) {
            panel.mouseScrolled(mouseX, mouseY, verticalAmount);
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ClickGuiDropdownPanel panel : panels) {
            panel.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void charTyped(char chr, int modifiers) {
        for (ClickGuiDropdownPanel panel : panels) {
            panel.charTyped(chr, modifiers);
        }
    }
}