package polar.ru.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.scissor.ScissorUtils;
import polar.ru.client.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiDropdownPanel {

    private final Module.ModuleCategory category;
    private final List<ClickGuiDropdownModule> moduleComponents = new ArrayList<>();
    private float x, y, width, height;
    private float scroll = 0.0f;
    private float animatedScroll = 0.0f;
    private String searchQuery = "";

    private static final float HEADER_HEIGHT = 36.0f;
    private static final float ROW_GAP = 3.0f;
    private static final float CONTENT_PAD = 5.0f;

    public ClickGuiDropdownPanel(Module.ModuleCategory category, List<Module> modules) {
        this.category = category;
        for (Module module : modules) {
            moduleComponents.add(new ClickGuiDropdownModule(module));
        }
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setSearchQuery(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        if (!normalized.equals(searchQuery)) {
            searchQuery = normalized;
            scroll = 0.0f;
            animatedScroll = 0.0f;
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        boolean panelHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        // Dense, almost-black panel like the reference instead of a giant surrounding window.
        RenderUtils.drawShadow(matrices, x, y, width, height, 16.0f, ColorUtils.rgba(0, 0, 0, 90));
        RenderUtils.drawRoundedRect(matrices, x, y, width, height, 10.0f, ColorUtils.rgba(10, 10, 12, 205));
        RenderUtils.drawRoundedRect(matrices, x + 1.0f, y + 1.0f, width - 2.0f, height - 2.0f, 9.0f, ColorUtils.rgba(18, 18, 21, 185));

        Font titleFont = Fonts.getFont("suisse", 13);
        if (titleFont != null) {
            String title = category.getName();
            float titleX = x + (width - titleFont.getWidth(title)) / 2.0f;
            titleFont.draw(matrices, title, titleX, y + 10.0f, ColorUtils.rgb(238, 238, 240));
        }
        RenderUtils.drawRoundedRect(matrices, x + 5.0f, y + HEADER_HEIGHT - 1.0f, width - 10.0f, 1.0f, 0.5f, ColorUtils.rgba(75, 75, 80, 85));

        List<ClickGuiDropdownModule> visible = getVisibleModules();
        float innerX = x + CONTENT_PAD;
        float innerY = y + HEADER_HEIGHT + CONTENT_PAD;
        float innerW = width - CONTENT_PAD * 2.0f;
        float innerH = height - HEADER_HEIGHT - CONTENT_PAD * 2.0f;

        float totalHeight = 0.0f;
        for (ClickGuiDropdownModule comp : visible) {
            totalHeight += comp.getHeight() + ROW_GAP;
        }
        if (!visible.isEmpty()) totalHeight -= ROW_GAP;

        float maxScroll = Math.max(0.0f, totalHeight - innerH);
        scroll = Math.max(0.0f, Math.min(maxScroll, scroll));
        animatedScroll += (scroll - animatedScroll) * 0.18f;
        if (Math.abs(scroll - animatedScroll) < 0.05f) animatedScroll = scroll;

        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(innerX, innerY, innerW, innerH);
        float offsetY = 0.0f;
        for (ClickGuiDropdownModule comp : visible) {
            comp.setPosition(innerX, innerY + offsetY - animatedScroll);
            comp.setWidth(innerW);
            comp.render(context, mouseX, mouseY);
            offsetY += comp.getHeight() + ROW_GAP;
        }
        ScissorUtils.pop();

        // Small scrollbar, only when the list actually overflows.
        if (maxScroll > 0.0f) {
            float trackX = x + width - 3.0f;
            float trackY = innerY;
            float trackH = innerH;
            float thumbH = Math.max(18.0f, innerH * (innerH / totalHeight));
            float thumbY = trackY + (trackH - thumbH) * (animatedScroll / maxScroll);
            RenderUtils.drawRoundedRect(matrices, trackX, trackY, 1.5f, trackH, 0.7f, ColorUtils.rgba(80, 80, 85, 35));
            RenderUtils.drawRoundedRect(matrices, trackX, thumbY, 1.5f, thumbH, 0.7f, ColorUtils.rgba(180, 180, 185, 105));
        }
    }

    private List<ClickGuiDropdownModule> getVisibleModules() {
        if (searchQuery.isEmpty()) return moduleComponents;
        List<ClickGuiDropdownModule> result = new ArrayList<>();
        for (ClickGuiDropdownModule module : moduleComponents) {
            if (module.matchesSearch(searchQuery)) result.add(module);
        }
        return result;
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return;
        for (ClickGuiDropdownModule comp : getVisibleModules()) {
            comp.mouseClicked(mouseX, mouseY, button);
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (ClickGuiDropdownModule comp : getVisibleModules()) {
            comp.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return;
        float innerH = height - HEADER_HEIGHT - CONTENT_PAD * 2.0f;
        float totalHeight = 0.0f;
        List<ClickGuiDropdownModule> visible = getVisibleModules();
        for (ClickGuiDropdownModule comp : visible) totalHeight += comp.getHeight() + ROW_GAP;
        if (!visible.isEmpty()) totalHeight -= ROW_GAP;
        float maxScroll = Math.max(0.0f, totalHeight - innerH);
        scroll = Math.max(0.0f, Math.min(maxScroll, scroll - (float) verticalAmount * 22.0f));
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ClickGuiDropdownModule comp : getVisibleModules()) {
            comp.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void charTyped(char chr, int modifiers) {
        for (ClickGuiDropdownModule comp : getVisibleModules()) {
            comp.charTyped(chr, modifiers);
        }
    }
}
