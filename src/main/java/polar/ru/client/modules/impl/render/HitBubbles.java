package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.concurrent.CopyOnWriteArrayList;
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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventAttackEntity;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.Module;

public class HitBubbles
extends Module {
    public static HitBubbles INSTANCE = new HitBubbles();
    private static final long LIFE_MS = 1600L;
    private final CopyOnWriteArrayList<HitBubble> bubbles = new CopyOnWriteArrayList();
    private final Identifier bubbleTexture = Identifier.of((String)"polar", (String)"textures/hitbubble/bubble.png");

    public HitBubbles() {
        super("HitBubbles", "Круг при ударе игрока", Module.ModuleCategory.RENDER);
    }

    @Override
    public void onDisable() {
        this.bubbles.clear();
        super.onDisable();
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        long now = System.currentTimeMillis();
        this.bubbles.removeIf(b2 -> now - b2.spawnTime() >= 1600L);
    }

    @EventLink
    public void onAttack(EventAttackEntity event) {
        if (event == null || event.getTarget() == null) {
            return;
        }
        Entity var_1297_2 = event.getTarget();
        if (!(var_1297_2 instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity)var_1297_2;
        if (event.getPlayer() == null) {
            return;
        }
        Vec3d sideDir = this.getHitSideDirection(living, event.getPlayer().getPos());
        Vec3d pos = this.getHitPosition(living, sideDir);
        float sideYaw = (float)Math.toDegrees(Math.atan2(sideDir.x, sideDir.z));
        this.bubbles.add(new HitBubble(pos, System.currentTimeMillis(), (float)(Math.random() * 360.0), sideYaw));
    }

    @EventLink
    public void onWorldRender(Event3DRender event) {
        if (this.bubbles.isEmpty() || HitBubbles.mc.player == null) {
            return;
        }
        MatrixStack stack = event.getMatrices();
        Vec3d cameraPos = event.getCamera().getPos();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.bubbleTexture);
        long now = System.currentTimeMillis();
        for (HitBubble bubble : this.bubbles) {
            this.renderSingleBubble(stack, cameraPos, bubble, now);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderSingleBubble(MatrixStack stack, Vec3d cameraPos, HitBubble bubble, long now) {
        float progress = (float)(now - bubble.spawnTime()) / 1600.0f;
        if (progress >= 1.0f) {
            return;
        }
        float inPhase = Math.max(0.0f, Math.min(1.0f, progress / 0.22f));
        float outPhase = Math.max(0.0f, Math.min(1.0f, (progress - 0.225f) / 0.4f));
        float scaleIn = inPhase * inPhase * (3.0f - 2.0f * inPhase);
        float scaleOut = 1.0f - outPhase * outPhase;
        float scale = 0.02f + 1.55f * scaleIn * scaleOut;
        float alpha = 1.0f - outPhase * outPhase * outPhase;
        float rotation = (float)(now - bubble.spawnTime()) / 1.5f + bubble.spinSeed();
        Vec3d rel = bubble.pos().subtract(cameraPos);
        int color = ColorUtils.multAlpha(ColorUtils.getThemeColor(), alpha);
        stack.push();
        stack.translate(rel.x, rel.y, rel.z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bubble.sideYaw()));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-210.0f));
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        this.drawTexturedQuad(stack, -scale * 0.5f, -scale * 0.5f, scale, scale, color);
        stack.pop();
    }

    private void drawTexturedQuad(MatrixStack stack, float x2, float y2, float width, float height, int color) {
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        int a2 = color >> 24 & 0xFF;
        if (a2 <= 0) {
            return;
        }
        Matrix4f mat = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(mat, x2, y2, 0.0f).texture(0.0f, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(mat, x2, y2 + height, 0.0f).texture(0.0f, 1.0f).color(r2, g2, b2, a2);
        buffer.vertex(mat, x2 + width, y2 + height, 0.0f).texture(1.0f, 1.0f).color(r2, g2, b2, a2);
        buffer.vertex(mat, x2 + width, y2, 0.0f).texture(1.0f, 0.0f).color(r2, g2, b2, a2);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private Vec3d getHitSideDirection(LivingEntity target, Vec3d attackerPos) {
        Vec3d dir = attackerPos.subtract(target.getPos());
        dir = new Vec3d(dir.x, 0.0, dir.z);
        if (dir.lengthSquared() < 1.0E-4) {
            Vec3d fallback = target.getRotationVector();
            dir = new Vec3d(fallback.x, 0.0, fallback.z);
        }
        if (dir.lengthSquared() < 1.0E-4) {
            dir = new Vec3d(0.0, 0.0, 1.0);
        }
        return dir.normalize();
    }

    private Vec3d getHitPosition(LivingEntity target, Vec3d sideDir) {
        Vec3d head = new Vec3d(target.getX(), target.getY() + (double)target.getHeight() + 0.18, target.getZ());
        return head.add(sideDir.multiply(0.1));
    }

    private record HitBubble(Vec3d pos, long spawnTime, float spinSeed, float sideYaw) {
    }
}

