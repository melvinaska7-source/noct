package zov.alphadlc.util.render.renderers.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zov.alphadlc.util.render.providers.ResourceProvider;

public final class HudShine {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final float SWEEP_MS = 2200f;
    private static final float PAUSE_MS = 1100f;

    private static final ShaderProgramKey SHINE_SHADER = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("hud_shine"),
            VertexFormats.POSITION,
            Defines.EMPTY
    );

    private HudShine() {}

    public static void render(MatrixStack matrices, float x, float y, float w, float h, float round) {
        render(matrices, x, y, w, h, round, 0.5f, 1f, 0.28f, 1.6f);
    }

    public static void render(MatrixStack matrices, float x, float y, float w, float h, float round,
                              float sizeScale, float speedScale, float brightness) {
        render(matrices, x, y, w, h, round, sizeScale, speedScale, brightness, 1.6f);
    }

    public static void render(MatrixStack matrices, float x, float y, float w, float h, float round,
                              float sizeScale, float speedScale, float brightness, float edgeK) {
        if (w <= 2f || h <= 2f) return;

        double cycle = SWEEP_MS + PAUSE_MS;
        double now = System.currentTimeMillis() * (double) speedScale;
        double t = now % cycle;
        if (t > SWEEP_MS) return;

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float scale = (float) mc.getWindow().getScaleFactor();
        Vector3f pos = matrix.transformPosition(x, y, 0f, new Vector3f()).mul(scale);
        Vector3f sz = matrix.getScale(new Vector3f()).mul(scale);

        float scaledWidth = w * sz.x;
        float scaledHeight = h * sz.y;
        float locationX = pos.x;
        float locationY = mc.getWindow().getFramebufferHeight() - scaledHeight - pos.y;
        float radius = Math.min(round, Math.min(w, h) * 0.5f) * sz.y;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        ShaderProgram shader = RenderSystem.setShader(SHINE_SHADER);
        shader.getUniform("Size").set(scaledWidth, scaledHeight);
        shader.getUniform("Location").set(locationX, locationY);
        shader.getUniform("Radius").set(radius, radius, radius, radius);
        shader.getUniform("BeamWidth").set(0.20f * sizeScale);
        shader.getUniform("Brightness").set(brightness);
        if (shader.getUniform("EdgeK") != null) {
            shader.getUniform("EdgeK").set(edgeK);
        }
        if (shader.getUniform("Time") != null) {
            shader.getUniform("Time").set((float) ((now % (4.0 * cycle)) / 1000.0));
        }

        float m = 8f;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buffer.vertex(matrix, x - m, y - m, 0f);
        buffer.vertex(matrix, x - m, y + h + m, 0f);
        buffer.vertex(matrix, x + w + m, y + h + m, 0f);
        buffer.vertex(matrix, x + w + m, y - m, 0f);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }
}
