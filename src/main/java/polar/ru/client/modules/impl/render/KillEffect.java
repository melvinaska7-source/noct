package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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

public class KillEffect
extends Module {
    public static KillEffect INSTANCE = new KillEffect();
    private static final Identifier GLOW_TEX = Identifier.of((String)"polar", (String)"textures/particle/bloom.png");
    private static final float DURATION = 1.5f;
    private static final float HEIGHT = 4.0f;
    private static final float MAX_RADIUS = 1.0f;
    private static final int SLICES = 40;
    private final Map<Entity, Vec3d> trackedEntities = new IdentityHashMap<Entity, Vec3d>();
    private final List<ActiveEffect> effects = new ArrayList<ActiveEffect>();

    public KillEffect() {
        super("KillEffect", "Эффект при исчезновении цели", Module.ModuleCategory.RENDER);
    }

    @Override
    public void onDisable() {
        this.trackedEntities.clear();
        this.effects.clear();
        super.onDisable();
    }

    @EventLink
    public void onAttack(EventAttackEntity event) {
        if (KillEffect.mc.player == null || KillEffect.mc.world == null) {
            return;
        }
        Entity target = event.getTarget();
        if (target instanceof LivingEntity && target != KillEffect.mc.player) {
            this.trackedEntities.put(target, target.getPos());
        }
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        if (KillEffect.mc.world == null || KillEffect.mc.player == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<Entity, Vec3d>> trackIterator = this.trackedEntities.entrySet().iterator();
        while (trackIterator.hasNext()) {
            Map.Entry<Entity, Vec3d> entry = trackIterator.next();
            Entity entity = entry.getKey();
            if (entity.isRemoved() || !entity.isAlive()) {
                this.effects.add(new ActiveEffect(entry.getValue(), currentTime));
                trackIterator.remove();
                continue;
            }
            entry.setValue(entity.getPos());
        }
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.blendFuncSeparate((GlStateManager.SrcFactor)GlStateManager.SrcFactor.SRC_ALPHA, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE, (GlStateManager.SrcFactor)GlStateManager.SrcFactor.ZERO, (GlStateManager.DstFactor)GlStateManager.DstFactor.ONE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)GLOW_TEX);
        Iterator<ActiveEffect> effectIterator = this.effects.iterator();
        while (effectIterator.hasNext()) {
            ActiveEffect effect = effectIterator.next();
            float progress = (float)(currentTime - effect.startTime) / 1500.0f;
            if (progress >= 1.0f) {
                effectIterator.remove();
                continue;
            }
            this.renderEffect(event.getMatrices(), effect, KillEffect.mc.gameRenderer.getCamera().getPos(), progress);
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderEffect(MatrixStack matrices, ActiveEffect effect, Vec3d cameraPos, float progress) {
        float t2;
        int i2;
        int color = ColorUtils.getThemeColor();
        float r2 = (float)(color >> 16 & 0xFF) / 255.0f;
        float g2 = (float)(color >> 8 & 0xFF) / 255.0f;
        float b2 = (float)(color & 0xFF) / 255.0f;
        float globalAlpha = progress < 0.15f ? progress / 0.15f : (progress > 0.75f ? (1.0f - progress) / 0.25f : 1.0f);
        float sliceHeight = 0.1f;
        for (i2 = 0; i2 < 40; ++i2) {
            t2 = (float)i2 / 40.0f;
            float y2 = t2 * 4.0f;
            float radius = 1.0f * MathHelper.sin((float)((float)(Math.PI * (double)t2)));
            float sliceAlpha = (1.0f - Math.abs(2.0f * t2 - 1.0f) * 0.25f) * globalAlpha;
            Vec3d pos = effect.position.add(0.0, (double)y2, 0.0);
            this.renderGlow(matrices, cameraPos, pos, radius * 2.1f, r2, g2, b2, sliceAlpha * 0.22f);
            this.renderGlow(matrices, cameraPos, pos, radius * 1.15f, r2, g2, b2, sliceAlpha * 0.48f);
            this.renderGlow(matrices, cameraPos, pos, radius * 0.55f, r2, g2, b2, sliceAlpha * 0.85f);
        }
        for (i2 = 0; i2 < 10; ++i2) {
            t2 = (float)i2 / 10.0f;
            float spread = 1.0f - t2;
            float bottomRadius = 3.6f * spread;
            float bottomAlpha = spread * spread * globalAlpha * 0.38f;
            Vec3d bPos = effect.position.add(0.0, (double)(t2 * 0.45f), 0.0);
            this.renderGlow(matrices, cameraPos, bPos, bottomRadius, r2, g2, b2, bottomAlpha);
            this.renderGlow(matrices, cameraPos, bPos, bottomRadius * 0.35f, r2, g2, b2, bottomAlpha * 1.7f);
        }
    }

    private void renderGlow(MatrixStack matrices, Vec3d cameraPos, Vec3d position, float size, float r2, float g2, float b2, float a2) {
        if (a2 <= 0.01f) {
            return;
        }
        matrices.push();
        matrices.translate(position.x - cameraPos.x, position.y - cameraPos.y, position.z - cameraPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-KillEffect.mc.gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(KillEffect.mc.gameRenderer.getCamera().getPitch()));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = size * 0.5f;
        int rInt = Math.min(255, (int)(r2 * 255.0f));
        int gInt = Math.min(255, (int)(g2 * 255.0f));
        int bInt = Math.min(255, (int)(b2 * 255.0f));
        int aInt = Math.min(255, (int)(a2 * 255.0f));
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -half, -half, 0.0f).texture(0.0f, 1.0f).color(rInt, gInt, bInt, aInt);
        buffer.vertex(matrix, -half, half, 0.0f).texture(0.0f, 0.0f).color(rInt, gInt, bInt, aInt);
        buffer.vertex(matrix, half, half, 0.0f).texture(1.0f, 0.0f).color(rInt, gInt, bInt, aInt);
        buffer.vertex(matrix, half, -half, 0.0f).texture(1.0f, 1.0f).color(rInt, gInt, bInt, aInt);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        matrices.pop();
    }

    private static class ActiveEffect {
        final Vec3d position;
        final long startTime;

        ActiveEffect(Vec3d position, long startTime) {
            this.position = position;
            this.startTime = startTime;
        }
    }
}

