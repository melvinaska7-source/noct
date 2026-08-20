package polar.ru.api.utils.render.glow;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import polar.ru.api.utils.render.glow.GlowCallback;

public class GlowProgram {
    private static GlowProgram instance;
    private static final MinecraftClient mc;
    private Framebuffer glowBuffer;
    private int lastWidth;
    private int lastHeight;
    private float glowRadius = 10.0f;
    private float glowIntensity = 1.0f;
    private Color glowColor = Color.WHITE;
    private Matrix4f savedProjection;
    private int savedFbo;
    private static final int RINGS = 6;
    private static final int ANGLES_PER_RING = 12;

    public static GlowProgram getInstance() {
        if (instance == null) {
            instance = new GlowProgram();
        }
        return instance;
    }

    private void checkFramebuffers() {
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();
        if (this.glowBuffer == null || this.lastWidth != width || this.lastHeight != height) {
            if (this.glowBuffer != null) {
                this.glowBuffer.delete();
            }
            this.glowBuffer = new SimpleFramebuffer(width, height, false);
            this.lastWidth = width;
            this.lastHeight = height;
        }
    }

    public void begin(float radius, Color color) {
        this.begin(radius, 1.0f, color);
    }

    public void begin(float radius, float intensity, Color color) {
        this.checkFramebuffers();
        this.glowRadius = radius;
        this.glowIntensity = intensity;
        this.glowColor = color;
        this.savedProjection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        this.savedFbo = GL11.glGetInteger((int)36006);
        GL30.glBindFramebuffer((int)36160, (int)this.glowBuffer.fbo);
        GL11.glViewport((int)0, (int)0, (int)this.lastWidth, (int)this.lastHeight);
        RenderSystem.clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        RenderSystem.clear((int)16384);
        RenderSystem.setProjectionMatrix((Matrix4f)this.savedProjection, (ProjectionType)ProjectionType.ORTHOGRAPHIC);
    }

    public void end(MatrixStack matrices, GlowCallback contentCallback) {
        GL30.glBindFramebuffer((int)36160, (int)this.savedFbo);
        GL11.glViewport((int)0, (int)0, (int)mc.getWindow().getFramebufferWidth(), (int)mc.getWindow().getFramebufferHeight());
        RenderSystem.setProjectionMatrix((Matrix4f)this.savedProjection, (ProjectionType)ProjectionType.ORTHOGRAPHIC);
        this.renderGlow(matrices);
        if (contentCallback != null) {
            contentCallback.render();
        }
    }

    private float gaussian(float x2, float sigma) {
        return (float)Math.exp(-(x2 * x2) / (2.0f * sigma * sigma));
    }

    private void renderGlow(MatrixStack matrices) {
        int i2;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.disableDepthTest();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();
        RenderSystem.setShaderTexture((int)0, (int)this.glowBuffer.getColorAttachment());
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float r2 = (float)this.glowColor.getRed() / 255.0f;
        float g2 = (float)this.glowColor.getGreen() / 255.0f;
        float b2 = (float)this.glowColor.getBlue() / 255.0f;
        float baseAlpha = (float)this.glowColor.getAlpha() / 255.0f * this.glowIntensity;
        float sigma = this.glowRadius * 0.4f;
        float[] ringWeights = new float[6];
        float totalWeight = 0.0f;
        for (i2 = 0; i2 < 6; ++i2) {
            float distance = this.glowRadius * (float)(i2 + 1) / 6.0f;
            ringWeights[i2] = this.gaussian(distance, sigma);
            totalWeight += ringWeights[i2];
        }
        i2 = 0;
        while (i2 < 6) {
            int n2 = i2++;
            ringWeights[n2] = ringWeights[n2] / totalWeight;
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int ring = 0; ring < 6; ++ring) {
            float distance = this.glowRadius * (float)(ring + 1) / 6.0f;
            float alpha = baseAlpha * ringWeights[ring] * 0.7f;
            if (alpha < 0.001f) continue;
            alpha = Math.min(alpha, 1.0f);
            for (int angle = 0; angle < 12; ++angle) {
                float a1 = (float)((double)angle * 2.0 * Math.PI) / 12.0f;
                float ox = (float)Math.cos(a1) * distance;
                float oy = (float)Math.sin(a1) * distance;
                buffer.vertex(matrix, ox, oy, 0.0f).texture(0.0f, 1.0f).color(r2, g2, b2, alpha);
                buffer.vertex(matrix, ox, (float)height + oy, 0.0f).texture(0.0f, 0.0f).color(r2, g2, b2, alpha);
                buffer.vertex(matrix, (float)width + ox, (float)height + oy, 0.0f).texture(1.0f, 0.0f).color(r2, g2, b2, alpha);
                buffer.vertex(matrix, (float)width + ox, oy, 0.0f).texture(1.0f, 1.0f).color(r2, g2, b2, alpha);
                if (ring <= 0) continue;
                float a2 = (float)(((double)angle + 0.5) * 2.0 * Math.PI) / 12.0f;
                float innerDist = distance * 0.6f;
                float ox2 = (float)Math.cos(a2) * innerDist;
                float oy2 = (float)Math.sin(a2) * innerDist;
                float alpha2 = alpha * 0.5f;
                buffer.vertex(matrix, ox2, oy2, 0.0f).texture(0.0f, 1.0f).color(r2, g2, b2, alpha2);
                buffer.vertex(matrix, ox2, (float)height + oy2, 0.0f).texture(0.0f, 0.0f).color(r2, g2, b2, alpha2);
                buffer.vertex(matrix, (float)width + ox2, (float)height + oy2, 0.0f).texture(1.0f, 0.0f).color(r2, g2, b2, alpha2);
                buffer.vertex(matrix, (float)width + ox2, oy2, 0.0f).texture(1.0f, 1.0f).color(r2, g2, b2, alpha2);
            }
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        GL11.glTexParameteri((int)3553, (int)10241, (int)9728);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9728);
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void startGlow(float radius, int color, GlowCallback callback, MatrixStack matrices) {
        GlowProgram.startGlow(radius, 1.0f, color, callback, matrices);
    }

    public static void startGlow(float radius, float intensity, int color, GlowCallback callback, MatrixStack matrices) {
        int a2 = color >> 24 & 0xFF;
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        if (a2 == 0) {
            a2 = 255;
        }
        GlowProgram glow = GlowProgram.getInstance();
        glow.begin(radius, intensity, new Color(r2, g2, b2, a2));
        callback.render();
        glow.end(matrices, callback);
    }

    public void cleanup() {
        if (this.glowBuffer != null) {
            this.glowBuffer.delete();
            this.glowBuffer = null;
        }
    }

    static {
        mc = MinecraftClient.getInstance();
    }
}

