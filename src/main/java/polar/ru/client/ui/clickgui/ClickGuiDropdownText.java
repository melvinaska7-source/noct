package polar.ru.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.math.HoveringUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.settings.implement.TextSetting;

public class ClickGuiDropdownText extends ClickGuiDropdownSetting {

    private final TextSetting setting;
    private boolean editing = false;

    public ClickGuiDropdownText(TextSetting setting) {
        this.setting = setting;
        setHeight(22);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        Font font = Fonts.getFont("moe3", 6);
        if (font == null) return;

        font.draw(matrices, setting.name(), x + 5, y + 2, ColorUtils.rgb(255,255,255));

        String text = setting.get();
        String display = text.isEmpty() ? "..." : text;
        if (editing && System.currentTimeMillis() % 1000 > 500) display += "_";

        float boxX = x + 5;
        float boxY = y + 10;
        float boxW = width - 10;
        float boxH = 9f;

        RenderUtils.drawRoundedRect(matrices, boxX, boxY, boxW, boxH, 2f, ColorUtils.rgb(25,26,40));
        font.draw(matrices, display, boxX + 3, boxY + (boxH - font.getHeight())/2f, ColorUtils.rgb(255,255,255));

        setHeight(22); // фиксированная высота, можно увеличить при длинном тексте, но упростим
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float boxX = x + 5;
            float boxY = y + 10;
            float boxW = width - 10;
            float boxH = 9f;
            if (HoveringUtils.isHovered(mouseX, mouseY, boxX, boxY, boxW, boxH)) {
                editing = !editing;
            } else {
                editing = false;
            }
        }
    }

    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editing) return;
        if (keyCode == 256 || keyCode == 257) {
            editing = false;
            return;
        }
        if (keyCode == 259) {
            String current = setting.get();
            if (!current.isEmpty()) {
                setting.setText(current.substring(0, current.length() - 1));
            }
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (editing) {
            setting.setText(setting.get() + chr);
        }
    }

    @Override
    public boolean isVisible() { return setting.visible(); }
}