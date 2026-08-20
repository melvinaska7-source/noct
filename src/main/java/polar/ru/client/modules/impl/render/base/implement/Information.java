package polar.ru.client.modules.impl.render.base.implement;

import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.math.MathUtils;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.render.fonts.ttf.MCFontRenderer;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.HudFx;
import polar.ru.polar;

public class Information
extends InterfaceProcessing {
    private final AnimationUtils pressAnimation = new AnimationUtils(0.0f, 14.0f, Easings.CUBIC_OUT);
    private final AnimationUtils appearAnimation = HudFx.newAppearAnimation();
    private final AnimationUtils bpsAnimation = HudFx.newValueAnimation(0.0f, 9.0f);
    private boolean pressing = false;
    private long pressTime = -1L;

    public Information(Draggable draggable) {
        super(draggable);
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        this.DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    private MCFontRenderer myfont(int size) {
        return Fonts.getTtfFont("myfont.ttf", size);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        float x2 = this.draggable.getX();
        float y2 = this.draggable.getY();
        Font font = Fonts.getFont("suisse", 13);
        Font iconFont = Fonts.getFont("icon", 16);
        Font smallIconFont = Fonts.getFont("icon", 15);
        int colorTheme = !polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow") ? polar.INSTANCE.themeStorage.getThemes().getTheme().color[0] : ColorUtils.getThemeColor();
        boolean drawSquares = this.isUnusualRectType();
        int px = (int)Math.floor(Information.mc.player.getX());
        int py = (int)Math.floor(Information.mc.player.getY());
        int pz = (int)Math.floor(Information.mc.player.getZ());
        float height = 16.0f;
        double bps = MathUtils.calculateBPS();
        this.bpsAnimation.update((float)bps);
        double smoothedBps = this.bpsAnimation.getValue();
        String xValue = String.valueOf(px);
        String yValue = String.valueOf(py);
        String zValue = String.valueOf(pz);
        String coordsText = xValue + "x " + yValue + "y " + zValue + "z";
        String bpsValue = this.formatTwoDecimals(smoothedBps);
        String bpsSuffix = " b/s";
        float widthbps = font.getWidth(bpsValue + bpsSuffix);
        float xbps = x2 + 17.0f + widthbps;
        float widthCords = font.getWidth(coordsText);
        float totalWidth = 13.0f + widthCords + widthbps + 2.0f + 13.8f;
        if (this.pressing && System.currentTimeMillis() - this.pressTime > 150L) {
            this.pressing = false;
        }
        this.pressAnimation.update(this.pressing ? 1.0f : 0.0f);
        this.appearAnimation.update(1.0f);
        float appearProgress = this.appearAnimation.getValue();
        MatrixStack matrices = eventRender.getContext().getMatrices();
        float pivotX = x2 + totalWidth / 2.0f;
        float pivotY = y2 + height / 2.0f;
        HudFx.pushTransform(matrices, appearProgress, pivotX, pivotY);
        if (this.isFlatStyle()) {
            int bgColor = ColorUtils.rgba(20, 20, 20, 255);
            RenderUtils.drawRoundedRect(matrices, x2, y2, totalWidth, height, 5.0f, bgColor);
        } else {
            int shadowColor = ColorUtils.rgba(0, 0, 0, 200);
            RenderUtils.drawShadow(matrices, x2 - 2.0f, y2 - 2.0f, totalWidth + 4.0f, height + 4.0f, 5.0f, shadowColor);
            int bgColor = ColorUtils.rgba(20, 20, 20, 100);
            RenderUtils.drawBlur(matrices, x2, y2, totalWidth, height, 5.0f, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
            RenderUtils.drawBlur(matrices, x2, y2, totalWidth, height, 5.0f, 5.0f, ColorUtils.rgba(0, 0, 0, 180));
            RenderUtils.drawRoundedRect(matrices, x2, y2, totalWidth, height, 5.0f, bgColor);
        }
        float speedTextX = x2 + 13.5f;
        float bpsValueWidth = font.getWidth(bpsValue);
        float textY = y2 + (height - font.getHeight()) / 2.0f;
        float iconY16 = y2 + (height - (float)this.myfont(16).getFontHeight()) / 2.0f;
        float iconY15 = y2 + (height - (float)this.myfont(15).getFontHeight()) / 2.0f;
        font.draw(matrices, bpsValue, speedTextX, textY, -1);
        font.draw(matrices, bpsSuffix, speedTextX + bpsValueWidth - 2.0f, textY, colorTheme);
        float coordsX = xbps + 9.0f;
        font.draw(matrices, xValue, coordsX, textY, -1);
        font.draw(matrices, "x", (coordsX += font.getWidth(xValue)) - 1.0f, textY, colorTheme);
        font.draw(matrices, yValue, coordsX += font.getWidth("x "), textY, -1);
        font.draw(matrices, "y", (coordsX += font.getWidth(yValue)) - 1.0f, textY, colorTheme);
        font.draw(matrices, zValue, coordsX += font.getWidth("y "), textY, -1);
        font.draw(matrices, "z", (coordsX += font.getWidth(zValue)) - 1.0f, textY, colorTheme);
        this.myfont(16).drawString("i", x2 + 3.25f, iconY16, colorTheme);
        this.myfont(15).drawString("j", xbps - 1.0f, iconY15, colorTheme);
        HudFx.popTransform(matrices);
        this.draggable.setHeight(height);
        this.draggable.setWidth(totalWidth);
    }

    public boolean handleClick(double mouseX, double mouseY) {
        return false;
    }

    public void handleCopyClick() {
        int px = (int)Math.floor(Information.mc.player.getX());
        int py = (int)Math.floor(Information.mc.player.getY());
        int pz = (int)Math.floor(Information.mc.player.getZ());
        Information.mc.keyboard.setClipboard(px + " " + py + " " + pz);
        this.pressing = true;
        this.pressTime = System.currentTimeMillis();
    }

    private String formatTwoDecimals(double value) {
        int scaled = (int)Math.round(value * 100.0);
        int fraction = Math.abs(scaled % 100);
        return scaled / 100 + "." + (fraction < 10 ? "0" : "") + fraction;
    }
}

