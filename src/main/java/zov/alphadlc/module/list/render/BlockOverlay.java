package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import zov.alphadlc.event.list.EventWorldRender;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ColorSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.render.providers.ResourceProvider;

@ModuleInformation(moduleName = "Block Overlay", moduleDesc = "Кастомная подсветка блока", moduleCategory = ModuleCategory.RENDER)
public class BlockOverlay extends Module {

    private static final ShaderProgramKey ENERGY_SHADER = shaderKey("block_overlay");
    private static final ShaderProgramKey WAVE_SHADER = shaderKey("block_overlay_wave");
    private static final ShaderProgramKey CRYSTAL_SHADER = shaderKey("block_overlay_crystal");
    private static final ShaderProgramKey GALAXY_SHADER = shaderKey("block_overlay_galaxy");
    private static final ShaderProgramKey LIGHTNING_SHADER = shaderKey("block_overlay_lightning");

    public final ModeSetting shaderMode = new ModeSetting(
            "Шейдер", "Energy", "Energy", "Wave", "Crystal", "Galaxy", "Lightning"
    );
    public final ColorSetting overlayColor = new ColorSetting("Цвет", 0xFF6C63D2);
    public final SliderSetting fillOpacity = new SliderSetting("Прозрачность заливки", 0.32, 0.0, 1.0, 0.05);
    public final BooleanSetting outline = new BooleanSetting("Обводка", true);
    public final SliderSetting outlineWidth = new SliderSetting("Толщина обводки", 2.0, 0.5, 5.0, 0.1)
            .setVisible(() -> outline.getValue());
    public final SliderSetting outlineOpacity = new SliderSetting("Прозрачность обводки", 1.0, 0.0, 1.0, 0.05)
            .setVisible(() -> outline.getValue());
    public final SliderSetting smoothness = new SliderSetting("Плавность", 6.0, 1.0, 10.0, 1.0);

    private Box currentBox;
    private long lastFrameTime;
    private Object lastWorld;
    private float visibility;

    private static ShaderProgramKey shaderKey(String name) {
        return new ShaderProgramKey(
                ResourceProvider.getShaderIdentifier(name),
                VertexFormats.POSITION_TEXTURE_COLOR,
                Defines.EMPTY
        );
    }

    @Subscribe
    public void onWorldRender(EventWorldRender event) {
        if (mc.world == null || mc.player == null) return;
        long now = System.nanoTime();
        float dt = lastFrameTime == 0L ? 1.0f / 60.0f : Math.min(0.1f, (now - lastFrameTime) / 1_000_000_000.0f);
        lastFrameTime = now;

        Box targetBox = getTargetBox();
        if (lastWorld != mc.world) {
            lastWorld = mc.world;
            currentBox = targetBox;
            visibility = 0.0f;
        }

        float smoothnessValue = smoothness.getFloatValue();
        if (smoothnessValue == 1.0f) {
            currentBox = targetBox;
            visibility = targetBox == null ? 0.0f : 1.0f;
            if (currentBox == null) return;
        } else {
            float smoothnessProgress = Math.max(0.0f, Math.min(1.0f, (smoothnessValue - 2.0f) / 8.0f));
            float responseRate = 18.0f + (3.0f - 18.0f) * smoothnessProgress;
            float easing = 1.0f - (float) Math.exp(-responseRate * dt);

            if (targetBox != null) {
                if (currentBox == null) {
                    currentBox = targetBox;
                } else {
                    currentBox = lerpBox(currentBox, targetBox, easing);
                }
                visibility += (1.0f - visibility) * easing;
            } else {
                visibility += (0.0f - visibility) * easing;
                if (visibility < 0.01f) {
                    currentBox = null;
                    visibility = 0.0f;
                    return;
                }
            }
        }
        if (currentBox == null) return;

        int color = overlayColor.getValue();

        MatrixStack matrices = event.getMatrixStack();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();

        matrices.push();

        double minX = currentBox.minX - camPos.x;
        double minY = currentBox.minY - camPos.y;
        double minZ = currentBox.minZ - camPos.z;
        double maxX = currentBox.maxX - camPos.x;
        double maxY = currentBox.maxY - camPos.y;
        double maxZ = currentBox.maxZ - camPos.z;

        drawFilled(matrices, minX, minY, minZ, maxX, maxY, maxZ, color, visibility);
        if (outline.getValue()) {
            drawOutline(matrices, minX, minY, minZ, maxX, maxY, maxZ, color, visibility);
        }

        matrices.pop();
    }

    @Override
    public void onDisable() {
        currentBox = null;
        lastFrameTime = 0L;
        lastWorld = null;
        visibility = 0.0f;
        super.onDisable();
    }

    private Box getTargetBox() {
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return null;
        BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return null;
        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) return null;
        return shape.getBoundingBox().offset(pos);
    }

    private Box lerpBox(Box from, Box to, float delta) {
        return new Box(
                from.minX + (to.minX - from.minX) * delta,
                from.minY + (to.minY - from.minY) * delta,
                from.minZ + (to.minZ - from.minZ) * delta,
                from.maxX + (to.maxX - from.maxX) * delta,
                from.maxY + (to.maxY - from.maxY) * delta,
                from.maxZ + (to.maxZ - from.maxZ) * delta);
    }

    private void drawOutline(MatrixStack matrices, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float visibility) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(outlineWidth.getFloatValue());

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float outlineAlpha = outlineOpacity.getFloatValue() * Math.max(0.0f, Math.min(1.0f, visibility));

        buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, outlineAlpha);

        buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, outlineAlpha);

        buffer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(r, g, b, outlineAlpha);
        buffer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(r, g, b, outlineAlpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
    }

    private void drawFilled(MatrixStack matrices, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float visibility) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        ShaderProgram shader = RenderSystem.setShader(getOverlayShader());
        if (shader == null) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        } else if (shader.getUniform("Time") != null) {
            shader.getUniform("Time").set((System.currentTimeMillis() % 100000L) / 1000.0f);
        }

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = fillOpacity.getFloatValue() * Math.max(0.0f, Math.min(1.0f, visibility));

        putFace(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        putFace(buffer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        putFace(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        putFace(buffer, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        putFace(buffer, matrix, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        putFace(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private ShaderProgramKey getOverlayShader() {
        return switch (shaderMode.getValue()) {
            case "Wave" -> WAVE_SHADER;
            case "Crystal" -> CRYSTAL_SHADER;
            case "Galaxy" -> GALAXY_SHADER;
            case "Lightning" -> LIGHTNING_SHADER;
            default -> ENERGY_SHADER;
        };
    }

    private void putFace(BufferBuilder buffer, Matrix4f matrix,
                         double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         double x3, double y3, double z3,
                         double x4, double y4, double z4,
                         float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).texture(0f, 0f).color(r, g, b, a);
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).texture(1f, 0f).color(r, g, b, a);
        buffer.vertex(matrix, (float) x3, (float) y3, (float) z3).texture(1f, 1f).color(r, g, b, a);
        buffer.vertex(matrix, (float) x4, (float) y4, (float) z4).texture(0f, 1f).color(r, g, b, a);
    }
}
