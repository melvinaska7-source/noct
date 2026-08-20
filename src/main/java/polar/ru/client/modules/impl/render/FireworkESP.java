package polar.ru.client.modules.impl.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.Fonts;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class FireworkESP
extends Module {
    public static FireworkESP INSTANCE = new FireworkESP();
    private final FloatSetting interval = new FloatSetting("Интервал (мс)", 100.0f, 10.0f, 1000.0f, 10.0f);
    private final FloatSetting lifetime = new FloatSetting("Время жизни (мс)", 1000.0f, 100.0f, 5000.0f, 100.0f);
    private final Matrix4f lastProjectionMatrix = new Matrix4f();
    private final Quaternionf lastCameraRotation = new Quaternionf();
    private Vec3d lastCameraPos = Vec3d.ZERO;
    private float lastTickDelta;
    private final Map<Integer, FireworkData> fireworks = new HashMap<Integer, FireworkData>();

    public FireworkESP() {
        super("FireworkESP", "Показывает теги и трейлы фейерверков", Module.ModuleCategory.RENDER);
        this.addSettings(this.interval, this.lifetime);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.fireworks.clear();
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        this.lastProjectionMatrix.set((Matrix4fc)event.getProjectionMatrix());
        this.lastCameraPos = event.getCamera().getPos();
        this.lastCameraRotation.set((Quaternionfc)event.getCamera().getRotation());
        this.lastTickDelta = event.getTickDelta();
        if (FireworkESP.mc.world == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        this.fireworks.entrySet().removeIf(entry -> {
            Entity entity = FireworkESP.mc.world.getEntityById(((Integer)entry.getKey()).intValue());
            boolean isDead = entity == null || !entity.isAlive();
            ((FireworkData)entry.getValue()).points.removeIf(p2 -> (float)(currentTime - p2.timestamp) > this.lifetime.get());
            return isDead && ((FireworkData)entry.getValue()).points.isEmpty();
        });
        for (Entity entity : FireworkESP.mc.world.getEntities()) {
            if (!(entity instanceof FireworkRocketEntity) || !entity.isAlive()) continue;
            FireworkData data = this.fireworks.computeIfAbsent(entity.getId(), k2 -> new FireworkData());
            if (!((float)(currentTime - data.lastSpawnTime) >= this.interval.get())) continue;
            Vec3d pos = new Vec3d(MathHelper.lerp((double)this.lastTickDelta, (double)entity.lastRenderX, (double)entity.getX()), MathHelper.lerp((double)this.lastTickDelta, (double)entity.lastRenderY, (double)entity.getY()) + 0.5, MathHelper.lerp((double)this.lastTickDelta, (double)entity.lastRenderZ, (double)entity.getZ()));
            float ageInSeconds = (float)entity.age / 20.0f;
            data.points.add(new TrailPoint(pos, currentTime, ageInSeconds));
            data.lastSpawnTime = currentTime;
        }
    }

    @EventLink
    public void onRender2D(EventRender.Default event) {
        if (FireworkESP.mc.player == null || FireworkESP.mc.world == null) {
            return;
        }
        MatrixStack matrices = event.getContext().getMatrices();
        ItemStack icon = new ItemStack((ItemConvertible)Items.FIREWORK_ROCKET);
        Font font = Fonts.getFont("sf_regular", 14);
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<Integer, FireworkData> entry : this.fireworks.entrySet()) {
            Vec3d currentPos;
            Vec3d screen;
            FireworkData data = entry.getValue();
            for (TrailPoint p2 : data.points) {
                screen = this.worldToScreen(p2.pos);
                if (screen == null) continue;
                float progress = 1.0f - (float)(currentTime - p2.timestamp) / this.lifetime.get();
                progress = MathHelper.clamp((float)progress, (float)0.0f, (float)1.0f);
                String text = String.format("%.1fs", Float.valueOf(p2.ageSec));
                this.renderIconRect(event, matrices, font, icon, screen, progress, text);
            }
            Entity entity = FireworkESP.mc.world.getEntityById(entry.getKey().intValue());
            if (!(entity instanceof FireworkRocketEntity) || !entity.isAlive() || (screen = this.worldToScreen(currentPos = new Vec3d(MathHelper.lerp((double)this.lastTickDelta, (double)entity.lastRenderX, (double)entity.getX()), MathHelper.lerp((double)this.lastTickDelta, (double)entity.lastRenderY, (double)entity.getY()) + 0.5, MathHelper.lerp((double)this.lastTickDelta, (double)entity.lastRenderZ, (double)entity.getZ())))) == null) continue;
            String text = String.format("%.1fs", Float.valueOf((float)entity.age / 20.0f));
            this.renderIconRect(event, matrices, font, icon, screen, 1.0f, text);
        }
    }

    private void renderIconRect(EventRender.Default event, MatrixStack matrices, Font font, ItemStack icon, Vec3d screen, float progress, String text) {
        float iconScale = 0.6f;
        float rectHeight = 12.0f;
        float padding = 2.5f;
        float gap = 2.0f;
        float textYOffset = 3.5f;
        float animScale = 0.35f + 0.65f * progress;
        int alpha = (int)(200.0f * progress);
        if (alpha <= 5) {
            return;
        }
        int bgColor = alpha << 24 | 0xA0A0A;
        int textColor = alpha << 24 | 0xFFFFFF;
        float textWidth = font != null ? font.getStringWidth(text) : 0.0f;
        float iconWidth = 16.0f * iconScale;
        float totalWidth = padding + iconWidth + gap + textWidth + padding;
        matrices.push();
        matrices.translate(screen.x, screen.y, 0.0);
        matrices.scale(animScale, animScale, 1.0f);
        RenderUtils.drawRoundedRect(matrices, -totalWidth / 2.0f, -rectHeight / 2.0f, totalWidth, rectHeight, 0.0f, bgColor);
        float currentX = -totalWidth / 2.0f + padding;
        matrices.push();
        matrices.translate(currentX, -(16.0f * iconScale) / 2.0f, 0.0f);
        matrices.scale(iconScale, iconScale, 1.0f);
        event.getContext().drawItem(icon, 0, 0);
        matrices.pop();
        currentX += iconWidth + gap;
        if (font != null) {
            font.drawString(matrices, text, currentX, -rectHeight / 2.0f + textYOffset + 0.5f, textColor);
        }
        matrices.pop();
    }

    private Vec3d worldToScreen(Vec3d worldPos) {
        Vector3f relative = new Vector3f((float)(worldPos.x - this.lastCameraPos.x), (float)(worldPos.y - this.lastCameraPos.y), (float)(worldPos.z - this.lastCameraPos.z));
        relative.rotate((Quaternionfc)new Quaternionf((Quaternionfc)this.lastCameraRotation).conjugate());
        Vector4f clip = new Vector4f(relative.x, relative.y, relative.z, 1.0f);
        this.lastProjectionMatrix.transform(clip);
        float w2 = clip.w;
        if (w2 <= 1.0E-5f) {
            return null;
        }
        float ndcX = clip.x / w2;
        float ndcY = clip.y / w2;
        float ndcZ = clip.z / w2;
        float screenX = (ndcX * 0.5f + 0.5f) * (float)mc.getWindow().getScaledWidth();
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * (float)mc.getWindow().getScaledHeight();
        if (Float.isNaN(screenX) || Float.isNaN(screenY) || Float.isInfinite(screenX) || Float.isInfinite(screenY)) {
            return null;
        }
        return new Vec3d((double)screenX, (double)screenY, (double)ndcZ);
    }

    private static class FireworkData {
        long lastSpawnTime;
        final List<TrailPoint> points = new ArrayList<TrailPoint>();

        private FireworkData() {
        }
    }

    private static class TrailPoint {
        final Vec3d pos;
        final long timestamp;
        final float ageSec;

        TrailPoint(Vec3d pos, long timestamp, float ageSec) {
            this.pos = pos;
            this.timestamp = timestamp;
            this.ageSec = ageSec;
        }
    }
}

