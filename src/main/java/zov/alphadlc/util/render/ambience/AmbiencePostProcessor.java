package zov.alphadlc.util.render.ambience;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import zov.alphadlc.module.list.render.Ambience;
import zov.alphadlc.util.render.providers.ResourceProvider;

/** Applies world-only post processing before hands and the HUD are rendered. */
public final class AmbiencePostProcessor {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final ShaderProgramKey SATURATION_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("saturation"), VertexFormats.POSITION_COLOR, Defines.EMPTY);

    private static Framebuffer sourceBuffer;
    private static int width = -1;
    private static int height = -1;

    private AmbiencePostProcessor() {
    }

    public static void apply(Ambience module) {
        if (module == null || !module.isEnabled() || !module.worldSaturation.getValue()) return;
        if (MC.world == null || MC.getFramebuffer() == null) return;

        int w = Math.max(1, MC.getWindow().getFramebufferWidth());
        int h = Math.max(1, MC.getWindow().getFramebufferHeight());
        ensureBuffer(w, h);
        if (sourceBuffer == null) return;

        int readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int sourceFbo = readFbo != 0 ? readFbo : drawFbo;

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sourceBuffer.fbo);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);

        try {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, drawFbo);
            RenderSystem.viewport(0, 0, w, h);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.setShaderTexture(0, sourceBuffer.getColorAttachment());

            ShaderProgram shader = RenderSystem.setShader(SATURATION_KEY);
            if (shader == null) return;
            if (shader.getUniform("u_Saturation") != null) {
                shader.getUniform("u_Saturation").set(module.saturationValue.getFloatValue());
            }
            if (shader.getUniform("u_Resolution") != null) {
                shader.getUniform("u_Resolution").set(
                        (float) Math.max(1, MC.getWindow().getScaledWidth()),
                        (float) Math.max(1, MC.getWindow().getScaledHeight()));
            }

            BufferBuilder buffer = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(-1.0f, 1.0f, 0.0f).color(0xFFFFFFFF);
            buffer.vertex(-1.0f, -1.0f, 0.0f).color(0xFFFFFFFF);
            buffer.vertex(1.0f, -1.0f, 0.0f).color(0xFFFFFFFF);
            buffer.vertex(1.0f, 1.0f, 0.0f).color(0xFFFFFFFF);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } finally {
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
            RenderSystem.viewport(0, 0, w, h);
        }
    }

    private static void ensureBuffer(int w, int h) {
        if (sourceBuffer != null && width == w && height == h) return;
        if (sourceBuffer != null) sourceBuffer.delete();
        sourceBuffer = new SimpleFramebuffer(w, h, false);
        sourceBuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        width = w;
        height = h;
    }
}
