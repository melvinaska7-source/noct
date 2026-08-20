package polar.ru.api.utils.render.hands;

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
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.ShaderUtils;
import polar.ru.client.modules.impl.render.ShaderHands;
import polar.ru.polar;

public class ShaderHandsRenderer
implements QClient {
    private static final float EPSILON = 0.001f;
    private static ShaderHandsRenderer instance;
    private Framebuffer beforeBuffer;
    private Framebuffer afterBuffer;
    private Framebuffer maskBuffer;
    private final List<Framebuffer> bloomBuffers = new ArrayList<Framebuffer>();
    private int width = -1;
    private int height = -1;
    private boolean hasBeforeCapture;
    private boolean pendingComposite;
    private int configuredBeforeDepthTex = -1;
    private int configuredAfterDepthTex = -1;

    public static ShaderHandsRenderer getInstance() {
        if (instance == null) {
            instance = new ShaderHandsRenderer();
        }
        return instance;
    }

    public void captureBeforeHands() {
        ShaderHands module = this.getModule();
        if (!this.isEffectEnabled(module)) {
            this.invalidateState();
            return;
        }
        this.ensureBuffers();
        if (this.beforeBuffer == null) {
            return;
        }
        this.copyMainFramebuffer(this.beforeBuffer);
        this.hasBeforeCapture = true;
    }

    public void captureAfterHands() {
        ShaderHands module = this.getModule();
        if (!this.isEffectEnabled(module)) {
            this.invalidateState();
            return;
        }
        this.ensureBuffers();
        if (this.beforeBuffer == null || this.afterBuffer == null || this.maskBuffer == null) {
            return;
        }
        if (!this.hasBeforeCapture) {
            return;
        }
        this.copyMainFramebuffer(this.afterBuffer);
        this.pendingComposite = true;
    }

    public void renderOverlayIfPending(float partialTicks) {
        ShaderProgram glowShader;
        int color1;
        if (!this.pendingComposite) {
            return;
        }
        this.ensureBuffers();
        if (this.beforeBuffer == null || this.afterBuffer == null || this.maskBuffer == null) {
            return;
        }
        ShaderHands module = this.getModule();
        if (!this.isEffectEnabled(module)) {
            this.invalidateState();
            return;
        }
        ShaderProgram maskShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsMaskDiff);
        if (maskShader == null) {
            this.invalidateState();
            return;
        }
        this.maskBuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        this.maskBuffer.clear();
        this.maskBuffer.beginWrite(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shaderHandsMaskDiff);
        RenderSystem.setShaderTexture((int)0, (int)this.beforeBuffer.getColorAttachment());
        RenderSystem.setShaderTexture((int)1, (int)this.afterBuffer.getColorAttachment());
        int beforeDepth = this.beforeBuffer.getDepthAttachment();
        int afterDepth = this.afterBuffer.getDepthAttachment();
        if (beforeDepth != 0 && beforeDepth != this.configuredBeforeDepthTex) {
            this.configureDepthTexture(beforeDepth);
            this.configuredBeforeDepthTex = beforeDepth;
        }
        if (afterDepth != 0 && afterDepth != this.configuredAfterDepthTex) {
            this.configureDepthTexture(afterDepth);
            this.configuredAfterDepthTex = afterDepth;
        }
        RenderSystem.setShaderTexture((int)2, (int)beforeDepth);
        RenderSystem.setShaderTexture((int)3, (int)afterDepth);
        this.drawFullscreenQuad();
        RenderSystem.enableDepthTest();
        float glowValue = module.glow.get();
        float fillValue = module.fill.get();
        float alphaValue = module.alpha.get();
        float outlineValue = module.outline.get();
        boolean hasGlow = glowValue > 0.001f;
        boolean hasFill = fillValue > 0.001f && alphaValue > 0.001f;
        int color2 = color1 = polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow") ? ColorUtils.getThemeColor(0) : polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        if (module.mode.is("Красивый")) {
            this.renderPrettyMode(module, color1, color2, glowValue, fillValue, alphaValue, outlineValue);
            this.invalidateState();
            return;
        }
        if (module.mode.is("Дым")) {
            this.renderSmokeMode(module, color1, color2, glowValue, fillValue, alphaValue, outlineValue);
            this.invalidateState();
            return;
        }
        int blurredMaskTexture = this.maskBuffer.getColorAttachment();
        if (hasGlow) {
            int iterations = Math.max(3, Math.min(8, 4 + Math.round(outlineValue * 0.7f)));
            blurredMaskTexture = this.runKawaseBloom(iterations);
        }
        mc.getFramebuffer().beginWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)false);
        RenderSystem.disableDepthTest();
        ShaderProgram var_5944_2 = glowShader = hasGlow ? mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsGlow) : null;
        if (glowShader != null) {
            RenderSystem.blendFuncSeparate((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE, (GlStateManager.SrcFactor)GlStateManager.SrcFactor.ZERO, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE);
            RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shaderHandsGlow);
            RenderSystem.setShaderTexture((int)0, (int)blurredMaskTexture);
            RenderSystem.setShaderTexture((int)1, (int)this.maskBuffer.getColorAttachment());
            this.setUniform(glowShader, "color", ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1));
            this.setUniform(glowShader, "color2", ColorUtils.redf(color2), ColorUtils.greenf(color2), ColorUtils.bluef(color2));
            this.setUniform(glowShader, "exposure", 1.0f + glowValue * 1.8f);
            this.drawFullscreenQuad();
        }
        if (hasFill) {
            ShaderProgram overlayShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsOverlay);
            if (overlayShader == null) {
                this.restoreCompositeState();
                this.invalidateState();
                return;
            }
            RenderSystem.blendFuncSeparate((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SrcFactor)GlStateManager.SrcFactor.ZERO, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE);
            RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shaderHandsOverlay);
            RenderSystem.setShaderTexture((int)0, (int)this.maskBuffer.getColorAttachment());
            this.setUniform(overlayShader, "color", ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1));
            this.setUniform(overlayShader, "fill", fillValue);
            this.setUniform(overlayShader, "alpha", alphaValue);
            this.drawFullscreenQuad();
        }
        this.restoreCompositeState();
        this.invalidateState();
    }

    public void invalidateState() {
        this.hasBeforeCapture = false;
        this.pendingComposite = false;
        this.configuredBeforeDepthTex = -1;
        this.configuredAfterDepthTex = -1;
    }

    private int runKawaseBloom(int iterations) {
        Framebuffer dst;
        int i2;
        this.ensureBloomBuffers(iterations);
        if (this.bloomBuffers.isEmpty()) {
            return this.maskBuffer.getColorAttachment();
        }
        int currentTexture = this.maskBuffer.getColorAttachment();
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

    private void copyMainFramebuffer(Framebuffer target) {
        int readFbo = GL11.glGetInteger((int)36010);
        int drawFbo = GL11.glGetInteger((int)36006);
        GL30.glBindFramebuffer((int)36008, (int)ShaderHandsRenderer.mc.getFramebuffer().fbo);
        GL30.glBindFramebuffer((int)36009, (int)target.fbo);
        GL30.glBlitFramebuffer((int)0, (int)0, (int)this.width, (int)this.height, (int)0, (int)0, (int)this.width, (int)this.height, (int)16640, (int)9728);
        GL30.glBindFramebuffer((int)36008, (int)readFbo);
        GL30.glBindFramebuffer((int)36009, (int)drawFbo);
        mc.getFramebuffer().beginWrite(true);
    }

    private void configureDepthTexture(int depthTex) {
        RenderSystem.bindTexture((int)depthTex);
        GL11.glTexParameteri((int)3553, (int)34892, (int)0);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
        RenderSystem.bindTexture((int)0);
    }

    private void ensureBuffers() {
        int w2 = mc.getWindow().getFramebufferWidth();
        int h2 = mc.getWindow().getFramebufferHeight();
        if (w2 == this.width && h2 == this.height && this.beforeBuffer != null && this.afterBuffer != null && this.maskBuffer != null) {
            return;
        }
        if (this.beforeBuffer != null) {
            this.beforeBuffer.delete();
        }
        if (this.afterBuffer != null) {
            this.afterBuffer.delete();
        }
        if (this.maskBuffer != null) {
            this.maskBuffer.delete();
        }
        for (Framebuffer fb : this.bloomBuffers) {
            fb.delete();
        }
        this.bloomBuffers.clear();
        this.beforeBuffer = new SimpleFramebuffer(w2, h2, true);
        this.afterBuffer = new SimpleFramebuffer(w2, h2, true);
        this.maskBuffer = new SimpleFramebuffer(w2, h2, true);
        this.width = w2;
        this.height = h2;
        this.configuredBeforeDepthTex = -1;
        this.configuredAfterDepthTex = -1;
    }

    private Framebuffer ensureBuffer(Framebuffer buf, boolean depth, int divisor) {
        int fw = mc.getWindow().getFramebufferWidth();
        int fh = mc.getWindow().getFramebufferHeight();
        int w2 = Math.max(2, fw / divisor);
        int h2 = Math.max(2, fh / divisor);
        if (buf == null) {
            buf = new SimpleFramebuffer(w2, h2, depth);
            this.setLinearFiltering(buf);
            return buf;
        }
        if (buf.textureWidth != w2 || buf.textureHeight != h2) {
            buf.delete();
            buf = new SimpleFramebuffer(w2, h2, depth);
            this.setLinearFiltering(buf);
        }
        return buf;
    }

    private void ensureBloomBuffers(int iterations) {
        while (this.bloomBuffers.size() > iterations) {
            int last = this.bloomBuffers.size() - 1;
            this.bloomBuffers.get(last).delete();
            this.bloomBuffers.remove(last);
        }
        for (int i2 = 0; i2 < iterations; ++i2) {
            Framebuffer fb;
            int w2 = Math.max(2, this.width >> i2 + 1);
            int h2 = Math.max(2, this.height >> i2 + 1);
            if (i2 >= this.bloomBuffers.size()) {
                fb = new SimpleFramebuffer(w2, h2, false);
                this.setLinearFiltering(fb);
                this.bloomBuffers.add(fb);
                continue;
            }
            fb = this.bloomBuffers.get(i2);
            if (fb.textureWidth == w2 && fb.textureHeight == h2) continue;
            fb.delete();
            fb = new SimpleFramebuffer(w2, h2, false);
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

    private ShaderHands getModule() {
        if (polar.INSTANCE == null || ModuleClass.INSTANCE == null) {
            return null;
        }
        return ModuleClass.shaderHands;
    }

    private void renderPrettyMode(ShaderHands module, int color1, int color2, float glowValue, float fillValue, float alphaValue, float outlineValue) {
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.blockOverlay);
        if (shader == null) {
            return;
        }
        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.blockOverlay);
        RenderSystem.setShaderTexture((int)0, (int)this.maskBuffer.getColorAttachment());
        this.setUniform(shader, "texelSize", 1.0f / (float)Math.max(1, mc.getWindow().getFramebufferWidth()), 1.0f / (float)Math.max(1, mc.getWindow().getFramebufferHeight()));
        this.setUniform(shader, "color", ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1));
        this.setUniform(shader, "color2", ColorUtils.redf(color2), ColorUtils.greenf(color2), ColorUtils.bluef(color2));
        this.setUniform(shader, "time", (float)(System.currentTimeMillis() % 100000L) / 1000.0f);
        this.setUniform(shader, "speed", module.waveSpeed.get());
        this.setUniform(shader, "scale", module.waveScale.get());
        this.setUniform(shader, "outline", outlineValue);
        this.setUniform(shader, "glow", glowValue);
        this.setUniform(shader, "fill", fillValue);
        this.setUniform(shader, "alpha", alphaValue);
        this.setUniform(shader, "outlineOnly", 0.0f);
        this.drawFullscreenQuad();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        this.restoreCompositeState();
    }

    private void renderSmokeMode(ShaderHands module, int color1, int color2, float glowValue, float fillValue, float alphaValue, float outlineValue) {
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsSmoke);
        if (shader == null) {
            return;
        }
        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SrcFactor)GlStateManager.SrcFactor.ONE, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shaderHandsSmoke);
        RenderSystem.setShaderTexture((int)0, (int)this.maskBuffer.getColorAttachment());
        this.setUniform(shader, "texelSize", 1.0f / (float)Math.max(1, mc.getWindow().getFramebufferWidth()), 1.0f / (float)Math.max(1, mc.getWindow().getFramebufferHeight()));
        this.setUniform(shader, "color", ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1));
        this.setUniform(shader, "color2", ColorUtils.redf(color2), ColorUtils.greenf(color2), ColorUtils.bluef(color2));
        this.setUniform(shader, "time", (float)(System.currentTimeMillis() % 100000L) / 1000.0f);
        this.setUniform(shader, "speed", module.smokeSpeed.get());
        this.setUniform(shader, "scale", module.smokeScale.get());
        this.setUniform(shader, "density", module.smokeDensity.get());
        this.setUniform(shader, "outline", outlineValue);
        this.setUniform(shader, "glow", glowValue);
        this.setUniform(shader, "fill", fillValue);
        this.setUniform(shader, "alpha", alphaValue);
        this.drawFullscreenQuad();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        this.restoreCompositeState();
    }

    private void restoreCompositeState() {
        RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.setShaderTexture((int)1, (int)0);
        RenderSystem.setShaderTexture((int)2, (int)0);
        RenderSystem.setShaderTexture((int)3, (int)0);
        mc.getFramebuffer().beginWrite(true);
    }

    private boolean isEffectEnabled(ShaderHands module) {
        if (module == null || !module.isEnable()) {
            return false;
        }
        boolean hasGlow = module.glow.get() > 0.001f;
        boolean hasFill = module.fill.get() > 0.001f && module.alpha.get() > 0.001f;
        return hasGlow || hasFill;
    }

    private void setUniform(ShaderProgram shader, String name, float v2) {
        GlUniform u2 = shader.getUniform(name);
        if (u2 != null) {
            u2.set(v2);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x2, float y2) {
        GlUniform u2 = shader.getUniform(name);
        if (u2 != null) {
            u2.set(x2, y2);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x2, float y2, float z2) {
        GlUniform u2 = shader.getUniform(name);
        if (u2 != null) {
            u2.set(x2, y2, z2);
        }
    }

    private void u1i(int program, String name, int value) {
        int location = GL20.glGetUniformLocation((int)program, (CharSequence)name);
        if (location >= 0) {
            GL20.glUniform1i((int)location, (int)value);
        }
    }

    private void u1f(int program, String name, float value) {
        int location = GL20.glGetUniformLocation((int)program, (CharSequence)name);
        if (location >= 0) {
            GL20.glUniform1f((int)location, (float)value);
        }
    }

    private void u2f(int program, String name, float x2, float y2) {
        int location = GL20.glGetUniformLocation((int)program, (CharSequence)name);
        if (location >= 0) {
            GL20.glUniform2f((int)location, (float)x2, (float)y2);
        }
    }

    private void u3f(int program, String name, float x2, float y2, float z2) {
        int location = GL20.glGetUniformLocation((int)program, (CharSequence)name);
        if (location >= 0) {
            GL20.glUniform3f((int)location, (float)x2, (float)y2, (float)z2);
        }
    }

    private void setHandsKawaseUniforms(ShaderProgram shader, int texWidth, int texHeight, float offset) {
        this.setUniform(shader, "uSize", Math.max(1, texWidth), Math.max(1, texHeight));
        this.setUniform(shader, "uOffset", offset, offset);
        this.setUniform(shader, "uHalfPixel", 0.5f / (float)Math.max(1, texWidth), 0.5f / (float)Math.max(1, texHeight));
    }

    private int createProgram(String vs, String fs) {
        int v2 = GL20.glCreateShader((int)35633);
        int f2 = GL20.glCreateShader((int)35632);
        int p2 = GL20.glCreateProgram();
        GL20.glShaderSource((int)v2, (CharSequence)vs);
        GL20.glCompileShader((int)v2);
        if (GL20.glGetShaderi((int)v2, (int)35713) == 0) {
            System.err.println("[Hands] VS error: " + GL20.glGetShaderInfoLog((int)v2, (int)1024));
        }
        GL20.glShaderSource((int)f2, (CharSequence)fs);
        GL20.glCompileShader((int)f2);
        if (GL20.glGetShaderi((int)f2, (int)35713) == 0) {
            System.err.println("[Hands] FS error: " + GL20.glGetShaderInfoLog((int)f2, (int)1024));
        }
        GL20.glAttachShader((int)p2, (int)v2);
        GL20.glAttachShader((int)p2, (int)f2);
        GL20.glLinkProgram((int)p2);
        return p2;
    }

    private void bindTex(int unit, int tex) {
        GL13.glActiveTexture((int)unit);
        RenderSystem.bindTexture((int)tex);
    }

    private void drawFullscreenQuad() {
        float sw = Math.max(mc.getWindow().getScaledWidth(), 1);
        float sh = Math.max(mc.getWindow().getScaledHeight(), 1);
        BufferBuilder b2 = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b2.vertex(0.0f, 0.0f, 0.0f).texture(0.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        b2.vertex(0.0f, sh, 0.0f).texture(0.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        b2.vertex(sw, sh, 0.0f).texture(1.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        b2.vertex(sw, 0.0f, 0.0f).texture(1.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)b2.end());
    }
}

