package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Optional;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventAttackEntity;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class HitMarker
extends Module {
    public static HitMarker INSTANCE = new HitMarker();
    private final FloatSetting size = new FloatSetting("Размер", 0.5f, 0.1f, 2.0f, 0.05f);
    private final FloatSetting fadeInTime = new FloatSetting("Время появления", 100.0f, 50.0f, 500.0f, 10.0f);
    private final FloatSetting displayTime = new FloatSetting("Время показа", 300.0f, 100.0f, 1000.0f, 50.0f);
    private final FloatSetting fadeOutTime = new FloatSetting("Время исчезновения", 200.0f, 50.0f, 500.0f, 10.0f);
    private final BooleanSetting glow = new BooleanSetting("Свечение", true);
    private final BooleanSetting scale = new BooleanSetting("Анимация масштаба", true);
    private final ArrayList<HitMarkerData> hitMarkers = new ArrayList();

    public HitMarker() {
        super("HitMarker", "Показывает маркер при ударе", Module.ModuleCategory.RENDER);
        this.addSettings(this.size, this.fadeInTime, this.displayTime, this.fadeOutTime, this.glow, this.scale);
    }

    @Override
    public void onDisable() {
        this.hitMarkers.clear();
        super.onDisable();
    }

    private Identifier getTexture() {
        return Identifier.of((String)"polar", (String)"textures/cross/cross.png");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventLink
    public void onAttack(EventAttackEntity event) {
        if (HitMarker.mc.player == null || HitMarker.mc.world == null) {
            return;
        }
        Entity target = event.getTarget();
        if (target != null) {
            ArrayList<HitMarkerData> arrayList = this.hitMarkers;
            synchronized (arrayList) {
                this.hitMarkers.add(new HitMarkerData(this.resolveHitPosition((Entity)event.getPlayer(), target), System.currentTimeMillis(), (long)this.fadeInTime.get(), (long)this.displayTime.get(), (long)this.fadeOutTime.get()));
            }
        }
    }

    private Vec3d resolveHitPosition(Entity attacker, Entity target) {
        Vec3d fallback = new Vec3d(target.getX(), target.getY() + (double)target.getHeight() / 2.0, target.getZ());
        if (attacker == null) {
            return fallback;
        }
        Vec3d eyePos = attacker.getCameraPosVec(1.0f);
        Vec3d lookVec = attacker.getRotationVec(1.0f);
        Vec3d targetCenter = target.getBoundingBox().getCenter();
        double distance = Math.max(eyePos.distanceTo(targetCenter) + 1.0, 6.0);
        Vec3d reachPos = eyePos.add(lookVec.multiply(distance));
        Optional hitPos = target.getBoundingBox().raycast(eyePos, reachPos);
        if (hitPos.isPresent()) {
            return (Vec3d)hitPos.get();
        }
        return eyePos.add(lookVec.multiply(eyePos.distanceTo(targetCenter)));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventLink
    public void onRender3D(Event3DRender e2) {
        ArrayList<HitMarkerData> renderList;
        if (HitMarker.mc.player == null || HitMarker.mc.world == null) {
            return;
        }
        ArrayList<HitMarkerData> arrayList = this.hitMarkers;
        synchronized (arrayList) {
            this.hitMarkers.removeIf(HitMarkerData::isDead);
        }
        if (this.hitMarkers.isEmpty()) {
            return;
        }
        MatrixStack matrices = e2.getMatrices();
        Vec3d camera = HitMarker.mc.gameRenderer.getCamera().getPos();
        Identifier texture = this.getTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        if (this.glow.isState()) {
            RenderSystem.blendFunc((int)770, (int)1);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.setShaderTexture((int)0, (Identifier)texture);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        ArrayList<HitMarkerData> arrayList2 = this.hitMarkers;
        synchronized (arrayList2) {
            renderList = new ArrayList<HitMarkerData>(this.hitMarkers);
        }
        int color = ColorUtils.getThemeColor();
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        for (HitMarkerData marker : renderList) {
            float alpha = marker.getAlpha();
            if (alpha <= 0.0f) continue;
            double x2 = marker.position.x - camera.x;
            double y2 = marker.position.y - camera.y;
            double z2 = marker.position.z - camera.z;
            matrices.push();
            matrices.translate((float)x2, (float)y2, (float)z2);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-HitMarker.mc.gameRenderer.getCamera().getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(HitMarker.mc.gameRenderer.getCamera().getPitch()));
            float currentSize = this.size.get();
            if (this.scale.isState()) {
                float scaleMultiplier = marker.getScaleMultiplier();
                currentSize *= scaleMultiplier;
            }
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float half = currentSize / 2.0f;
            int alphaInt = (int)(alpha * 255.0f);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, -half, -half, 0.0f).texture(0.0f, 1.0f).color(r2, g2, b2, alphaInt);
            buffer.vertex(matrix, -half, half, 0.0f).texture(0.0f, 0.0f).color(r2, g2, b2, alphaInt);
            buffer.vertex(matrix, half, half, 0.0f).texture(1.0f, 0.0f).color(r2, g2, b2, alphaInt);
            buffer.vertex(matrix, half, -half, 0.0f).texture(1.0f, 1.0f).color(r2, g2, b2, alphaInt);
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
            matrices.pop();
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    static class HitMarkerData {
        Vec3d position;
        long birthTime;
        long fadeInTime;
        long displayTime;
        long fadeOutTime;

        HitMarkerData(Vec3d position, long birthTime, long fadeInTime, long displayTime, long fadeOutTime) {
            this.position = position;
            this.birthTime = birthTime;
            this.fadeInTime = fadeInTime;
            this.displayTime = displayTime;
            this.fadeOutTime = fadeOutTime;
        }

        boolean isDead() {
            return System.currentTimeMillis() - this.birthTime >= this.fadeInTime + this.displayTime + this.fadeOutTime;
        }

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - this.birthTime;
            if (elapsed < this.fadeInTime) {
                float progress = (float)elapsed / (float)this.fadeInTime;
                return this.easeOutCubic(progress);
            }
            if (elapsed < this.fadeInTime + this.displayTime) {
                return 1.0f;
            }
            long fadeOutElapsed = elapsed - this.fadeInTime - this.displayTime;
            float progress = Math.min(1.0f, (float)fadeOutElapsed / (float)this.fadeOutTime);
            return 1.0f - this.easeInCubic(progress);
        }

        float getScaleMultiplier() {
            long elapsed = System.currentTimeMillis() - this.birthTime;
            if (elapsed < this.fadeInTime) {
                float progress = (float)elapsed / (float)this.fadeInTime;
                return 0.5f + 0.5f * this.easeOutBack(progress);
            }
            if (elapsed < this.fadeInTime + this.displayTime) {
                return 1.0f;
            }
            long fadeOutElapsed = elapsed - this.fadeInTime - this.displayTime;
            float progress = Math.min(1.0f, (float)fadeOutElapsed / (float)this.fadeOutTime);
            return 1.0f - 0.3f * this.easeInCubic(progress);
        }

        private float easeOutCubic(float x2) {
            return 1.0f - (float)Math.pow(1.0 - (double)x2, 3.0);
        }

        private float easeInCubic(float x2) {
            return x2 * x2 * x2;
        }

        private float easeOutBack(float x2) {
            float c1 = 1.70158f;
            float c3 = c1 + 1.0f;
            return 1.0f + c3 * (float)Math.pow((double)x2 - 1.0, 3.0) + c1 * (float)Math.pow((double)x2 - 1.0, 2.0);
        }
    }
}

