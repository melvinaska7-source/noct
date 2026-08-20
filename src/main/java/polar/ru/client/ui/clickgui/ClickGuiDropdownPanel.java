package polar.ru.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
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
    private final List<ClickGuiDropdownModule> modules = new ArrayList<>();
    private float x, y;
    private static final float WIDTH = 105f;
    private static final float HEIGHT = 220f;
    private static final float HEADER = 20f;
    private float scroll;
    private float animatedScroll;
    private final AnimationUtils headerAnimation = new AnimationUtils(1f, 8f, Easings.CUBIC_OUT);

    public ClickGuiDropdownPanel(Module.ModuleCategory category, List<Module> modules) {
        this.category = category;
        for (Module module : modules) this.modules.add(new ClickGuiDropdownModule(module));
    }

    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public static float getWidth() { return WIDTH; }
    public static float getHeight() { return HEIGHT; }

    public void render(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();

        int outer = ColorUtils.rgba(25, 26, 40, 165);
        int inner = ColorUtils.rgba(25, 26, 40, 125);
        RenderUtils.drawRoundedRect(matrices, x, y, WIDTH, HEIGHT, 13f, outer);
        RenderUtils.drawRoundedRect(matrices, x + 3.8f, y + 3.5f, WIDTH - 8f, HEIGHT - 7f, 12f, inner);
        RenderUtils.drawShadow(matrices, x + 3.8f, y + 3.5f, WIDTH - 8f, HEIGHT - 7f,
                12f, ColorUtils.rgba(0, 0, 0, 50));

        Font font = Fonts.getFont("moe3", 8);
        if (font != null) {
            String title = category.getName();
            float titleX = x + WIDTH / 2f - font.getWidth(title) / 2f;
            font.draw(matrices, title, titleX, y + 6f, ColorUtils.rgb(255, 255, 255));
        }

        float innerX = x + 5f;
        float innerY = y + HEADER + 5f;
        float innerW = WIDTH - 10f;
        float innerH = HEIGHT - HEADER - 10f;

        float totalHeight = 0f;
        for (ClickGuiDropdownModule module : modules) {
            totalHeight += module.getHeight() + 3.5f;
        }

        animatedScroll += (scroll - animatedScroll) * 0.2f;
        float maxScroll = Math.max(0f, totalHeight - innerH);
        float actualScroll = Math.max(0f, Math.min(maxScroll, animatedScroll));

        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(innerX, innerY, innerW, innerH);

        float offset = 0f;
        for (ClickGuiDropdownModule module : modules) {
            module.setPosition(innerX, innerY + offset - actualScroll);
            module.setWidth(innerW);
            module.render(context, mouseX, mouseY);
            offset += module.getHeight() + 3.5f;
        }

        ScissorUtils.pop();
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!inside(mouseX, mouseY)) return;
        for (ClickGuiDropdownModule module : modules) {
            module.mouseClicked(mouseX, mouseY, button);
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (!inside(mouseX, mouseY)) return;
        for (ClickGuiDropdownModule module : modules) {
            module.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!inside(mouseX, mouseY)) return;
        float totalHeight = 0f;
        for (ClickGuiDropdownModule module : modules) totalHeight += module.getHeight() + 3.5f;
        float maxScroll = Math.max(0f, totalHeight - (HEIGHT - HEADER - 10f));
        scroll = Math.max(0f, Math.min(maxScroll, scroll - (float) amount * 10f));
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ClickGuiDropdownModule module : modules) module.keyPressed(keyCode, scanCode, modifiers);
    }

    public void charTyped(char chr, int modifiers) {
        for (ClickGuiDropdownModule module : modules) module.charTyped(chr, modifiers);
    }

    private boolean inside(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEIGHT;
    }
}
