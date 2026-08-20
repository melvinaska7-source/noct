package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventAttackEntity;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class Cubes
extends Module {
    public static Cubes INSTANCE = new Cubes();
    private static final Identifier GLOW_TEX = Identifier.of((String)"polar", (String)"textures/particle/bloom.png");
    private static final float SPAWN_RADIUS = 12.0f;
    private static final float PARTICLE_SIZE = 0.18f;
    private static final float PARTICLE_SPEED = 0.25f;
    private static final float GLOW_INTENSITY = 1.7f;
    private static final float MAX_RENDER_DISTANCE_SQ = 900.0f;
    private static final byte[][] CUBE_EDGES = new byte[][]{{-1, -1, -1, 1, -1, -1}, {1, -1, -1, 1, -1, 1}, {1, -1, 1, -1, -1, 1}, {-1, -1, 1, -1, -1, -1}, {-1, 1, -1, 1, 1, -1}, {1, 1, -1, 1, 1, 1}, {1, 1, 1, -1, 1, 1}, {-1, 1, 1, -1, 1, -1}, {-1, -1, -1, -1, 1, -1}, {1, -1, -1, 1, 1, -1}, {1, -1, 1, 1, 1, 1}, {-1, -1, 1, -1, 1, 1}};
    private static final byte[][] TRIANGLE_EDGES = new byte[][]{{0, 1}, {0, 2}, {0, 3}, {0, 4}, {1, 2}, {2, 3}, {3, 4}, {4, 1}};
    private static final float[] GLOW_SCALES = new float[]{10.0f, 6.0f, 3.5f};
    private static final float[] GLOW_ALPHA_SCALES = new float[]{0.06f, 0.14f, 0.25f};
    private final ModeSetting animation = new ModeSetting("Анимация", "Разлет", "Разлет", "Падение");
    private final ModeSetting shape = new ModeSetting("Форма", "Кубы", "Кубы", "Треугольники");
    private final FloatSetting count = new FloatSetting("Количество", 30.0f, 5.0f, 100.0f, 1.0f);
    private final FloatSetting size = new FloatSetting("Размер", 1.0f, 0.1f, 3.0f, 0.1f);
    private final FloatSetting speed = new FloatSetting("Скорость", 1.0f, 0.1f, 5.0f, 0.1f);
    private final List<CubeParticle> cubes = new ArrayList<CubeParticle>();
    private final List<CubeParticle> visibleCubes = new ArrayList<CubeParticle>();
    private final Random random = new Random();
    private boolean lastAttackPressed;
    private float cr;
    private float cg;
    private float cb;
    private int updateCounter = 0;

    public Cubes() {
        super("Cubes", "3D Кубы по миру", Module.ModuleCategory.RENDER);
        this.addSettings(this.animation, this.shape, this.count, this.size, this.speed);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.cubes.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.cubes.clear();
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        if (Cubes.mc.player == null || Cubes.mc.world == null) {
            return;
        }
        boolean attackPressed = Cubes.mc.options.attackKey.isPressed();
        if (attackPressed && !this.lastAttackPressed) {
            this.applyHitImpulseFromCrosshair(event.getCamera());
        }
        this.lastAttackPressed = attackPressed;
        ++this.updateCounter;
        if (this.updateCounter % 2 == 0) {
            this.updateCubes();
        }
        this.renderCubes(event);
    }

    @EventLink
    public void onAttack(EventAttackEntity event) {
        if (Cubes.mc.gameRenderer != null && Cubes.mc.gameRenderer.getCamera() != null) {
            this.applyHitImpulseFromCrosshair(Cubes.mc.gameRenderer.getCamera());
        }
    }

    private void applyHitImpulseFromCrosshair(Camera camera) {
        if (this.cubes.isEmpty() || camera == null) {
            return;
        }
        Vec3d origin = camera.getPos();
        float yaw = (float)Math.toRadians(camera.getYaw());
        float pitch = (float)Math.toRadians(camera.getPitch());
        double dirX = -MathHelper.sin((float)yaw) * MathHelper.cos((float)pitch);
        double dirY = -MathHelper.sin((float)pitch);
        double dirZ = MathHelper.cos((float)yaw) * MathHelper.cos((float)pitch);
        CubeParticle best = null;
        double bestT = Double.MAX_VALUE;
        int sz = this.cubes.size();
        for (int i2 = 0; i2 < sz; ++i2) {
            double closestZ;
            double dz;
            double closestY;
            double dy;
            double closestX;
            double dx;
            double distSq;
            CubeParticle p2 = this.cubes.get(i2);
            double opX = p2.x - origin.x;
            double opY = p2.y - origin.y;
            double opZ = p2.z - origin.z;
            double t2 = opX * dirX + opY * dirY + opZ * dirZ;
            if (t2 < 0.0 || t2 > 128.0 || (distSq = (dx = p2.x - (closestX = origin.x + dirX * t2)) * dx + (dy = p2.y - (closestY = origin.y + dirY * t2)) * dy + (dz = p2.z - (closestZ = origin.z + dirZ * t2)) * dz) > 1.32 || t2 >= bestT) continue;
            bestT = t2;
            best = p2;
        }
        if (best != null) {
            double force = 0.08 * (double)this.speed.get();
            best.vx = (float)((double)best.vx + dirX * force);
            best.vy = (float)((double)best.vy + (dirY * force + 0.02));
            best.vz = (float)((double)best.vz + dirZ * force);
        }
    }

    private void updateCubes() {
        int target = (int)this.count.get();
        int currentSize = this.cubes.size();
        if (currentSize < target) {
            int toAdd = Math.min(target - currentSize, 5);
            for (int i2 = 0; i2 < toAdd; ++i2) {
                this.cubes.add(this.spawnCube());
            }
        } else if (currentSize > target) {
            this.cubes.subList(target, currentSize).clear();
        }
        float spd = 0.25f * this.speed.get();
        float maxR = 12.0f;
        boolean falling = this.animation.is("Падение");
        Vec3d playerPos = Cubes.mc.player.getPos();
        double maxRSq = (double)(maxR * maxR) * 6.25;
        for (int i3 = this.cubes.size() - 1; i3 >= 0; --i3) {
            CubeParticle p2 = this.cubes.get(i3);
            if (falling) {
                p2.wobblePhase += 0.06f * spd;
                p2.x += (double)(p2.vx * spd) + Math.sin(p2.wobblePhase + p2.wobbleOffset) * (double)0.0024f * (double)spd;
                p2.y += (double)(p2.vy * spd);
                p2.z += (double)(p2.vz * spd) + Math.cos(p2.wobblePhase * 0.8f + p2.wobbleOffset) * (double)0.002f * (double)spd;
                p2.vy = Math.max(p2.vy - 8.0E-5f * spd, -0.032f);
                p2.rotX += p2.rotSpeedX * 0.2f * spd;
                p2.rotY += p2.rotSpeedY * 0.2f * spd;
                p2.rotZ += p2.rotSpeedZ * 0.2f * spd;
            } else {
                p2.x += (double)(p2.vx * spd);
                p2.y += (double)(p2.vy * spd);
                p2.z += (double)(p2.vz * spd);
                p2.rotX += p2.rotSpeedX * spd;
                p2.rotY += p2.rotSpeedY * spd;
                p2.rotZ += p2.rotSpeedZ * spd;
                p2.vx *= 0.995f;
                p2.vy *= 0.995f;
                p2.vz *= 0.995f;
            }
            --p2.life;
            double dx = p2.x - playerPos.x;
            double dy = p2.y - playerPos.y;
            double dz = p2.z - playerPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (p2.life > 0 && !(distSq > maxRSq) && (!falling || !(p2.y < playerPos.y - 2.5))) continue;
            this.cubes.remove(i3);
            this.cubes.add(this.spawnCube());
        }
    }

    private void renderCubes(Event3DRender e2) {
        if (Cubes.mc.player == null) {
            return;
        }
        MatrixStack ms = e2.getMatrices();
        Vec3d cam = e2.getCamera().getPos();
        Camera camera = e2.getCamera();
        float s2 = 0.18f * this.size.get();
        float glow = 1.7f;
        int baseRGB = ColorUtils.getThemeColor();
        this.cr = (float)(baseRGB >> 16 & 0xFF) / 255.0f;
        this.cg = (float)(baseRGB >> 8 & 0xFF) / 255.0f;
        this.cb = (float)(baseRGB & 0xFF) / 255.0f;
        this.visibleCubes.clear();
        float yaw = (float)Math.toRadians(camera.getYaw());
        float pitch = (float)Math.toRadians(camera.getPitch());
        double lookX = -MathHelper.sin((float)yaw) * MathHelper.cos((float)pitch);
        double lookY = -MathHelper.sin((float)pitch);
        double lookZ = MathHelper.cos((float)yaw) * MathHelper.cos((float)pitch);
        int sz = this.cubes.size();
        for (int i2 = 0; i2 < sz; ++i2) {
            CubeParticle p2 = this.cubes.get(i2);
            double dx = p2.x - cam.x;
            double dy = p2.y - cam.y;
            double dz = p2.z - cam.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > 900.0 || dx * lookX + dy * lookY + dz * lookZ < -1.0) continue;
            p2.renderAlpha = this.getAlpha(p2);
            if (p2.renderAlpha < 0.01f) continue;
            this.visibleCubes.add(p2);
        }
        if (this.visibleCubes.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.blendFuncSeparate((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE, (GlStateManager.SrcFactor)GlStateManager.SrcFactor.ZERO, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)GLOW_TEX);
        this.drawGlowBatch(ms, camera, cam, s2, glow);
        RenderSystem.blendFunc((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        boolean isCubes = this.shape.is("Кубы");
        boolean isTriangles = this.shape.is("Треугольники");
        if (isCubes) {
            this.drawCubeFacesBatch(ms, cam, s2);
        }
        if (isTriangles) {
            this.drawTriangleFacesBatch(ms, cam, s2);
        }
        RenderSystem.blendFunc((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE);
        if (isCubes) {
            this.drawCubeDashedEdgesBatch(ms, cam, s2);
        } else if (isTriangles) {
            this.drawTriangleDashedEdgesBatch(ms, cam, s2);
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawGlowBatch(MatrixStack ms, Camera camera, Vec3d cam, float s2, float glow) {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int sz = this.visibleCubes.size();
        for (int particleIndex = 0; particleIndex < sz; ++particleIndex) {
            CubeParticle p2 = this.visibleCubes.get(particleIndex);
            float alpha = p2.renderAlpha;
            ms.push();
            ms.translate(p2.x - cam.x, p2.y - cam.y, p2.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            Matrix4f matrix = ms.peek().getPositionMatrix();
            for (int i2 = 0; i2 < 3; ++i2) {
                float scale = s2 * GLOW_SCALES[i2] * glow;
                float a2 = alpha * GLOW_ALPHA_SCALES[i2] * glow;
                float hs = scale * 0.5f;
                builder.vertex(matrix, -hs, hs, 0.0f).texture(0.0f, 1.0f).color(this.cr, this.cg, this.cb, a2);
                builder.vertex(matrix, hs, hs, 0.0f).texture(1.0f, 1.0f).color(this.cr, this.cg, this.cb, a2);
                builder.vertex(matrix, hs, -hs, 0.0f).texture(1.0f, 0.0f).color(this.cr, this.cg, this.cb, a2);
                builder.vertex(matrix, -hs, -hs, 0.0f).texture(0.0f, 0.0f).color(this.cr, this.cg, this.cb, a2);
            }
            ms.pop();
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)builder.end());
    }

    private float getAlpha(CubeParticle p2) {
        float lifePct = MathHelper.clamp((float)((float)p2.life / (float)p2.maxLife), (float)0.0f, (float)1.0f);
        float fadeIn = Math.min(1.0f, (float)(p2.maxLife - p2.life) / 20.0f);
        return lifePct * fadeIn;
    }

    private void drawCubeFacesBatch(MatrixStack ms, Vec3d cam, float s2) {
        if (!this.hasFaceRenderableParticles()) {
            return;
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int sz = this.visibleCubes.size();
        for (int i2 = 0; i2 < sz; ++i2) {
            CubeParticle p2 = this.visibleCubes.get(i2);
            float alpha = p2.renderAlpha * 0.4f;
            if (alpha < 0.01f) continue;
            ms.push();
            ms.translate(p2.x - cam.x, p2.y - cam.y, p2.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p2.rotX));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p2.rotY));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p2.rotZ));
            this.appendCubeFaces(buffer, ms.peek().getPositionMatrix(), s2, alpha);
            ms.pop();
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void drawTriangleFacesBatch(MatrixStack ms, Vec3d cam, float s2) {
        if (!this.hasFaceRenderableParticles()) {
            return;
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        int sz = this.visibleCubes.size();
        for (int i2 = 0; i2 < sz; ++i2) {
            CubeParticle p2 = this.visibleCubes.get(i2);
            float alpha = p2.renderAlpha * 0.4f;
            if (alpha < 0.01f) continue;
            ms.push();
            ms.translate(p2.x - cam.x, p2.y - cam.y, p2.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p2.rotX));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p2.rotY));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p2.rotZ));
            this.appendTriangleFaces(buffer, ms.peek().getPositionMatrix(), s2, alpha);
            ms.pop();
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private boolean hasFaceRenderableParticles() {
        int sz = this.visibleCubes.size();
        for (int i2 = 0; i2 < sz; ++i2) {
            if (!(this.visibleCubes.get((int)i2).renderAlpha >= 0.025f)) continue;
            return true;
        }
        return false;
    }

    private void appendCubeFaces(BufferBuilder buffer, Matrix4f m2, float s2, float a2) {
        buffer.vertex(m2, -s2, -s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, -s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -s2, s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, -s2, -s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -s2, -s2, -s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -s2, s2, -s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, s2, -s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -s2, s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, s2, -s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -s2, s2, -s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -s2, -s2, -s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, -s2, -s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, -s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -s2, -s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, -s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, -s2, -s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, s2, -s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, s2, s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -s2, -s2, -s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -s2, -s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -s2, s2, s2).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -s2, s2, -s2).color(this.cr, this.cg, this.cb, a2);
    }

    private void appendTriangleFaces(BufferBuilder buffer, Matrix4f m2, float s2, float a2) {
        float top = s2;
        float bottom = -s2;
        float halfBase = s2 * 0.866f;
        buffer.vertex(m2, 0.0f, top, 0.0f).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, 0.0f, top, 0.0f).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, 0.0f, top, 0.0f).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, 0.0f, top, 0.0f).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -halfBase, bottom, halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a2);
        buffer.vertex(m2, -halfBase, bottom, -halfBase).color(this.cr, this.cg, this.cb, a2);
    }

    private void drawCubeDashedEdgesBatch(MatrixStack ms, Vec3d cam, float s2) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int lineCount = 0;
        int sz = this.visibleCubes.size();
        for (int i2 = 0; i2 < sz; ++i2) {
            CubeParticle p2 = this.visibleCubes.get(i2);
            float alpha = p2.renderAlpha;
            ms.push();
            ms.translate(p2.x - cam.x, p2.y - cam.y, p2.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p2.rotX));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p2.rotY));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p2.rotZ));
            lineCount += this.appendCubeDashedEdges(buf, ms.peek().getPositionMatrix(), s2, alpha);
            ms.pop();
        }
        if (lineCount > 0) {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buf.end());
        }
    }

    private void drawTriangleDashedEdgesBatch(MatrixStack ms, Vec3d cam, float s2) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        int lineCount = 0;
        int sz = this.visibleCubes.size();
        for (int i2 = 0; i2 < sz; ++i2) {
            CubeParticle p2 = this.visibleCubes.get(i2);
            float alpha = p2.renderAlpha;
            ms.push();
            ms.translate(p2.x - cam.x, p2.y - cam.y, p2.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p2.rotX));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p2.rotY));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p2.rotZ));
            lineCount += this.appendTriangleDashedEdges(buf, ms.peek().getPositionMatrix(), s2, alpha);
            ms.pop();
        }
        if (lineCount > 0) {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buf.end());
        }
    }

    private int appendCubeDashedEdges(BufferBuilder buf, Matrix4f mat, float s2, float alpha) {
        int color = Cubes.colorToInt(Math.min(1.0f, this.cr * 1.5f), Math.min(1.0f, this.cg * 1.5f), Math.min(1.0f, this.cb * 1.5f), alpha);
        float dashLen = s2 * 0.3f;
        float gapLen = s2 * 0.25f;
        int lineCount = 0;
        for (byte[] edge : CUBE_EDGES) {
            float x2 = (float)edge[3] * s2;
            float x1 = (float)edge[0] * s2;
            float dx = x2 - x1;
            float y2 = (float)edge[4] * s2;
            float y1 = (float)edge[1] * s2;
            float dy = y2 - y1;
            float z2 = (float)edge[5] * s2;
            float z1 = (float)edge[2] * s2;
            float dz = z2 - z1;
            float len = MathHelper.sqrt((float)(dx * dx + dy * dy + dz * dz));
            if (len < 0.001f) continue;
            float nx = dx / len;
            float ny = dy / len;
            float nz = dz / len;
            float pos = 0.0f;
            boolean drawing = true;
            while (pos < len) {
                float segLen = drawing ? dashLen : gapLen;
                float end = Math.min(pos + segLen, len);
                if (drawing) {
                    buf.vertex(mat, x1 + nx * pos, y1 + ny * pos, z1 + nz * pos).color(color);
                    buf.vertex(mat, x1 + nx * end, y1 + ny * end, z1 + nz * end).color(color);
                    ++lineCount;
                }
                pos = end;
                drawing = !drawing;
            }
        }
        return lineCount;
    }

    private int appendTriangleDashedEdges(BufferBuilder buf, Matrix4f mat, float s2, float alpha) {
        int color = Cubes.colorToInt(Math.min(1.0f, this.cr * 1.5f), Math.min(1.0f, this.cg * 1.5f), Math.min(1.0f, this.cb * 1.5f), alpha);
        float dashLen = s2 * 0.3f;
        float gapLen = s2 * 0.25f;
        int lineCount = 0;
        float top = s2;
        float bottom = -s2;
        float halfBase = s2 * 0.866f;
        for (byte[] edge : TRIANGLE_EDGES) {
            float z2;
            float dz;
            float y2;
            float dy;
            float x1 = this.trianglePointX(edge[0], halfBase);
            float y1 = edge[0] == 0 ? top : bottom;
            float z1 = this.trianglePointZ(edge[0], halfBase);
            float x2 = this.trianglePointX(edge[1], halfBase);
            float dx = x2 - x1;
            float len = MathHelper.sqrt((float)(dx * dx + (dy = (y2 = edge[1] == 0 ? top : bottom) - y1) * dy + (dz = (z2 = this.trianglePointZ(edge[1], halfBase)) - z1) * dz));
            if (len < 0.001f) continue;
            float nx = dx / len;
            float ny = dy / len;
            float nz = dz / len;
            float pos = 0.0f;
            boolean drawing = true;
            while (pos < len) {
                float segLen = drawing ? dashLen : gapLen;
                float end = Math.min(pos + segLen, len);
                if (drawing) {
                    buf.vertex(mat, x1 + nx * pos, y1 + ny * pos, z1 + nz * pos).color(color);
                    buf.vertex(mat, x1 + nx * end, y1 + ny * end, z1 + nz * end).color(color);
                    ++lineCount;
                }
                pos = end;
                drawing = !drawing;
            }
        }
        return lineCount;
    }

    private float trianglePointX(int index, float halfBase) {
        return switch (index) {
            case 1, 4 -> -halfBase;
            case 2, 3 -> halfBase;
            default -> 0.0f;
        };
    }

    private float trianglePointZ(int index, float halfBase) {
        return switch (index) {
            case 1, 2 -> halfBase;
            case 3, 4 -> -halfBase;
            default -> 0.0f;
        };
    }

    private CubeParticle spawnCube() {
        float vz;
        float vy;
        float vx;
        float r2 = 12.0f;
        boolean falling = this.animation.is("Падение");
        int life = falling ? 260 + this.random.nextInt(220) : 420 + this.random.nextInt(420);
        double x2 = Cubes.mc.player.getX() + (this.random.nextDouble() * 2.0 - 1.0) * (double)r2;
        double y2 = falling ? Cubes.mc.player.getY() + 4.0 + this.random.nextDouble() * ((double)r2 * 0.55) : Cubes.mc.player.getY() + 2.0 + this.random.nextDouble() * ((double)r2 * 0.8);
        double z2 = Cubes.mc.player.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * (double)r2;
        float speedMult = this.speed.get();
        if (falling) {
            vx = (this.random.nextFloat() - 0.5f) * 0.008f * speedMult;
            vy = (-0.012f - this.random.nextFloat() * 0.012f) * speedMult;
            vz = (this.random.nextFloat() - 0.5f) * 0.008f * speedMult;
        } else {
            float yaw = this.random.nextFloat() * 360.0f;
            float vel = (0.01f + this.random.nextFloat() * 0.02f) * speedMult;
            vx = -MathHelper.sin((float)((float)Math.toRadians(yaw))) * vel;
            vz = MathHelper.cos((float)((float)Math.toRadians(yaw))) * vel;
            vy = (this.random.nextFloat() - 0.5f) * 0.01f * speedMult;
        }
        return new CubeParticle(x2, y2, z2, vx, vy, vz, this.random.nextFloat() * 360.0f, this.random.nextFloat() * 360.0f, this.random.nextFloat() * 360.0f, (this.random.nextFloat() - 0.5f) * 1.5f, (this.random.nextFloat() - 0.5f) * 1.5f, (this.random.nextFloat() - 0.5f) * 1.5f, life, (float)(this.random.nextDouble() * Math.PI * 2.0), this.random.nextFloat() * 10.0f);
    }

    private static int colorToInt(float r2, float g2, float b2, float a2) {
        return (int)(a2 * 255.0f) << 24 | (int)(r2 * 255.0f) << 16 | (int)(g2 * 255.0f) << 8 | (int)(b2 * 255.0f);
    }

    private static class CubeParticle {
        double x;
        double y;
        double z;
        float vx;
        float vy;
        float vz;
        float rotX;
        float rotY;
        float rotZ;
        float rotSpeedX;
        float rotSpeedY;
        float rotSpeedZ;
        float wobblePhase;
        float wobbleOffset;
        float renderAlpha;
        int life;
        int maxLife;

        CubeParticle(double x2, double y2, double z2, float vx, float vy, float vz, float rotX, float rotY, float rotZ, float rotSpeedX, float rotSpeedY, float rotSpeedZ, int life, float wobblePhase, float wobbleOffset) {
            this.x = x2;
            this.y = y2;
            this.z = z2;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.rotX = rotX;
            this.rotY = rotY;
            this.rotZ = rotZ;
            this.rotSpeedX = rotSpeedX;
            this.rotSpeedY = rotSpeedY;
            this.rotSpeedZ = rotSpeedZ;
            this.life = this.maxLife = life;
            this.wobblePhase = wobblePhase;
            this.wobbleOffset = wobbleOffset;
        }
    }
}

