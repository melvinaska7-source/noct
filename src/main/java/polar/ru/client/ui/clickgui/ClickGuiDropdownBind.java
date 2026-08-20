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
import polar.ru.client.modules.settings.implement.BindSetting;

public class ClickGuiDropdownBind extends ClickGuiDropdownSetting {

    private final BindSetting setting;
    private final ClickGuiDropdownModule parent;
    private final AnimationUtils anim = new AnimationUtils(0f, 8f, Easings.CUBIC_OUT);

    public ClickGuiDropdownBind(BindSetting setting, ClickGuiDropdownModule parent) {
        this.setting = setting;
        this.parent = parent;
        setHeight(16);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        boolean binding = parent.isBindMode();
        anim.update(binding ? 1f : 0f);

        Font font = Fonts.getFont("moe3", 5.5f);
        if (font == null) return;

        font.draw(matrices, setting.name(), x + 5, y + 6.5f/2f + 1, ColorUtils.rgb(255,255,255));

        String bindText = binding ? "..." : String.valueOf(setting.getKey());
        float bindWidth = font.getWidth(bindText) + 8;
        float bindX = x + width - bindWidth - 5;
        float bindY = y + 3;

        int bgColor = binding ? ColorUtils.rgb(129,135,255) : ColorUtils.rgb(25,26,40);
        RenderUtils.drawRoundedRect(matrices, bindX, bindY, bindWidth, 10f, 2f, bgColor);
        font.draw(matrices, bindText, bindX + (bindWidth - font.getWidth(bindText))/2f, bindY + (10f - font.getHeight())/2f, ColorUtils.rgb(255,255,255));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float bindWidth = Fonts.getFont("moe3", 5.5f).getWidth(setting.getKey() == -1 ? "Нету" : String.valueOf(setting.getKey())) + 8;
            float bindX = x + width - bindWidth - 5;
            float bindY = y + 3;
            if (HoveringUtils.isHovered(mouseX, mouseY, bindX, bindY, bindWidth, 10f)) {
                parent.setBindMode(!parent.isBindMode());
            }
        }
        // Мышиные кнопки для биндинга обрабатываются в родителе
    }

    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void charTyped(char chr, int modifiers) {}
    @Override public boolean isVisible() { return setting.visible(); }
}