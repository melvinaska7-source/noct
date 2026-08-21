package zov.alphadlc.util.render.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import zov.alphadlc.util.render.providers.ResourceProvider;

import java.util.HashMap;
import java.util.Map;

/** Resolves MAS animation JSON files into mre resources and renders their current billboard frame. */
public final class MasEffectRenderer {
    private static final Map<String, Identifier[]> CACHE = new HashMap<>();

    private MasEffectRenderer() {
    }

    public static void render(String effect, MatrixStack matrices, Camera camera, Vec3d position,
                              long ageMs, long durationMs, float size, int color) {
        Identifier[] frames = frames(effect);
        if (frames.length == 0 || ageMs < 0 || ageMs >= durationMs) return;
        float progress = Math.min(0.999f, ageMs / (float) durationMs);
        int frame = Math.min(frames.length - 1, (int) (progress * frames.length));
        float fade = Math.min(1.0f, Math.min(progress * 5.0f, (1.0f - progress) * 5.0f));
        int alpha = Math.max(0, Math.min(255, (int) (((color >>> 24) & 0xFF) * fade)));
        int rgba = (alpha << 24) | (color & 0x00FFFFFF);

        Vec3d cameraPos = camera.getPos();
        matrices.push();
        matrices.translate(position.x - cameraPos.x, position.y - cameraPos.y, position.z - cameraPos.z);
        matrices.multiply(camera.getRotation());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, frames[frame]);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -size, size, 0).texture(0, 1).color(rgba);
        buffer.vertex(matrix, size, size, 0).texture(1, 1).color(rgba);
        buffer.vertex(matrix, size, -size, 0).texture(1, 0).color(rgba);
        buffer.vertex(matrix, -size, -size, 0).texture(0, 0).color(rgba);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, 0);
        matrices.pop();
    }

    public static Identifier[] frames(String effect) {
        return CACHE.computeIfAbsent(effect, MasEffectRenderer::loadFrames);
    }

    private static Identifier[] loadFrames(String effect) {
        try {
            JsonObject json = ResourceProvider.toJson(Identifier.of("mre", "maseffects/" + effect + ".json"));
            JsonArray textures = json.getAsJsonArray("textures");
            Identifier[] frames = new Identifier[textures.size()];
            int i = 0;
            for (JsonElement element : textures) {
                String raw = element.getAsString();
                String path = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
                frames[i++] = Identifier.of("mre", "maseffects/" + path + ".png");
            }
            return frames;
        } catch (RuntimeException ignored) {
            return new Identifier[0];
        }
    }
}
