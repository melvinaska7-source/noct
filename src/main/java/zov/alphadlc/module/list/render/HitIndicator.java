package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.render.renderers.DrawUtil;
import zov.alphadlc.util.render.providers.ColorProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInformation(moduleName = "HitIndicator", moduleDesc = "Показывает красную дугу со стороны, откуда пришел урон", moduleCategory = ModuleCategory.RENDER)
public class HitIndicator extends Module {

    private final SliderSetting radius = new SliderSetting("Радиус", 125.0f, 60.0f, 260.0f, 5.0f);
    private final SliderSetting arcLength = new SliderSetting("Длина дуги", 76.0f, 30.0f, 150.0f, 1.0f);
    private final SliderSetting thickness = new SliderSetting("Толщина", 2.4f, 1.0f, 7.0f, 0.1f);
    private final SliderSetting duration = new SliderSetting("Время показа", 700.0f, 250.0f, 1600.0f, 25.0f);
    private final SliderSetting opacity = new SliderSetting("Прозрачность", 0.9f, 0.2f, 1.0f, 0.05f);
    private final SliderSetting sourceRange = new SliderSetting("Дистанция источника", 32.0f, 6.0f, 64.0f, 1.0f);
    private final BooleanSetting glow = new BooleanSetting("Свечение", true);

    private final List<Indicator> indicators = new ArrayList<>();
    private float lastHealth = -1.0f;

    @Override
    public void onEnable() {
        super.onEnable();
        lastHealth = getTotalHealth();
        indicators.clear();
    }

    @Override
    public void onDisable() {
        indicators.clear();
        lastHealth = -1.0f;
        super.onDisable();
    }

    @Subscribe
    private void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) {
            lastHealth = -1.0f;
            indicators.clear();
            return;
        }

        float currentHealth = getTotalHealth();
        if (lastHealth < 0.0f) {
            lastHealth = currentHealth;
            return;
        }

        if (currentHealth + 0.01f < lastHealth) {
            Float yaw = resolveDamageYaw();
            if (yaw != null) {
                addIndicator(yaw);
            }
        }

        lastHealth = currentHealth;
    }

    @Subscribe
    private void onHud(EventHUD event) {
        if (mc.player == null || mc.world == null || indicators.isEmpty() || mc.options.hudHidden) {
            return;
        }

        long now = System.currentTimeMillis();
        float width = mc.getWindow().getScaledWidth();
        float height = mc.getWindow().getScaledHeight();
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;

        float maxRadius = Math.max(24.0f, Math.min(centerX, centerY) - 8.0f);
        float drawRadius = Math.min(radius.getFloatValue(), maxRadius);

        float cameraYaw = mc.gameRenderer.getCamera().getYaw();

        Iterator<Indicator> iterator = indicators.iterator();
        while (iterator.hasNext()) {
            Indicator indicator = iterator.next();
            float age = now - indicator.spawnTime;
            float lifetime = duration.getFloatValue();

            if (age >= lifetime) {
                iterator.remove();
                continue;
            }

            float progress = MathHelper.clamp(age / lifetime, 0.0f, 1.0f);
            float fadeIn = MathHelper.clamp(age / 90.0f, 0.0f, 1.0f);
            float alpha = opacity.getFloatValue() * fadeIn * (1.0f - progress * progress);

            if (alpha <= 0.02f) {
                continue;
            }

            float relativeYaw = MathHelper.wrapDegrees(indicator.worldYaw - cameraYaw);

            int baseAlpha = (int) (alpha * 255.0f);
            int color = ColorProvider.rgba(255, 35, 45, baseAlpha);

            if (glow.getValue()) {
                int glowAlpha = (int) (baseAlpha * 0.22f);
                int glowColor = ColorProvider.rgba(255, 35, 45, glowAlpha);
                drawArc(centerX, centerY, drawRadius, relativeYaw, arcLength.getFloatValue(), thickness.getFloatValue() + 3.2f, glowColor);
            }

            drawArc(centerX, centerY, drawRadius, relativeYaw, arcLength.getFloatValue(), thickness.getFloatValue(), color);
        }
    }

    private float getTotalHealth() {
        if (mc.player == null) {
            return -1.0f;
        }
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    private void addIndicator(float worldYaw) {
        indicators.add(new Indicator(worldYaw, System.currentTimeMillis()));
        while (indicators.size() > 8) {
            indicators.remove(0);
        }
    }

    private Float resolveDamageYaw() {
        Entity source = getNearestLivingSource();
        if (!isValidSource(source)) {
            return null;
        }

        double dx = source.getX() - mc.player.getX();
        double dz = source.getZ() - mc.player.getZ();

        if (dx * dx + dz * dz < 1.0E-4D) {
            return null;
        }

        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
    }

    private Entity getNearestLivingSource() {
        Entity nearest = null;
        Entity nearestSwinging = null;
        double bestDistance = sourceRange.getFloatValue() * sourceRange.getFloatValue();
        double bestSwingingDistance = bestDistance;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || !isValidSource(entity)) {
                continue;
            }

            double distance = mc.player.squaredDistanceTo(entity);

            // Проверяем, недавно ли атаковал этот моб
            if (living.handSwingTicks < 10 && distance < bestSwingingDistance) {
                bestSwingingDistance = distance;
                nearestSwinging = entity;
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = entity;
            }
        }

        return nearestSwinging != null ? nearestSwinging : nearest;
    }

    private boolean isValidSource(Entity entity) {
        return entity != null
                && entity != mc.player
                && entity.isAlive()
                && mc.player != null
                && mc.player.squaredDistanceTo(entity) <= sourceRange.getFloatValue() * sourceRange.getFloatValue();
    }

    private void drawArc(float centerX, float centerY, float radius, float yaw, float length, float lineWidth, int color) {
        float start = yaw - length / 2.0f;
        float end = yaw + length / 2.0f;

        // Используем DrawUtil.drawRingArc для рисования дуги
        DrawUtil.drawRingArc(centerX, centerY, radius, lineWidth, start, end, color);
    }

    private static class Indicator {
        private final float worldYaw;
        private final long spawnTime;

        private Indicator(float worldYaw, long spawnTime) {
            this.worldYaw = worldYaw;
            this.spawnTime = spawnTime;
        }
    }
}
