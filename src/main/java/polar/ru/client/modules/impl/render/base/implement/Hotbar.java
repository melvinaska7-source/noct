package polar.ru.client.modules.impl.render.base.implement;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.render.fonts.ttf.MCFontRenderer;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.HudFx;
import polar.ru.polar;

public class Hotbar
extends InterfaceProcessing {
    private static final float SLOT_SIZE = 20.0f;
    private static final float SLOT_GAP = 2.0f;
    private static final float PADDING = 4.0f;
    private final AnimationUtils panelAlphaAnimation = HudFx.newAppearAnimation();
    private final AnimationUtils[] slotAnimations = new AnimationUtils[9];
    private int lastSelectedSlot = -1;

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    public Hotbar(Draggable draggable) {
        super(draggable);
        for (int i = 0; i < 9; ++i) {
            this.slotAnimations[i] = new AnimationUtils(0.0f, 10.0f, Easings.CUBIC_OUT);
        }
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        this.DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        if (Hotbar.mc.player == null) {
            return;
        }
        float baseX = this.draggable.getX();
        float baseY = this.draggable.getY();
        int colorTheme = this.getStableThemeColor();
        float hotbarWidth = 196.0f;
        float totalWidth = hotbarWidth + 8.0f;
        float totalHeight = 28.0f;
        int currentSelectedSlot = Hotbar.mc.player.getInventory().selectedSlot;
        if (currentSelectedSlot != this.lastSelectedSlot) {
            this.lastSelectedSlot = currentSelectedSlot;
        }
        this.panelAlphaAnimation.update(1.0f);
        float panelProgress = this.panelAlphaAnimation.getValue();
        int panelAlphaMul = (int)(255.0f * panelProgress);
        MatrixStack matrices = eventRender.getContext().getMatrices();
        float pivotX = baseX + totalWidth / 2.0f;
        float pivotY = baseY + totalHeight / 2.0f;
        float eased = HudFx.pushTransform(matrices, panelProgress, pivotX, pivotY);
        if (this.isFlatStyle()) {
            int bgColor = ColorUtils.rgba(20, 20, 20, 255);
            RenderUtils.drawRoundedRect(matrices, baseX, baseY, totalWidth, totalHeight, 6.0f, bgColor);
        } else {
            int shadowColor = ColorUtils.rgba(0, 0, 0, (int)(200.0f * eased));
            RenderUtils.drawShadow(matrices, baseX - 2.0f, baseY - 2.0f, totalWidth + 4.0f, totalHeight + 4.0f, 6.0f, shadowColor);
        }
        float hotbarY = baseY + 4.0f;
        float slotX = baseX + 4.0f;
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = (ItemStack)Hotbar.mc.player.getInventory().main.get(i);
            boolean isSelected = i == currentSelectedSlot;
            this.slotAnimations[i].update(isSelected ? 1.0f : 0.0f);
            float slotAnimValue = this.slotAnimations[i].getValue();
            this.drawSlot(eventRender.getContext(), matrices, stack, slotX, hotbarY, isSelected, colorTheme, panelAlphaMul, slotAnimValue, i);
            slotX += 22.0f;
        }
        HudFx.popTransform(matrices);
        this.draggable.setWidth(totalWidth);
        this.draggable.setHeight(totalHeight);
    }

    private void drawSlot(DrawContext context, MatrixStack matrices, ItemStack stack, float x, float y, boolean selected, int colorTheme, int alpha, float animValue, int slotIndex) {
        float scale = 1.0f + animValue * 0.1f;
        float yOffset = -animValue * 2.0f;
        float actualX = x + 20.0f * (1.0f - scale) / 2.0f;
        float actualY = y + yOffset + 20.0f * (1.0f - scale) / 2.0f;
        float actualSize = 20.0f * scale;
        int bgColor;
        if (this.isFlatStyle()) {
            bgColor = selected ? ColorUtils.setAlphaColor(colorTheme, (int)(76.5f * animValue)) : ColorUtils.rgba(30, 30, 30, 255);
        } else {
            bgColor = selected ? ColorUtils.setAlphaColor(colorTheme, (int)(102.0f * animValue)) : ColorUtils.rgba(30, 30, 30, (int)((float)(200 * alpha) / 255.0f));
        }
        RenderUtils.drawRoundedRect(matrices, actualX, actualY, actualSize, actualSize, 3.0f, bgColor);
        if (!stack.isEmpty()) {
            matrices.push();
            matrices.translate(actualX + 2.0f + (actualSize - 20.0f) / 2.0f, actualY + 2.0f + (actualSize - 20.0f) / 2.0f, 0.0f);
            matrices.scale(scale, scale, scale);
            context.drawItem(stack, 0, 0);
            matrices.pop();
            if (stack.getCount() > 1) {
                String count = String.valueOf(stack.getCount());
                float countX = actualX + actualSize - this.issue(10).getStringWidth(count) - 2.0f;
                float countY = actualY + actualSize - this.issue(10).getHeight() - 1.0f;
                this.issue(10).draw(matrices, count, countX, countY, ColorUtils.rgba(255, 255, 255, alpha));
            }
        }
        if (selected && !this.isFlatStyle()) {
            int borderColor = ColorUtils.setAlphaColor(colorTheme, (int)((float)alpha * animValue));
            float outlineSize = 1.5f * animValue;
            RenderUtils.drawRoundedRectOutline(matrices, actualX - outlineSize, actualY - outlineSize, actualSize + outlineSize * 2.0f, actualSize + outlineSize * 2.0f, 3.0f, 3.0f, 3.0f, 3.0f, outlineSize, borderColor, borderColor, borderColor, borderColor);
        }
    }

    private int getStableThemeColor() {
        if (!polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor();
    }
}
