package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventChunkReload;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.ShaderUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class Sonar
extends Module {
    public static Sonar INSTANCE = new Sonar();
    private final FloatSetting duration = new FloatSetting("Длительность", 5.6f, 0.8f, 10.0f, 0.1f);
    private final FloatSetting alpha = new FloatSetting("Яркость", 1.0f, 0.1f, 1.0f, 0.01f);
    private final FloatSetting widthMul = new FloatSetting("Ширина", 1.0f, 0.35f, 2.2f, 0.05f);
    private final FloatSetting sharpness = new FloatSetting("Резкость", 24.0f, 4.0f, 80.0f, 1.0f);
    private Framebuffer depthCopyBuffer;
    private int lastFbWidth = -1;
    private int lastFbHeight = -1;
    private long currentStart;
    private Vec3d center = Vec3d.ZERO;

    public Sonar() {
        super("Sonar", "Сканирует новые чанки", Module.ModuleCategory.RENDER);
        this.addSettings(this.duration, this.alpha, this.widthMul, this.sharpness);
    }

    @Override
    public void onEnable() {
        if (Sonar.mc.player != null) {
            this.ping(Sonar.mc.player.getPos());
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.currentStart = 0L;
        this.deleteDepthCopyFramebuffer();
        super.onDisable();
    }

    @EventLink
    public void onChunkReload(EventChunkReload event) {
        if (Sonar.mc.player != null) {
            this.ping(Sonar.mc.player.getPos());
        }
    }

    public void renderFromMixin(Matrix4f positionMatrix, Matrix4f projectionMatrix, Vec3d camPos) {
        if (Sonar.mc.player == null || Sonar.mc.world == null || this.currentStart <= 0L) {
            return;
        }
        float durationMs = this.duration.get() * 1000.0f;
        float elapsed = System.currentTimeMillis() - this.currentStart;
        if (elapsed >= durationMs) {
            this.currentStart = 0L;
            return;
        }
        Framebuffer framebuffer = mc.getFramebuffer();
        this.ensureDepthCopyFramebuffer(framebuffer.textureWidth, framebuffer.textureHeight);
        if (this.depthCopyBuffer == null) {
            return;
        }
        this.depthCopyBuffer.copyDepthFrom(framebuffer);
        Matrix4f invView = new Matrix4f((Matrix4fc)positionMatrix).invert();
        Matrix4f invProj = new Matrix4f((Matrix4fc)projectionMatrix).invert();
        float far = Sonar.mc.gameRenderer.getFarPlaneDistance();
        float t2 = MathHelper.clamp((float)(elapsed / durationMs), (float)0.0f, (float)1.0f);
        float r1 = this.lerp(1.0f, far, (float)Easings.QUINT_OUT.ease(t2));
        float r2 = this.lerp(1.0f, far, (float)Easings.QUART_IN_OUT.ease(t2));
        float baseRadius = MathHelper.lerp((float)0.85f, (float)r1, (float)r2);
        float alphaPc = 1.0f - t2;
        float alphaWave = (alphaPc > 0.5f ? 1.0f - alphaPc : alphaPc) * 2.0f;
        alphaWave = Math.min(alphaWave * 1.75f, 1.0f);
        float baseAlpha = MathHelper.clamp((float)(this.alpha.get() * alphaWave), (float)0.0f, (float)1.0f);
        int c1 = ColorUtils.getThemeColor(0);
        int c2 = ColorUtils.getThemeColor(90);
        int c3 = ColorUtils.getThemeColor(180);
        int c4 = ColorUtils.getThemeColor(270);
        float baseWidth = MathHelper.clamp((float)(6.0f + baseRadius * (0.18f * this.widthMul.get())), (float)4.0f, (float)Math.max(10.0f, far * 0.42f));
        float baseSharp = this.sharpness.get();
        this.renderPass(invView, invProj, camPos, framebuffer, baseRadius, baseWidth, baseSharp, this.applyAlpha(c1, baseAlpha), this.applyAlpha(c2, baseAlpha), this.applyAlpha(c3, baseAlpha), this.applyAlpha(c4, baseAlpha));
        RenderSystem.defaultBlendFunc();
    }

    private void renderPass(Matrix4f invView, Matrix4f invProj, Vec3d camPos, Framebuffer framebuffer, float radius, float width, float sharp, int outerColor, int midColor, int innerColor, int scanlineColor) {
        if (radius <= 0.001f || width <= 0.001f) {
            return;
        }
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.scanEffect);
        GlUniform invViewUniform = shader.getUniform("invViewMat");
        GlUniform invProjUniform = shader.getUniform("invProjMat");
        GlUniform posUniform = shader.getUniform("pos");
        GlUniform centerUniform = shader.getUniform("center");
        GlUniform radiusUniform = shader.getUniform("radius");
        GlUniform widthUniform = shader.getUniform("width");
        GlUniform sharpnessUniform = shader.getUniform("sharpness");
        GlUniform outerColorUniform = shader.getUniform("outerColor");
        GlUniform midColorUniform = shader.getUniform("midColor");
        GlUniform innerColorUniform = shader.getUniform("innerColor");
        GlUniform scanlineColorUniform = shader.getUniform("scanlineColor");
        GlUniform debugModeUniform = shader.getUniform("DebugMode");
        if (invViewUniform != null) {
            invViewUniform.set(invView);
        }
        if (invProjUniform != null) {
            invProjUniform.set(invProj);
        }
        if (posUniform != null) {
            posUniform.set((float)camPos.x, (float)camPos.y, (float)camPos.z);
        }
        if (centerUniform != null) {
            centerUniform.set((float)this.center.x, (float)this.center.y, (float)this.center.z);
        }
        if (radiusUniform != null) {
            radiusUniform.set(radius);
        }
        if (widthUniform != null) {
            widthUniform.set(width);
        }
        if (sharpnessUniform != null) {
            sharpnessUniform.set(sharp);
        }
        if (outerColorUniform != null) {
            this.setColor(outerColorUniform, outerColor);
        }
        if (midColorUniform != null) {
            this.setColor(midColorUniform, midColor);
        }
        if (innerColorUniform != null) {
            this.setColor(innerColorUniform, innerColor);
        }
        if (scanlineColorUniform != null) {
            this.setColor(scanlineColorUniform, scanlineColor);
        }
        if (debugModeUniform != null) {
            debugModeUniform.set(0);
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask((boolean)false);
        int depthTex = this.depthCopyBuffer.getDepthAttachment();
        if (depthTex == 0) {
            depthTex = mc.getFramebuffer().getDepthAttachment();
        }
        RenderSystem.bindTexture((int)depthTex);
        GL11.glTexParameteri((int)3553, (int)34892, (int)0);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
        framebuffer.beginWrite(false);
        RenderSystem.setShaderTexture((int)0, (int)depthTex);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.scanEffect);
        this.drawFullscreenQuad();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void drawFullscreenQuad() {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(-1.0f, -1.0f, 0.0f).texture(0.0f, 0.0f);
        buffer.vertex(-1.0f, 1.0f, 0.0f).texture(0.0f, 1.0f);
        buffer.vertex(1.0f, 1.0f, 0.0f).texture(1.0f, 1.0f);
        buffer.vertex(1.0f, -1.0f, 0.0f).texture(1.0f, 0.0f);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void ensureDepthCopyFramebuffer(int width, int height) {
        if (this.depthCopyBuffer == null || this.lastFbWidth != width || this.lastFbHeight != height) {
            this.deleteDepthCopyFramebuffer();
            this.depthCopyBuffer = new SimpleFramebuffer(width, height, true);
            this.lastFbWidth = width;
            this.lastFbHeight = height;
        }
    }

    private void deleteDepthCopyFramebuffer() {
        if (this.depthCopyBuffer != null) {
            this.depthCopyBuffer.delete();
            this.depthCopyBuffer = null;
        }
        this.lastFbWidth = -1;
        this.lastFbHeight = -1;
    }

    private void ping(Vec3d pos) {
        this.currentStart = System.currentTimeMillis();
        this.center = pos;
    }

    private void setColor(GlUniform uniform, int color) {
        int a2 = color >> 24 & 0xFF;
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        if (a2 == 0) {
            a2 = 255;
        }
        uniform.set((float)r2 / 255.0f, (float)g2 / 255.0f, (float)b2 / 255.0f, (float)a2 / 255.0f);
    }

    private int applyAlpha(int color, float alphaMul) {
        int a2 = color >> 24 & 0xFF;
        if (a2 == 0) {
            a2 = 255;
        }
        a2 = (int)((float)a2 * MathHelper.clamp((float)alphaMul, (float)0.0f, (float)1.0f));
        return color & 0xFFFFFF | a2 << 24;
    }

    private float lerp(float a2, float b2, float t2) {
        return a2 + (b2 - a2) * t2;
    }
}

