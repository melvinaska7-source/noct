package zov.alphadlc.module.list.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Draws Chams while vanilla is rendering the player. The renderer therefore
 * reuses both the render state and the already-posed vanilla player model.
 */
public final class ChamsRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final double PULSE_PERIOD_NANOS = 1_000_000_000.0;
    private static final float PULSE_LOW = 0.2f;
    private static final long ANIMATION_START_NANOS = System.nanoTime();

    public ChamsRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        Chams module = Chams.getInstance();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (module == null || !module.isEnabled() || mc.player == null || mc.world == null) {
            return;
        }
        if (state.id == mc.player.getId() && mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        // Finish vanilla skin/armor before changing the global render state.
        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }

        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();

            long nowNanos = System.nanoTime();
            float pulse = module.isBlinking() ? pulse(nowNanos) : 1.0f;
            float baseAlpha = MathHelper.clamp(module.getAlpha(), 0.0f, 1.0f);
            float fillAlpha = (module.isMode("Vanilla")
                    ? baseAlpha
                    : MathHelper.clamp(module.getShaderAlpha(), 0.0f, 1.0f)) * pulse;
            float outlineBaseAlpha = MathHelper.clamp(baseAlpha + 0.2f, 0.0f, 1.0f);
            int sourceColor = module.getColor();
            float brightness = module.getBrightness();
            float red = ((sourceColor >> 16) & 0xFF) / 255.0f * brightness;
            float green = ((sourceColor >> 8) & 0xFF) / 255.0f * brightness;
            float blue = (sourceColor & 0xFF) / 255.0f * brightness;
            EntityModel<?> model = getContextModel();
            float animTime = (nowNanos - ANIMATION_START_NANOS) / 1_000_000_000.0f;

            // FIRST PASS: Normal render (visible without walls)
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            
            if (module.hasGlow()) {
                RenderSystem.blendFuncSeparate(
                        GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE,
                        GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
                int layers = module.getGlowLayers();
                float intensity = module.getGlowIntensity();
                for (int i = layers; i >= 1; i--) {
                    float scale = 1.0f + i * 0.004f * intensity;
                    float layerAlpha = outlineBaseAlpha * (1.0f / (i + 1.0f)) * 0.7f * pulse;
                    renderSolidModel(matrices, model, scale, red, green, blue, layerAlpha);
                }
                RenderSystem.defaultBlendFunc();
            }

            float outlineScale = 1.0f + module.getLineWidth() * 0.006f;
            renderSolidModel(matrices, model, outlineScale,
                    MathHelper.clamp(red + 0.15f, 0.0f, 1.0f),
                    MathHelper.clamp(green + 0.15f, 0.0f, 1.0f),
                    MathHelper.clamp(blue + 0.15f, 0.0f, 1.0f),
                    outlineBaseAlpha * pulse);

            renderFill(matrices, model, state.skinTextures.texture(), module,
                    red, green, blue, fillAlpha, animTime);

            // SECOND PASS: Through walls (if enabled)
            if (module.isThroughWalls()) {
                RenderSystem.disableDepthTest();
                
                if (module.hasGlow()) {
                    RenderSystem.blendFuncSeparate(
                            GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE,
                            GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
                    int layers = module.getGlowLayers();
                    float intensity = module.getGlowIntensity();
                    for (int i = layers; i >= 1; i--) {
                        float scale = 1.0f + i * 0.004f * intensity;
                        float layerAlpha = outlineBaseAlpha * (1.0f / (i + 1.0f)) * 0.7f * pulse * 0.5f;
                        renderSolidModel(matrices, model, scale, red, green, blue, layerAlpha);
                    }
                    RenderSystem.defaultBlendFunc();
                }

                renderSolidModel(matrices, model, outlineScale,
                        MathHelper.clamp(red + 0.15f, 0.0f, 1.0f),
                        MathHelper.clamp(green + 0.15f, 0.0f, 1.0f),
                        MathHelper.clamp(blue + 0.15f, 0.0f, 1.0f),
                        outlineBaseAlpha * pulse * 0.5f);

                renderFill(matrices, model, state.skinTextures.texture(), module,
                        red, green, blue, fillAlpha * 0.5f, animTime);
            }
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(depthMask);
            if (depthEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cullEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            if (blendEnabled) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static float pulse(double nowNanos) {
        double phase = (nowNanos % PULSE_PERIOD_NANOS) / PULSE_PERIOD_NANOS;
        float wave = (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2.0));
        return PULSE_LOW + (1.0f - PULSE_LOW) * wave;
    }

    private static void renderFill(MatrixStack matrices, EntityModel<?> model, Identifier skin,
                                   Chams module, float red, float green, float blue,
                                   float alpha, float timeSeconds) {
        if (alpha <= 0.001f) return;

        if (module.isMode("Animated Fill")) {
            Chams.bindAnimatedFill(timeSeconds, module.getShaderSpeed(), module.getShaderScale(),
                    module.hasGlow() ? module.getGlowIntensity() : 0.0f);
        } else if (module.isMode("Wave")) {
            Chams.bindWave(timeSeconds * module.getShaderSpeed());
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, skin);
        }

        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        ModelVertexConsumer consumer = new ModelVertexConsumer(buffer, true);
        model.render(matrices, consumer, 0xF000F0, OverlayTexture.DEFAULT_UV,
                argb(red, green, blue, alpha));
        draw(buffer);
    }

    private static void renderSolidModel(MatrixStack matrices, EntityModel<?> model, float scale,
                                         float red, float green, float blue, float alpha) {
        if (alpha <= 0.001f) return;

        matrices.push();
        matrices.scale(scale, scale, scale);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        ModelVertexConsumer consumer = new ModelVertexConsumer(buffer, false);
        model.render(matrices, consumer, 0xF000F0, OverlayTexture.DEFAULT_UV,
                argb(red, green, blue, alpha));
        draw(buffer);
        matrices.pop();
    }

    private static int argb(float red, float green, float blue, float alpha) {
        int a = MathHelper.clamp((int) (alpha * 255.0f), 0, 255);
        int r = MathHelper.clamp((int) (red * 255.0f), 0, 255);
        int g = MathHelper.clamp((int) (green * 255.0f), 0, 255);
        int b = MathHelper.clamp((int) (blue * 255.0f), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void draw(BufferBuilder buffer) {
        BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
    }

    /**
     * ModelPart writes the entity vertex format. Chams shaders need only
     * position/UV/color, so this adapter deliberately drops light, overlay and
     * normals while preserving every vertex generated by the vanilla model.
     */
    private static final class ModelVertexConsumer implements VertexConsumer {
        private final BufferBuilder target;
        private final boolean textured;
        private float x;
        private float y;
        private float z;
        private float u;
        private float v;
        private int red = 255;
        private int green = 255;
        private int blue = 255;
        private int alpha = 255;

        private ModelVertexConsumer(BufferBuilder target, boolean textured) {
            this.target = target;
            this.textured = textured;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            if (textured) {
                target.vertex(this.x, this.y, this.z)
                        .texture(u, v)
                        .color(red, green, blue, alpha);
            } else {
                target.vertex(this.x, this.y, this.z)
                        .color(red, green, blue, alpha);
            }
            return this;
        }
    }
}
