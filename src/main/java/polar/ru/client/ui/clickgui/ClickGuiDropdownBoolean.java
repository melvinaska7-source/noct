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
import polar.ru.client.modules.settings.implement.BooleanSetting;

public class ClickGuiDropdownBoolean extends ClickGuiDropdownSetting {

    private final BooleanSetting setting;
    private final AnimationUtils anim = new AnimationUtils(0f, 8f, Easings.CUBIC_OUT);

    public ClickGuiDropdownBoolean(BooleanSetting setting) {
        this.setting = setting;
        setHeight(16);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        anim.update(setting.isState() ? 1f : 0f);

        Font font = Fonts.getFont("moe3", 6);
        if (font != null) {
            font.draw(matrices, setting.name(), x + 5, y + 6.5f/2f + 1, ColorUtils.rgb(255,255,255));
        }

        float toggleW = 15;
        float toggleH = 7;
        float toggleX = x + width - toggleW - 7;
        float toggleY = y + getHeight()/2f - toggleH/2f;

        RenderUtils.drawRoundedRect(matrices, toggleX, toggleY, toggleW, toggleH, 3f, ColorUtils.rgb(29,29,31));
        float progress = anim.getValue();
        int color = ColorUtils.interpolate(ColorUtils.rgb(129,135,255), ColorUtils.rgb(129,135,255), 1 - progress);
        float circleX = toggleX + 4 + (7 * progress);
        float circleY = toggleY + toggleH/2f;
        RenderUtils.drawRoundCircle(matrices, circleX, circleY, 5f, color);
        RenderUtils.drawShadow(matrices, circleX - 4f, circleY - 4f, 8f, 8f, 6f, ColorUtils.applyAlpha(color, 80));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float toggleW = 15;
            float toggleH = 7;
            float toggleX = x + width - toggleW - 7;
            float toggleY = y + getHeight()/2f - toggleH/2f;
            if (HoveringUtils.isHovered(mouseX, mouseY, toggleX, toggleY, toggleW, toggleH)) {
                setting.setState(!setting.isState());
                anim.update(setting.isState() ? 1f : 0f);
            }
        }
    }

    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void charTyped(char chr, int modifiers) {}
    @Override public boolean isVisible() { return setting.visible(); }
}