package polar.ru.client.modules.impl.render.base.implement;

import java.awt.Color;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.storages.implement.helpertstorages.Theme;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.api.utils.render.fonts.ttf.MCFontRenderer;
import polar.ru.api.utils.rpc.DiscordProfileCache;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.HudFx;
import polar.ru.polar;

public class WaterMark
extends InterfaceProcessing {
    private static final Identifier LOGO_TEXTURE = Identifier.of((String)"polar", (String)"polar.png");
    private static final int BG_COLOR = new Color(20, 20, 20, 100).getRGB();
    private static final int WHITE_COLOR = new Color(255, 255, 255, 255).getRGB();
    private static final float BAR_H = 17.0f;
    private static final float BAR_RADIUS = 5.0f;
    private static final float H_PAD = 4.0f;
    private static final float H_PAD_RIGHT = 3.0f;
    private static final float ELEMENT_GAP = 1.0f;
    private static final float PILL_GAP = 1.0f;
    private boolean showFps = true;
    private boolean showMs = true;
    private boolean showServer = true;
    private boolean showName = true;
    private final AnimationUtils fpsAnimation = HudFx.newValueAnimation(0.0f, 9.0f);
    private final AnimationUtils pingAnimation = HudFx.newValueAnimation(0.0f, 9.0f);
    private final AnimationUtils appearAnimation = HudFx.newAppearAnimation();

    public static String getUsername() {
        return "zenZ";
    }

    public static String getUID() {
        return "1";
    }

    public WaterMark(Draggable draggable) {
        super(draggable);
    }

    public boolean isShowFps() {
        return this.showFps;
    }

    public void setShowFps(boolean v2) {
        this.showFps = v2;
    }

    public boolean isShowMs() {
        return this.showMs;
    }

    public void setShowMs(boolean v2) {
        this.showMs = v2;
    }

    public boolean isShowServer() {
        return this.showServer;
    }

    public void setShowServer(boolean v2) {
        this.showServer = v2;
    }

    public boolean isShowName() {
        return this.showName;
    }

    public void setShowName(boolean v2) {
        this.showName = v2;
    }

    private String getServerAddress() {
        if (mc == null) {
            return "localhost";
        }
        if (mc.isIntegratedServerRunning() || mc.getCurrentServerEntry() == null) {
            return "localhost";
        }
        String address = WaterMark.mc.getCurrentServerEntry().address;
        if (address == null || address.isEmpty()) {
            return "localhost";
        }
        if (address.endsWith(":25565")) {
            address = address.substring(0, address.length() - 6);
        }
        return address;
    }

    private MCFontRenderer myfont(int size) {
        return Fonts.getTtfFont("myfont.ttf", (float)size);
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        this.renderStyle(eventRender);
        super.onRender(eventRender);
    }

    private void renderStyle(EventRender.Default event) {
        float iw;
        float iw2;
        float iconW;
        PlayerListEntry entry;
        MatrixStack matrices = event.getContext().getMatrices();
        float x = this.draggable.getX();
        float y = this.draggable.getY();
        Font textFont = Fonts.getFont("suisse", 13);
        Font iconNew14 = Fonts.getFont("iconnew", 14);
        MCFontRenderer myFont14 = this.myfont(14);
        String username = DiscordProfileCache.getUsername();
        if (username == null || username.isEmpty()) {
            username = this.getDisplayUsername();
        }
        String serverAddress = this.getServerAddress();
        int fps = mc != null ? mc.getCurrentFps() : 0;
        this.fpsAnimation.update((float)fps);
        String fpsText = Math.round(this.fpsAnimation.getValue()) + "fps";
        int ping = 0;
        if (mc != null && WaterMark.mc.player != null && mc.getNetworkHandler() != null && (entry = mc.getNetworkHandler().getPlayerListEntry(WaterMark.mc.player.getUuid())) != null) {
            ping = entry.getLatency();
        }
        this.pingAnimation.update((float)ping);
        String pingText = Math.round(this.pingAnimation.getValue()) + "ms";
        float logoSize = 10.0f;
        float brandPillW = 4.0f + logoSize + 4.0f;
        float mainPillW = 7.0f;
        boolean firstMain = true;
        if (this.showName && !username.isEmpty()) {
            iconW = iconNew14.getStringWidth("u") + 2.0f;
            mainPillW += (firstMain ? 0.0f : 1.0f) + iconW + textFont.getStringWidth(username);
            firstMain = false;
        }
        if (this.showServer && !serverAddress.isEmpty()) {
            iconW = iconNew14.getStringWidth("e") + 2.0f;
            mainPillW += (firstMain ? 0.0f : 1.0f) + iconW + textFont.getStringWidth(serverAddress);
            firstMain = false;
        }
        if (this.showFps) {
            iw2 = (float)myFont14.getStringWidth("f") + 2.0f;
            mainPillW += (firstMain ? 0.0f : 1.0f) + iw2 + textFont.getStringWidth(fpsText);
            firstMain = false;
        }
        if (this.showMs) {
            iw2 = iconNew14.getStringWidth("m") + 2.0f;
            mainPillW += (firstMain ? 0.0f : 1.0f) + iw2 + textFont.getStringWidth(pingText);
            firstMain = false;
        }
        float totalW = brandPillW + 1.0f + mainPillW;
        float totalH = 17.0f;
        int iconColor = this.getThemeColor();
        this.appearAnimation.update(1.0f);
        float appearProgress = this.appearAnimation.getValue();
        float pivotX = x + totalW / 2.0f;
        float pivotY = y + totalH / 2.0f;
        HudFx.pushTransform((MatrixStack)matrices, (float)appearProgress, (float)pivotX, (float)pivotY);
        if (this.glassSettings.enabled.isState()) {
            this.glassSettings.drawGlass(matrices, x, y, totalW, totalH, iconColor);
        } else if (this.isFlatStyle()) {
            int bgColor = ColorUtils.rgba((int)20, (int)20, (int)20, (int)255);
            RenderUtils.drawRoundedRect((MatrixStack)matrices, (float)x, (float)y, (float)totalW, (float)totalH, (float)5.0f, (int)bgColor);
        } else {
            int shadowColor = ColorUtils.rgba((int)0, (int)0, (int)0, (int)200);
            RenderUtils.drawShadow((MatrixStack)matrices, (float)(x - 2.0f), (float)(y - 2.0f), (float)(totalW + 4.0f), (float)(totalH + 4.0f), (float)6.0f, (int)shadowColor);
        }
        this.drawBar(matrices, x, y, brandPillW);
        float brandCx = x + 4.0f;
        RenderUtils.drawImage((MatrixStack)matrices, (Identifier)LOGO_TEXTURE, (float)brandCx, (float)(y + (17.0f - logoSize) / 2.0f), (float)logoSize, (float)logoSize, (int)-1);
        brandCx += logoSize + 4.0f;
        float x1b = x + brandPillW + 1.0f;
        this.drawBar(matrices, x1b, y, mainPillW);
        float cx = x1b + 4.0f;
        float textY = y + (17.0f - textFont.getHeight()) / 2.0f;
        float iconY = y + (17.0f - iconNew14.getHeight()) / 2.0f;
        float myFontY = y + (17.0f - (float)myFont14.getFontHeight()) / 2.0f;
        boolean drawnMain = false;
        if (this.showName && !username.isEmpty()) {
            if (drawnMain) {
                cx += 1.0f;
            }
            iw = iconNew14.getStringWidth("u");
            iconNew14.drawGradientStringHorizontal(matrices, "u", cx, iconY, iconColor, iconColor);
            textFont.drawString(matrices, username, cx += iw + 2.0f, textY, WHITE_COLOR);
            cx += textFont.getStringWidth(username);
            drawnMain = true;
        }
        if (this.showServer && !serverAddress.isEmpty()) {
            if (drawnMain) {
                cx += 1.0f;
            }
            iw = iconNew14.getStringWidth("e");
            iconNew14.drawGradientStringHorizontal(matrices, "e", cx, iconY, iconColor, iconColor);
            textFont.drawString(matrices, serverAddress, cx += iw + 2.0f, textY, WHITE_COLOR);
            cx += textFont.getStringWidth(serverAddress);
            drawnMain = true;
        }
        if (this.showFps) {
            if (drawnMain) {
                cx += 1.0f;
            }
            iw = myFont14.getStringWidth("f");
            myFont14.drawGradientStringHorizontal("f", cx, myFontY, iconColor, iconColor);
            textFont.drawString(matrices, fpsText, cx += iw + 2.0f, textY, WHITE_COLOR);
            cx += textFont.getStringWidth(fpsText);
            drawnMain = true;
        }
        if (this.showMs) {
            if (drawnMain) {
                cx += 1.0f;
            }
            iw = iconNew14.getStringWidth("m");
            iconNew14.drawGradientStringHorizontal(matrices, "m", cx, iconY, iconColor, iconColor);
            textFont.drawString(matrices, pingText, cx += iw + 2.0f, textY, WHITE_COLOR);
            cx += textFont.getStringWidth(pingText);
            drawnMain = true;
        }
        HudFx.popTransform((MatrixStack)matrices);
        if (this.glassSettings.glowEnabled.isState()) {
            this.glassSettings.drawGlow(matrices, x, y, totalW, totalH, iconColor);
        }
        this.draggable.setWidth(totalW);
        this.draggable.setHeight(totalH);
    }

    private void drawBar(MatrixStack matrices, float x, float y, float w) {
        if (this.isFlatStyle()) {
            int bgColor = ColorUtils.rgba((int)20, (int)20, (int)20, (int)255);
            RenderUtils.drawRoundedRect((MatrixStack)matrices, (float)x, (float)y, (float)w, (float)17.0f, (float)5.0f, (int)bgColor);
        } else {
            RenderUtils.drawBlur((MatrixStack)matrices, (float)x, (float)y, (float)w, (float)17.0f, (float)5.0f, (float)5.0f, (int)ColorUtils.rgba((int)255, (int)255, (int)255, (int)255));
            RenderUtils.drawBlur((MatrixStack)matrices, (float)x, (float)y, (float)w, (float)17.0f, (float)5.0f, (float)5.0f, (int)ColorUtils.rgba((int)0, (int)0, (int)0, (int)180));
            RenderUtils.drawRoundedRect((MatrixStack)matrices, (float)x, (float)y, (float)w, (float)17.0f, (float)5.0f, (int)BG_COLOR);
        }
    }

    private int getThemeColor() {
        Theme theme;
        if (polar.INSTANCE != null && polar.INSTANCE.themeStorage != null && !(theme = polar.INSTANCE.themeStorage.getThemes().getTheme()).getName().equals("Rainbow")) {
            return theme.color[0];
        }
        return ColorUtils.getThemeColor();
    }

    private String getDisplayUsername() {
        if (mc != null && WaterMark.mc.player != null) {
            return WaterMark.mc.player.getName().getString();
        }
        return WaterMark.getUsername();
    }
}
