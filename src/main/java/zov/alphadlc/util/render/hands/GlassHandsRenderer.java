package zov.alphadlc.util.render.hands;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import zov.alphadlc.module.list.render.GlassHands;

import java.nio.ByteBuffer;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.providers.ResourceProvider;

import java.util.ArrayList;
import java.util.List;


public class GlassHandsRenderer {

    private static final float EPSILON = 0.001f;
    private static GlassHandsRenderer instance;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final ShaderProgramKey KEY_MASK_DIFF    = key("glass_hands_mask_diff");
    private static final ShaderProgramKey KEY_GLOW         = key("glass_hands_glow");
    private static final ShaderProgramKey KEY_OVERLAY      = key("glass_hands_overlay");
    private static final ShaderProgramKey KEY_KAWASE_DOWN  = key("glass_hands_kawase_down");
    private static final ShaderProgramKey KEY_KAWASE_UP    = key("glass_hands_kawase_up");
    private static final ShaderProgramKey KEY_PRETTY       = key("glass_hands_pretty");
    private static final ShaderProgramKey KEY_BLUR_HANDS   = key("glass_hands_blur");
    private static final ShaderProgramKey KEY_TRAIL        = key("glass_hands_trail");
    private static final ShaderProgramKey KEY_TRAIL_MOTION = key("glass_hands_trail_motion");
    private static final ShaderProgramKey KEY_TRAIL_ACCUMULATE = key("glass_hands_trail_accumulate");
    private static final ShaderProgramKey KEY_TRAIL_COMPOSITE = key("glass_hands_trail_composite");
    private static final ShaderProgramKey KEY_GLASS = key("glass_hands_glass");
    private static final ShaderProgramKey KEY_GLASS_GLOW = key("glass_hands_glass_glow");
    private static final ShaderProgramKey KEY_PLASMA = key("glass_hands_plasma");
    private static final ShaderProgramKey KEY_FILL = key("glass_hands_fill");
    private static final ShaderProgramKey KEY_FILL_GLOW = key("glass_hands_fill_glow");
    private static final ShaderProgramKey KEY_FILL_OUTLINE = key("glass_hands_fill_outline");
    private static final ShaderProgramKey KEY_FILL_TRAIL_FADE = key("glass_hands_fill_trail_fade");
    private static final ShaderProgramKey KEY_FILL_TRAIL_COLOR = key("glass_hands_fill_trail_color");
    private static final ShaderProgramKey KEY_WAVE = key("glass_hands_wave");

