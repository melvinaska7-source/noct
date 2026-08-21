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
import polar.ru.client.modules.settings.implement.ModeSetting;

import java.util.List;

public class ClickGuiDropdownMode extends ClickGuiDropdownSetting {

    private final ModeSetting setting;
    private final AnimationUtils[] anims;

    public ClickGuiDropdownMode(ModeSetting setting) {
        this.setting = setting;
        List<String> modes = setting.getMods();
        anims = new AnimationUtils[modes.size()];
        for (int i = 0; i < modes.size(); i++) {
            boolean selected = modes.get(i).equals(setting.getCurrent());
            anims[i] = new AnimationUtils(selected ? 1f : 0f, 8f, Easings.CUBIC_OUT);
        }
        setHeight(22);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        MatrixStack matrices = context.getMatrices();
        Font font = Fonts.getFont("moe3", 5);
        if (font == null) return;

        font.draw(matrices, setting.name(), x + 5, y + 2, ColorUtils.rgb(255,255,255));

        List<String> modes = setting.getMods();
        float chipX = x + 5;
        float chipY = y + 10;
        float availableWidth = width - 10;
        float chipHeight = 11f;
        float gap = 2f;
        float offsetX = 0;
        float offsetY = 0;
        int itemsInRow = 0;
        int maxPerRow = 2;

        for (int i = 0; i < modes.size(); i++) {
            String mode = modes.get(i);
            boolean selected = mode.equals(setting.getCurrent());
            anims[i].update(selected ? 1f : 0f);

            float textWidth = font.getWidth(mode);
            float chipWidth = textWidth + 8;
            if (offsetX > 0 && offsetX + chipWidth > availableWidth || itemsInRow >= maxPerRow) {
                offsetX = 0;
                offsetY += chipHeight + gap;
                itemsInRow = 0;
            }

            float cx = chipX + offsetX;
            float cy = chipY + offsetY;
            float progress = anims[i].getValue();
            int bgColor = ColorUtils.interpolate(ColorUtils.rgb(25,26,40), ColorUtils.rgb(129,135,255), progress);
            int textColor = ColorUtils.interpolate(ColorUtils.rgb(160,163,175), ColorUtils.rgb(255,255,255), progress);

            RenderUtils.drawRoundedRect(matrices, cx, cy, chipWidth, chipHeight, 2f, bgColor);
            font.draw(matrices, mode, cx + (chipWidth - textWidth)/2f, cy + (chipHeight - font.getHeight())/2f, textColor);

            offsetX += chipWidth + gap;
            itemsInRow++;
        }

        float totalRows = (float) Math.ceil(modes.size() / (double) maxPerRow);
        float totalHeight = 10 + totalRows * (chipHeight + gap) - gap;
        setHeight(totalHeight + 2);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        List<String> modes = setting.getMods();
        Font font = Fonts.getFont("moe3", 5);
        if (font == null) return;

        float chipX = x + 5;
        float chipY = y + 10;
        float availableWidth = width - 10;
        float chipHeight = 11f;
        float gap = 2f;
        float offsetX = 0;
        float offsetY = 0;
        int itemsInRow = 0;
        int maxPerRow = 2;

        for (int i = 0; i < modes.size(); i++) {
            String mode = modes.get(i);
            float textWidth = font.getWidth(mode);
            float chipWidth = textWidth + 8;
            if (offsetX > 0 && offsetX + chipWidth > availableWidth || itemsInRow >= maxPerRow) {
                offsetX = 0;
                offsetY += chipHeight + gap;
                itemsInRow = 0;
            }
            if (HoveringUtils.isHovered(mouseX, mouseY, chipX + offsetX, chipY + offsetY, chipWidth, chipHeight)) {
                setting.set(mode);
                for (int j = 0; j < anims.length; j++) {
                    anims[j].update(modes.get(j).equals(mode) ? 1f : 0f);
                }
                return;
            }
            offsetX += chipWidth + gap;
            itemsInRow++;
        }
    }

    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void charTyped(char chr, int modifiers) {}
    @Override public boolean isVisible() { return setting.visible(); }
}