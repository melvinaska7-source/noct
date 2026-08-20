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
    private float x, y;
    private final float width = 105f;
    private final float height = 220f;
    private float scroll = 0;
    private float animatedScroll = 0;

    public ClickGuiDropdownPanel(Module.ModuleCategory category, List<Module> modules) {
        this.category = category;
        for (Module module : modules) {
            moduleComponents.add(new ClickGuiDropdownModule(module));
        }
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void render(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        float headerHeight = 20f;

        int bgColor = ColorUtils.rgba(25, 26, 40, 165);
        int bgInner = ColorUtils.rgba(25, 26, 40, 125);
        RenderUtils.drawRoundedRect(matrices, x, y, width, height, 13f, bgColor);
        RenderUtils.drawRoundedRect(matrices, x + 3.8f, y + 3.5f, width - 8, height - 7, 12f, bgInner);
        RenderUtils.drawShadow(matrices, x + 3.8f, y + 3.5f, width - 8, height - 7, 12f, ColorUtils.rgba(0,0,0,50));

        Font font = Fonts.getFont("moe3", 8);
        if (font != null) {
            String catName = category.name();
            float textX = x + width / 2f - font.getWidth(catName) / 2f;
            float textY = y + headerHeight / 2f - font.getHeight() / 2f + 4;
            font.draw(matrices, catName, textX, textY, ColorUtils.rgb(255, 255, 255));
        }

        float innerX = x + 5;
        float innerY = y + headerHeight + 5;
        float innerW = width - 10;
        float innerH = height - headerHeight - 10;

        float totalModulesHeight = 0;
        for (ClickGuiDropdownModule comp : moduleComponents) {
            totalModulesHeight += comp.getHeight() + 3.5f;
        }

        animatedScroll += (scroll - animatedScroll) * 0.1f;
        float maxScroll = Math.max(0, totalModulesHeight - innerH);
        float clampedScroll = Math.max(0, Math.min(maxScroll, animatedScroll));

        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(innerX, innerY, innerW, innerH);

        float offsetY = 0;
        for (ClickGuiDropdownModule comp : moduleComponents) {
            comp.setPosition(innerX, innerY + offsetY - clampedScroll);
            comp.setWidth(innerW);
            comp.render(context, mouseX, mouseY);
            offsetY += comp.getHeight() + 3.5f;
        }

        ScissorUtils.pop();
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            for (ClickGuiDropdownModule comp : moduleComponents) {
                comp.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            for (ClickGuiDropdownModule comp : moduleComponents) {
                comp.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            float totalModulesHeight = 0;
            for (ClickGuiDropdownModule comp : moduleComponents) {
                totalModulesHeight += comp.getHeight() + 3.5f;
            }
            float maxScroll = Math.max(0, totalModulesHeight - (height - 20f - 10f));
            scroll = Math.max(0, Math.min(maxScroll, scroll - (float)verticalAmount * 10f));
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ClickGuiDropdownModule comp : moduleComponents) {
            comp.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void charTyped(char chr, int modifiers) {
        for (ClickGuiDropdownModule comp : moduleComponents) {
            comp.charTyped(chr, modifiers);
        }
    }
}