package polar.ru.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact multi-column ClickGUI.  The layout intentionally follows the classic
 * client-style reference: one narrow panel per category, dense module rows,
 * no giant outer card and a small search field below the columns.
 */
public class ClickGuiDropdownRenderer {

    private static final float PANEL_WIDTH = 205.0f;
    private static final float PANEL_HEIGHT = 493.0f;
    private static final float GAP = 10.0f;
    private static final float SEARCH_WIDTH = 220.0f;
    private static final float SEARCH_HEIGHT = 26.0f;

    private final List<ClickGuiDropdownPanel> panels = new ArrayList<>();
    private String searchText = "";
    private boolean searchActive = false;
    private float searchX;
    private float searchY;
    private float searchRenderWidth;

    public ClickGuiDropdownRenderer() {
        rebuildPanels();
    }

    private void rebuildPanels() {
        panels.clear();
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
        if (window == null) return;

        // The module registry can be populated after the GUI object is constructed.
        if (panels.size() != countNonEmptyCategories()) {
            rebuildPanels();
        }

        int screenWidth = window.getScaledWidth();
        int screenHeight = window.getScaledHeight();
        float panelWidth = getPanelWidth(screenWidth);
        float totalWidth = panels.size() * panelWidth + Math.max(0, panels.size() - 1) * GAP;
        float startX = (screenWidth - totalWidth) / 2.0f;
        float startY = Math.max(18.0f, (screenHeight - PANEL_HEIGHT) / 2.0f - 20.0f);

        float x = startX;
        for (ClickGuiDropdownPanel panel : panels) {
            panel.setBounds(x, startY, panelWidth, PANEL_HEIGHT);
            panel.setSearchQuery(searchText);
            panel.render(context, mouseX, mouseY);
            x += panelWidth + GAP;
        }

        searchRenderWidth = Math.min(SEARCH_WIDTH, Math.max(150.0f, screenWidth - 40.0f));
        searchX = (screenWidth - searchRenderWidth) / 2.0f;
        searchY = Math.min(screenHeight - SEARCH_HEIGHT - 28.0f, startY + PANEL_HEIGHT + 30.0f);
        renderSearch(context, mouseX, mouseY);
    }

    private void renderSearch(DrawContext context, int mouseX, int mouseY) {
        boolean hovered = mouseX >= searchX && mouseX <= searchX + searchRenderWidth
                && mouseY >= searchY && mouseY <= searchY + SEARCH_HEIGHT;
        int background = searchActive || hovered
                ? ColorUtils.rgba(24, 24, 27, 235)
                : ColorUtils.rgba(12, 12, 14, 215);
        int outline = searchActive ? ColorUtils.rgba(115, 115, 125, 180) : ColorUtils.rgba(70, 70, 76, 130);

        RenderUtils.drawRoundedRect(context.getMatrices(), searchX, searchY, searchRenderWidth, SEARCH_HEIGHT, 6.0f, background);
        RenderUtils.drawRoundedRect(context.getMatrices(), searchX + 0.5f, searchY + 0.5f,
                searchRenderWidth - 1.0f, SEARCH_HEIGHT - 1.0f, 5.5f, ColorUtils.rgba(0, 0, 0, 0));

        Font icon = Fonts.getFont("moe3", 9);
        Font font = Fonts.getFont("suisse", 12);
        if (icon != null) {
            icon.draw(context.getMatrices(), "q", searchX + 10.0f, searchY + 7.0f, ColorUtils.rgb(125, 125, 132));
        }
        if (font != null) {
            String text = searchText.isEmpty() && !searchActive ? "Search" : searchText;
            int color = searchText.isEmpty() && !searchActive ? ColorUtils.rgb(120, 120, 128) : ColorUtils.rgb(235, 235, 238);
            font.draw(context.getMatrices(), text, searchX + 28.0f, searchY + 7.0f, color);
            if (searchActive && System.currentTimeMillis() / 500L % 2L == 0L) {
                float cursorX = searchX + 28.0f + font.getWidth(searchText) + 1.0f;
                RenderUtils.drawRoundedRect(context.getMatrices(), cursorX, searchY + 6.0f, 1.0f, 13.0f, 0.5f, ColorUtils.rgb(220, 220, 225));
            }
        }

        // A thin outline is drawn as four strips to avoid depending on a separate outline API.
        RenderUtils.drawRoundedRect(context.getMatrices(), searchX, searchY, searchRenderWidth, 1.0f, 0.5f, outline);
        RenderUtils.drawRoundedRect(context.getMatrices(), searchX, searchY + SEARCH_HEIGHT - 1.0f, searchRenderWidth, 1.0f, 0.5f, outline);
    }

    private float getPanelWidth(int screenWidth) {
        if (panels.isEmpty()) return PANEL_WIDTH;
        float available = screenWidth - 40.0f - Math.max(0, panels.size() - 1) * GAP;
        return Math.min(PANEL_WIDTH, Math.max(150.0f, available / panels.size()));
    }

    private int countNonEmptyCategories() {
        int count = 0;
        for (Module.ModuleCategory category : Module.ModuleCategory.values()) {
            if (ModuleClass.INSTANCE.getObject().stream().anyMatch(m -> m.getCategory() == category)) count++;
        }
        return count;
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= searchX && mouseX <= searchX + searchRenderWidth
                && mouseY >= searchY && mouseY <= searchY + SEARCH_HEIGHT) {
            if (button == 0) {
                searchActive = true;
            }
            return;
        }

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
        if (keyCode == GLFW.GLFW_KEY_F && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            searchActive = true;
            return;
        }
        if (searchActive) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchActive = false;
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchActive = false;
                return;
            }
            return;
        }

        for (ClickGuiDropdownPanel panel : panels) {
            panel.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (searchActive && !Character.isISOControl(chr)) {
            if (searchText.length() < 32) {
                searchText += chr;
            }
            return;
        }
        for (ClickGuiDropdownPanel panel : panels) {
            panel.charTyped(chr, modifiers);
        }
    }
}
