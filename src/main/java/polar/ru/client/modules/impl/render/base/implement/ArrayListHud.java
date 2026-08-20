package polar.ru.client.modules.impl.render.base.implement;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;

public class ArrayListHud
extends InterfaceProcessing {
    private static final float LINE_HEIGHT = 9.5f;
    private static final float FLOW_SPEED = 1000.0f;
    private static final Comparator<ModuleEntry> MODULE_WIDTH_COMPARATOR = Comparator.comparingDouble((ModuleEntry entry) -> (double)entry.width).reversed();
    private final List<ModuleEntry> visibleModules = new ArrayList<ModuleEntry>();
    private final AnimationUtils bgAlphaAnimation = new AnimationUtils(0.0f, 8.5f, Easings.CUBIC_OUT);

    public ArrayListHud(Draggable draggable) {
        super(draggable);
    }

    private Font font() {
        return Fonts.getFont("suisse", 14);
    }

    private void drawFlowingText(MatrixStack matrices, Font font, String text, float x2, float y2, int color, float alphaMul) {
        int textColor = ColorUtils.setAlphaColor(color, (int)(255.0f * alphaMul));
        font.draw(matrices, text, x2, y2, textColor);
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        MatrixStack matrices = eventRender.getContext().getMatrices();
        Font font = this.font();
        ObjectArrayList<Module> modules = ModuleClass.INSTANCE.getObject();
        this.visibleModules.clear();
        for (Module module : modules) {
            module.getArrayAnimka().update(module.isEnable() ? 1.0f : 0.0f);
            float anim = module.getArrayAnimka().getValue();
            if (anim <= 0.03f) continue;
            String displayName = module.getDisplayName();
            this.visibleModules.add(new ModuleEntry(displayName.toLowerCase(), font.getWidth(displayName), anim));
        }
        this.visibleModules.sort(MODULE_WIDTH_COMPARATOR);
        long now = System.currentTimeMillis();
        float x2 = this.draggable.getX();
        float y2 = this.draggable.getY();
        float maxWidth = 0.0f;
        boolean leftHalf = x2 <= (float)mc.getWindow().getScaledWidth() * 0.5f;
        for (ModuleEntry entry : this.visibleModules) {
            maxWidth = Math.max(maxWidth, entry.width);
        }
        float yOffset = 0.0f;
        for (int i2 = 0; i2 < this.visibleModules.size(); ++i2) {
            ModuleEntry entry = this.visibleModules.get(i2);
            float anim = entry.anim;
            float lineStep = 9.5f * anim;
            int indexShift = (int)((float)now * 1000.0f / 10.0f) + i2 * 42;
            int rowColor = ColorUtils.getThemeColor(indexShift);
            int rowColor2 = ColorUtils.getThemeColor(indexShift + 90);
            int glowAlpha = (int)((float)(leftHalf ? 140 : 170) * anim);
            int glow1 = ColorUtils.setAlphaColor(rowColor, glowAlpha);
            int glow2 = ColorUtils.setAlphaColor(rowColor2, glowAlpha);
            float textWidth = entry.width;
            float drawX = leftHalf ? x2 - 3.0f : x2 + (maxWidth - textWidth) - 3.0f;
            float drawY = y2 + yOffset + (1.0f - anim) * 7.0f;
            float shadowX = leftHalf ? drawX - 0.6f : drawX - 1.5f;
            float shadowW = leftHalf ? textWidth - 4.0f : textWidth;
            RenderUtils.drawShadow(matrices, shadowX, drawY, shadowW, 6.0f, 5.0f, 11.0f, glow2, glow2, glow1, glow1);
            float textX = leftHalf ? drawX - 0.8f : drawX - 2.0f;
            float rowTextY = drawY + (lineStep - font.getHeight()) / 2.0f;
            this.drawFlowingText(matrices, font, entry.lowerName, textX, rowTextY, rowColor, anim);
            yOffset += lineStep;
        }
        this.bgAlphaAnimation.update(yOffset > 0.5f ? 1.0f : 0.0f);
        float bgProgress = this.bgAlphaAnimation.getValue();
        if (bgProgress > 0.01f) {
            int themeColor = ColorUtils.getThemeColor(0);
            if (this.isFlatStyle()) {
                int bgColor = ColorUtils.rgba(20, 20, 20, 255);
                RenderUtils.drawRoundedRect(matrices, x2, y2, maxWidth, yOffset, 6.0f, bgColor);
            } else if (this.glassSettings.enabled.isState()) {
                this.glassSettings.drawGlass(matrices, x2, y2, maxWidth, yOffset, themeColor);
            } else {
                float lineX = leftHalf ? x2 - 6.5f : x2 + maxWidth - 7.0f;
                float lineWidth = 2.5f;
                int topLineColor = ColorUtils.setAlphaColor(themeColor, (int)(220.0f * bgProgress));
                int bottomLineColor = ColorUtils.setAlphaColor(ColorUtils.getThemeColor(180), (int)(220.0f * bgProgress));
                RenderUtils.drawGradientRect(matrices, lineX, y2, lineWidth, yOffset - 2.0f, 0.0f, topLineColor, bottomLineColor);
                int shadowColor = ColorUtils.rgba(0, 0, 0, (int)(200.0f * bgProgress));
                RenderUtils.drawShadow(matrices, x2 - 2.0f, y2 - 2.0f, maxWidth + 8.0f, yOffset + 4.0f, 6.0f, shadowColor);
                int bgColor = ColorUtils.rgba(20, 20, 20, (int)(100.0f * bgProgress));
                RenderUtils.drawBlur(matrices, x2, y2, maxWidth, yOffset, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, (int)(255.0f * bgProgress)));
                RenderUtils.drawBlur(matrices, x2, y2, maxWidth, yOffset, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, (int)(180.0f * bgProgress)));
                RenderUtils.drawRoundedRect(matrices, x2, y2, maxWidth, yOffset, 6.0f, bgColor);
                float blueLineWidth = maxWidth * 0.4f - 5.0f;
                float blueLineX = x2 + (maxWidth - blueLineWidth) / 2.0f + 13.0f;
                int themeLineColor = ColorUtils.setAlphaColor(themeColor, (int)(255.0f * bgProgress));
                RenderUtils.drawRoundedRect(matrices, blueLineX, y2 - 1.5f, blueLineWidth, 3.5f, 1.0f, themeLineColor);
            }
            if (!this.isFlatStyle() && this.glassSettings.glowEnabled.isState()) {
                this.glassSettings.drawGlow(matrices, x2, y2, maxWidth, yOffset, themeColor);
            }
        }
        this.draggable.setWidth(maxWidth + 4.0f);
        this.draggable.setHeight(yOffset);
        super.onRender(eventRender);
    }

    private record ModuleEntry(String lowerName, float width, float anim) {
    }
}

