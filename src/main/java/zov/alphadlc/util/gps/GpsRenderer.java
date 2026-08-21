package zov.alphadlc.util.gps;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;
import zov.alphadlc.util.render.msdf.Fonts;

public final class GpsRenderer implements IMinecraft {
    private static final GpsRenderer INSTANCE = new GpsRenderer();
    private static final Identifier GPS_ICON = Identifier.of("mre", "textures/gps.png");
    private double targetX;
    private double targetZ;
    private boolean enabled;

    public static GpsRenderer get() {
        return INSTANCE;
    }

    private GpsRenderer() {
        AlphaDLC.getInstance().getEventBus().register(this);
    }

    public void setTarget(double x2, double z2) {
        this.targetX = x2;
        this.targetZ = z2;
    }

    @Subscribe
    private void onHud(EventHUD e2) {
        if (!this.enabled) {
            return;
        }
        if (MinecraftClient.getInstance().player == null) {
            return;
        }
        DrawContext drawContext = e2.getDrawContext();
        MatrixStack ms = drawContext.getMatrices();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        Vec3d pos = GpsRenderer.mc.player.getPos();
        double dx = this.targetX - pos.x;
        double dz = this.targetZ - pos.z;
        int dist = (int)Math.sqrt(dx * dx + dz * dz);
        float angle = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float rot = MathHelper.wrapDegrees((float)(angle - GpsRenderer.mc.player.getYaw()));
        String distTxt = dist + "m";
        int white = ColorProvider.rgba(255, 255, 255, 255.0f);
        double cx = (double)sw / 2.0;
        double cy = (double)sh / 2.0 - 80.0;
        
        // Отрисовка дистанции
        float w1 = Fonts.SFSEMIBOLD.get().getWidth(distTxt, 7.0f);
        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), distTxt, (float)(cx - (double)(w1 / 2.0f)), (float)(cy + 8.0), white, 7.0f);
        
        // Отрисовка вращающейся иконки GPS
        int iconSize = 16;
        float centerX = (float)cx;
        float centerY = (float)(cy + 22.0);
        
        ms.push();
        ms.translate(centerX, centerY, 0);
        ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(rot));
        ms.translate(-iconSize / 2.0f, -iconSize / 2.0f, 0);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        drawContext.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, GPS_ICON, 0, 0, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
        
        ms.pop();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
