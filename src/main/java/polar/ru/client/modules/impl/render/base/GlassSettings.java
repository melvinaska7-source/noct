package polar.ru.client.modules.impl.render.base;

import java.awt.Color;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class GlassSettings {
    public final BooleanSetting enabled = new BooleanSetting("Жидкое стекло", false);
    public final FloatSetting alpha = new FloatSetting("Прозрачность", 0.7f, 0.1f, 1.0f, 0.05f).visible(this.enabled::isState);
    public final FloatSetting radius = new FloatSetting("Радиус", 6.0f, 0.0f, 20.0f, 0.5f).visible(this.enabled::isState);
    public final FloatSetting blurStrength = new FloatSetting("Сила размытия", 5.0f, 1.0f, 15.0f, 0.5f).visible(this.enabled::isState);
    public final FloatSetting tintColor = new FloatSetting("Оттенок цвета", 0.0f, 0.0f, 360.0f, 1.0f).visible(this.enabled::isState);
    public final BooleanSetting glowEnabled = new BooleanSetting("Свечение", false);
    public final FloatSetting glowSpeed = new FloatSetting("Скорость свечения", 1.0f, 0.5f, 3.0f, 0.1f).visible(this.glowEnabled::isState);
    public final FloatSetting glowWidth = new FloatSetting("Ширина блика", 30.0f, 10.0f, 100.0f, 5.0f).visible(this.glowEnabled::isState);
    public final FloatSetting glowPauseDuration = new FloatSetting("Пауза (сек)", 3.0f, 1.0f, 10.0f, 0.5f).visible(this.glowEnabled::isState);
    private long glowStartTime = System.currentTimeMillis();
    private boolean glowPaused = false;
    private long glowPauseStartTime = 0L;

    public void drawGlass(MatrixStack matrices, float x2, float y2, float width, float height, int themeColor) {
        if (!this.enabled.isState()) {
            return;
        }
        float r2 = this.radius.getValue().floatValue();
        float a2 = this.alpha.getValue().floatValue();
        float blur = this.blurStrength.getValue().floatValue();
        float tintHue = this.tintColor.getValue().floatValue();
        int tintRgb = Color.HSBtoRGB(tintHue / 360.0f, 0.5f, 1.0f);
        int tintWithAlpha = ColorUtils.rgba(ColorUtils.red(tintRgb), ColorUtils.green(tintRgb), ColorUtils.blue(tintRgb), (int)(a2 * 255.0f));
        RenderUtils.drawBlur(matrices, x2, y2, width, height, r2, blur, ColorUtils.rgba(255, 255, 255, (int)(a2 * 200.0f)));
        RenderUtils.drawBlur(matrices, x2, y2, width, height, r2, blur * 0.8f, ColorUtils.rgba(0, 0, 0, (int)(a2 * 150.0f)));
        int bgColor = ColorUtils.rgba(20, 20, 20, (int)(a2 * 100.0f));
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, r2, bgColor);
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, r2, tintWithAlpha);
    }

    public void drawGlow(MatrixStack matrices, float x2, float y2, float width, float height, int themeColor) {
        if (!this.glowEnabled.isState()) {
            return;
        }
        float progress = this.getGlowProgress(width, height);
        if (progress <= 0.0f || progress >= 1.0f) {
            return;
        }
        float glowW = this.glowWidth.getValue().floatValue();
        float shineX = x2 - glowW + progress * (width + glowW * 2.0f);
        int transparent = ColorUtils.applyAlpha(themeColor, 0.0f);
        int brightColor = ColorUtils.applyAlpha(ColorUtils.interpolateColor(themeColor, 0xFFFFFF, 0.3f), 200.0f);
        int midColor = ColorUtils.applyAlpha(themeColor, 150.0f);
        float shineWidth = glowW;
        RenderUtils.drawGradientRect(matrices, shineX - shineWidth, y2, shineWidth, height, 0.0f, transparent, brightColor, true);
        RenderUtils.drawGradientRect(matrices, shineX, y2, shineWidth * 0.5f, height, 0.0f, brightColor, midColor, true);
        RenderUtils.drawGradientRect(matrices, shineX + shineWidth * 0.5f, y2, shineWidth * 0.5f, height, 0.0f, midColor, transparent, true);
        float glowAlpha = (float)Math.sin((double)progress * Math.PI) * 0.3f;
        int glowOverlay = ColorUtils.applyAlpha(ColorUtils.interpolateColor(themeColor, 0xFFFFFF, 0.5f), (int)(glowAlpha * 255.0f));
        RenderUtils.drawRoundedRect(matrices, shineX - shineWidth * 0.5f, y2, shineWidth * 2.0f, height, 0.0f, glowOverlay);
    }

    public float getGlowProgress(float width, float height) {
        if (!this.glowEnabled.isState()) {
            return 0.0f;
        }
        long now = System.currentTimeMillis();
        float cycleDuration = 2000.0f / this.glowSpeed.getValue().floatValue();
        if (this.glowPaused) {
            if ((float)(now - this.glowPauseStartTime) > this.glowPauseDuration.getValue().floatValue() * 1000.0f) {
                this.glowPaused = false;
                this.glowStartTime = now;
            }
            return 0.0f;
        }
        float elapsed = now - this.glowStartTime;
        float progress = elapsed / cycleDuration;
        if (progress >= 1.0f) {
            this.glowPaused = true;
            this.glowPauseStartTime = now;
            return 1.0f;
        }
        return progress;
    }

    public void resetGlow() {
        this.glowStartTime = System.currentTimeMillis();
        this.glowPaused = false;
        this.glowPauseStartTime = 0L;
    }
}

