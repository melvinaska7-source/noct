package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.ShaderUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.mixin.WorldRendererAccessor;

public class ShaderEsp
extends Module {
    public static ShaderEsp INSTANCE = new ShaderEsp();
    private static final float EPSILON = 0.001f;
    private static final long OUTLINE_RETRY_DELAY_MS = 3000L;
    private static final double MAX_RANGE = 256.0;
    private static final float FILL_ALPHA = 0.7f;
    private static final int FILL_MIN_ITERATIONS = 2;
    private static final float GLOW_VALUE = 0.55f;
    private static final float WIDTH_VALUE = 0.9f;
    private final ListSetting targets = new ListSetting("Цели", new BooleanSetting("Игроки", true), new BooleanSetting("Кристаллы", true), new BooleanSetting("Предметы", false), new BooleanSetting("Себя", false));
    private final BooleanSetting fill = new BooleanSetting("Заливка", false);
    private final List<Framebuffer> bloomBuffers = new ArrayList<Framebuffer>();
    private Framebuffer depthCopyBuffer;
    private int bloomWidth = -1;
    private int bloomHeight = -1;
    private boolean outlineReady;
    private boolean hasOutlineTargetsCached;
    private long nextOutlineRetryAt;

    public ShaderEsp() {
        super("ShaderESP", "Красивая обводка энтити", Module.ModuleCategory.RENDER);
        this.addSettings(this.targets, this.fill);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.outlineReady = false;
        this.nextOutlineRetryAt = 0L;
        this.tryEnsureOutlineProcessor();
    }

    @Override
    public void onDisable() {
        for (Framebuffer fb : this.bloomBuffers) {
            fb.delete();
        }
        this.bloomBuffers.clear();
        if (this.depthCopyBuffer != null) {
            this.depthCopyBuffer.delete();
            this.depthCopyBuffer = null;
        }
        this.bloomWidth = -1;
        this.bloomHeight = -1;
        this.outlineReady = false;
        this.hasOutlineTargetsCached = false;
        this.nextOutlineRetryAt = 0L;
        super.onDisable();
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (!this.isEnable()) {
            return;
        }
        if (ShaderEsp.mc.world == null || ShaderEsp.mc.worldRenderer == null) {
            this.outlineReady = false;
            this.hasOutlineTargetsCached = false;
            return;
        }
        this.hasOutlineTargetsCached = this.hasOutlineTargets();
        if (!this.hasOutlineTargetsCached) {
            this.outlineReady = false;
            return;
        }
        if (!this.outlineReady && System.currentTimeMillis() >= this.nextOutlineRetryAt) {
            this.tryEnsureOutlineProcessor();
        }
    }

    @EventLink(priority=200)
    public void onRender2D(EventRender.Default event) {
        ShaderProgram glowShader;
        ShaderProgram fillShader;
        if (!this.isEnable() || ShaderEsp.mc.world == null || ShaderEsp.mc.player == null || ShaderEsp.mc.worldRenderer == null) {
            return;
        }
        boolean hasGlow = true;
        boolean hasFill = this.fill.isState();
        if (!hasGlow && !hasFill) {
            return;
        }
        if (!this.hasOutlineTargetsCached) {
            return;
        }
        if (!this.tryEnsureOutlineProcessor()) {
            return;
        }
        Framebuffer outlineBuffer = this.getOutlineSourceFramebuffer();
        if (outlineBuffer == null || outlineBuffer.getColorAttachment() == 0) {
            return;
        }
        Framebuffer mainBuffer = mc.getFramebuffer();
        this.ensureDepthCopyBuffer(mainBuffer.textureWidth, mainBuffer.textureHeight);
        int iterations = Math.max(1, Math.min(8, (int)Math.ceil(1.125)));
        int fillTexture = 0;
        if (hasFill) {
            int fillIterations = Math.max(2, Math.min(6, iterations + 1));
            fillTexture = this.runKawaseBloom(outlineBuffer.getColorAttachment(), fillIterations);
        }
        int blurredTexture = hasGlow ? this.runKawaseBloom(outlineBuffer.getColorAttachment(), iterations) : fillTexture;
        int color = this.getOutlineColor();
        mainBuffer.beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)false);
        if (hasFill && (fillShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderEspFill)) != null) {
            RenderSystem.blendFuncSeparate((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SrcFactor)GlStateManager.SrcFactor.ZERO, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE);
            RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shaderEspFill);
            RenderSystem.setShaderTexture((int)0, (int)outlineBuffer.getColorAttachment());
            RenderSystem.setShaderTexture((int)1, (int)(fillTexture == 0 ? blurredTexture : fillTexture));
            this.setUniform(fillShader, "color", ColorUtils.redf(color), ColorUtils.greenf(color), ColorUtils.bluef(color));
            this.setUniform(fillShader, "alpha", 0.7f);
            this.setUniform(fillShader, "time", (float)(System.currentTimeMillis() % 100000L) / 1000.0f);
            this.drawFullscreenQuad();
        }
        if (hasGlow && (glowShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderEspGlow)) != null) {
            RenderSystem.blendFuncSeparate((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SrcFactor)GlStateManager.SrcFactor.ZERO, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE);
            RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shaderEspGlow);
            RenderSystem.setShaderTexture((int)0, (int)blurredTexture);
            RenderSystem.setShaderTexture((int)1, (int)outlineBuffer.getColorAttachment());
            this.setUniform(glowShader, "color", ColorUtils.redf(color), ColorUtils.greenf(color), ColorUtils.bluef(color));
            this.setUniform(glowShader, "color2", ColorUtils.redf(color), ColorUtils.greenf(color), ColorUtils.bluef(color));
            this.setUniform(glowShader, "exposure", 0.05075f);
            this.setUniform(glowShader, "time", (float)(System.currentTimeMillis() % 100000L) / 1000.0f);
            this.setUniform(glowShader, "animate", 1.0f);
            this.drawFullscreenQuadWithDepthTest(mainBuffer, outlineBuffer);
        }
        RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.setShaderTexture((int)1, (int)0);
        mainBuffer.beginWrite(true);
    }

    private void drawFullscreenQuadWithDepthTest(Framebuffer mainBuffer, Framebuffer outlineBuffer) {
        if (this.depthCopyBuffer == null) {
            this.drawFullscreenQuad();
            return;
        }
        GL30.glBindFramebuffer((int)36008, (int)mainBuffer.fbo);
        GL30.glBindFramebuffer((int)36009, (int)this.depthCopyBuffer.fbo);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)mainBuffer.textureWidth, (int)mainBuffer.textureHeight, (int)0, (int)0, (int)this.depthCopyBuffer.textureWidth, (int)this.depthCopyBuffer.textureHeight, (int)256, (int)9728);
        GL30.glBindFramebuffer((int)36008, (int)outlineBuffer.fbo);
        GL30.glBindFramebuffer((int)36009, (int)mainBuffer.fbo);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)outlineBuffer.textureWidth, (int)outlineBuffer.textureHeight, (int)0, (int)0, (int)mainBuffer.textureWidth, (int)mainBuffer.textureHeight, (int)256, (int)9728);
        mainBuffer.beginWrite(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc((int)515);
        RenderSystem.depthMask((boolean)false);
        this.drawFullscreenQuad();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableDepthTest();
        GL30.glBindFramebuffer((int)36008, (int)this.depthCopyBuffer.fbo);
        GL30.glBindFramebuffer((int)36009, (int)mainBuffer.fbo);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)this.depthCopyBuffer.textureWidth, (int)this.depthCopyBuffer.textureHeight, (int)0, (int)0, (int)mainBuffer.textureWidth, (int)mainBuffer.textureHeight, (int)256, (int)9728);
        mainBuffer.beginWrite(false);
    }

    private void ensureDepthCopyBuffer(int width, int height) {
        if (this.depthCopyBuffer != null && (this.depthCopyBuffer.textureWidth != width || this.depthCopyBuffer.textureHeight != height)) {
            this.depthCopyBuffer.delete();
            this.depthCopyBuffer = null;
        }
        if (this.depthCopyBuffer == null) {
            this.depthCopyBuffer = new SimpleFramebuffer(width, height, true);
        }
    }

    private boolean tryEnsureOutlineProcessor() {
        if (ShaderEsp.mc.world == null || ShaderEsp.mc.worldRenderer == null) {
            this.outlineReady = false;
            return false;
        }
        Framebuffer outlines = this.getOutlineSourceFramebuffer();
        if (outlines != null && outlines.getColorAttachment() != 0) {
            this.outlineReady = true;
            return true;
        }
        if (this.outlineReady) {
            this.outlineReady = false;
        }
        if (System.currentTimeMillis() < this.nextOutlineRetryAt) {
            return false;
        }
        try {
            ShaderEsp.mc.worldRenderer.loadEntityOutlinePostProcessor();
            outlines = this.getOutlineSourceFramebuffer();
            boolean bl = this.outlineReady = outlines != null && outlines.getColorAttachment() != 0;
            if (!this.outlineReady) {
                this.nextOutlineRetryAt = System.currentTimeMillis() + 3000L;
            }
            return this.outlineReady;
        }
        catch (Throwable ignored) {
            this.outlineReady = false;
            this.nextOutlineRetryAt = System.currentTimeMillis() + 3000L;
            return false;
        }
    }

    private Framebuffer getOutlineSourceFramebuffer() {
        WorldRendererAccessor accessor;
        Framebuffer raw;
        WorldRenderer var_761_2 = ShaderEsp.mc.worldRenderer;
        if (var_761_2 instanceof WorldRendererAccessor && (raw = (accessor = (WorldRendererAccessor)var_761_2).polar$getEntityOutlineFramebufferRaw()) != null && raw.getColorAttachment() != 0) {
            return raw;
        }
        return ShaderEsp.mc.worldRenderer.getEntityOutlinesFramebuffer();
    }

    public boolean shouldOutline(Entity entity) {
        if (!this.isEnable() || entity == null || ShaderEsp.mc.player == null || ShaderEsp.mc.world == null) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        if (entity.isRemoved()) {
            return false;
        }
        if (entity == ShaderEsp.mc.player && !this.targets.is("Себя")) {
            return false;
        }
        if (entity.squaredDistanceTo((Entity)ShaderEsp.mc.player) > 65536.0) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            return this.targets.is("Игроки");
        }
        if (entity instanceof EndCrystalEntity) {
            return this.targets.is("Кристаллы");
        }
        if (entity instanceof ItemEntity) {
            return this.targets.is("Предметы");
        }
        return false;
    }

    private boolean hasOutlineTargets() {
        if (ShaderEsp.mc.world == null || ShaderEsp.mc.player == null) {
            return false;
        }
        for (Entity entity : ShaderEsp.mc.world.getEntities()) {
            if (!this.shouldOutline(entity)) continue;
            return true;
        }
        return false;
    }

    public int getOutlineColor() {
        return ColorUtils.setAlphaColor(ColorUtils.getThemeColor(), 255) & 0xFFFFFF;
    }

    private int runKawaseBloom(int sourceTexture, int iterations) {
        Framebuffer dst;
        int i2;
        this.ensureBloomBuffers(iterations);
        if (this.bloomBuffers.isEmpty()) {
            return sourceTexture;
        }
        int currentTexture = sourceTexture;
        ShaderProgram downShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsKawaseDown);
        ShaderProgram upShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsKawaseUp);
        if (downShader == null || upShader == null) {
            return currentTexture;
        }
        for (i2 = 0; i2 < iterations; ++i2) {
            dst = this.bloomBuffers.get(i2);
            dst.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            dst.clear();
            dst.beginWrite(true);
            RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shaderHandsKawaseDown);
            RenderSystem.setShaderTexture((int)0, (int)currentTexture);
            this.setHandsKawaseUniforms(downShader, dst.textureWidth, dst.textureHeight, 1.0f + (float)i2);
            this.drawFullscreenQuad();
            currentTexture = dst.getColorAttachment();
        }
        for (i2 = iterations - 1; i2 >= 1; --i2) {
            dst = this.bloomBuffers.get(i2 - 1);
            dst.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            dst.clear();
            dst.beginWrite(true);
            RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shaderHandsKawaseUp);
            RenderSystem.setShaderTexture((int)0, (int)currentTexture);
            this.setHandsKawaseUniforms(upShader, dst.textureWidth, dst.textureHeight, 1.0f + (float)i2);
            this.setUniform(upShader, "color", 1.0f, 1.0f, 1.0f);
            this.drawFullscreenQuad();
            currentTexture = dst.getColorAttachment();
        }
        mc.getFramebuffer().beginWrite(true);
        return currentTexture;
    }

    private void ensureBloomBuffers(int iterations) {
        int w2 = mc.getWindow().getFramebufferWidth();
        int h2 = mc.getWindow().getFramebufferHeight();
        if (this.bloomWidth != w2 || this.bloomHeight != h2) {
            for (Framebuffer fb : this.bloomBuffers) {
                fb.delete();
            }
            this.bloomBuffers.clear();
            this.bloomWidth = w2;
            this.bloomHeight = h2;
        }
        while (this.bloomBuffers.size() > iterations) {
            int last = this.bloomBuffers.size() - 1;
            this.bloomBuffers.get(last).delete();
            this.bloomBuffers.remove(last);
        }
        for (int i2 = 0; i2 < iterations; ++i2) {
            Framebuffer fb;
            int tw = Math.max(2, w2 >> i2 + 1);
            int th = Math.max(2, h2 >> i2 + 1);
            if (i2 >= this.bloomBuffers.size()) {
                fb = new SimpleFramebuffer(tw, th, false);
                this.setLinearFiltering(fb);
                this.bloomBuffers.add(fb);
                continue;
            }
            fb = this.bloomBuffers.get(i2);
            if (fb.textureWidth == tw && fb.textureHeight == th) continue;
            fb.delete();
            fb = new SimpleFramebuffer(tw, th, false);
            this.setLinearFiltering(fb);
            this.bloomBuffers.set(i2, fb);
        }
    }

    private void setLinearFiltering(Framebuffer fb) {
        RenderSystem.bindTexture((int)fb.getColorAttachment());
        GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        RenderSystem.bindTexture((int)0);
    }

    private void setUniform(ShaderProgram shader, String name, float value) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x2, float y2) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x2, y2);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x2, float y2, float z2) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x2, y2, z2);
        }
    }

    private void setHandsKawaseUniforms(ShaderProgram shader, int texWidth, int texHeight, float offset) {
        this.setUniform(shader, "uSize", Math.max(1, texWidth), Math.max(1, texHeight));
        this.setUniform(shader, "uOffset", offset, offset);
        this.setUniform(shader, "uHalfPixel", 0.5f / (float)Math.max(1, texWidth), 0.5f / (float)Math.max(1, texHeight));
    }

    private void drawFullscreenQuad() {
        float width = Math.max(mc.getWindow().getScaledWidth(), 1);
        float height = Math.max(mc.getWindow().getScaledHeight(), 1);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(0.0f, 0.0f, 0.0f).texture(0.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(0.0f, height, 0.0f).texture(0.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(width, height, 0.0f).texture(1.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(width, 0.0f, 0.0f).texture(1.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }
}

