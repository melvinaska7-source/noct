package polar.ru.client.modules.impl.render.base.implement;

import net.minecraft.client.network.ServerInfo;
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

public class Session
extends InterfaceProcessing {
    private long sessionStartTime = System.currentTimeMillis();
    private final AnimationUtils appearAnimation = HudFx.newAppearAnimation();

    public Session(Draggable draggable) {
        super(draggable);
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        ServerInfo info;
        float x2 = this.draggable.getX();
        float y2 = this.draggable.getY();
        long now = System.currentTimeMillis();
        float height = 18.0f;
        String serverName = "local";
        if (mc != null && (info = mc.getCurrentServerEntry()) != null && info.address != null && !info.address.isEmpty()) {
            serverName = info.address;
        }
        String playerName = "unknown";
        if (mc != null && Session.mc.player != null) {
            playerName = Session.mc.player.getName().getString();
        } else if (mc != null && mc.getSession() != null) {
            playerName = mc.getSession().getUsername();
        }
        long elapsed = now - this.sessionStartTime;
        long totalSeconds = elapsed / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        String playTime = hours + "h " + minutes + "m " + seconds + "s";
        String titleText = "sessioninfo";
        String serverText = "server: " + serverName;
        String nameText = "name: " + playerName;
        String playTimeText = "playtime: " + playTime;
        Font font = Fonts.getFont("suisse", 15);
        float titleWidth = font.getWidth(titleText);
        float serverWidth = font.getWidth(serverText);
        float nameWidth = font.getWidth(nameText);
        float playTimeWidth = font.getWidth(playTimeText);
        float maxTextWidth = Math.max(titleWidth, Math.max(serverWidth, Math.max(nameWidth, playTimeWidth)));
        float width = maxTextWidth + 10.0f;
        int time = (int)((float)(now % 2000L) / 2000.0f * 360.0f);
        int leftTop = ColorUtils.getThemeColor(time);
        int leftBottom = ColorUtils.getThemeColor(time + 30);
        int centerTop = ColorUtils.getThemeColor(time + 90);
        int centerBottom = ColorUtils.getThemeColor(time + 120);
        int rightTop = ColorUtils.getThemeColor(time + 180);
        int rightBottom = ColorUtils.getThemeColor(time + 210);
        this.appearAnimation.update(1.0f);
        float appearProgress = this.appearAnimation.getValue();
        MatrixStack matrices = eventRender.getContext().getMatrices();
        float pivotX = x2 + width / 2.0f;
        float pivotY = y2 + (height + 25.0f) / 2.0f;
        HudFx.pushTransform(matrices, appearProgress, pivotX, pivotY);
        if (this.isFlatStyle()) {
            int bgColor = ColorUtils.rgba(20, 20, 20, 255);
            RenderUtils.drawRoundedRect(matrices, x2, y2, width, height + 25.0f, 6.0f, bgColor);
        } else {
            int shadowColor = ColorUtils.rgba(0, 0, 0, 200);
            RenderUtils.drawShadow(matrices, x2 - 2.0f, y2 - 2.0f, width + 4.0f, height + 27.0f, 6.0f, shadowColor);
            int bgColor = ColorUtils.rgba(20, 20, 20, 100);
            RenderUtils.drawBlur(matrices, x2, y2, width, height + 25.0f, 6.0f, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
            RenderUtils.drawBlur(matrices, x2, y2, width, height + 25.0f, 6.0f, 5.0f, ColorUtils.rgba(0, 0, 0, 180));
            RenderUtils.drawRoundedRect(matrices, x2, y2, width, height + 25.0f, 6.0f, bgColor);
            float blueLineWidth = width * 0.4f - 5.0f;
            float blueLineX = x2 + (width - blueLineWidth) / 2.0f + 13.0f;
            int themeLineColor = centerTop;
            RenderUtils.drawRoundedRect(matrices, blueLineX, y2 - 1.5f, blueLineWidth, 3.5f, 1.0f, themeLineColor);
        }
        font.drawStringWithShadow(matrices, titleText, x2 + 3.0f, y2 + 5.0f, -1);
        font.drawStringWithShadow(matrices, serverText, x2 + 3.0f, y2 + 18.0f, -1);
        font.drawStringWithShadow(matrices, nameText, x2 + 3.0f, y2 + 25.5f, -1);
        font.drawStringWithShadow(matrices, playTimeText, x2 + 3.0f, y2 + 33.5f, -1);
        HudFx.popTransform(matrices);
        this.draggable.setHeight(height + 25.0f);
        this.draggable.setWidth(width);
        super.onRender(eventRender);
    }
}

