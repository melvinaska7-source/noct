package polar.ru.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;
import polar.ru.api.QClient;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.render.ShaderUtils;
import polar.ru.api.utils.render.glow.GlowCallback;
import polar.ru.api.utils.render.glow.GlowProgram;
import polar.ru.api.utils.scissor.ScissorUtils;

public final class RenderUtils
implements QClient {
    private static final ConcurrentHashMap<String, Identifier> skinCache = new ConcurrentHashMap();
    private static final UUID DEFAULT_SKIN_UUID = new UUID(0L, 0L);
    private static Framebuffer liquidBlurBuffer1;
    private static Framebuffer liquidBlurBuffer2;
    private static Framebuffer liquidBlurResultBuffer;
    private static int liquidBlurLastWidth;
    private static int liquidBlurLastHeight;
    private static float liquidBlurOffset;
    private static final int LIQUID_BLUR_ITERATIONS = 4;

    public static void beginLiquidBlurFrame() {
        Framebuffer dst;
        Framebuffer src;
        int i2;
        int fbWidth = mc.getWindow().getFramebufferWidth();
        int fbHeight = mc.getWindow().getFramebufferHeight();
        if (liquidBlurBuffer1 == null || liquidBlurBuffer2 == null || liquidBlurLastWidth != fbWidth || liquidBlurLastHeight != fbHeight) {
            RenderUtils.deleteLiquidBlurBuffers();
            liquidBlurBuffer1 = RenderUtils.createLiquidBlurBuffer(fbWidth, fbHeight);
            liquidBlurBuffer2 = RenderUtils.createLiquidBlurBuffer(fbWidth, fbHeight);
            liquidBlurLastWidth = fbWidth;
            liquidBlurLastHeight = fbHeight;
        }
        ShaderProgram kawaseDown = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.kawaseDown);
        ShaderProgram kawaseUp = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.kawaseUp);
        float scaledW = mc.getWindow().getScaledWidth();
        float scaledH = mc.getWindow().getScaledHeight();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderUtils.blitLiquidBlurPass(kawaseDown, mc.getFramebuffer(), liquidBlurBuffer1, fbWidth, fbHeight, scaledW, scaledH);
        Framebuffer[] buffers = new Framebuffer[]{liquidBlurBuffer1, liquidBlurBuffer2};
        for (i2 = 1; i2 < 4; ++i2) {
            src = buffers[(i2 + 1) % 2];
            dst = buffers[i2 % 2];
            RenderUtils.blitLiquidBlurPass(kawaseDown, src, dst, src.textureWidth, src.textureHeight, scaledW, scaledH);
        }
        for (i2 = 0; i2 < 4; ++i2) {
            src = buffers[i2 % 2];
            dst = buffers[(i2 + 1) % 2];
            RenderUtils.blitLiquidBlurPass(kawaseUp, src, dst, src.textureWidth, src.textureHeight, scaledW, scaledH);
        }
        liquidBlurResultBuffer = buffers[0];
        RenderSystem.disableBlend();
        mc.getFramebuffer().beginWrite(true);
        RenderSystem.setShaderTexture((int)0, (int)0);
    }

    public static void requestLiquidBlur() {
    }

    public static int getLiquidBlurTexture() {
        return liquidBlurResultBuffer != null ? liquidBlurResultBuffer.getColorAttachment() : 0;
    }

    public static void setLiquidBlurOffset(float offset) {
        liquidBlurOffset = offset;
    }

    private static void blitLiquidBlurPass(ShaderProgram shader, Framebuffer src, Framebuffer dst, int texW, int texH, float quadW, float quadH) {
        dst.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        dst.clear();
        dst.beginWrite(true);
        RenderSystem.setShader((ShaderProgram)shader);
        src.beginRead();
        RenderSystem.setShaderTexture((int)0, (int)src.getColorAttachment());
        RenderUtils.setKawaseUniforms(shader, texW, texH);
        RenderUtils.drawFullscreenQuad(quadW, quadH);
        src.endRead();
        dst.endWrite();
    }

    private static void setKawaseUniforms(ShaderProgram shader, int texW, int texH) {
        GlUniform res = shader.getUniform("Resolution");
        GlUniform off = shader.getUniform("Offset");
        GlUniform sat = shader.getUniform("Saturation");
        GlUniform tintI = shader.getUniform("TintIntensity");
        GlUniform tintC = shader.getUniform("TintColor");
        if (res != null) {
            res.set(1.0f / (float)texW, 1.0f / (float)texH);
        }
        if (off != null) {
            off.set(liquidBlurOffset);
        }
        if (sat != null) {
            sat.set(1.0f);
        }
        if (tintI != null) {
            tintI.set(0.0f);
        }
        if (tintC != null) {
            tintC.set(1.0f, 1.0f, 1.0f);
        }
    }

    private static void drawFullscreenQuad(float w2, float h2) {
        BufferBuilder bb2 = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bb2.vertex(0.0f, 0.0f, 0.0f).texture(0.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        bb2.vertex(0.0f, h2, 0.0f).texture(0.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        bb2.vertex(w2, h2, 0.0f).texture(1.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        bb2.vertex(w2, 0.0f, 0.0f).texture(1.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)bb2.end());
    }

    private static Framebuffer createLiquidBlurBuffer(int w2, int h2) {
        SimpleFramebuffer fb = new SimpleFramebuffer(w2, h2, false);
        RenderSystem.bindTexture((int)fb.getColorAttachment());
        GL30.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL30.glTexParameteri((int)3553, (int)10240, (int)9729);
        RenderSystem.bindTexture((int)0);
        return fb;
    }

    private static void deleteLiquidBlurBuffers() {
        if (liquidBlurBuffer1 != null) {
            liquidBlurBuffer1.delete();
            liquidBlurBuffer1 = null;
        }
        if (liquidBlurBuffer2 != null) {
            liquidBlurBuffer2.delete();
            liquidBlurBuffer2 = null;
        }
        liquidBlurResultBuffer = null;
    }

    public static void cleanupLiquidBlur() {
        RenderUtils.deleteLiquidBlurBuffers();
        liquidBlurLastWidth = -1;
        liquidBlurLastHeight = -1;
    }

    public static void drawHudItem(DrawContext context, ItemStack stack, float x2, float y2, float scale, float z2) {
        if (context == null || stack == null || stack.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.getMatrices();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        matrices.push();
        matrices.translate(x2, y2, z2);
        matrices.scale(scale, scale, 1.0f);
        context.drawItem(stack, 0, 0);
        matrices.pop();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)true);
    }

    public static void drawGradient6Rect(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int leftTopColor, int leftBottomColor, int centerTopColor, int centerBottomColor, int rightTopColor, int rightBottomColor) {
        int a2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.gradient6Rect);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        GlUniform sizeUniform = shader.getUniform("Size");
        GlUniform radiusUniform = shader.getUniform("Radius");
        GlUniform smoothnessUniform = shader.getUniform("Smoothness");
        GlUniform leftTopColorUniform = shader.getUniform("LeftTopColor");
        GlUniform leftBottomColorUniform = shader.getUniform("LeftBottomColor");
        GlUniform centerTopColorUniform = shader.getUniform("CenterTopColor");
        GlUniform centerBottomColorUniform = shader.getUniform("CenterBottomColor");
        GlUniform rightTopColorUniform = shader.getUniform("RightTopColor");
        GlUniform rightBottomColorUniform = shader.getUniform("RightBottomColor");
        if (sizeUniform != null) {
            sizeUniform.set(width, height);
        }
        if (radiusUniform != null) {
            radiusUniform.set(radius, radius, radius, radius);
        }
        if (smoothnessUniform != null) {
            smoothnessUniform.set(1.0f);
        }
        if (leftTopColorUniform != null) {
            a2 = leftTopColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            leftTopColorUniform.set((float)(leftTopColor >> 16 & 0xFF) / 255.0f, (float)(leftTopColor >> 8 & 0xFF) / 255.0f, (float)(leftTopColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (leftBottomColorUniform != null) {
            a2 = leftBottomColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            leftBottomColorUniform.set((float)(leftBottomColor >> 16 & 0xFF) / 255.0f, (float)(leftBottomColor >> 8 & 0xFF) / 255.0f, (float)(leftBottomColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (centerTopColorUniform != null) {
            a2 = centerTopColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            centerTopColorUniform.set((float)(centerTopColor >> 16 & 0xFF) / 255.0f, (float)(centerTopColor >> 8 & 0xFF) / 255.0f, (float)(centerTopColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (centerBottomColorUniform != null) {
            a2 = centerBottomColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            centerBottomColorUniform.set((float)(centerBottomColor >> 16 & 0xFF) / 255.0f, (float)(centerBottomColor >> 8 & 0xFF) / 255.0f, (float)(centerBottomColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (rightTopColorUniform != null) {
            a2 = rightTopColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            rightTopColorUniform.set((float)(rightTopColor >> 16 & 0xFF) / 255.0f, (float)(rightTopColor >> 8 & 0xFF) / 255.0f, (float)(rightTopColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (rightBottomColorUniform != null) {
            a2 = rightBottomColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            rightBottomColorUniform.set((float)(rightBottomColor >> 16 & 0xFF) / 255.0f, (float)(rightBottomColor >> 8 & 0xFF) / 255.0f, (float)(rightBottomColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x2, y2, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(matrix, x2, y2 + height, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(matrix, x2 + width, y2 + height, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(matrix, x2 + width, y2, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.gradient6Rect);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawShadow(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float softness, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        int a2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shadowRect);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float extendedWidth = width + softness * 2.0f;
        float extendedHeight = height + softness * 2.0f;
        float drawX = x2 - softness;
        float drawY = y2 - softness;
        GlUniform sizeUniform = shader.getUniform("Size");
        GlUniform softnessUniform = shader.getUniform("Softness");
        GlUniform radiusUniform = shader.getUniform("Radius");
        GlUniform topLeftColorUniform = shader.getUniform("TopLeftColor");
        GlUniform topRightColorUniform = shader.getUniform("TopRightColor");
        GlUniform bottomLeftColorUniform = shader.getUniform("BottomLeftColor");
        GlUniform bottomRightColorUniform = shader.getUniform("BottomRightColor");
        if (sizeUniform != null) {
            sizeUniform.set(extendedWidth, extendedHeight);
        }
        if (softnessUniform != null) {
            softnessUniform.set(softness);
        }
        if (radiusUniform != null) {
            radiusUniform.set(radius);
        }
        if (topLeftColorUniform != null) {
            a2 = topLeftColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            topLeftColorUniform.set((float)(topLeftColor >> 16 & 0xFF) / 255.0f, (float)(topLeftColor >> 8 & 0xFF) / 255.0f, (float)(topLeftColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (topRightColorUniform != null) {
            a2 = topRightColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            topRightColorUniform.set((float)(topRightColor >> 16 & 0xFF) / 255.0f, (float)(topRightColor >> 8 & 0xFF) / 255.0f, (float)(topRightColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (bottomLeftColorUniform != null) {
            a2 = bottomLeftColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            bottomLeftColorUniform.set((float)(bottomLeftColor >> 16 & 0xFF) / 255.0f, (float)(bottomLeftColor >> 8 & 0xFF) / 255.0f, (float)(bottomLeftColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (bottomRightColorUniform != null) {
            a2 = bottomRightColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            bottomRightColorUniform.set((float)(bottomRightColor >> 16 & 0xFF) / 255.0f, (float)(bottomRightColor >> 8 & 0xFF) / 255.0f, (float)(bottomRightColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, drawX, drawY, 0.0f).texture(0.0f, 0.0f);
        buffer.vertex(matrix, drawX, drawY + extendedHeight, 0.0f).texture(0.0f, 1.0f);
        buffer.vertex(matrix, drawX + extendedWidth, drawY + extendedHeight, 0.0f).texture(1.0f, 1.0f);
        buffer.vertex(matrix, drawX + extendedWidth, drawY, 0.0f).texture(1.0f, 0.0f);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shadowRect);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawShadow(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        RenderUtils.drawShadow(matrices, x2, y2, width, height, radius, 10.0f, topLeftColor, topRightColor, bottomLeftColor, bottomRightColor);
    }

    public static void drawShadow(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float softness, int color) {
        RenderUtils.drawShadow(matrices, x2, y2, width, height, radius, softness, color, color, color, color);
    }

    public static void drawShadow(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int color) {
        RenderUtils.drawShadow(matrices, x2, y2, width, height, radius, 10.0f, color, color, color, color);
    }

    public static void drawShadow(MatrixStack matrices, float x2, float y2, float width, float height, int color) {
        RenderUtils.drawShadow(matrices, x2, y2, width, height, 0.0f, 10.0f, color, color, color, color);
    }

    public static void drawShadow(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float softness, int topColor, int bottomColor) {
        RenderUtils.drawShadow(matrices, x2, y2, width, height, radius, softness, topColor, topColor, bottomColor, bottomColor);
    }

    public static void drawShadowHorizontal(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float softness, int leftColor, int rightColor) {
        RenderUtils.drawShadow(matrices, x2, y2, width, height, radius, softness, leftColor, rightColor, leftColor, rightColor);
    }

    public static void drawShadow(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float softness, float offsetX, float offsetY, int color) {
        RenderUtils.drawShadow(matrices, x2 + offsetX, y2 + offsetY, width, height, radius, softness, color, color, color, color);
    }

    public static void drawShadow(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float softness, float offsetX, float offsetY, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        RenderUtils.drawShadow(matrices, x2 + offsetX, y2 + offsetY, width, height, radius, softness, topLeftColor, topRightColor, bottomLeftColor, bottomRightColor);
    }

    public static void drawShadow6(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float softness, int leftTopColor, int leftBottomColor, int centerTopColor, int centerBottomColor, int rightTopColor, int rightBottomColor) {
        int a2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shadow6Rect);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float extendedWidth = width + softness * 2.0f;
        float extendedHeight = height + softness * 2.0f;
        float drawX = x2 - softness;
        float drawY = y2 - softness;
        GlUniform sizeUniform = shader.getUniform("Size");
        GlUniform softnessUniform = shader.getUniform("Softness");
        GlUniform radiusUniform = shader.getUniform("Radius");
        GlUniform leftTopColorUniform = shader.getUniform("LeftTopColor");
        GlUniform leftBottomColorUniform = shader.getUniform("LeftBottomColor");
        GlUniform centerTopColorUniform = shader.getUniform("CenterTopColor");
        GlUniform centerBottomColorUniform = shader.getUniform("CenterBottomColor");
        GlUniform rightTopColorUniform = shader.getUniform("RightTopColor");
        GlUniform rightBottomColorUniform = shader.getUniform("RightBottomColor");
        if (sizeUniform != null) {
            sizeUniform.set(extendedWidth, extendedHeight);
        }
        if (softnessUniform != null) {
            softnessUniform.set(softness);
        }
        if (radiusUniform != null) {
            radiusUniform.set(radius);
        }
        if (leftTopColorUniform != null) {
            a2 = leftTopColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            leftTopColorUniform.set((float)(leftTopColor >> 16 & 0xFF) / 255.0f, (float)(leftTopColor >> 8 & 0xFF) / 255.0f, (float)(leftTopColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (leftBottomColorUniform != null) {
            a2 = leftBottomColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            leftBottomColorUniform.set((float)(leftBottomColor >> 16 & 0xFF) / 255.0f, (float)(leftBottomColor >> 8 & 0xFF) / 255.0f, (float)(leftBottomColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (centerTopColorUniform != null) {
            a2 = centerTopColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            centerTopColorUniform.set((float)(centerTopColor >> 16 & 0xFF) / 255.0f, (float)(centerTopColor >> 8 & 0xFF) / 255.0f, (float)(centerTopColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (centerBottomColorUniform != null) {
            a2 = centerBottomColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            centerBottomColorUniform.set((float)(centerBottomColor >> 16 & 0xFF) / 255.0f, (float)(centerBottomColor >> 8 & 0xFF) / 255.0f, (float)(centerBottomColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (rightTopColorUniform != null) {
            a2 = rightTopColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            rightTopColorUniform.set((float)(rightTopColor >> 16 & 0xFF) / 255.0f, (float)(rightTopColor >> 8 & 0xFF) / 255.0f, (float)(rightTopColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (rightBottomColorUniform != null) {
            a2 = rightBottomColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            rightBottomColorUniform.set((float)(rightBottomColor >> 16 & 0xFF) / 255.0f, (float)(rightBottomColor >> 8 & 0xFF) / 255.0f, (float)(rightBottomColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, drawX, drawY, 0.0f).texture(0.0f, 0.0f);
        buffer.vertex(matrix, drawX, drawY + extendedHeight, 0.0f).texture(0.0f, 1.0f);
        buffer.vertex(matrix, drawX + extendedWidth, drawY + extendedHeight, 0.0f).texture(1.0f, 1.0f);
        buffer.vertex(matrix, drawX + extendedWidth, drawY, 0.0f).texture(1.0f, 0.0f);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.shadow6Rect);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawTexture(MatrixStack matrices, Identifier texture, float x2, float y2, float width, float height, float u1, float v1, float u2, float v2, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture((int)0, (Identifier)texture);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int alpha = color >> 24 & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        float r2 = (float)(color >> 16 & 0xFF) / 255.0f;
        float g2 = (float)(color >> 8 & 0xFF) / 255.0f;
        float b2 = (float)(color & 0xFF) / 255.0f;
        float a2 = (float)alpha / 255.0f;
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, x2, y2, 0.0f).texture(u1, v1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2, y2 + height, 0.0f).texture(u1, v2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2 + width, y2 + height, 0.0f).texture(u2, v2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2 + width, y2, 0.0f).texture(u2, v1).color(r2, g2, b2, a2);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.disableBlend();
    }

    public static void drawImage(MatrixStack matrices, Identifier texture, float x2, float y2, float width, float height, int color) {
        RenderUtils.drawTexture(matrices, texture, x2, y2, width, height, 0.0f, 0.0f, 1.0f, 1.0f, color);
    }

    public static void drawImage(MatrixStack matrices, String namespace, String path, float x2, float y2, float width, float height, int color) {
        RenderUtils.drawImage(matrices, Identifier.of((String)namespace, (String)path), x2, y2, width, height, color);
    }

    public static void drawSprite(MatrixStack matrices, Sprite sprite, float x2, float y2, float size, int color) {
        RenderUtils.drawTexture(matrices, sprite.getAtlasId(), x2, y2, size, size, sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), color);
    }

    public static void drawPlayerHead(MatrixStack matrices, PlayerEntity player, float x2, float y2, float size, float radius, float hurtPercent) {
        if (player == null) {
            return;
        }
        Identifier skinTexture = RenderUtils.getSkinTexture(player);
        RenderUtils.drawHeadInternal(matrices, skinTexture, x2, y2, size, radius, 1.0f, hurtPercent);
    }

    public static void drawPlayerHead(MatrixStack matrices, String username, float x2, float y2, float size, float radius) {
        RenderUtils.drawPlayerHead(matrices, username, x2, y2, size, radius, 1.0f, 0.0f);
    }

    public static void drawPlayerHead(MatrixStack matrices, String username, float x2, float y2, float size, float radius, float alpha, float hurtPercent) {
        if (username == null || username.isEmpty()) {
            return;
        }
        Identifier skinTexture = RenderUtils.getSkinTextureByName(username);
        RenderUtils.drawHeadInternal(matrices, skinTexture, x2, y2, size, radius, alpha, hurtPercent);
    }

    public static void drawPlayerHead(MatrixStack matrices, UUID uuid, float x2, float y2, float size, float radius) {
        RenderUtils.drawPlayerHead(matrices, uuid, x2, y2, size, radius, 1.0f, 0.0f);
    }

    public static void drawPlayerHead(MatrixStack matrices, UUID uuid, float x2, float y2, float size, float radius, float alpha, float hurtPercent) {
        if (uuid == null) {
            return;
        }
        Identifier skinTexture = RenderUtils.getSkinTextureByUUID(uuid);
        RenderUtils.drawHeadInternal(matrices, skinTexture, x2, y2, size, radius, alpha, hurtPercent);
    }

    public static void drawPlayerHead(MatrixStack matrices, PlayerListEntry entry, float x2, float y2, float size, float radius) {
        RenderUtils.drawPlayerHead(matrices, entry, x2, y2, size, radius, 1.0f, 0.0f);
    }

    public static void drawPlayerHead(MatrixStack matrices, PlayerListEntry entry, float x2, float y2, float size, float radius, float alpha, float hurtPercent) {
        if (entry == null) {
            return;
        }
        Identifier skinTexture = entry.getSkinTextures().texture();
        if (skinTexture == null) {
            skinTexture = DefaultSkinHelper.getSkinTextures((UUID)entry.getProfile().getId()).texture();
        }
        RenderUtils.drawHeadInternal(matrices, skinTexture, x2, y2, size, radius, alpha, hurtPercent);
    }

    public static void drawPlayerHead(MatrixStack matrices, Identifier skinTexture, float x2, float y2, float size, float radius) {
        RenderUtils.drawHeadInternal(matrices, skinTexture, x2, y2, size, radius, 1.0f, 0.0f);
    }

    public static void drawRoundedImage(MatrixStack matrices, Identifier texture, float x2, float y2, float size, float radius, float alpha) {
        if (texture == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture((int)0, (Identifier)texture);
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.face);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        GlUniform locationUniform = shader.getUniform("location");
        GlUniform sizeUniform = shader.getUniform("size");
        GlUniform radiusUniform = shader.getUniform("radius");
        GlUniform alphaUniform = shader.getUniform("alpha");
        GlUniform uUniform = shader.getUniform("u");
        GlUniform vUniform = shader.getUniform("v");
        GlUniform wUniform = shader.getUniform("w");
        GlUniform hUniform = shader.getUniform("h");
        GlUniform hurtPercentUniform = shader.getUniform("hurtPercent");
        if (locationUniform != null) {
            locationUniform.set(x2, y2);
        }
        if (sizeUniform != null) {
            sizeUniform.set(size, size);
        }
        if (radiusUniform != null) {
            radiusUniform.set(radius);
        }
        if (alphaUniform != null) {
            alphaUniform.set(alpha);
        }
        if (uUniform != null) {
            uUniform.set(0.0f);
        }
        if (vUniform != null) {
            vUniform.set(0.0f);
        }
        if (wUniform != null) {
            wUniform.set(1.0f);
        }
        if (hUniform != null) {
            hUniform.set(1.0f);
        }
        if (hurtPercentUniform != null) {
            hurtPercentUniform.set(0.0f);
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, x2, y2, 0.0f).texture(0.0f, 0.0f);
        buffer.vertex(matrix, x2, y2 + size, 0.0f).texture(0.0f, 1.0f);
        buffer.vertex(matrix, x2 + size, y2 + size, 0.0f).texture(1.0f, 1.0f);
        buffer.vertex(matrix, x2 + size, y2, 0.0f).texture(1.0f, 0.0f);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.face);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.disableBlend();
    }

    private static void drawHeadInternal(MatrixStack matrices, Identifier skinTexture, float x2, float y2, float size, float radius, float alpha, float hurtPercent) {
        if (skinTexture == null) {
            skinTexture = DefaultSkinHelper.getSkinTextures((UUID)DEFAULT_SKIN_UUID).texture();
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture((int)0, (Identifier)skinTexture);
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.face);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        GlUniform locationUniform = shader.getUniform("location");
        GlUniform sizeUniform = shader.getUniform("size");
        GlUniform radiusUniform = shader.getUniform("radius");
        GlUniform alphaUniform = shader.getUniform("alpha");
        GlUniform uUniform = shader.getUniform("u");
        GlUniform vUniform = shader.getUniform("v");
        GlUniform wUniform = shader.getUniform("w");
        GlUniform hUniform = shader.getUniform("h");
        GlUniform hurtPercentUniform = shader.getUniform("hurtPercent");
        if (locationUniform != null) {
            locationUniform.set(x2, y2);
        }
        if (sizeUniform != null) {
            sizeUniform.set(size, size);
        }
        if (radiusUniform != null) {
            radiusUniform.set(radius);
        }
        if (alphaUniform != null) {
            alphaUniform.set(alpha);
        }
        if (uUniform != null) {
            uUniform.set(0.125f);
        }
        if (vUniform != null) {
            vUniform.set(0.125f);
        }
        if (wUniform != null) {
            wUniform.set(0.125f);
        }
        if (hUniform != null) {
            hUniform.set(0.125f);
        }
        if (hurtPercentUniform != null) {
            hurtPercentUniform.set(hurtPercent);
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, x2, y2, 0.0f).texture(0.0f, 0.0f);
        buffer.vertex(matrix, x2, y2 + size, 0.0f).texture(0.0f, 1.0f);
        buffer.vertex(matrix, x2 + size, y2 + size, 0.0f).texture(1.0f, 1.0f);
        buffer.vertex(matrix, x2 + size, y2, 0.0f).texture(1.0f, 0.0f);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.face);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderUtils.drawHeadOverlay(matrices, skinTexture, x2, y2, size, radius, alpha, hurtPercent);
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.disableBlend();
    }

    private static void drawHeadOverlay(MatrixStack matrices, Identifier skinTexture, float x2, float y2, float size, float radius, float alpha, float hurtPercent) {
        RenderSystem.setShaderTexture((int)0, (Identifier)skinTexture);
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.face);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        GlUniform locationUniform = shader.getUniform("location");
        GlUniform sizeUniform = shader.getUniform("size");
        GlUniform radiusUniform = shader.getUniform("radius");
        GlUniform alphaUniform = shader.getUniform("alpha");
        GlUniform uUniform = shader.getUniform("u");
        GlUniform vUniform = shader.getUniform("v");
        GlUniform wUniform = shader.getUniform("w");
        GlUniform hUniform = shader.getUniform("h");
        GlUniform hurtPercentUniform = shader.getUniform("hurtPercent");
        if (locationUniform != null) {
            locationUniform.set(x2, y2);
        }
        if (sizeUniform != null) {
            sizeUniform.set(size, size);
        }
        if (radiusUniform != null) {
            radiusUniform.set(radius);
        }
        if (alphaUniform != null) {
            alphaUniform.set(alpha);
        }
        if (uUniform != null) {
            uUniform.set(0.625f);
        }
        if (vUniform != null) {
            vUniform.set(0.125f);
        }
        if (wUniform != null) {
            wUniform.set(0.125f);
        }
        if (hUniform != null) {
            hUniform.set(0.125f);
        }
        if (hurtPercentUniform != null) {
            hurtPercentUniform.set(hurtPercent);
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, x2, y2, 0.0f).texture(0.0f, 0.0f);
        buffer.vertex(matrix, x2, y2 + size, 0.0f).texture(0.0f, 1.0f);
        buffer.vertex(matrix, x2 + size, y2 + size, 0.0f).texture(1.0f, 1.0f);
        buffer.vertex(matrix, x2 + size, y2, 0.0f).texture(1.0f, 0.0f);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.face);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private static Identifier getSkinTexture(PlayerEntity player) {
        if (mc.getNetworkHandler() == null) {
            return DefaultSkinHelper.getSkinTextures((UUID)player.getUuid()).texture();
        }
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry != null) {
            return entry.getSkinTextures().texture();
        }
        return DefaultSkinHelper.getSkinTextures((UUID)player.getUuid()).texture();
    }

    private static Identifier getSkinTextureByName(String username) {
        String key = username.toLowerCase(Locale.ROOT);
        Identifier cachedTexture = skinCache.get(key);
        if (cachedTexture != null) {
            return cachedTexture;
        }
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                if (!entry.getProfile().getName().equalsIgnoreCase(username)) continue;
                Identifier texture = entry.getSkinTextures().texture();
                skinCache.put(key, texture);
                return texture;
            }
        }
        if (RenderUtils.mc.world != null) {
            for (PlayerEntity player : RenderUtils.mc.world.getPlayers()) {
                if (!player.getName().getString().equalsIgnoreCase(username)) continue;
                Identifier texture = RenderUtils.getSkinTexture(player);
                skinCache.put(key, texture);
                return texture;
            }
        }
        Identifier texture = DefaultSkinHelper.getSkinTextures((UUID)UUID.nameUUIDFromBytes(username.getBytes())).texture();
        skinCache.put(key, texture);
        return texture;
    }

    private static Identifier getSkinTextureByUUID(UUID uuid) {
        PlayerEntity player;
        PlayerListEntry entry;
        String key = uuid.toString();
        if (skinCache.containsKey(key)) {
            return skinCache.get(key);
        }
        if (mc.getNetworkHandler() != null && (entry = mc.getNetworkHandler().getPlayerListEntry(uuid)) != null) {
            Identifier texture = entry.getSkinTextures().texture();
            skinCache.put(key, texture);
            return texture;
        }
        if (RenderUtils.mc.world != null && (player = RenderUtils.mc.world.getPlayerByUuid(uuid)) != null) {
            Identifier texture = RenderUtils.getSkinTexture(player);
            skinCache.put(key, texture);
            return texture;
        }
        return DefaultSkinHelper.getSkinTextures((UUID)uuid).texture();
    }

    public static void clearSkinCache() {
        skinCache.clear();
    }

    public static void removeSkinFromCache(String username) {
        skinCache.remove(username.toLowerCase(Locale.ROOT));
    }

    public static void drawRoundedRect(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int color) {
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, radius, radius, radius, radius, color);
    }

    public static void drawDefaultHudElementRects(MatrixStack matrices, float x2, float y2, float width, float height, int themeColor) {
        RenderUtils.drawDefaultHudElementRects(matrices, x2, y2, width, height, themeColor, true);
    }

    public static void drawDefaultHudElementRects(MatrixStack matrices, float x2, float y2, float width, float height, int themeColor, boolean drawPattern) {
        RenderUtils.drawBlur(matrices, x2, y2, width, height, 4.0f, 5.0f, ColorUtils.rgba(255, 255, 255, 255));
        RenderUtils.drawBlur(matrices, x2, y2, width, height, 4.0f, 5.0f, ColorUtils.rgba(0, 0, 0, 150));
        if (drawPattern) {
            RenderUtils.drawHudSquarePattern(matrices, x2, y2, width, height, themeColor);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void drawHudSquarePattern(MatrixStack matrices, float x2, float y2, float width, float height, int themeColor) {
        if (width <= 6.0f || height <= 6.0f) {
            return;
        }
        float clipX = x2 - 1.0f;
        float clipY = y2 + 1.0f;
        float clipW = Math.max(1.0f, width - 2.0f);
        float clipH = Math.max(1.0f, height - 2.0f);
        float themeAlphaMul = (float)(themeColor >>> 24 & 0xFF) / 255.0f;
        if (themeAlphaMul <= 0.001f) {
            return;
        }
        if (clipH <= 20.0f) {
            float[][] compactSlots = new float[][]{{0.05f, 0.08f, 8.6f}, {0.92f, 0.1f, 8.8f}, {0.16f, 0.78f, 6.3f}, {0.77f, 0.8f, 6.5f}, {0.31f, 0.18f, 6.0f}, {0.58f, 0.74f, 5.8f}, {0.45f, 0.45f, 5.1f}, {0.86f, 0.46f, 5.3f}, {0.23f, 0.52f, 4.9f}, {0.67f, 0.3f, 5.0f}, {0.11f, 0.34f, 5.5f}, {0.38f, 0.7f, 5.2f}, {0.72f, 0.16f, 5.7f}, {0.95f, 0.68f, 5.1f}};
            float desiredCount = Math.min((float)compactSlots.length, 3.7f + Math.max(0.0f, (clipW - 84.0f) / 32.0f));
            int outlineColorBase = ColorUtils.setAlphaColor(ColorUtils.darken(themeColor, 0.62f), Math.max(0, Math.min(255, (int)(82.0f * themeAlphaMul))));
            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(clipX, clipY, clipW, clipH);
            try {
                for (int i2 = 0; i2 < compactSlots.length; ++i2) {
                    float reveal = desiredCount - (float)i2;
                    if (reveal <= 0.0f) continue;
                    float alphaMul = Math.max(0.0f, Math.min(1.0f, reveal));
                    if ((alphaMul = alphaMul * alphaMul * (3.0f - 2.0f * alphaMul)) <= 0.02f) continue;
                    float size = compactSlots[i2][2];
                    float px = clipX + 0.8f + compactSlots[i2][0] * Math.max(1.0f, clipW - size + 1.6f);
                    float py = clipY - 1.2f + compactSlots[i2][1] * Math.max(1.0f, clipH - size + 2.4f);
                    int outlineAlpha = Math.max(0, Math.min(255, (int)(86.0f * alphaMul * themeAlphaMul)));
                    if (outlineAlpha <= 0) continue;
                    int outlineColor = ColorUtils.setAlphaColor(outlineColorBase, outlineAlpha);
                    RenderUtils.drawRoundedRectOutline(matrices, px, py, size, size, 0.0f, 0.5f, outlineColor, outlineColor, outlineColor, outlineColor);
                }
            }
            finally {
                ScissorUtils.unset();
                ScissorUtils.pop();
            }
            return;
        }
        float[][] slots = new float[][]{{0.05f, 4.0f, 9.6f}, {0.87f, 4.0f, 9.2f}, {0.5f, 8.0f, 7.4f}, {0.18f, 13.0f, 6.2f}, {0.72f, 13.0f, 6.0f}, {0.07f, 21.0f, 5.6f}, {0.91f, 21.0f, 5.8f}, {0.24f, 30.0f, 5.4f}, {0.66f, 30.0f, 5.5f}, {0.04f, 38.0f, 6.8f}, {0.9f, 38.0f, 7.0f}, {0.15f, 47.0f, 5.4f}, {0.78f, 47.0f, 5.5f}, {0.08f, 56.0f, 5.1f}, {0.92f, 56.0f, 5.2f}, {0.23f, 65.0f, 5.8f}, {0.69f, 65.0f, 5.9f}, {0.52f, 71.0f, 7.2f}, {0.06f, 74.0f, 7.6f}, {0.88f, 74.0f, 7.4f}, {0.14f, 85.0f, 5.7f}, {0.82f, 85.0f, 5.8f}, {0.09f, 97.0f, 6.5f}, {0.9f, 98.0f, 6.6f}};
        int baseCount = 10;
        float extraHeight = Math.max(0.0f, clipH - 24.0f);
        float desiredCount = Math.min((float)slots.length, (float)baseCount + extraHeight / 10.0f);
        float panelAlpha = Math.max(0.0f, Math.min(1.0f, (clipH - 10.0f) / 16.0f));
        panelAlpha = panelAlpha * panelAlpha * (3.0f - 2.0f * panelAlpha);
        int outlineColorBase = ColorUtils.setAlphaColor(ColorUtils.darken(themeColor, 0.72f), Math.max(0, Math.min(255, (int)(40.0f * themeAlphaMul))));
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(clipX, clipY, clipW, clipH);
        try {
            for (int i3 = 0; i3 < slots.length; ++i3) {
                int outlineAlpha;
                float reveal = desiredCount - (float)i3;
                if (reveal <= 0.0f) continue;
                float alphaMul = Math.max(0.0f, Math.min(1.0f, reveal));
                alphaMul = alphaMul * alphaMul * (3.0f - 2.0f * alphaMul);
                if ((alphaMul *= panelAlpha) <= 0.015f) continue;
                float size = slots[i3][2];
                float px = clipX + 2.0f + slots[i3][0] * Math.max(1.0f, clipW - size - 4.0f);
                float py = clipY + slots[i3][1];
                float bottomLimit = clipY + clipH - 1.0f;
                if (py >= bottomLimit) continue;
                if (py + size > bottomLimit) {
                    float visible = Math.max(0.0f, Math.min(1.0f, (bottomLimit - py) / Math.max(1.0f, size)));
                    if ((alphaMul *= (visible = visible * visible * (3.0f - 2.0f * visible))) <= 0.015f) continue;
                }
                if ((outlineAlpha = Math.max(0, Math.min(255, (int)(58.0f * alphaMul * themeAlphaMul)))) <= 0) continue;
                int outlineColor = ColorUtils.setAlphaColor(outlineColorBase, outlineAlpha);
                RenderUtils.drawRoundedRectOutline(matrices, px, py, size, size, 0.0f, 0.55f, outlineColor, outlineColor, outlineColor, outlineColor);
            }
        }
        finally {
            ScissorUtils.unset();
            ScissorUtils.pop();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void drawHudSquarePatternOld(MatrixStack matrices, float x2, float y2, float width, float height, int themeColor) {
        if (width <= 6.0f || height <= 6.0f) {
            return;
        }
        float clipX = x2 - 1.0f;
        float clipY = y2 + 1.0f;
        float clipW = Math.max(1.0f, width - 2.0f);
        float clipH = Math.max(1.0f, height - 2.0f);
        float themeAlphaMul = (float)(themeColor >>> 24 & 0xFF) / 255.0f;
        if (themeAlphaMul <= 0.001f) {
            return;
        }
        if (clipH <= 20.0f) {
            float[][] compactSlots = new float[][]{{0.05f, 0.08f, 8.6f}, {0.92f, 0.1f, 8.8f}, {0.16f, 0.78f, 6.3f}, {0.77f, 0.8f, 6.5f}, {0.31f, 0.18f, 6.0f}, {0.58f, 0.74f, 5.8f}, {0.45f, 0.45f, 5.1f}, {0.86f, 0.46f, 5.3f}, {0.23f, 0.52f, 4.9f}, {0.67f, 0.3f, 5.0f}, {0.11f, 0.34f, 5.5f}, {0.38f, 0.7f, 5.2f}, {0.72f, 0.16f, 5.7f}, {0.95f, 0.68f, 5.1f}};
            float desiredCount = Math.min((float)compactSlots.length, 3.7f + Math.max(0.0f, (clipW - 84.0f) / 32.0f));
            int outlineColorBase = ColorUtils.setAlphaColor(ColorUtils.darken(themeColor, 0.62f), Math.max(0, Math.min(255, (int)(82.0f * themeAlphaMul))));
            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(clipX, clipY, clipW, clipH);
            try {
                for (int i2 = 0; i2 < compactSlots.length; ++i2) {
                    float reveal = desiredCount - (float)i2;
                    if (reveal <= 0.0f) continue;
                    float alphaMul = Math.max(0.0f, Math.min(1.0f, reveal));
                    if ((alphaMul = alphaMul * alphaMul * (3.0f - 2.0f * alphaMul)) <= 0.02f) continue;
                    float size = compactSlots[i2][2];
                    float px = clipX + 0.8f + compactSlots[i2][0] * Math.max(1.0f, clipW - size + 1.6f);
                    float py = clipY - 1.2f + compactSlots[i2][1] * Math.max(1.0f, clipH - size + 2.4f);
                    int outlineAlpha = Math.max(0, Math.min(255, (int)(86.0f * alphaMul * themeAlphaMul)));
                    if (outlineAlpha <= 0) continue;
                    int outlineColor = ColorUtils.setAlphaColor(outlineColorBase, outlineAlpha);
                    RenderUtils.drawRoundedRectOutline(matrices, px, py, size, size, 0.0f, 0.5f, outlineColor, outlineColor, outlineColor, outlineColor);
                }
            }
            finally {
                ScissorUtils.unset();
                ScissorUtils.pop();
            }
            return;
        }
        float[][] slots = new float[][]{{0.05f, 4.0f, 9.6f}, {0.87f, 4.0f, 9.2f}, {0.5f, 8.0f, 7.4f}, {0.18f, 13.0f, 6.2f}, {0.72f, 13.0f, 6.0f}, {0.07f, 21.0f, 5.6f}, {0.91f, 21.0f, 5.8f}, {0.24f, 30.0f, 5.4f}, {0.66f, 30.0f, 5.5f}, {0.04f, 38.0f, 6.8f}, {0.9f, 38.0f, 7.0f}, {0.15f, 47.0f, 5.4f}, {0.78f, 47.0f, 5.5f}, {0.08f, 56.0f, 5.1f}, {0.92f, 56.0f, 5.2f}, {0.23f, 65.0f, 5.8f}, {0.69f, 65.0f, 5.9f}, {0.52f, 71.0f, 7.2f}, {0.06f, 74.0f, 7.6f}, {0.88f, 74.0f, 7.4f}, {0.14f, 85.0f, 5.7f}, {0.82f, 85.0f, 5.8f}, {0.09f, 97.0f, 6.5f}, {0.9f, 98.0f, 6.6f}};
        int baseCount = 10;
        float extraHeight = Math.max(0.0f, clipH - 24.0f);
        float desiredCount = Math.min((float)slots.length, (float)baseCount + extraHeight / 10.0f);
        float panelAlpha = Math.max(0.0f, Math.min(1.0f, (clipH - 10.0f) / 16.0f));
        panelAlpha = panelAlpha * panelAlpha * (3.0f - 2.0f * panelAlpha);
        int outlineColorBase = ColorUtils.setAlphaColor(ColorUtils.darken(themeColor, 0.72f), Math.max(0, Math.min(255, (int)(40.0f * themeAlphaMul))));
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(clipX, clipY, clipW, clipH);
        try {
            for (int i3 = 0; i3 < slots.length; ++i3) {
                int outlineAlpha;
                float reveal = desiredCount - (float)i3;
                if (reveal <= 0.0f) continue;
                float alphaMul = Math.max(0.0f, Math.min(1.0f, reveal));
                alphaMul = alphaMul * alphaMul * (3.0f - 2.0f * alphaMul);
                if ((alphaMul *= panelAlpha) <= 0.015f) continue;
                float size = slots[i3][2];
                float px = clipX + 2.0f + slots[i3][0] * Math.max(1.0f, clipW - size - 4.0f);
                float py = clipY + slots[i3][1];
                float bottomLimit = clipY + clipH - 1.0f;
                if (py >= bottomLimit) continue;
                if (py + size > bottomLimit) {
                    float visible = Math.max(0.0f, Math.min(1.0f, (bottomLimit - py) / Math.max(1.0f, size)));
                    if ((alphaMul *= (visible = visible * visible * (3.0f - 2.0f * visible))) <= 0.015f) continue;
                }
                if ((outlineAlpha = Math.max(0, Math.min(255, (int)(58.0f * alphaMul * themeAlphaMul)))) <= 0) continue;
                int outlineColor = ColorUtils.setAlphaColor(outlineColorBase, outlineAlpha);
                RenderUtils.drawRoundedRectOutline(matrices, px, py, size, size, 0.0f, 0.55f, outlineColor, outlineColor, outlineColor, outlineColor);
            }
        }
        finally {
            ScissorUtils.unset();
            ScissorUtils.pop();
        }
    }

    public static void drawLine(MatrixStack matrices, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length < 0.01f) {
            return;
        }
        float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
        matrices.push();
        matrices.translate(x1, y1, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        RenderUtils.drawRoundedRect(matrices, 0.0f, -thickness / 2.0f, length, thickness, thickness / 2.0f, color);
        matrices.pop();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void drawHudStripePattern(MatrixStack matrices, float x2, float y2, float width, float height, int themeColor) {
        if (width <= 6.0f || height <= 6.0f) {
            return;
        }
        float themeAlphaMul = (float)(themeColor >>> 24 & 0xFF) / 255.0f;
        if (themeAlphaMul <= 0.001f) {
            return;
        }
        int stripeColor = ColorUtils.setAlphaColor(ColorUtils.darken(themeColor, 0.65f), Math.max(0, Math.min(255, (int)(45.0f * themeAlphaMul))));
        float stripeWidth = 2.0f;
        float stripeSpacing = 6.0f;
        float stripeAngle = 45.0f;
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(x2, y2, width, height);
        try {
            float diagonal = (float)Math.sqrt(width * width + height * height);
            int numStripes = (int)((diagonal + stripeSpacing) / stripeSpacing) + 2;
            for (int i2 = -1; i2 < numStripes; ++i2) {
                float offset = (float)i2 * stripeSpacing;
                float x1 = x2 + offset;
                float y1 = y2 + height;
                float x22 = x2 + offset + height;
                float y22 = y2;
                RenderUtils.drawLine(matrices, x1, y1, x22, y22, stripeWidth, stripeColor);
            }
        }
        finally {
            ScissorUtils.unset();
            ScissorUtils.pop();
        }
    }

    public static void drawDefaultHudInfoBox(MatrixStack matrices, float x2, float y2, float width, int outerColor, int innerColor) {
        RenderUtils.drawRoundedRect(matrices, x2 - 0.25f, y2 - 1.25f, width + 0.5f, 9.0f, 1.3f, outerColor);
        RenderUtils.drawRoundedRect(matrices, x2, y2 - 1.0f, width, 8.5f, 1.0f, innerColor);
    }

    public static void drawDefaultHudPanel(MatrixStack matrices, float x2, float y2, float width, float height, float gradientRadius, float borderRadius, int borderColor, int topColor, int bottomColor) {
        RenderUtils.drawDefaultHudPanelGlass(matrices, x2, y2, width, height, borderRadius);
    }

    public static void drawDefaultHudPanel1(MatrixStack matrices, float x2, float y2, float width, float height, float gradientRadius, float borderRadius, int borderColor, int topColor, int bottomColor) {
        RenderUtils.drawDefaultHudPanelGlass(matrices, x2, y2, width, height, borderRadius);
    }

    public static void drawDefaultHudThemedPanel(MatrixStack matrices, float x2, float y2, float width, float height, float gradientRadius, float borderRadius, int themeColor) {
        RenderUtils.drawDefaultHudPanelGlass(matrices, x2, y2, width, height, borderRadius);
    }

    private static void drawDefaultHudPanelGlass(MatrixStack matrices, float x2, float y2, float width, float height, float borderRadius) {
        RenderUtils.drawBlur(matrices, x2 - 0.5f, y2 - 0.5f, width + 1.0f, height + 1.0f, 3.0f, ColorUtils.rgba(255, 255, 255, 255));
        RenderUtils.drawBlur(matrices, x2 - 0.5f, y2 - 0.5f, width + 1.0f, height + 1.0f, 3.0f, ColorUtils.rgba(0, 0, 0, 150));
    }

    public static void drawWaveHudHeader(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float shadowRadius, float shadowSoftness, int leftTop, int leftBottom, int centerTop, int centerBottom, int rightTop, int rightBottom) {
        RenderUtils.drawShadow6(matrices, x2, y2, width, height, shadowRadius, shadowSoftness, leftTop, leftBottom, centerTop, centerBottom, rightTop, rightBottom);
        RenderUtils.drawGradient6Rect(matrices, x2, y2, width, height, radius, leftTop, leftBottom, centerTop, centerBottom, rightTop, rightBottom);
    }

    public static void drawWaveHudPanel(MatrixStack matrices, float x2, float y2, float width, float height, int bgColor, float headerHeight, float headerRadius, float shadowRadius, float shadowSoftness, int leftTop, int leftBottom, int centerTop, int centerBottom, int rightTop, int rightBottom) {
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 0.0f, bgColor);
        RenderUtils.drawWaveHudHeader(matrices, x2, y2, width, headerHeight, headerRadius, shadowRadius, shadowSoftness, leftTop, leftBottom, centerTop, centerBottom, rightTop, rightBottom);
    }

    public static void drawTargetHudWaveFrame(MatrixStack matrices, float x2, float y2, float width, float height, float padding, float entityBoxSize, float alpha) {
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, 0.0f, ColorUtils.applyAlpha(ColorUtils.rgba(40, 40, 40, 255), alpha));
        RenderUtils.drawRoundedRect(matrices, x2 + padding, y2 + padding, width - padding * 2.0f, height - padding * 2.0f, 0.0f, ColorUtils.applyAlpha(ColorUtils.rgba(20, 20, 20, 255), alpha));
        RenderUtils.drawRoundedRect(matrices, x2 + padding + 2.0f, y2 + padding + 2.0f, entityBoxSize, entityBoxSize, 0.0f, ColorUtils.applyAlpha(ColorUtils.rgba(40, 40, 40, 255), alpha));
        RenderUtils.drawRoundedRect(matrices, x2 + padding + 3.0f, y2 + padding + 3.0f, entityBoxSize - 2.0f, entityBoxSize - 2.0f, 0.0f, ColorUtils.applyAlpha(ColorUtils.rgba(25, 25, 25, 255), alpha));
    }

    public static void drawTargetHudDefaultPlaceholder(MatrixStack matrices, float x2, float y2, float alpha) {
        RenderUtils.drawRoundedRect(matrices, x2 - 1.0f, y2 - 1.0f, 22.0f, 22.0f, 1.0f, ColorUtils.applyAlpha(ColorUtils.rgba(21, 21, 21, 255), alpha));
    }

    public static void drawTargetHudHealthBars(MatrixStack matrices, float x2, float y2, float width, float trailProgress, float progress, int themeColor, int themecolor2, float alpha) {
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, 5.5f, 1.25f, ColorUtils.applyAlpha(ColorUtils.darken(themeColor, 0.5f), alpha * 0.8f));
        RenderUtils.drawRoundedRect(matrices, x2, y2, width * trailProgress, 5.5f, 1.25f, ColorUtils.applyAlpha(ColorUtils.darken(themeColor, 0.8f), alpha * 0.8f));
        RenderUtils.drawGradientRect(matrices, x2, y2, width * progress, 5.5f, 1.25f, ColorUtils.applyAlpha(themeColor, alpha), ColorUtils.applyAlpha(themecolor2, alpha), true);
    }

    public static void drawTargetHudGoldenBars(MatrixStack matrices, float x2, float y2, float width, float height, float trailProgress, float progress, float alpha, float goldenAlpha) {
        int goldenColor = ColorUtils.rgba(255, 215, 0, 255);
        RenderUtils.drawRoundedRect(matrices, x2, y2, width * trailProgress, height, 1.25f, ColorUtils.applyAlpha(ColorUtils.darken(goldenColor, 0.65f), alpha * goldenAlpha * 0.8f));
        RenderUtils.drawGradientRect(matrices, x2, y2, width * progress, height, 1.25f, ColorUtils.applyAlpha(ColorUtils.darken(goldenColor, 0.55f), alpha * goldenAlpha), ColorUtils.applyAlpha(goldenColor, alpha * goldenAlpha), true);
    }

    public static void drawTargetHudHeartBase(MatrixStack matrices, float x2, float y2, float alpha) {
        RenderUtils.drawRoundedRect(matrices, x2, y2, 6.2f, 4.5f, 0.0f, ColorUtils.applyAlpha(ColorUtils.rgba(0, 0, 0, 255), alpha));
    }

    public static void drawTargetHudHeartFill(MatrixStack matrices, float x2, float y2, float width, int heartColor, int shadowColor) {
        RenderUtils.drawShadow(matrices, x2 + 1.0f, y2 + 1.0f, width, 2.0f, 0.0f, 8.0f, shadowColor);
        RenderUtils.drawRoundedRect(matrices, x2, y2, width + 1.2f, 4.5f, 0.0f, heartColor);
    }

    public static void drawKeyStrokeRect(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int color) {
        RenderUtils.drawRoundedRect(matrices, x2, y2, width, height, radius, color);
    }

    public static void drawRoundedRect(MatrixStack matrices, float x2, float y2, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.roundedRect);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        GlUniform sizeUniform = shader.getUniform("Size");
        GlUniform radiusUniform = shader.getUniform("Radius");
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int alpha = color >> 24 & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        float r2 = (float)(color >> 16 & 0xFF) / 255.0f;
        float g2 = (float)(color >> 8 & 0xFF) / 255.0f;
        float b2 = (float)(color & 0xFF) / 255.0f;
        float a2 = (float)alpha / 255.0f;
        buffer.vertex(matrix, x2, y2, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2, y2 + height, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2 + width, y2 + height, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2 + width, y2, 0.0f).color(r2, g2, b2, a2);
        if (sizeUniform != null) {
            sizeUniform.set(width, height);
        }
        if (radiusUniform != null) {
            radiusUniform.set(topLeft, topRight, bottomRight, bottomLeft);
        }
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.roundedRect);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawRoundCircle(MatrixStack matrices, float x2, float y2, float radius, int color) {
        RenderUtils.drawRoundedRect(matrices, x2 - radius / 2.0f, y2 - radius / 2.0f, radius, radius, radius / 2.0f - 0.5f, color);
    }

    public static void drawRingArc(MatrixStack matrices, float x2, float y2, float size, float thickness, float startDeg, float endDeg, int color) {
        if (size <= 0.0f || thickness <= 0.0f) {
            return;
        }
        float radius = size / 2.0f;
        float start = (float)Math.toRadians(startDeg);
        float end = (float)Math.toRadians(endDeg);
        float twoPi = (float)Math.PI * 2;
        if (start < 0.0f) {
            start += twoPi;
        }
        if (end < 0.0f) {
            end += twoPi;
        }
        while (end < start) {
            end += twoPi;
        }
        if (end - start <= 1.0E-4f) {
            end = start + twoPi;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.ringArc);
        GlUniform sizeUniform = shader.getUniform("Size");
        GlUniform radiusUniform = shader.getUniform("Radius");
        GlUniform thicknessUniform = shader.getUniform("Thickness");
        GlUniform startUniform = shader.getUniform("StartAngle");
        GlUniform endUniform = shader.getUniform("EndAngle");
        GlUniform smoothnessUniform = shader.getUniform("Smoothness");
        GlUniform colorModulatorUniform = shader.getUniform("ColorModulator");
        if (sizeUniform != null) {
            sizeUniform.set(size, size);
        }
        if (radiusUniform != null) {
            radiusUniform.set(radius);
        }
        if (thicknessUniform != null) {
            thicknessUniform.set(thickness);
        }
        if (startUniform != null) {
            startUniform.set(start);
        }
        if (endUniform != null) {
            endUniform.set(end);
        }
        if (smoothnessUniform != null) {
            smoothnessUniform.set(Math.min(1.0f, thickness * 0.5f));
        }
        if (colorModulatorUniform != null) {
            colorModulatorUniform.set(1.0f, 1.0f, 1.0f, 1.0f);
        }
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int alpha = color >> 24 & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        float r2 = (float)(color >> 16 & 0xFF) / 255.0f;
        float g2 = (float)(color >> 8 & 0xFF) / 255.0f;
        float b2 = (float)(color & 0xFF) / 255.0f;
        float a2 = (float)alpha / 255.0f;
        buffer.vertex(matrix, x2, y2, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2, y2 + size, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2 + size, y2 + size, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2 + size, y2, 0.0f).color(r2, g2, b2, a2);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.ringArc);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawGradientRect(MatrixStack matrices, float x2, float y2, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        int brAlpha;
        int trAlpha;
        int blAlpha;
        int tlAlpha;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.gradientRect);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        GlUniform sizeUniform = shader.getUniform("Size");
        GlUniform radiusUniform = shader.getUniform("Radius");
        GlUniform smoothnessUniform = shader.getUniform("Smoothness");
        GlUniform colorModulatorUniform = shader.getUniform("ColorModulator");
        GlUniform topLeftColorUniform = shader.getUniform("TopLeftColor");
        GlUniform bottomLeftColorUniform = shader.getUniform("BottomLeftColor");
        GlUniform topRightColorUniform = shader.getUniform("TopRightColor");
        GlUniform bottomRightColorUniform = shader.getUniform("BottomRightColor");
        if (sizeUniform != null) {
            sizeUniform.set(width, height);
        }
        if (radiusUniform != null) {
            radiusUniform.set(topLeft, topRight, bottomRight, bottomLeft);
        }
        if (smoothnessUniform != null) {
            smoothnessUniform.set(1.0f);
        }
        if (colorModulatorUniform != null) {
            colorModulatorUniform.set(1.0f, 1.0f, 1.0f, 1.0f);
        }
        if ((tlAlpha = topLeftColor >> 24 & 0xFF) == 0) {
            tlAlpha = 255;
        }
        if (topLeftColorUniform != null) {
            topLeftColorUniform.set((float)(topLeftColor >> 16 & 0xFF) / 255.0f, (float)(topLeftColor >> 8 & 0xFF) / 255.0f, (float)(topLeftColor & 0xFF) / 255.0f, (float)tlAlpha / 255.0f);
        }
        if ((blAlpha = bottomLeftColor >> 24 & 0xFF) == 0) {
            blAlpha = 255;
        }
        if (bottomLeftColorUniform != null) {
            bottomLeftColorUniform.set((float)(bottomLeftColor >> 16 & 0xFF) / 255.0f, (float)(bottomLeftColor >> 8 & 0xFF) / 255.0f, (float)(bottomLeftColor & 0xFF) / 255.0f, (float)blAlpha / 255.0f);
        }
        if ((trAlpha = topRightColor >> 24 & 0xFF) == 0) {
            trAlpha = 255;
        }
        if (topRightColorUniform != null) {
            topRightColorUniform.set((float)(topRightColor >> 16 & 0xFF) / 255.0f, (float)(topRightColor >> 8 & 0xFF) / 255.0f, (float)(topRightColor & 0xFF) / 255.0f, (float)trAlpha / 255.0f);
        }
        if ((brAlpha = bottomRightColor >> 24 & 0xFF) == 0) {
            brAlpha = 255;
        }
        if (bottomRightColorUniform != null) {
            bottomRightColorUniform.set((float)(bottomRightColor >> 16 & 0xFF) / 255.0f, (float)(bottomRightColor >> 8 & 0xFF) / 255.0f, (float)(bottomRightColor & 0xFF) / 255.0f, (float)brAlpha / 255.0f);
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, x2, y2, 0.0f).texture(0.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(matrix, x2, y2 + height, 0.0f).texture(0.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(matrix, x2 + width, y2 + height, 0.0f).texture(1.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(matrix, x2 + width, y2, 0.0f).texture(1.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.gradientRect);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawGradientRect(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        RenderUtils.drawGradientRect(matrices, x2, y2, width, height, radius, radius, radius, radius, topLeftColor, topRightColor, bottomLeftColor, bottomRightColor);
    }

    public static void drawGradientRect(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int topColor, int bottomColor) {
        RenderUtils.drawGradientRect(matrices, x2, y2, width, height, radius, radius, radius, radius, topColor, topColor, bottomColor, bottomColor);
    }

    public static void drawGradientRect(MatrixStack matrices, float x2, float y2, float width, float height, int topColor, int bottomColor) {
        RenderUtils.drawGradientRect(matrices, x2, y2, width, height, 0.0f, 0.0f, 0.0f, 0.0f, topColor, topColor, bottomColor, bottomColor);
    }

    public static void drawGradientRect(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int leftColor, int rightColor, boolean horizontal) {
        if (horizontal) {
            RenderUtils.drawGradientRect(matrices, x2, y2, width, height, radius, radius, radius, radius, leftColor, rightColor, leftColor, rightColor);
        } else {
            RenderUtils.drawGradientRect(matrices, x2, y2, width, height, radius, radius, radius, radius, leftColor, leftColor, rightColor, rightColor);
        }
    }

    public static void drawRoundedRectOutline(MatrixStack matrices, float x2, float y2, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, float outline, int outlineColor) {
        RenderUtils.drawRoundedRectOutline(matrices, x2, y2, width, height, topLeft, topRight, bottomRight, bottomLeft, outline, outlineColor, outlineColor, outlineColor, outlineColor);
    }

    public static void drawRoundedRectOutline(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float outline, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        RenderUtils.drawRoundedRectOutline(matrices, x2, y2, width, height, radius, radius, radius, radius, outline, topLeftColor, topRightColor, bottomLeftColor, bottomRightColor);
    }

    public static void drawRoundedRectOutline(MatrixStack matrices, float x2, float y2, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, float outline, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        int a2;
        if (outline <= 0.0f) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.roundedRectOutline);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        GlUniform sizeUniform = shader.getUniform("Size");
        GlUniform radiusUniform = shader.getUniform("Radius");
        GlUniform smoothnessUniform = shader.getUniform("Smoothness");
        GlUniform colorModulatorUniform = shader.getUniform("ColorModulator");
        GlUniform outlineUniform = shader.getUniform("Outline");
        GlUniform topLeftColorUniform = shader.getUniform("TopLeftColor");
        GlUniform bottomLeftColorUniform = shader.getUniform("BottomLeftColor");
        GlUniform topRightColorUniform = shader.getUniform("TopRightColor");
        GlUniform bottomRightColorUniform = shader.getUniform("BottomRightColor");
        if (sizeUniform != null) {
            sizeUniform.set(width, height);
        }
        if (radiusUniform != null) {
            radiusUniform.set(topLeft, topRight, bottomRight, bottomLeft);
        }
        if (smoothnessUniform != null) {
            smoothnessUniform.set(1.0f);
        }
        if (colorModulatorUniform != null) {
            colorModulatorUniform.set(1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (outlineUniform != null) {
            outlineUniform.set(outline);
        }
        if (topLeftColorUniform != null) {
            a2 = topLeftColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            topLeftColorUniform.set((float)(topLeftColor >> 16 & 0xFF) / 255.0f, (float)(topLeftColor >> 8 & 0xFF) / 255.0f, (float)(topLeftColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (bottomLeftColorUniform != null) {
            a2 = bottomLeftColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            bottomLeftColorUniform.set((float)(bottomLeftColor >> 16 & 0xFF) / 255.0f, (float)(bottomLeftColor >> 8 & 0xFF) / 255.0f, (float)(bottomLeftColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (topRightColorUniform != null) {
            a2 = topRightColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            topRightColorUniform.set((float)(topRightColor >> 16 & 0xFF) / 255.0f, (float)(topRightColor >> 8 & 0xFF) / 255.0f, (float)(topRightColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        if (bottomRightColorUniform != null) {
            a2 = bottomRightColor >> 24 & 0xFF;
            if (a2 == 0) {
                a2 = 255;
            }
            bottomRightColorUniform.set((float)(bottomRightColor >> 16 & 0xFF) / 255.0f, (float)(bottomRightColor >> 8 & 0xFF) / 255.0f, (float)(bottomRightColor & 0xFF) / 255.0f, (float)a2 / 255.0f);
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x2, y2, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(matrix, x2, y2 + height, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(matrix, x2 + width, y2 + height, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(matrix, x2 + width, y2, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.roundedRectOutline);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawBlur(MatrixStack matrices, float x2, float y2, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, int color) {
        if (RenderUtils.getLiquidBlurTexture() == 0) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.roundedTexture);
        GlUniform sizeUniform = shader.getUniform("Size");
        GlUniform radiusUniform = shader.getUniform("Radius");
        GlUniform smoothnessUniform = shader.getUniform("Smoothness");
        GlUniform colorModulatorUniform = shader.getUniform("ColorModulator");
        if (sizeUniform != null) {
            sizeUniform.set(width, height);
        }
        if (radiusUniform != null) {
            radiusUniform.set(topLeft, topRight, bottomRight, bottomLeft);
        }
        if (smoothnessUniform != null) {
            smoothnessUniform.set(1.0f);
        }
        if (colorModulatorUniform != null) {
            colorModulatorUniform.set(1.0f, 1.0f, 1.0f, 1.0f);
        }
        RenderSystem.setShaderTexture((int)0, (int)RenderUtils.getLiquidBlurTexture());
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.roundedTexture);
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float u1 = x2 / (float)screenWidth;
        float v1 = ((float)screenHeight - y2) / (float)screenHeight;
        float u2 = (x2 + width) / (float)screenWidth;
        float v2 = ((float)screenHeight - y2 - height) / (float)screenHeight;
        int alpha = color >> 24 & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        float r2 = (float)(color >> 16 & 0xFF) / 255.0f;
        float g2 = (float)(color >> 8 & 0xFF) / 255.0f;
        float b2 = (float)(color & 0xFF) / 255.0f;
        float a2 = (float)alpha / 255.0f;
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix, x2, y2, 0.0f).texture(u1, v1).color(r2, g2, b2, a2);
        builder.vertex(matrix, x2, y2 + height, 0.0f).texture(u1, v2).color(r2, g2, b2, a2);
        builder.vertex(matrix, x2 + width, y2 + height, 0.0f).texture(u2, v2).color(r2, g2, b2, a2);
        builder.vertex(matrix, x2 + width, y2, 0.0f).texture(u2, v1).color(r2, g2, b2, a2);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)builder.end());
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.disableBlend();
    }

    public static void drawBlur(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int color) {
        RenderUtils.drawBlur(matrices, x2, y2, width, height, radius, radius, radius, radius, color);
    }

    public static void startGlow(float radius, int color, GlowCallback callback, MatrixStack matrices) {
        int a2 = color >> 24 & 0xFF;
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        if (a2 == 0) {
            a2 = 255;
        }
        GlowProgram.getInstance().begin(radius, new Color(r2, g2, b2, a2));
        callback.render();
        GlowProgram.getInstance().end(matrices, callback);
    }

    public static void startGlow(float radius, float intensity, int color, GlowCallback callback, MatrixStack matrices) {
        int a2 = color >> 24 & 0xFF;
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        if (a2 == 0) {
            a2 = 255;
        }
        GlowProgram.getInstance().begin(radius, intensity, new Color(r2, g2, b2, a2));
        callback.render();
        GlowProgram.getInstance().end(matrices, callback);
    }

    public static void drawBlur1(MatrixStack matrices, float x2, float y2, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, int color) {
        int tex = RenderUtils.getLiquidBlurTexture();
        if (tex == 0) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.roundedTexture);
        GlUniform sizeUniform = shader.getUniform("Size");
        GlUniform radiusUniform = shader.getUniform("Radius");
        GlUniform smoothnessUniform = shader.getUniform("Smoothness");
        GlUniform colorModulatorUniform = shader.getUniform("ColorModulator");
        if (sizeUniform != null) {
            sizeUniform.set(width, height);
        }
        if (radiusUniform != null) {
            radiusUniform.set(topLeft, topRight, bottomRight, bottomLeft);
        }
        if (smoothnessUniform != null) {
            smoothnessUniform.set(1.0f);
        }
        if (colorModulatorUniform != null) {
            colorModulatorUniform.set(1.0f, 1.0f, 1.0f, 1.0f);
        }
        RenderSystem.setShaderTexture((int)0, (int)tex);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.roundedTexture);
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float u1 = x2 / (float)screenWidth;
        float v1 = ((float)screenHeight - y2) / (float)screenHeight;
        float u2 = (x2 + width) / (float)screenWidth;
        float v2 = ((float)screenHeight - y2 - height) / (float)screenHeight;
        int alpha = color >> 24 & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        float r2 = (float)(color >> 16 & 0xFF) / 255.0f;
        float g2 = (float)(color >> 8 & 0xFF) / 255.0f;
        float b2 = (float)(color & 0xFF) / 255.0f;
        float a2 = (float)alpha / 255.0f;
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix, x2, y2, 0.0f).texture(u1, v1).color(r2, g2, b2, a2);
        builder.vertex(matrix, x2, y2 + height, 0.0f).texture(u1, v2).color(r2, g2, b2, a2);
        builder.vertex(matrix, x2 + width, y2 + height, 0.0f).texture(u2, v2).color(r2, g2, b2, a2);
        builder.vertex(matrix, x2 + width, y2, 0.0f).texture(u2, v1).color(r2, g2, b2, a2);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)builder.end());
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.disableBlend();
    }

    public static void drawBlur(MatrixStack matrices, float x2, float y2, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, float blurStrength, int color) {
        RenderUtils.setLiquidBlurOffset(blurStrength);
        RenderUtils.drawBlur(matrices, x2, y2, width, height, topLeft, topRight, bottomRight, bottomLeft, color);
    }

    public static void drawBlur(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float blurStrength, int color) {
        RenderUtils.drawBlur(matrices, x2, y2, width, height, radius, radius, radius, radius, blurStrength, color);
    }

    public static void drawBlurRect(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int color, float blurStrength) {
        RenderUtils.drawBlur(matrices, x2, y2, width, height, radius, blurStrength, color);
    }

    public static void drawBlurRect(MatrixStack matrices, float x2, float y2, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, int color, float blurStrength) {
        RenderUtils.drawBlur(matrices, x2, y2, width, height, topLeft, topRight, bottomRight, bottomLeft, blurStrength, color);
    }

    public static void drawBlurRect(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int color, int color2, float blurStrength) {
        RenderUtils.drawBlur(matrices, x2, y2, width, height, radius, blurStrength, color);
    }

    public static void drawBlurRect(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int color, int color2, int color3, int color4, float blurStrength) {
        RenderUtils.drawBlur(matrices, x2, y2, width, height, radius, blurStrength, color);
    }

    public static void drawLiquidGlass(MatrixStack matrices, float x2, float y2, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, int color, float globalAlpha, float fresnelPower, int fresnelColor, float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength, float squirt, boolean clean) {
        int fAlpha;
        int textureId;
        if (clean) {
            textureId = mc.getFramebuffer().getColorAttachment();
        } else {
            RenderUtils.requestLiquidBlur();
            textureId = RenderUtils.getLiquidBlurTexture();
            if (textureId == 0) {
                textureId = mc.getFramebuffer().getColorAttachment();
            }
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture((int)0, (int)textureId);
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.liquidGlass);
        GlUniform globalAlphaUniform = shader.getUniform("GlobalAlpha");
        GlUniform sizeUniform = shader.getUniform("Size");
        GlUniform radiusUniform = shader.getUniform("Radius");
        GlUniform smoothnessUniform = shader.getUniform("Smoothness");
        GlUniform fresnelPowerUniform = shader.getUniform("FresnelPower");
        GlUniform fresnelColorUniform = shader.getUniform("FresnelColor");
        GlUniform fresnelAlphaUniform = shader.getUniform("FresnelAlpha");
        GlUniform baseAlphaUniform = shader.getUniform("BaseAlpha");
        GlUniform fresnelInvertUniform = shader.getUniform("FresnelInvert");
        GlUniform fresnelMixUniform = shader.getUniform("FresnelMix");
        GlUniform distortStrengthUniform = shader.getUniform("DistortStrength");
        GlUniform cornerSmoothnessUniform = shader.getUniform("CornerSmoothness");
        if (globalAlphaUniform != null) {
            globalAlphaUniform.set(globalAlpha);
        }
        if (sizeUniform != null) {
            sizeUniform.set(width, height);
        }
        if (radiusUniform != null) {
            radiusUniform.set(topLeft, topRight, bottomRight, bottomLeft);
        }
        if (smoothnessUniform != null) {
            smoothnessUniform.set(1.0f);
        }
        if (fresnelPowerUniform != null) {
            fresnelPowerUniform.set(fresnelPower);
        }
        if ((fAlpha = fresnelColor >> 24 & 0xFF) == 0) {
            fAlpha = 255;
        }
        if (fresnelColorUniform != null) {
            fresnelColorUniform.set((float)(fresnelColor >> 16 & 0xFF) / 255.0f, (float)(fresnelColor >> 8 & 0xFF) / 255.0f, (float)(fresnelColor & 0xFF) / 255.0f);
        }
        if (fresnelAlphaUniform != null) {
            fresnelAlphaUniform.set((float)fAlpha / 255.0f);
        }
        if (baseAlphaUniform != null) {
            baseAlphaUniform.set(baseAlpha);
        }
        if (fresnelInvertUniform != null) {
            fresnelInvertUniform.set(fresnelInvert ? 1 : 0);
        }
        if (fresnelMixUniform != null) {
            fresnelMixUniform.set(fresnelMix);
        }
        if (distortStrengthUniform != null) {
            distortStrengthUniform.set(distortStrength);
        }
        if (cornerSmoothnessUniform != null) {
            cornerSmoothnessUniform.set(squirt);
        }
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float screenWidth = mc.getWindow().getFramebufferWidth();
        float screenHeight = mc.getWindow().getFramebufferHeight();
        float scaleFactor = (float)mc.getWindow().getScaleFactor();
        float scaledX = x2 * scaleFactor;
        float scaledY = y2 * scaleFactor;
        float scaledW = width * scaleFactor;
        float scaledH = height * scaleFactor;
        float u1 = scaledX / screenWidth;
        float v1 = 1.0f - scaledY / screenHeight;
        float u2 = (scaledX + scaledW) / screenWidth;
        float v2 = 1.0f - (scaledY + scaledH) / screenHeight;
        int alpha = color >> 24 & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        float r2 = (float)(color >> 16 & 0xFF) / 255.0f;
        float g2 = (float)(color >> 8 & 0xFF) / 255.0f;
        float b2 = (float)(color & 0xFF) / 255.0f;
        float a2 = (float)alpha / 255.0f;
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.liquidGlass);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix, x2, y2, 0.0f).texture(u1, v1).color(r2, g2, b2, a2);
        builder.vertex(matrix, x2, y2 + height, 0.0f).texture(u1, v2).color(r2, g2, b2, a2);
        builder.vertex(matrix, x2 + width, y2 + height, 0.0f).texture(u2, v2).color(r2, g2, b2, a2);
        builder.vertex(matrix, x2 + width, y2, 0.0f).texture(u2, v1).color(r2, g2, b2, a2);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)builder.end());
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawLiquidGlass(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float squirt, int color) {
        float opacity = 0.8f;
        float strength = height >= 240.0f ? 2.0f : 1.0f;
        float distortion = 0.08f;
        float rounding = radius;
        RenderUtils.drawLiquidGlass(matrices, x2, y2, width, height, rounding, rounding, rounding, rounding, color, opacity, strength, ColorUtils.rgba(255, 255, 255, 255), 0.2f, false, 1.0f, distortion, squirt, false);
    }

    public static void drawLiquidGlass(MatrixStack matrices, float x2, float y2, float width, float height, float radius, float squirt, int color, boolean clean) {
        float opacity = 0.8f;
        float strength = height >= 240.0f ? 2.0f : 1.0f;
        float distortion = 0.08f;
        float rounding = radius;
        RenderUtils.drawLiquidGlass(matrices, x2, y2, width, height, rounding, rounding, rounding, rounding, color, opacity, strength, ColorUtils.rgba(255, 255, 255, 255), 0.2f, false, 1.0f, distortion, squirt, clean);
    }

    public static void drawLiquidGlass(MatrixStack matrices, float x2, float y2, float width, float height, int color) {
        RenderUtils.drawLiquidGlass(matrices, x2, y2, width, height, 0.0f, 1.0f, color);
    }

    public static void drawLiquidGlass(MatrixStack matrices, float x2, float y2, float width, float height, int color, boolean clean) {
        RenderUtils.drawLiquidGlass(matrices, x2, y2, width, height, 0.0f, 1.0f, color, clean);
    }

    public static void drawLiquidBlur(MatrixStack matrices, float x2, float y2, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, int color, float globalAlpha, float fresnelPower, int fresnelColor, float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength, float squirt, boolean clean) {
        RenderUtils.drawLiquidGlass(matrices, x2, y2, width, height, topLeft, topRight, bottomRight, bottomLeft, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, clean);
    }

    public static void drawLiquidBlur(MatrixStack matrices, float x2, float y2, float width, float height, float radius, int color, float globalAlpha, float fresnelPower, int fresnelColor, float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength, float squirt, boolean clean) {
        RenderUtils.drawLiquidBlur(matrices, x2, y2, width, height, radius, radius, radius, radius, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, clean);
    }
    private RenderUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static {
        liquidBlurLastWidth = -1;
        liquidBlurLastHeight = -1;
        liquidBlurOffset = 1.0f;
    }
}

