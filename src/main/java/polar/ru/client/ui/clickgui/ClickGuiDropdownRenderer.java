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
                    .filter(m -> m != null && !isCheatLike(m.getName()))
                    .toList();
            if (!modules.isEmpty()) panels.add(new ClickGuiDropdownPanel(category, modules));
        }
    }

    private static boolean isCheatLike(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();
        String[] blocked = {
            "aura", "killaura", "triggerbot", "aimbot", "aim assist", "reach",
            "antibot", "targetstrafe", "critical", "packetcritical", "freecam",
            "airstuck", "fly", "speed", "noslow", "hitbox", "entityesp",
            "blockesp", "shaderesp", "seeinvis", "trajectories", "itemaim",
            "maceexploit", "autowarden", "autoexplosion", "clickpearl"
        };
        for (String word : blocked) if (n.contains(word)) return true;
        return false;
    }

    public void render(DrawContext context, int mouseX, int mouseY, Window window) {
        int screenWidth = window.getScaledWidth();
        int screenHeight = window.getScaledHeight();
        float gap = 10f;
        float panelWidth = ClickGuiDropdownPanel.getWidth();
        float totalWidth = panels.size() * panelWidth + Math.max(0, panels.size() - 1) * gap;
        float scale = Math.min(1f, (screenWidth - 8f) / Math.max(1f, totalWidth));

        context.getMatrices().push();
        context.getMatrices().translate(screenWidth / 2f, screenHeight / 2f, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-screenWidth / 2f, -screenHeight / 2f, 0);

        float startX = screenWidth / 2f - totalWidth / 2f;
        float startY = screenHeight / 2f - ClickGuiDropdownPanel.getHeight() / 2f;

        float x = startX;
        for (ClickGuiDropdownPanel panel : panels) {
            panel.setPosition(x, startY);
            panel.render(context, (int)(mouseX / scale + screenWidth * (1f - 1f / scale) / 2f),
                    (int)(mouseY / scale + screenHeight * (1f - 1f / scale) / 2f));
            x += panelWidth + gap;
        }
        context.getMatrices().pop();
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        for (ClickGuiDropdownPanel panel : panels) panel.mouseClicked(mouseX, mouseY, button);
    }
    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (ClickGuiDropdownPanel panel : panels) panel.mouseReleased(mouseX, mouseY, button);
    }
    public void mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        for (ClickGuiDropdownPanel panel : panels) panel.mouseScrolled(mouseX, mouseY, verticalAmount);
    }
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ClickGuiDropdownPanel panel : panels) panel.keyPressed(keyCode, scanCode, modifiers);
    }
    public void charTyped(char chr, int modifiers) {
        for (ClickGuiDropdownPanel panel : panels) panel.charTyped(chr, modifiers);
    }
}
