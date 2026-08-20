package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import polar.ru.api.QClient;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class Ghost
extends Module
implements QClient {
    public static Ghost INSTANCE = new Ghost();
    private final FloatSetting trailLength = new FloatSetting("Длина трейла", 25.0f, 5.0f, 50.0f, 1.0f);
    private final BooleanSetting firstPerson = new BooleanSetting("В первом лице", false);
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", true);
    private static final Identifier GLOW_TEXTURE = Identifier.of((String)"polar", (String)"textures/targetesp/bloom.png");
    private final float[] SCALE_CACHE = new float[101];
    private final Vec3d[] ghostPos = new Vec3d[3];
    private final Vec3d[] ghostVel = new Vec3d[3];
    private final List<Vec3d>[] trails = new ArrayList[3];
    private long lastUpdate = 0L;

    public Ghost() {
        super("Ghost", "Призрачный эффект вокруг игрока", Module.ModuleCategory.RENDER);
        int i2;
        for (i2 = 0; i2 <= 100; ++i2) {
            this.SCALE_CACHE[i2] = Math.max(0.28f * ((float)i2 / 100.0f), 0.15f);
        }
        for (i2 = 0; i2 < 3; ++i2) {
            this.trails[i2] = new ArrayList<Vec3d>();
        }
        this.addSettings(this.trailLength, this.firstPerson, this.throughWalls);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.lastUpdate = System.currentTimeMillis();
        for (int i2 = 0; i2 < 3; ++i2) {
            this.ghostPos[i2] = null;
            this.ghostVel[i2] = Vec3d.ZERO;
            this.trails[i2].clear();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        for (int i2 = 0; i2 < 3; ++i2) {
            this.ghostPos[i2] = null;
            this.ghostVel[i2] = Vec3d.ZERO;
            this.trails[i2].clear();
        }
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        if (Ghost.mc.player == null || Ghost.mc.world == null) {
            return;
        }
        if (!this.firstPerson.isState() && Ghost.mc.options.getPerspective().isFirstPerson()) {
            return;
        }
        float tickDelta = event.getTickDelta();
        MatrixStack matrices = event.getMatrices();
        Camera camera = event.getCamera();
        long now = System.currentTimeMillis();
        if (this.lastUpdate == 0L) {
            this.lastUpdate = now;
        }
        float dt = (float)(now - this.lastUpdate) / 1000.0f;
        this.lastUpdate = now;
        if (dt > 0.1f) {
            dt = 0.1f;
        }
        if (dt <= 0.0f) {
            dt = 0.001f;
        }
        double x2 = MathHelper.lerp((double)tickDelta, (double)Ghost.mc.player.lastRenderX, (double)Ghost.mc.player.getX());
        double y2 = MathHelper.lerp((double)tickDelta, (double)Ghost.mc.player.lastRenderY, (double)Ghost.mc.player.getY());
        double z2 = MathHelper.lerp((double)tickDelta, (double)Ghost.mc.player.lastRenderZ, (double)Ghost.mc.player.getZ());
        Vec3d playerPos = new Vec3d(x2, y2, z2);
        float age = (float)Ghost.mc.player.age + tickDelta;
        int currentTrailLength = (int)this.trailLength.get();
        int factor = 4;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShaderTexture((int)0, (Identifier)GLOW_TEXTURE);
        if (this.throughWalls.isState()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.depthMask((boolean)false);
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();
        Vec3d cameraPos = camera.getPos();
        double time = (double)(now % 100000L) / 1000.0;
        for (int j2 = 0; j2 < 3; ++j2) {
            double radians = Math.toRadians((age * (float)factor + (float)(j2 * 120)) % (float)(factor * 360));
            float baseR = Ghost.mc.player.getWidth() * 1.15f;
            float dynR = baseR * (0.95f + 0.12f * (float)Math.sin(time * 0.8 + (double)j2));
            double tx = Math.cos(radians) * (double)dynR;
            double tz = Math.sin(radians) * (double)dynR;
            double ty = 0.15 + 0.12 * Math.sin(time * 2.5 + (double)j2);
            Vec3d targetWorldPos = playerPos.add(tx, ty, tz);
            if (this.ghostPos[j2] == null || this.ghostPos[j2].distanceTo(targetWorldPos) > 15.0) {
                this.ghostPos[j2] = targetWorldPos;
                this.ghostVel[j2] = Vec3d.ZERO;
            }
            Vec3d diff = targetWorldPos.subtract(this.ghostPos[j2]);
            double spring = 15.0;
            double damping = 0.85;
            this.ghostVel[j2] = this.ghostVel[j2].add(diff.multiply(spring * (double)dt)).multiply(Math.pow(damping, dt * 60.0f));
            this.ghostPos[j2] = this.ghostPos[j2].add(this.ghostVel[j2].multiply((double)(dt * 60.0f)));
            if (this.trails[j2].isEmpty() || this.trails[j2].get(0).distanceTo(this.ghostPos[j2]) > 0.015) {
                this.trails[j2].add(0, this.ghostPos[j2]);
                while (this.trails[j2].size() > currentTrailLength) {
                    this.trails[j2].remove(this.trails[j2].size() - 1);
                }
            }
            int primaryColor = ColorUtils.getThemeColor();
            int secondaryColor = ColorUtils.darken(primaryColor, 0.7f);
            int baseColor = j2 == 0 ? primaryColor : (j2 == 1 ? secondaryColor : ColorUtils.interpolate(primaryColor, secondaryColor, 0.5f));
            int brightenedColor = ColorUtils.brighten(baseColor, 0.4f);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (int i2 = 0; i2 < this.trails[j2].size(); ++i2) {
                Vec3d p2 = this.trails[j2].get(i2);
                float offset = 1.0f - (float)i2 / (float)currentTrailLength;
                matrices.push();
                matrices.translate(p2.x - cameraPos.x, p2.y - cameraPos.y, p2.z - cameraPos.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                float opacity = (float)Math.pow(offset, 1.5) * 0.85f;
                int alpha = (int)(opacity * 255.0f);
                int color = ColorUtils.replAlpha(brightenedColor, alpha);
                float scale = this.SCALE_CACHE[Math.min((int)(offset * 100.0f), 100)] * 0.65f;
                int r2 = ColorUtils.red(color);
                int g2 = ColorUtils.green(color);
                int b2 = ColorUtils.blue(color);
                int a2 = ColorUtils.alpha(color);
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                buffer.vertex(matrix, -scale, -scale, 0.0f).texture(0.0f, 1.0f).color(r2, g2, b2, a2);
                buffer.vertex(matrix, -scale, scale, 0.0f).texture(0.0f, 0.0f).color(r2, g2, b2, a2);
                buffer.vertex(matrix, scale, scale, 0.0f).texture(1.0f, 0.0f).color(r2, g2, b2, a2);
                buffer.vertex(matrix, scale, -scale, 0.0f).texture(1.0f, 1.0f).color(r2, g2, b2, a2);
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }
}

