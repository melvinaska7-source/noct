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
import polar.ru.client.modules.settings.implement.FloatSetting;

public class ClickGuiDropdownSlider extends ClickGuiDropdownSetting {

    private final FloatSetting setting;
    private final AnimationUtils anim = new AnimationUtils(0f, 12f, Easings.CUBIC_OUT);
    private boolean dragging = false;

    public ClickGuiDropdownSlider(FloatSetting setting) {
        this.setting = setting;
        setHeight(18);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        float progress = (setting.get() - setting.getMin()) / (setting.getMax() - setting.getMin());
        anim.update(progress);

        Font font = Fonts.getFont("moe3", 5);
        if (font != null) {
            font.draw(matrices, setting.name(), x + 5, y + 4.5f/2f + 1, ColorUtils.rgb(255,255,255));
            String valueStr = String.valueOf(setting.get());
            float valueX = x + width - 5 - font.getWidth(valueStr);
            font.draw(matrices, valueStr, valueX, y + 4.5f/2f + 1, ColorUtils.rgb(255,255,255));
        }

        float trackX = x + 5;
        float trackY = y + 11;
        float trackW = width - 10;
        float trackH = 2;
        RenderUtils.drawRoundedRect(matrices, trackX, trackY, trackW, trackH, 0.6f, ColorUtils.rgb(28,28,31));
        float fillW = trackW * anim.getValue();
        RenderUtils.drawRoundedRect(matrices, trackX, trackY, fillW, trackH, 0.6f, ColorUtils.rgb(129,135,255));
        float knobX = trackX + fillW;
        float knobY = trackY + trackH/2f;
        RenderUtils.drawRoundCircle(matrices, knobX, knobY, 5f, ColorUtils.rgb(129,135,255));
        RenderUtils.drawShadow(matrices, knobX - 4f, knobY - 4f, 8f, 8f, 6f, ColorUtils.rgba(129,135,255,80));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float trackX = x + 5;
            float trackY = y + 11;
            float trackW = width - 10;
            float trackH = 2;
            if (HoveringUtils.isHovered(mouseX, mouseY, trackX, trackY, trackW, trackH + 6)) {
                dragging = true;
                updateValue(mouseX);
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
    }

    private void updateValue(double mouseX) {
        float trackX = x + 5;
        float trackW = width - 10;
        float relative = (float) ((mouseX - trackX) / trackW);
        float clamped = Math.max(0, Math.min(1, relative));
        float value = setting.getMin() + (setting.getMax() - setting.getMin()) * clamped;
        float increment = setting.getIncrement();
        value = Math.round(value / increment) * increment;
        value = Math.max(setting.getMin(), Math.min(setting.getMax(), value));
        setting.setValue(value);
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void charTyped(char chr, int modifiers) {}
    @Override public boolean isVisible() { return setting.visible(); }
}