    private static ShaderProgramKey key(String name) {
        return new ShaderProgramKey(ResourceProvider.getShaderIdentifier(name),
                VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
    }

    private Framebuffer beforeBuffer;
    private Framebuffer afterBuffer;
    private Framebuffer maskBuffer;
    private Framebuffer trailBuffer;
    private Framebuffer fillTrailRead;
    private Framebuffer fillTrailWrite;
    private Framebuffer colorMask;
    private final List<Framebuffer> bloomBuffers = new ArrayList<>();
    private final List<Framebuffer> glassBloomBuffers = new ArrayList<>();
    private final List<Framebuffer> fillBloomBuffers = new ArrayList<>();

    private int width = -1;
    private int height = -1;
    private boolean hasBeforeCapture = false;
    private boolean pendingComposite = false;
    private int configuredBeforeDepthTex = -1;
    private int configuredAfterDepthTex = -1;

    private float prevYaw = 0f;
    private float prevPitch = 0f;
    private float smoothDeltaYaw = 0f;
    private float smoothDeltaPitch = 0f;
    private float smoothTime = 0f;
    private float slashPulse = 0f;
    private float slashDirection = 1f;

    private long fillLastTrailTime = 0L;
    private float fillSmoothDt = 1f / 60f;
    private float fillSmoothTrailRise;
    private float fillSmoothTrailSway;
    private float fillSmoothBurst;
    private long fillLastSwingMs = -10000L;
    private boolean fillWasSwinging = false;
    private int fillLastBloomTex = -1;

    // Wave trail state
    private Framebuffer waveColorBuf;
    private Framebuffer waveTrailRead;
    private Framebuffer waveTrailWrite;
    private long waveLastTrailTime = 0L;
    private float waveSmoothDt = 1f / 60f;
    private float waveSmoothTrailRise;
    private float waveSmoothTrailSway;
    private float waveSmoothBurst;
    private long waveLastSwingMs = -10000L;
    private boolean waveWasSwinging = false;

    private String previousMode = "";

    public static GlassHandsRenderer getInstance() {
        if (instance == null) instance = new GlassHandsRenderer();
        return instance;
    }

    public void captureBeforeHands(GlassHands module) {
        if (!isEffectEnabled(module)) { invalidateState(); return; }
        ensureBuffers();
        if (beforeBuffer == null) return;

        // Сохраняем текущий framebuffer для проверки
        int currentDrawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        copyMainFramebuffer(beforeBuffer);
        hasBeforeCapture = true;
    }

    public void captureAfterHands(GlassHands module) {
        if (!isEffectEnabled(module)) { invalidateState(); return; }
        ensureBuffers();
        if (beforeBuffer == null || afterBuffer == null || maskBuffer == null) return;
        if (!hasBeforeCapture) return;
        copyMainFramebuffer(afterBuffer);
        pendingComposite = true;
    }

    public void renderOverlayIfPending(GlassHands module) {
        if (!pendingComposite) return;
        ensureBuffers();
        if (beforeBuffer == null || afterBuffer == null || maskBuffer == null) return;
        if (!isEffectEnabled(module)) { invalidateState(); return; }

        ShaderProgram maskShader = mc.getShaderLoader().getOrCreateProgram(KEY_MASK_DIFF);
        if (maskShader == null) { invalidateState(); return; }

        maskBuffer.setClearColor(0f, 0f, 0f, 0f);
        maskBuffer.clear();
        maskBuffer.beginWrite(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(KEY_MASK_DIFF);
        RenderSystem.setShaderTexture(0, beforeBuffer.getColorAttachment());
        RenderSystem.setShaderTexture(1, afterBuffer.getColorAttachment());

        int beforeDepth = beforeBuffer.getDepthAttachment();
        int afterDepth  = afterBuffer.getDepthAttachment();
        if (beforeDepth != 0 && beforeDepth != configuredBeforeDepthTex) {
            configureDepthTexture(beforeDepth);
            configuredBeforeDepthTex = beforeDepth;
        }
        if (afterDepth != 0 && afterDepth != configuredAfterDepthTex) {
            configureDepthTexture(afterDepth);
            configuredAfterDepthTex = afterDepth;
        }
        RenderSystem.setShaderTexture(2, beforeDepth);
        RenderSystem.setShaderTexture(3, afterDepth);
        drawFullscreenQuad();
        RenderSystem.enableDepthTest();

        float glowValue    = module.glow.getFloatValue();
        float fillValue    = module.fill.getFloatValue();
        float alphaValue   = module.alpha.getFloatValue();
        float outlineValue = module.outline.getFloatValue();

        int color1 = ColorProvider.getThemeColor();
        int color2 = ColorProvider.getThemeColorTwo();

        String currentMode = module.mode.getValue();
        if (!currentMode.equals(previousMode)) {
            // Сбрасываем trail-буферы чтобы не было мусора от предыдущего режима
            clearTrailBuffer(waveTrailRead);
            clearTrailBuffer(waveTrailWrite);
            clearTrailBuffer(fillTrailRead);
            clearTrailBuffer(fillTrailWrite);
            waveLastTrailTime = 0L;
            fillLastTrailTime = 0L;
            previousMode = currentMode;
        }

        if (module.mode.is("Блюр")) {
            renderBlurMode(module);
            invalidateState();
            return;
        }

        if (module.mode.is("Красивый")) {
            renderPrettyMode(module, color1, color2, glowValue, fillValue, alphaValue, outlineValue);
            invalidateState();
            return;
        }

        if (module.mode.is("Обводка")) {
            renderOutlineMode(module, color1, color2);
            invalidateState();
            return;
        }

        if (module.mode.is("Шлейф")) {
            renderTrailMode(module, color1, color2);
            invalidateState();
            return;
        }

        if (module.mode.is("Trail")) {
            renderTrailMotionMode(module, color1, color2);
            invalidateState();
            return;
        }

        if (module.mode.is("Стекло")) {
            renderGlassMode(module, color1, color2);
            invalidateState();
            return;
        }

        if (module.mode.is("Plasma")) {
            renderPlasmaMode(module, color1, color2);
            invalidateState();
            return;
        }

        if (module.mode.is("Заливка")) {
            renderFillMode(module, color1, color2);
            invalidateState();
            return;
        }

        if (module.mode.is("Волна")) {
            renderWaveMode(module, color1, color2);
            invalidateState();
            return;
        }


        boolean hasGlow = glowValue > EPSILON;
        boolean hasFill = fillValue > EPSILON && alphaValue > EPSILON;

        int blurredMaskTex = 0;
        if (hasGlow) {
            int iterations = Math.max(3, Math.min(8, 4 + Math.round(outlineValue * 0.7f)));
            blurredMaskTex = runKawaseBloom(iterations);
        }

        mc.getFramebuffer().beginWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.colorMask(true, true, true, false);
        RenderSystem.disableDepthTest();

        if (hasGlow) {
            ShaderProgram glowShader = mc.getShaderLoader().getOrCreateProgram(KEY_GLOW);
            if (glowShader != null) {
                RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE,
                        GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
                RenderSystem.setShader(KEY_GLOW);
                RenderSystem.setShaderTexture(0, blurredMaskTex);
                RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());


                int effectiveColor1 = color1;
                int effectiveColor2 = color2;
                if (module.mode.is("Свечение") && module.glowItemColor.getValue()) {
                    int itemColor = getHeldItemColor();
                    effectiveColor1 = itemColor;
                    effectiveColor2 = itemColor;
                }

                setUniform(glowShader, "color",    rf(effectiveColor1), gf(effectiveColor1), bf(effectiveColor1));
                setUniform(glowShader, "color2",   rf(effectiveColor2), gf(effectiveColor2), bf(effectiveColor2));
                setUniform(glowShader, "exposure", 1.0f + glowValue * 1.8f);
                drawFullscreenQuad();
            }
        }

        if (hasFill) {
            ShaderProgram overlayShader = mc.getShaderLoader().getOrCreateProgram(KEY_OVERLAY);
            if (overlayShader != null) {
                RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
                RenderSystem.setShader(KEY_OVERLAY);
                RenderSystem.setShaderTexture(0, maskBuffer.getColorAttachment());


                int effectiveColor = color1;
                if (module.mode.is("Свечение") && module.glowItemColor.getValue()) {
                    effectiveColor = getHeldItemColor();
                }

                setUniform(overlayShader, "color", rf(effectiveColor), gf(effectiveColor), bf(effectiveColor));
                setUniform(overlayShader, "fill",  fillValue);
                setUniform(overlayShader, "alpha", alphaValue);
                drawFullscreenQuad();
            }
        }

        restoreCompositeState();
        invalidateState();
    }

    public void invalidateState() {
        hasBeforeCapture = false;
        pendingComposite = false;
        configuredBeforeDepthTex = -1;
        configuredAfterDepthTex = -1;
        fillLastBloomTex = -1;
    }

    private void renderBlurMode(GlassHands module) {
        int iterations = Math.max(1, Math.min(8, (int) module.blurStrength.getFloatValue()));
        int blurredBgTex = runKawaseBloomFrom(beforeBuffer.getColorAttachment(), iterations);

        int color1 = module.blurRainbow.getValue()
                ? rainbowColors(module.blurRainbowSpeed.getFloatValue())[0]
                : ColorProvider.getThemeColor();
        float tint = module.blurTint.getFloatValue();

        mc.getFramebuffer().beginWrite(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        ShaderProgram sh = mc.getShaderLoader().getOrCreateProgram(KEY_BLUR_HANDS);
        if (sh != null) {

            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
            RenderSystem.setShader(KEY_BLUR_HANDS);
            RenderSystem.setShaderTexture(0, beforeBuffer.getColorAttachment());
            RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());
            setUniform(sh, "tintColor", rf(color1), gf(color1), bf(color1));
            setUniform(sh, "tintStrength", 0.0f);
            drawFullscreenQuad();


            RenderSystem.setShader(KEY_BLUR_HANDS);
            RenderSystem.setShaderTexture(0, blurredBgTex);
            RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());
            setUniform(sh, "tintColor", rf(color1), gf(color1), bf(color1));
            setUniform(sh, "tintStrength", tint);
            drawFullscreenQuad();
        }

        restoreCompositeState();
    }

    private void renderOutlineMode(GlassHands module, int color1, int color2) {
        mc.getFramebuffer().beginWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.colorMask(true, true, true, false);
        RenderSystem.disableDepthTest();

        ShaderProgram glowShader = mc.getShaderLoader().getOrCreateProgram(KEY_GLOW);
        if (glowShader != null) {
            RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE,
                    GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
            RenderSystem.setShader(KEY_GLOW);
            RenderSystem.setShaderTexture(0, maskBuffer.getColorAttachment());
            RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());
            setUniform(glowShader, "color",    rf(color1), gf(color1), bf(color1));
            setUniform(glowShader, "color2",   rf(color2), gf(color2), bf(color2));
            setUniform(glowShader, "exposure", 1.0f);
            drawFullscreenQuad();
        }

