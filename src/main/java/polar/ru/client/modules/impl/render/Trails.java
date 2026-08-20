package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class Trails
extends Module {
    public static Trails INSTANCE = new Trails();
    private static final float BLINK_SPEED = 0.002f;
    private final FloatSetting trailLength = new FloatSetting("Длина", 2.0f, 2.0f, 4.0f, 0.5f);
    private final List<Point> points = new ArrayList<Point>();

    public Trails() {
        super("Trails", "Создаёт плавную линию ходьбы", Module.ModuleCategory.RENDER);
        this.addSettings(this.trailLength);
    }

    @Override
    public void onDisable() {
        this.points.clear();
        super.onDisable();
    }

    @EventLink
    public void onRender(Event3DRender event) {
        if (Trails.mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            return;
        }
        if (Trails.mc.player == null || Trails.mc.world == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        this.points.removeIf(p2 -> (float)(currentTime - p2.time) > this.trailLength.get() * 100.0f);
        Vec3d playerPos = this.interpolatePlayerPosition(event.getTickDelta());
        this.points.add(new Point(playerPos));
        this.render3DPoints(event.getMatrices());
    }

    private Vec3d interpolatePlayerPosition(float partialTicks) {
        return new Vec3d(MathHelper.lerp((double)partialTicks, (double)Trails.mc.player.prevX, (double)Trails.mc.player.getX()), MathHelper.lerp((double)partialTicks, (double)Trails.mc.player.prevY, (double)Trails.mc.player.getY()), MathHelper.lerp((double)partialTicks, (double)Trails.mc.player.prevZ, (double)Trails.mc.player.getZ()));
    }

    private void render3DPoints(MatrixStack matrixStack) {
        if (this.points.size() < 2) {
            return;
        }
        this.startRendering();
        matrixStack.push();
        Vec3d view = Trails.mc.gameRenderer.getCamera().getPos();
        matrixStack.translate(-view.x, -view.y, -view.z);
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        float blinkFactor = (float)(Math.sin((float)System.currentTimeMillis() * 0.002f) * 0.5 + 0.5);
        int themeColor = ColorUtils.getThemeColor();
        float red = this.applyBlink(ColorUtils.redf(themeColor), blinkFactor);
        float green = this.applyBlink(ColorUtils.greenf(themeColor), blinkFactor);
        float blue = this.applyBlink(ColorUtils.bluef(themeColor), blinkFactor);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int index = 0;
        for (Point p2 : this.points) {
            float alpha = (float)index / (float)this.points.size() * 0.7f;
            int alphaInt = (int)(alpha * 255.0f);
            buffer.vertex(matrix, (float)p2.pos.x, (float)(p2.pos.y + (double)Trails.mc.player.getStandingEyeHeight()), (float)p2.pos.z).color((int)(red * 255.0f), (int)(green * 255.0f), (int)(blue * 255.0f), alphaInt);
            buffer.vertex(matrix, (float)p2.pos.x, (float)p2.pos.y, (float)p2.pos.z).color((int)(red * 255.0f), (int)(green * 255.0f), (int)(blue * 255.0f), alphaInt);
            ++index;
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.lineWidth((float)2.0f);
        this.renderLineStrip(matrix, this.points, true, red, green, blue);
        this.renderLineStrip(matrix, this.points, false, red, green, blue);
        matrixStack.pop();
        this.stopRendering();
    }

    private float applyBlink(float channel, float blinkFactor) {
        return MathHelper.lerp((float)blinkFactor, (float)channel, (float)Math.min(channel + 0.3f, 1.0f));
    }

    private void renderLineStrip(Matrix4f matrix, List<Point> points, boolean withHeight, float red, float green, float blue) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        int index = 0;
        for (Point p2 : points) {
            float alpha = Math.min((float)index / (float)points.size() * 1.5f, 1.0f);
            int alphaInt = (int)(alpha * 255.0f);
            float y2 = withHeight ? (float)(p2.pos.y + (double)Trails.mc.player.getHeight()) : (float)p2.pos.y;
            buffer.vertex(matrix, (float)p2.pos.x, y2, (float)p2.pos.z).color((int)(red * 255.0f), (int)(green * 255.0f), (int)(blue * 255.0f), alphaInt);
            ++index;
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void startRendering() {
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
    }

    private void stopRendering() {
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static class Point {
        public Vec3d pos;
        public long time;

        public Point(Vec3d pos) {
            this.pos = pos;
            this.time = System.currentTimeMillis();
        }
    }
}

