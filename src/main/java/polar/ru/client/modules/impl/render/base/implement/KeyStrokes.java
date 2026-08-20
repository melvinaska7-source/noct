package polar.ru.client.modules.impl.render.base.implement;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.HudFx;

public class KeyStrokes
extends InterfaceProcessing {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<Long> leftClicks = new ArrayList<Long>();
    private boolean wasLmbPressed = false;
    private final AnimationUtils appearAnimation = HudFx.newAppearAnimation();
    private final AnimationUtils cpsAnimation = HudFx.newValueAnimation(0.0f, 10.0f);

    public KeyStrokes(Draggable draggable) {
        super(draggable);
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        float x2 = this.draggable.getX();
        float y2 = this.draggable.getY();
        Font font = Fonts.getFont("suisse", 15);
        Font smallFont = Fonts.getFont("suisse", 10);
        float keySize = 20.0f;
        float gap = 2.0f;
        boolean wPressed = this.mc.options.forwardKey.isPressed();
        boolean aPressed = this.mc.options.leftKey.isPressed();
        boolean sPressed = this.mc.options.backKey.isPressed();
        boolean dPressed = this.mc.options.rightKey.isPressed();
        boolean spacePressed = this.mc.options.jumpKey.isPressed();
        boolean lmbPressed = this.mc.options.attackKey.isPressed();
        boolean rmbPressed = this.mc.options.useKey.isPressed();
        long currentTime = System.currentTimeMillis();
        if (lmbPressed && !this.wasLmbPressed) {
            this.leftClicks.add(currentTime);
        }
        this.wasLmbPressed = lmbPressed;
        this.leftClicks.removeIf(time -> currentTime - time > 1000L);
        int lmbCps = this.leftClicks.size();
        this.cpsAnimation.update(lmbCps);
        float smoothedCps = this.cpsAnimation.getValue();
        this.appearAnimation.update(1.0f);
        float appearProgress = this.appearAnimation.getValue();
        float wX = x2 + keySize + gap;
        float wY = y2;
        this.drawKey(eventRender, wX, wY, keySize, keySize, "W", wPressed, font);
        float aX = x2;
        float aY = y2 + keySize + gap;
        this.drawKey(eventRender, aX, aY, keySize, keySize, "A", aPressed, font);
        float sX = x2 + keySize + gap;
        float sY = y2 + keySize + gap;
        this.drawKey(eventRender, sX, sY, keySize, keySize, "S", sPressed, font);
        float dX = x2 + (keySize + gap) * 2.0f;
        float dY = y2 + keySize + gap;
        this.drawKey(eventRender, dX, dY, keySize, keySize, "D", dPressed, font);
        float spaceWidth = keySize * 3.0f + gap * 2.0f;
        float spaceHeight = 20.0f;
        float spaceX = x2;
        float spaceY = y2 + (keySize + gap) * 2.0f;
        this.drawKey(eventRender, spaceX, spaceY, spaceWidth, spaceHeight, "Space", spacePressed, font);
        float mouseWidth = (spaceWidth - gap) / 2.0f;
        float mouseHeight = 20.0f;
        float lmbX = x2;
        float lmbY = y2 + (keySize + gap) * 2.0f + spaceHeight + gap;
        float time2 = (float)(System.currentTimeMillis() % 2000L) / 2000.0f * 360.0f;
        int themeColor = ColorUtils.getThemeColor((int)time2);
        this.drawKeyWithCps(eventRender, lmbX, lmbY, mouseWidth, mouseHeight, "LMB", lmbPressed, font, smallFont, smoothedCps, themeColor);
        float rmbX = x2 + mouseWidth + gap;
        float rmbY = y2 + (keySize + gap) * 2.0f + spaceHeight + gap;
        this.drawKey(eventRender, rmbX, rmbY, mouseWidth, mouseHeight, "RMB", rmbPressed, font);
        float totalWidth = keySize * 3.0f + gap * 2.0f;
        float totalHeight = keySize * 2.0f + gap + spaceHeight + gap + mouseHeight + gap;
        MatrixStack matrices = eventRender.getContext().getMatrices();
        float pivotX = x2 + totalWidth / 2.0f;
        float pivotY = y2 + totalHeight / 2.0f;
        HudFx.pushTransform(matrices, appearProgress, pivotX, pivotY);
        if (this.isFlatStyle()) {
            int bgColor = ColorUtils.rgba(20, 20, 20, 255);
            RenderUtils.drawRoundedRect(matrices, x2, y2, totalWidth, totalHeight, 6.0f, bgColor);
        } else {
            int shadowColor = ColorUtils.rgba(0, 0, 0, 200);
            RenderUtils.drawShadow(matrices, x2 - 2.0f, y2 - 2.0f, totalWidth + 4.0f, totalHeight + 4.0f, 6.0f, shadowColor);
            int bgColor = ColorUtils.rgba(20, 20, 20, 100);
            RenderUtils.drawBlur(matrices, x2, y2, totalWidth, totalHeight, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
            RenderUtils.drawBlur(matrices, x2, y2, totalWidth, totalHeight, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, 180));
            RenderUtils.drawRoundedRect(matrices, x2, y2, totalWidth, totalHeight, 6.0f, bgColor);
            float blueLineWidth = totalWidth * 0.4f - 5.0f;
            float blueLineX = x2 + (totalWidth - blueLineWidth) / 2.0f + 13.0f;
            int themeLineColor = themeColor;
            RenderUtils.drawRoundedRect(matrices, blueLineX, y2 - 1.5f, blueLineWidth, 3.5f, 1.0f, themeLineColor);
        }
        HudFx.popTransform(matrices);
        this.draggable.setWidth(totalWidth);
        this.draggable.setHeight(totalHeight);
        super.onRender(eventRender);
    }

    private void drawKey(EventRender.Default eventRender, float x2, float y2, float width, float height, String text, boolean pressed, Object font) {
        int bgColor = pressed ? ColorUtils.rgba(180, 180, 180, 200) : ColorUtils.rgba(25, 25, 25, 150);
        int textColor = pressed ? ColorUtils.rgba(0, 0, 0, 255) : ColorUtils.rgba(255, 255, 255, 255);
        RenderUtils.drawKeyStrokeRect(eventRender.getContext().getMatrices(), x2, y2, width, height, 3.0f, bgColor);
        Font f2 = Fonts.getFont("suisse", 15);
        float textWidth = f2.getWidth(text);
        float textX = x2 + (width - textWidth) / 2.0f;
        float textY = y2 + (height - f2.getHeight()) / 2.0f;
        f2.draw(eventRender.getContext().getMatrices(), text, textX, textY, textColor);
    }

    private void drawKeyWithCps(EventRender.Default eventRender, float x2, float y2, float width, float height, String text, boolean pressed, Object font, Object smallFont, float cps, int themeColor) {
        int bgColor = pressed ? ColorUtils.rgba(180, 180, 180, 200) : ColorUtils.rgba(25, 25, 25, 150);
        int textColor = pressed ? ColorUtils.rgba(0, 0, 0, 255) : ColorUtils.rgba(255, 255, 255, 255);
        RenderUtils.drawKeyStrokeRect(eventRender.getContext().getMatrices(), x2, y2, width, height, 3.0f, bgColor);
        Font f2 = Fonts.getFont("suisse", 15);
        Font sf = Fonts.getFont("suisse", 12);
        float textWidth = f2.getWidth(text);
        float textX = x2 + (width - textWidth) / 2.0f;
        float totalTextH = f2.getHeight() + 2.0f + sf.getHeight();
        float startY = y2 + (height - totalTextH) / 2.0f;
        f2.draw(eventRender.getContext().getMatrices(), text, textX, startY, textColor);
        String cpsText = "cps: " + Math.round(cps);
        float cpsWidth = sf.getWidth(cpsText);
        float cpsX = x2 + (width - cpsWidth) / 2.0f;
        float cpsY = startY + f2.getHeight() + 2.0f;
        sf.draw(eventRender.getContext().getMatrices(), cpsText, cpsX, cpsY, themeColor);
    }
}