        restoreCompositeState();
    }

    private void renderTrailMode(GlassHands module, int color1, int color2) {

        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(KEY_TRAIL);
        if (shader == null) return;


        int iterations = Math.max(3, Math.min(10, (int) module.trailBlur.getFloatValue()));
        int blurredMaskTex = runKawaseBloom(iterations);


        mc.getFramebuffer().beginWrite(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE,
                GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);

        RenderSystem.setShader(KEY_TRAIL);
        RenderSystem.setShaderTexture(0, maskBuffer.getColorAttachment());
        RenderSystem.setShaderTexture(1, blurredMaskTex);

        int fw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fh = Math.max(1, mc.getWindow().getFramebufferHeight());

        setUniform(shader, "texelSize", 1.0f / fw, 1.0f / fh);
        setUniform(shader, "color", rf(color1), gf(color1), bf(color1));
        setUniform(shader, "color2", rf(color2), gf(color2), bf(color2));
        setUniform(shader, "trailIntensity", module.trailIntensity.getFloatValue());
        setUniform(shader, "glowSize", module.trailGlowSize.getFloatValue());
        setUniform(shader, "time", System.currentTimeMillis() % 100000L / 1000.0f);
        setUniform(shader, "rainbowEnabled", module.rainbow.getValue() ? 1.0f : 0.0f);
        setUniform(shader, "rainbowSpeed", module.rainbowSpeed.getFloatValue());

        drawFullscreenQuad();

        restoreCompositeState();
    }

    public void pulseAttack(float direction) {
        slashPulse = 1.0f;
        slashDirection = direction >= 0 ? 1f : -1f;
    }

    private int getHeldItemColor() {
        if (mc.player == null) return ColorProvider.getThemeColor();

        var stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) stack = mc.player.getOffHandStack();
        if (stack.isEmpty()) return ColorProvider.getThemeColor();

        var item = stack.getItem();
        String path = item.toString().toLowerCase();


        if (path.contains("diamond")) return 0x55DDE0;
        if (path.contains("netherite")) return 0x5B4A67;
        if (path.contains("golden") || path.contains("gold")) return 0xFFD45A;
        if (path.contains("iron")) return 0xD8DEE8;
        if (path.contains("stone")) return 0x8A8A8A;
        if (path.contains("wood")) return 0xB98245;
        if (path.contains("emerald")) return 0x35D06F;
        if (path.contains("redstone")) return 0xE23B3B;
        if (path.contains("lapis")) return 0x3156D4;
        if (path.contains("copper")) return 0xD17A45;
        if (path.contains("amethyst")) return 0xB06CFF;
        if (path.contains("ender") || path.contains("prismarine")) return 0x45E0C0;
        if (path.contains("blaze")) return 0xFF8A2A;

        return ColorProvider.getThemeColor();
    }

    private void renderTrailMotionMode(GlassHands module, int color1, int color2) {
        ShaderProgram accumShader = mc.getShaderLoader().getOrCreateProgram(KEY_TRAIL_ACCUMULATE);
        if (accumShader == null || trailBuffer == null) return;

        smoothTime += 0.016f;


        float currentYaw   = mc.player != null ? mc.player.getYaw()   : 0f;
        float currentPitch = mc.player != null ? mc.player.getPitch() : 0f;
        float deltaYaw   = currentYaw   - prevYaw;
        float deltaPitch = currentPitch - prevPitch;
        prevYaw   = currentYaw;
        prevPitch = currentPitch;
        while (deltaYaw >  180f) deltaYaw -= 360f;
        while (deltaYaw < -180f) deltaYaw += 360f;
        smoothDeltaYaw   = smoothDeltaYaw   * 0.85f + deltaYaw   * 0.15f;
        smoothDeltaPitch = smoothDeltaPitch * 0.85f + deltaPitch * 0.15f;


        slashPulse = Math.max(0f, slashPulse - 0.045f);

        int fw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fh = Math.max(1, mc.getWindow().getFramebufferHeight());

        float cameraInfluence = module.trailCamera.getFloatValue();
        float camX = smoothDeltaYaw   * -0.00035f * cameraInfluence;
        float camY = smoothDeltaPitch *  0.00035f * cameraInfluence;


        Framebuffer tempBuffer = new SimpleFramebuffer(fw, fh, false);
        tempBuffer.setClearColor(0f, 0f, 0f, 0f);
        tempBuffer.clear();
        tempBuffer.beginWrite(false);

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(KEY_TRAIL_ACCUMULATE);
        RenderSystem.setShaderTexture(0, trailBuffer.getColorAttachment());
        RenderSystem.setShaderTexture(1, afterBuffer.getColorAttachment());
        RenderSystem.setShaderTexture(2, maskBuffer.getColorAttachment());


        setUniform(accumShader, "resolution", (float) fw, (float) fh,
                smoothTime, module.trailIntensityM.getFloatValue());

        setUniform(accumShader, "glowColor",
                rf(color1), gf(color1), bf(color1), 1.0f);

        setUniform(accumShader, "settings",
                module.trailItemColor.getValue() ? 1f : 0f,
                module.trailSpeed.getFloatValue(),
                module.trailLength.getFloatValue(),
                module.trailSoftness.getFloatValue());

        setUniform(accumShader, "settings2",
                module.trailBlurRadius.getFloatValue(),
                module.trailSmoke.getFloatValue(),
                module.trailAttack.getFloatValue(),
                0f);

        setUniform(accumShader, "reserved",
                slashPulse, slashDirection, camX, camY);

        drawFullscreenQuad();


        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, tempBuffer.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, trailBuffer.fbo);
        GL30.glBlitFramebuffer(0, 0, fw, fh, 0, 0, fw, fh, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        tempBuffer.delete();


        mc.getFramebuffer().beginWrite(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ZERO,      GlStateManager.DstFactor.ONE);

        RenderSystem.setShaderTexture(0, trailBuffer.getColorAttachment());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);

        drawFullscreenQuad();

        restoreCompositeState();
    }

    private int runKawaseBloomFrom(int sourceTex, int iterations) {
        ensureBloomBuffers(iterations);
        if (bloomBuffers.isEmpty()) return sourceTex;

        ShaderProgram downShader = mc.getShaderLoader().getOrCreateProgram(KEY_KAWASE_DOWN);
        ShaderProgram upShader   = mc.getShaderLoader().getOrCreateProgram(KEY_KAWASE_UP);
        if (downShader == null || upShader == null) return sourceTex;

        int currentTex = sourceTex;

        for (int i = 0; i < iterations; i++) {
            Framebuffer dst = bloomBuffers.get(i);
            dst.setClearColor(0f, 0f, 0f, 0f);
            dst.clear();
            dst.beginWrite(true);
            RenderSystem.setShader(KEY_KAWASE_DOWN);
            RenderSystem.setShaderTexture(0, currentTex);
            setKawaseUniforms(downShader, dst.textureWidth, dst.textureHeight, 1.0f + i);
            drawFullscreenQuad();
            currentTex = dst.getColorAttachment();
        }

        for (int i = iterations - 1; i >= 1; i--) {
            Framebuffer dst = bloomBuffers.get(i - 1);
            dst.setClearColor(0f, 0f, 0f, 0f);
            dst.clear();
            dst.beginWrite(true);
            RenderSystem.setShader(KEY_KAWASE_UP);
            RenderSystem.setShaderTexture(0, currentTex);
            setKawaseUniforms(upShader, dst.textureWidth, dst.textureHeight, 1.0f + i);
            setUniform(upShader, "color", 1f, 1f, 1f);
            drawFullscreenQuad();
            currentTex = dst.getColorAttachment();
        }

        mc.getFramebuffer().beginWrite(true);
        return currentTex;
    }

    private void renderPrettyMode(GlassHands module, int color1, int color2,
                                  float glowValue, float fillValue, float alphaValue, float outlineValue) {
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(KEY_PRETTY);
        if (shader == null) return;

        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(KEY_PRETTY);
        RenderSystem.setShaderTexture(0, maskBuffer.getColorAttachment());

        int fw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fh = Math.max(1, mc.getWindow().getFramebufferHeight());
        setUniform(shader, "texelSize", 1.0f / fw, 1.0f / fh);
        setUniform(shader, "color",  rf(color1), gf(color1), bf(color1));
        setUniform(shader, "color2", rf(color2), gf(color2), bf(color2));
        setUniform(shader, "time",   System.currentTimeMillis() % 100000L / 1000.0f);
        setUniform(shader, "speed",  module.waveSpeed.getFloatValue());
        setUniform(shader, "scale",  module.waveScale.getFloatValue());
        setUniform(shader, "outline", outlineValue);
        setUniform(shader, "glow",   glowValue);
        setUniform(shader, "fill",   fillValue);
        setUniform(shader, "alpha",  alphaValue);
        drawFullscreenQuad();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        restoreCompositeState();
    }

    private int runKawaseBloom(int iterations) {
        return runKawaseBloomFrom(maskBuffer.getColorAttachment(), iterations);
    }

    private void copyMainFramebuffer(Framebuffer target) {
        // Сохраняем текущий framebuffer binding
        int currentFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        // Используем текущий active framebuffer - это важно для шейдеров
        // С Iris/Oculus руки могут рендериться в промежуточный буфер
        int sourceFbo = (currentFbo != 0) ? currentFbo : mc.getFramebuffer().fbo;

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.fbo);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);

        // Восстанавливаем original binding
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, currentFbo);
    }

    private void configureDepthTexture(int depthTex) {
        RenderSystem.bindTexture(depthTex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL30.GL_TEXTURE_COMPARE_MODE, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        RenderSystem.bindTexture(0);
    }

    private void ensureBuffers() {
        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (w == width && h == height && beforeBuffer != null && afterBuffer != null && maskBuffer != null && trailBuffer != null) return;
        deleteBuffers();
        beforeBuffer = new SimpleFramebuffer(w, h, true);
        afterBuffer  = new SimpleFramebuffer(w, h, true);
        maskBuffer   = new SimpleFramebuffer(w, h, true);
        trailBuffer  = new SimpleFramebuffer(w, h, false);
        width  = w;
        height = h;
        configuredBeforeDepthTex = -1;
        configuredAfterDepthTex  = -1;
    }

    private void deleteBuffers() {
        if (beforeBuffer != null) { beforeBuffer.delete(); beforeBuffer = null; }
        if (afterBuffer  != null) { afterBuffer.delete();  afterBuffer  = null; }
        if (maskBuffer   != null) { maskBuffer.delete();   maskBuffer   = null; }
        if (trailBuffer  != null) { trailBuffer.delete();  trailBuffer  = null; }
        if (fillTrailRead != null) { fillTrailRead.delete(); fillTrailRead = null; }
        if (fillTrailWrite != null) { fillTrailWrite.delete(); fillTrailWrite = null; }
        if (colorMask != null) { colorMask.delete(); colorMask = null; }
        if (waveColorBuf  != null) { waveColorBuf.delete();  waveColorBuf  = null; }
        if (waveTrailRead  != null) { waveTrailRead.delete();  waveTrailRead  = null; }
        if (waveTrailWrite != null) { waveTrailWrite.delete(); waveTrailWrite = null; }
        for (Framebuffer fb : bloomBuffers) fb.delete();
        bloomBuffers.clear();
        for (Framebuffer fb : fillBloomBuffers) fb.delete();
        fillBloomBuffers.clear();
    }

    private void ensureBloomBuffers(int iterations) {
        while (bloomBuffers.size() > iterations) {
            int last = bloomBuffers.size() - 1;
            bloomBuffers.get(last).delete();
            bloomBuffers.remove(last);
        }
        for (int i = 0; i < iterations; i++) {
            int w = Math.max(2, width  >> (i + 1));
            int h = Math.max(2, height >> (i + 1));
            if (i >= bloomBuffers.size()) {
                Framebuffer fb = new SimpleFramebuffer(w, h, false);
                setLinearFiltering(fb);
                bloomBuffers.add(fb);
            } else {
                Framebuffer fb = bloomBuffers.get(i);
                if (fb.textureWidth != w || fb.textureHeight != h) {
                    fb.delete();
                    fb = new SimpleFramebuffer(w, h, false);
                    setLinearFiltering(fb);
                    bloomBuffers.set(i, fb);
                }
            }
        }
    }

    private void setLinearFiltering(Framebuffer fb) {
        RenderSystem.bindTexture(fb.getColorAttachment());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.bindTexture(0);
    }

    private void clearTrailBuffer(Framebuffer buf) {
        if (buf == null) return;
        buf.setClearColor(0f, 0f, 0f, 0f);
        buf.clear();
    }

    private void restoreCompositeState() {
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.setShaderTexture(1, 0);
        RenderSystem.setShaderTexture(2, 0);
        RenderSystem.setShaderTexture(3, 0);
        mc.getFramebuffer().beginWrite(true);
    }

    private void renderGlassMode(GlassHands module, int color1, int color2) {
        int iterations = 3;
        int blurredBgTex = runKawaseBloomFrom(beforeBuffer.getColorAttachment(), iterations);

        ShaderProgram glassShader = mc.getShaderLoader().getOrCreateProgram(KEY_GLASS);
        if (glassShader == null) return;

        mc.getFramebuffer().beginWrite(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);

        RenderSystem.setShader(KEY_GLASS);
        RenderSystem.setShaderTexture(0, blurredBgTex);
        RenderSystem.setShaderTexture(1, afterBuffer.getColorAttachment());
        RenderSystem.setShaderTexture(2, maskBuffer.getColorAttachment());
        setUniform(glassShader, "mixFactor", module.glassMixFactor.getFloatValue());

        drawFullscreenQuad();

        RenderSystem.defaultBlendFunc();

        if (module.glassGlowEnabled.getValue() && module.glassOuterGlow.getValue()) {
            renderGlassOuterGlow(module, color1, color2);
        }

        restoreCompositeState();
    }

    private void renderGlassOuterGlow(GlassHands module, int color1, int color2) {
        int iterations = Math.max(1, Math.min(6, (int) module.glassGlowRadius.getFloatValue()));
        int glowTex = runKawaseBloom(iterations);

        int c1, c2;
        if (module.glassRainbow.getValue()) {
            int[] rb = rainbowColors(module.glassRainbowSpeed.getFloatValue());
            c1 = rb[0]; c2 = rb[1];
        } else {
            c1 = module.glassGlowColor1.getValue();
            c2 = module.glassGlowColor2.getValue();
        }
        float[] col1 = {rf(c1), gf(c1), bf(c1)};
        float[] col2 = {rf(c2), gf(c2), bf(c2)};

        ShaderProgram glowShader = mc.getShaderLoader().getOrCreateProgram(KEY_GLASS_GLOW);
        if (glowShader == null) return;

        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE,
                GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);

        RenderSystem.setShader(KEY_GLASS_GLOW);
        RenderSystem.setShaderTexture(0, glowTex);
        RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());

        setUniform(glowShader, "glowColor1", col1[0], col1[1], col1[2]);
        setUniform(glowShader, "glowColor2", col2[0], col2[1], col2[2]);
        setUniform(glowShader, "exposure", module.glassGlowExposure.getFloatValue());

        drawFullscreenQuad();
        RenderSystem.defaultBlendFunc();
    }

    private void renderPlasmaMode(GlassHands module, int color1, int color2) {
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(KEY_PLASMA);
        if (shader == null) return;

        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(KEY_PLASMA);
        RenderSystem.setShaderTexture(0, maskBuffer.getColorAttachment());

        float time = System.currentTimeMillis() % 100000L / 1000.0f;
        setUniform(shader, "iTime", time * module.plasmaSpeed.getFloatValue());
        setUniform(shader, "uColor", rf(color1), gf(color1), bf(color1));
        setUniform(shader, "plasmaScale", module.plasmaScale.getFloatValue());
        setUniform(shader, "uShowStars", module.plasmaStars.getValue() ? 1.0f : 0.0f);

        drawFullscreenQuad();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        restoreCompositeState();
    }

    private void renderWaveMode(GlassHands module, int color1, int color2) {
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(KEY_WAVE);
        if (shader == null) return;

        // Rainbow overrides wave fill colors (only when autoColor is off)
        // Compute once here so the shader and bloom/trail share the exact same color
        if (module.waveRainbow.getValue() && !module.waveAutoColor.getValue()) {
            int[] rb = rainbowColors(module.waveRainbowSpeed.getFloatValue());
            color1 = rb[0]; color2 = rb[0]; // same color for both so glow matches wave
        }

        waveColorBuf = ensureBuffer(waveColorBuf, false, 1);
        waveColorBuf.setClearColor(0f, 0f, 0f, 0f);
        waveColorBuf.clear();
        waveColorBuf.beginWrite(false);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(KEY_WAVE);
        RenderSystem.setShaderTexture(0, maskBuffer.getColorAttachment());
        float t = System.currentTimeMillis() % 100000L / 1000.0f;
        setUniform(shader, "time",            t);
        setUniform(shader, "color1",          rf(color1), gf(color1), bf(color1));
        setUniform(shader, "color2",          rf(color2), gf(color2), bf(color2));
        setUniform(shader, "waveSpeedX",      module.waveSpeedX.getFloatValue());
        setUniform(shader, "waveSpeedY",      module.waveSpeedY.getFloatValue());
        setUniform(shader, "waveScale",       module.waveScaleM.getFloatValue());
        setUniform(shader, "waveDensity",     module.waveDensity.getFloatValue());
        setUniform(shader, "waveGlow",        module.waveGlow.getFloatValue());
        setUniform(shader, "fillAlpha",       1.0f);
        setUniform(shader, "modelVisibility", module.waveModelVisibility.getFloatValue());
        RenderSystem.setShaderTexture(1, afterBuffer.getColorAttachment());
        drawFullscreenQuad();

        if (module.waveGlowEnabled.getValue() && module.waveOuterGlow.getValue()) {
            // Pass pre-computed colors so bloom/trail match the wave shader exactly
            doWaveBloomChain(module, color1, color2);
        }

        // Рисуем волну поверх — полностью заменяем пиксели рук (как fill заменяет через KEY_FILL)
        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1f, 1f, 1f, module.waveFillAlpha.getFloatValue());
        RenderSystem.setShaderTexture(0, waveColorBuf.getColorAttachment());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        drawFullscreenQuad();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        restoreCompositeState();
    }

    private void doWaveBloomChain(GlassHands module, int color1, int color2) {
        int iter = (int) module.waveGlowRadius.getFloatValue();

        // Если авто цвет — bloom от waveColorBuf (цвета волны), иначе от maskBuffer (контур)
        int bloomSource = module.waveAutoColor.getValue()
                ? waveColorBuf.getColorAttachment()
                : maskBuffer.getColorAttachment();

        ensureFillBloomBuffers(iter);
        ShaderProgram downShader = mc.getShaderLoader().getOrCreateProgram(KEY_KAWASE_DOWN);
        ShaderProgram upShader   = mc.getShaderLoader().getOrCreateProgram(KEY_KAWASE_UP);
        if (downShader == null || upShader == null) return;

        int cur = bloomSource;
        for (int i = 0; i < iter; i++) {
            Framebuffer b = fillBloomBuffers.get(i);
            b.setClearColor(0f, 0f, 0f, 0f); b.clear(); b.beginWrite(true);
            RenderSystem.setShader(KEY_KAWASE_DOWN);
            RenderSystem.setShaderTexture(0, cur);
            setKawaseUniforms(downShader, b.textureWidth, b.textureHeight, 1f + i);
            drawFullscreenQuad();
            cur = b.getColorAttachment();
        }
        for (int i = iter - 1; i >= 1; i--) {
            Framebuffer b = fillBloomBuffers.get(i - 1);
            b.beginWrite(true);
            RenderSystem.setShader(KEY_KAWASE_UP);
            RenderSystem.setShaderTexture(0, cur);
            setKawaseUniforms(upShader, b.textureWidth, b.textureHeight, 1f + i);
            setUniform(upShader, "color", 1f, 1f, 1f);
            drawFullscreenQuad();
            cur = b.getColorAttachment();
        }
        fillLastBloomTex = cur;

        // Use colors passed from renderWaveMode (already rainbow-resolved if needed)
        int g1, g2;
        if (module.waveRainbow.getValue() && !module.waveAutoColor.getValue()) {
            g1 = color1; g2 = color1; // same color — glow matches wave shader
        } else {
            g1 = module.waveGlowColor1.getValue();
            g2 = module.waveGlowColor2.getValue();
        }
        float[] c1 = {rf(g1), gf(g1), bf(g1)};
        float[] c2 = {rf(g2), gf(g2), bf(g2)};

        if (module.waveOuterGlow.getValue()) {
            if (module.waveTrailEnabled.getValue()) {
                renderWaveTrail(module, cur, c1, c2);
            } else {
                mc.getFramebuffer().beginWrite(true);
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                ShaderProgram glowShader = mc.getShaderLoader().getOrCreateProgram(KEY_FILL_GLOW);
                if (glowShader != null) {
                    RenderSystem.setShader(KEY_FILL_GLOW);
                    RenderSystem.setShaderTexture(0, cur);
                    RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());
                    setUniform(glowShader, "glowColor1", c1[0], c1[1], c1[2]);
                    setUniform(glowShader, "glowColor2", c2[0], c2[1], c2[2]);
                    setUniform(glowShader, "exposure",   module.waveGlowExposure.getFloatValue());
                    setUniform(glowShader, "autoColor",  module.waveAutoColor.getValue() ? 1.0f : 0.0f);
                    setUniform(glowShader, "saturation", module.waveSaturation.getFloatValue());
                    drawFullscreenQuad();
                }
                RenderSystem.defaultBlendFunc();
            }
        }
    }

    private void renderWaveTrail(GlassHands module, int bloomTex, float[] c1, float[] c2) {
        waveTrailRead  = ensureBuffer(waveTrailRead,  false, 2);
        waveTrailWrite = ensureBuffer(waveTrailWrite, false, 2);

        long now = System.currentTimeMillis();
        float rawDt = waveLastTrailTime > 0L ? (now - waveLastTrailTime) / 1000f : 1f / 60f;
        waveLastTrailTime = now;
        if (rawDt <= 0.0f || rawDt > 0.05f) rawDt = waveSmoothDt;
        rawDt = Math.max(1f / 144f, Math.min(1f / 60f, rawDt));
        waveSmoothDt += (rawDt - waveSmoothDt) * 0.08f;
        float dt = waveSmoothDt;
        float t  = (now % 100000L) / 1000f;

        float swayFreq = 3.6f;
        float upDelta   = module.waveTrailRise.getFloatValue() * dt;
        float amp       = module.waveTrailSway.getFloatValue();
        float swayDelta = (float)(Math.sin(t * swayFreq) - Math.sin((t - dt) * swayFreq)) * amp;
        float trailSmooth = 1.0f - (float) Math.exp(-dt * 8.0f);
        waveSmoothTrailRise += (upDelta   - waveSmoothTrailRise) * trailSmooth;
        waveSmoothTrailSway += (swayDelta - waveSmoothTrailSway) * trailSmooth;

        boolean swinging = mc.player != null && mc.player.handSwingProgress > 0;
        if (module.waveTrailBurst.getValue() && swinging && !waveWasSwinging) waveLastSwingMs = now;
        waveWasSwinging = swinging;
        float burstAge = (now - waveLastSwingMs) / 1000f;
        float burst    = Math.max(0f, 1f - burstAge / 0.45f);
        waveSmoothBurst += (burst - waveSmoothBurst) * (1.0f - (float) Math.exp(-dt * 6.0f));
        float fadeAdd = waveSmoothBurst * module.waveTrailBurstPower.getFloatValue() * 0.012f;

        // Всё пишем в waveTrailWrite, потом одним пассом в основной fb — как в renderFillTrail
        waveTrailWrite.beginWrite(false);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.disableBlend();

        ShaderProgram fadeShader = mc.getShaderLoader().getOrCreateProgram(KEY_FILL_TRAIL_FADE);
        if (fadeShader != null) {
            RenderSystem.setShader(KEY_FILL_TRAIL_FADE);
            RenderSystem.setShaderTexture(0, waveTrailRead.getColorAttachment());
            setUniform(fadeShader, "offset",   waveSmoothTrailSway, waveSmoothTrailRise);
            setUniform(fadeShader, "fade",     module.waveTrailFade.getFloatValue() + fadeAdd);
            setUniform(fadeShader, "t",        t);
            setUniform(fadeShader, "dt",       dt);
            setUniform(fadeShader, "turb",     module.waveTrailTurb.getFloatValue());
            setUniform(fadeShader, "flickAmp", module.waveTrailFlicker.getFloatValue());
            setUniform(fadeShader, "texSize",  (float) waveTrailRead.textureWidth, (float) waveTrailRead.textureHeight);
            drawFullscreenQuad();
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        ShaderProgram colorShader = mc.getShaderLoader().getOrCreateProgram(KEY_FILL_TRAIL_COLOR);
        if (colorShader != null) {
            RenderSystem.setShader(KEY_FILL_TRAIL_COLOR);
            RenderSystem.setShaderTexture(0, bloomTex);
            setUniform(colorShader, "glowColor1", c1[0], c1[1], c1[2]);
            setUniform(colorShader, "glowColor2", c2[0], c2[1], c2[2]);
            setUniform(colorShader, "exposure",   module.waveGlowExposure.getFloatValue());
            setUniform(colorShader, "autoColor",  module.waveAutoColor.getValue() ? 1.0f : 0.0f);
            setUniform(colorShader, "saturation", module.waveSaturation.getFloatValue());
            drawFullscreenQuad();
        }

        if (module.waveTrailModel.getValue()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShaderColor(1f, 1f, 1f, module.waveTrailModelAlpha.getFloatValue());
            // colorMask содержит RGB волны — как в fill colorMask содержит RGB рук
            RenderSystem.setShaderTexture(0, colorMask != null ? colorMask.getColorAttachment() : waveColorBuf.getColorAttachment());
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
            drawFullscreenQuad();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        mc.getFramebuffer().beginWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, waveTrailWrite.getColorAttachment());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        drawFullscreenQuad();
        RenderSystem.defaultBlendFunc();

        Framebuffer tmp = waveTrailRead;
        waveTrailRead   = waveTrailWrite;
        waveTrailWrite  = tmp;
    }

    private void renderFillMode(GlassHands module, int color1, int color2) {
        // First render glow and trail effects if enabled
        if (module.fillGlowEnabled.getValue() || module.fillOutlineEnabled.getValue()) {
            renderFillEffects(module, color1, color2);
        }

        // Then apply glass effect OR normal fill
        if (module.fillGlassMode.getValue()) {
            renderFillGlassMode(module);
        } else {
            renderFillColorMode(module);
        }

        restoreCompositeState();
    }

    private void renderFillColorMode(GlassHands module) {
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(KEY_FILL);
        if (shader == null) return;

        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(KEY_FILL);
        RenderSystem.setShaderTexture(0, afterBuffer.getColorAttachment());
        RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());

        int fc = module.fillRainbow.getValue()
                ? rainbowColors(module.fillRainbowSpeed.getFloatValue())[0]
                : module.fillColor.getValue();
        setUniform(shader, "fillColor", rf(fc), gf(fc), bf(fc));
        setUniform(shader, "fillAlpha", module.fillAlpha.getFloatValue());
        setUniform(shader, "keepShading", module.fillKeepShading.getValue() ? 1.0f : 0.0f);
        setUniform(shader, "shadingStrength", module.fillShadingStrength.getFloatValue());

        drawFullscreenQuad();

        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
    }

    private void renderFillGlassMode(GlassHands module) {
        // Create blurred background like in glass mode (no glow, just blur)
        int iterations = 3;
        int blurredBgTex = runKawaseBloomFrom(beforeBuffer.getColorAttachment(), iterations);

        ShaderProgram glassShader = mc.getShaderLoader().getOrCreateProgram(KEY_GLASS);
        if (glassShader == null) return;

        mc.getFramebuffer().beginWrite(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);

        RenderSystem.setShader(KEY_GLASS);
        RenderSystem.setShaderTexture(0, blurredBgTex);
        RenderSystem.setShaderTexture(1, afterBuffer.getColorAttachment());
        RenderSystem.setShaderTexture(2, maskBuffer.getColorAttachment());
        setUniform(glassShader, "mixFactor", module.fillGlassMix.getFloatValue());

        drawFullscreenQuad();

        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
    }

    private void renderFillEffects(GlassHands module, int color1, int color2) {
        boolean doBloom = module.fillGlowEnabled.getValue() && module.fillOuterGlow.getValue();
        if (doBloom) {
            doFillBloomChain(module);
        } else if (module.fillOutlineEnabled.getValue() && module.fillAutoColor.getValue()) {
            doSmallBlurForFillAuto(module);
        }

        if (module.fillOutlineEnabled.getValue()) {
            renderFillOutline(module);
        }
    }

    private void doFillBloomChain(GlassHands module) {
        int iter = (int) module.fillGlowRadius.getFloatValue();

        // Create color+alpha bloom from afterBuffer, masked by maskBuffer
        colorMask = ensureBuffer(colorMask, false, 1);
        colorMask.setClearColor(0f, 0f, 0f, 0f);
        colorMask.clear();
        colorMask.beginWrite(false);

        // Combine afterBuffer RGB with maskBuffer alpha
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, afterBuffer.getColorAttachment());
        RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());

        ShaderProgram colorMaskShader = mc.getShaderLoader().getOrCreateProgram(new ShaderProgramKey(
                ResourceProvider.getShaderIdentifier("glass_hands_color_mask"),
                VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY));
        if (colorMaskShader != null) {
            RenderSystem.setShader(new ShaderProgramKey(
                    ResourceProvider.getShaderIdentifier("glass_hands_color_mask"),
                    VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY));
            drawFullscreenQuad();
        }

        int cur = colorMask.getColorAttachment();
        ensureFillBloomBuffers(iter);

        ShaderProgram downShader = mc.getShaderLoader().getOrCreateProgram(KEY_KAWASE_DOWN);
        ShaderProgram upShader = mc.getShaderLoader().getOrCreateProgram(KEY_KAWASE_UP);
        if (downShader == null || upShader == null) return;

        for (int i = 0; i < iter; i++) {
            Framebuffer b = fillBloomBuffers.get(i);
            b.setClearColor(0f, 0f, 0f, 0f);
            b.clear();
            b.beginWrite(true);
            RenderSystem.setShader(KEY_KAWASE_DOWN);
            RenderSystem.setShaderTexture(0, cur);
            setKawaseUniforms(downShader, b.textureWidth, b.textureHeight, 1f + i);
            drawFullscreenQuad();
            cur = b.getColorAttachment();
        }

        for (int i = iter - 1; i >= 1; i--) {
            Framebuffer b = fillBloomBuffers.get(i - 1);
            b.beginWrite(true);
            RenderSystem.setShader(KEY_KAWASE_UP);
            RenderSystem.setShaderTexture(0, cur);
            setKawaseUniforms(upShader, b.textureWidth, b.textureHeight, 1f + i);
            setUniform(upShader, "color", 1f, 1f, 1f);
            drawFullscreenQuad();
            cur = b.getColorAttachment();
        }

        fillLastBloomTex = cur;

        int g1, g2;
        if (module.fillRainbow.getValue()) {
            int[] rb = rainbowColors(module.fillRainbowSpeed.getFloatValue());
            g1 = rb[0]; g2 = rb[1];
        } else {
            g1 = module.fillGlowColor1.getValue();
            g2 = module.fillGlowColor2.getValue();
        }
        float[] c1 = {rf(g1), gf(g1), bf(g1)};
        float[] c2 = {rf(g2), gf(g2), bf(g2)};

        if (module.fillTrailEnabled.getValue()) {
            renderFillTrail(module, cur, c1, c2);
        } else {
            mc.getFramebuffer().beginWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            ShaderProgram glowShader = mc.getShaderLoader().getOrCreateProgram(KEY_FILL_GLOW);
            if (glowShader != null) {
                RenderSystem.setShader(KEY_FILL_GLOW);
                RenderSystem.setShaderTexture(0, cur);
                RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());
                setUniform(glowShader, "glowColor1", c1[0], c1[1], c1[2]);
                setUniform(glowShader, "glowColor2", c2[0], c2[1], c2[2]);
                setUniform(glowShader, "exposure", module.fillGlowExposure.getFloatValue());
                setUniform(glowShader, "autoColor", module.fillAutoColor.getValue() ? 1.0f : 0.0f);
                setUniform(glowShader, "saturation", module.fillSaturation.getFloatValue());
                drawFullscreenQuad();
            }
            RenderSystem.defaultBlendFunc();
        }
    }

    private void doSmallBlurForFillAuto(GlassHands module) {
        int iter = 3;

        // Create color+alpha bloom from afterBuffer, masked by maskBuffer
        colorMask = ensureBuffer(colorMask, false, 1);
        colorMask.setClearColor(0f, 0f, 0f, 0f);
        colorMask.clear();
        colorMask.beginWrite(false);

        // Combine afterBuffer RGB with maskBuffer alpha
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, afterBuffer.getColorAttachment());
        RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());

        ShaderProgram colorMaskShader = mc.getShaderLoader().getOrCreateProgram(new ShaderProgramKey(
                ResourceProvider.getShaderIdentifier("glass_hands_color_mask"),
                VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY));
        if (colorMaskShader != null) {
            RenderSystem.setShader(new ShaderProgramKey(
                    ResourceProvider.getShaderIdentifier("glass_hands_color_mask"),
                    VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY));
            drawFullscreenQuad();
        }

        ensureFillBloomBuffers(iter);
        int cur = colorMask.getColorAttachment();

        ShaderProgram downShader = mc.getShaderLoader().getOrCreateProgram(KEY_KAWASE_DOWN);
        ShaderProgram upShader = mc.getShaderLoader().getOrCreateProgram(KEY_KAWASE_UP);
        if (downShader == null || upShader == null) return;

        for (int i = 0; i < iter; i++) {
            Framebuffer b = fillBloomBuffers.get(i);
            b.setClearColor(0f, 0f, 0f, 0f);
            b.clear();
            b.beginWrite(true);
            RenderSystem.setShader(KEY_KAWASE_DOWN);
            RenderSystem.setShaderTexture(0, cur);
            setKawaseUniforms(downShader, b.textureWidth, b.textureHeight, 1f + i);
            drawFullscreenQuad();
            cur = b.getColorAttachment();
        }

        for (int i = iter - 1; i >= 1; i--) {
            Framebuffer b = fillBloomBuffers.get(i - 1);
            b.beginWrite(true);
            RenderSystem.setShader(KEY_KAWASE_UP);
            RenderSystem.setShaderTexture(0, cur);
            setKawaseUniforms(upShader, b.textureWidth, b.textureHeight, 1f + i);
            setUniform(upShader, "color", 1f, 1f, 1f);
            drawFullscreenQuad();
            cur = b.getColorAttachment();
        }

        fillLastBloomTex = cur;
        mc.getFramebuffer().beginWrite(true);
    }

    private void renderFillTrail(GlassHands module, int bloomTex, float[] c1, float[] c2) {
        fillTrailRead = ensureBuffer(fillTrailRead, false, 2);
        fillTrailWrite = ensureBuffer(fillTrailWrite, false, 2);

        long now = System.currentTimeMillis();
        float rawDt = fillLastTrailTime > 0L ? (now - fillLastTrailTime) / 1000f : 1f / 60f;
        fillLastTrailTime = now;
        if (rawDt <= 0.0f || rawDt > 0.05f) {
            rawDt = fillSmoothDt;
        }
        rawDt = Math.max(1f / 144f, Math.min(1f / 60f, rawDt));
        fillSmoothDt += (rawDt - fillSmoothDt) * 0.08f;
        float dt = fillSmoothDt;
        float t = (now % 100000L) / 1000f;

        float swayFreq = 3.6f;
        float upDelta = module.fillTrailRise.getFloatValue() * dt;
        float amp = module.fillTrailSway.getFloatValue();
        float swayDelta = (float)(Math.sin(t * swayFreq) - Math.sin((t - dt) * swayFreq)) * amp;
        float trailSmooth = 1.0f - (float) Math.exp(-dt * 8.0f);
        fillSmoothTrailRise += (upDelta - fillSmoothTrailRise) * trailSmooth;
        fillSmoothTrailSway += (swayDelta - fillSmoothTrailSway) * trailSmooth;

        boolean swinging = mc.player != null && mc.player.handSwingProgress > 0;
        if (module.fillTrailBurst.getValue() && swinging && !fillWasSwinging) {
            fillLastSwingMs = now;
        }
        fillWasSwinging = swinging;

        float burstAge = (now - fillLastSwingMs) / 1000f;
        float burst = Math.max(0f, 1f - burstAge / 0.45f);
        fillSmoothBurst += (burst - fillSmoothBurst) * (1.0f - (float) Math.exp(-dt * 6.0f));
        float fadeAdd = fillSmoothBurst * module.fillTrailBurstPower.getFloatValue() * 0.012f;

        fillTrailWrite.beginWrite(false);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.disableBlend();

        ShaderProgram trailFadeShader = mc.getShaderLoader().getOrCreateProgram(KEY_FILL_TRAIL_FADE);
        if (trailFadeShader != null) {
            RenderSystem.setShader(KEY_FILL_TRAIL_FADE);
            RenderSystem.setShaderTexture(0, fillTrailRead.getColorAttachment());
            setUniform(trailFadeShader, "offset", fillSmoothTrailSway, fillSmoothTrailRise);
            setUniform(trailFadeShader, "fade", module.fillTrailFade.getFloatValue() + fadeAdd);
            setUniform(trailFadeShader, "t", t);
            setUniform(trailFadeShader, "dt", dt);
            setUniform(trailFadeShader, "turb", module.fillTrailTurb.getFloatValue());
            setUniform(trailFadeShader, "flickAmp", module.fillTrailFlicker.getFloatValue());
            setUniform(trailFadeShader, "texSize", (float)fillTrailRead.textureWidth, (float)fillTrailRead.textureHeight);
            drawFullscreenQuad();
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ShaderProgram trailColorShader = mc.getShaderLoader().getOrCreateProgram(KEY_FILL_TRAIL_COLOR);
        if (trailColorShader != null) {
            RenderSystem.setShader(KEY_FILL_TRAIL_COLOR);
            RenderSystem.setShaderTexture(0, bloomTex);
            setUniform(trailColorShader, "glowColor1", c1[0], c1[1], c1[2]);
            setUniform(trailColorShader, "glowColor2", c2[0], c2[1], c2[2]);
            setUniform(trailColorShader, "exposure", module.fillGlowExposure.getFloatValue());
            setUniform(trailColorShader, "autoColor", module.fillAutoColor.getValue() ? 1.0f : 0.0f);
            setUniform(trailColorShader, "saturation", module.fillSaturation.getFloatValue());
            drawFullscreenQuad();
        }

        if (module.fillTrailModel.getValue()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            float modelAlpha = module.fillTrailModelAlpha.getFloatValue();
            RenderSystem.setShaderColor(1f, 1f, 1f, modelAlpha);
            // Use colorMask which has original hand RGB colors
            if (colorMask != null) {
                RenderSystem.setShaderTexture(0, colorMask.getColorAttachment());
            } else {
                RenderSystem.setShaderTexture(0, afterBuffer.getColorAttachment());
            }
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
            drawFullscreenQuad();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        mc.getFramebuffer().beginWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, fillTrailWrite.getColorAttachment());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        drawFullscreenQuad();
        RenderSystem.defaultBlendFunc();

        Framebuffer tmp = fillTrailRead;
        fillTrailRead = fillTrailWrite;
        fillTrailWrite = tmp;
    }

    private void renderFillOutline(GlassHands module) {
        boolean auto = module.fillAutoColor.getValue() && fillLastBloomTex != -1;
        int colorMode = auto ? 2 : 0;

        int oc = module.fillOutlineColor.getValue();
        java.awt.Color col = new java.awt.Color(oc, true);
        float a = col.getAlpha() / 255f;

        ShaderProgram outlineShader = mc.getShaderLoader().getOrCreateProgram(KEY_FILL_OUTLINE);
        if (outlineShader == null) return;

        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(KEY_FILL_OUTLINE);
        RenderSystem.setShaderTexture(0, maskBuffer.getColorAttachment());
        if (auto) RenderSystem.setShaderTexture(1, fillLastBloomTex);

        int fw = Math.max(1, mc.getWindow().getFramebufferWidth());
        int fh = Math.max(1, mc.getWindow().getFramebufferHeight());

        setUniform(outlineShader, "colorMode", (float)colorMode);
        setUniform(outlineShader, "width", module.fillOutlineWidth.getFloatValue());
        setUniform(outlineShader, "texelSize", 1f / fw, 1f / fh);
        setUniform(outlineShader, "alpha", a);
        setUniform(outlineShader, "saturation", module.fillSaturation.getFloatValue());
        setUniform(outlineShader, "solidColor", col.getRed() / 255f, col.getGreen() / 255f, col.getBlue() / 255f);

        drawFullscreenQuad();
        RenderSystem.defaultBlendFunc();
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    /** Two rainbow colors offset by 180° hue */
    private int[] rainbowColors(float speed) {
        float hue = (System.currentTimeMillis() % (long)(10000.0 / speed)) / (10000f / speed);
        int c1 = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0xFFFFFF;
        int c2 = java.awt.Color.HSBtoRGB((hue + 0.5f) % 1.0f, 1.0f, 1.0f) & 0xFFFFFF;
        return new int[]{c1, c2};
    }

    private Framebuffer ensureBuffer(Framebuffer buf, boolean depth, int divisor) {
        int fw = mc.getWindow().getFramebufferWidth();
        int fh = mc.getWindow().getFramebufferHeight();
        int w = Math.max(2, fw / divisor);
        int h = Math.max(2, fh / divisor);
        if (buf == null) {
            buf = new SimpleFramebuffer(w, h, depth);
            setLinearFiltering(buf);
        } else if (buf.textureWidth != w || buf.textureHeight != h) {
            buf.delete();
            buf = new SimpleFramebuffer(w, h, depth);
            setLinearFiltering(buf);
        }
        return buf;
    }

    private void ensureFillBloomBuffers(int n) {
        int fw = mc.getWindow().getFramebufferWidth();
        int fh = mc.getWindow().getFramebufferHeight();
        if (fillBloomBuffers.size() < n) {
            fillBloomBuffers.forEach(Framebuffer::delete);
            fillBloomBuffers.clear();
            for (int i = 0; i < n; i++) {
                Framebuffer f = new SimpleFramebuffer(
                        Math.max(2, fw >> (i + 1)),
                        Math.max(2, fh >> (i + 1)),
                        false);
                setLinearFiltering(f);
                fillBloomBuffers.add(f);
            }
        }
        for (int i = 0; i < n; i++) {
            int w = Math.max(2, fw >> (i + 1));
            int h = Math.max(2, fh >> (i + 1));
            Framebuffer b = fillBloomBuffers.get(i);
            if (b.textureWidth != w || b.textureHeight != h) {
                b.delete();
                b = new SimpleFramebuffer(w, h, false);
                setLinearFiltering(b);
                fillBloomBuffers.set(i, b);
            }
        }
    }

    private boolean isEffectEnabled(GlassHands module) {
        if (module == null || !module.isEnabled()) return false;
        if (module.mode.is("Блюр")) return true;
        if (module.mode.is("Обводка")) return true;
        if (module.mode.is("Шлейф")) return true;
        if (module.mode.is("Trail")) return true;
        if (module.mode.is("Стекло")) return true;
        if (module.mode.is("Plasma")) return true;
        if (module.mode.is("Заливка")) return true;
        if (module.mode.is("Волна")) return true;
        boolean hasGlow = module.glow.getFloatValue() > EPSILON;
        boolean hasFill = module.fill.getFloatValue() > EPSILON && module.alpha.getFloatValue() > EPSILON;
        return hasGlow || hasFill;
    }

    private void drawFullscreenQuad() {
        float sw = Math.max(mc.getWindow().getScaledWidth(),  1);
        float sh = Math.max(mc.getWindow().getScaledHeight(), 1);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(0f,  0f,  0f).texture(0f, 1f).color(1f, 1f, 1f, 1f);
        b.vertex(0f,  sh, 0f).texture(0f, 0f).color(1f, 1f, 1f, 1f);
        b.vertex(sw, sh, 0f).texture(1f, 0f).color(1f, 1f, 1f, 1f);
        b.vertex(sw, 0f,  0f).texture(1f, 1f).color(1f, 1f, 1f, 1f);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private void setUniform(ShaderProgram s, String name, float v) {
        var u = s.getUniform(name); if (u != null) u.set(v);
    }
    private void setUniform(ShaderProgram s, String name, float x, float y) {
        var u = s.getUniform(name); if (u != null) u.set(x, y);
    }
    private void setUniform(ShaderProgram s, String name, float x, float y, float z) {
        var u = s.getUniform(name); if (u != null) u.set(x, y, z);
    }
    private void setUniform(ShaderProgram s, String name, float x, float y, float z, float w) {
        var u = s.getUniform(name); if (u != null) u.set(x, y, z, w);
    }
    private void setKawaseUniforms(ShaderProgram s, int w, int h, float offset) {
        setUniform(s, "uSize",      (float) Math.max(1, w), (float) Math.max(1, h));
        setUniform(s, "uOffset",    offset, offset);
        setUniform(s, "uHalfPixel", 0.5f / Math.max(1, w), 0.5f / Math.max(1, h));
    }

    private float rf(int c) { return ((c >> 16) & 0xFF) / 255f; }
    private float gf(int c) { return ((c >>  8) & 0xFF) / 255f; }
    private float bf(int c) { return  (c        & 0xFF) / 255f; }
}


