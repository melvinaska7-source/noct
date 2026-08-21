package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import java.util.Locale;
import net.minecraft.util.math.MathHelper;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.event.list.FireworkEvent;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.list.combat.KillAura;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.base.Instance;
import zov.alphadlc.util.render.providers.ColorProvider;
import zov.alphadlc.util.render.renderers.DrawUtil;
import zov.alphadlc.util.render.msdf.Fonts;

@ModuleInformation(moduleName = "SuperFirework", moduleDesc = "Ускорение фейерверков", moduleCategory = ModuleCategory.MOVEMENT)
public final class ElytraBooster extends Module {
    public static final ElytraBooster INSTANCE = new ElytraBooster();
    
    private static final int[] AI_YAW_VECTORS = {-45, 45, 135, -135};
    private static final int[] AI_PITCH_VECTORS = {-45, 45};
    
    private final BooleanSetting showOverlay = new BooleanSetting("Показать инфо", true);
    private final BooleanSetting boost = new BooleanSetting("Ускорение", true);
    private final ModeSetting mode = new ModeSetting("Режим ускорения", "Bravo", "Bravo", "Кастомный", "ReallyWorld").setVisible(this.boost::getValue);
    
    private final BooleanSetting maxspeed = new BooleanSetting("Скорость по Углам", false).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный"));
    private final SliderSetting speedxz = new SliderSetting("Скорость XZ", 1.65, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && !maxspeed.getValue());
    private final SliderSetting speedy = new SliderSetting("Скорость Y", 1.59, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && !maxspeed.getValue());
    
    private final SliderSetting yaw05 = new SliderSetting("Yaw 0-5°", 1.6, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting yaw510 = new SliderSetting("Yaw 5-10°", 1.62, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting yaw1015 = new SliderSetting("Yaw 10-15°", 1.65, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting yaw1520 = new SliderSetting("Yaw 15-20°", 1.68, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting yaw2025 = new SliderSetting("Yaw 20-25°", 1.74, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting yaw2530 = new SliderSetting("Yaw 25-30°", 1.8, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting yaw3035 = new SliderSetting("Yaw 30-35°", 1.8, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting yaw3540 = new SliderSetting("Yaw 35-40°", 1.8, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting yaw4045 = new SliderSetting("Yaw 40-45°", 1.82, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    
    private final SliderSetting pitch05 = new SliderSetting("Pitch 0-5°", 1.59, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting pitch510 = new SliderSetting("Pitch 5-10°", 1.6, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting pitch1015 = new SliderSetting("Pitch 10-15°", 1.61, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting pitch1520 = new SliderSetting("Pitch 15-20°", 1.62, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting pitch2025 = new SliderSetting("Pitch 20-25°", 1.68, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting pitch2530 = new SliderSetting("Pitch 25-30°", 1.74, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting pitch3035 = new SliderSetting("Pitch 30-35°", 1.95, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting pitch3540 = new SliderSetting("Pitch 35-40°", 2.0, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    private final SliderSetting pitch4045 = new SliderSetting("Pitch 40-45°", 2.2, 1.5, 2.5, 0.01).setVisible(() -> this.boost.getValue() && this.mode.is("Кастомный") && maxspeed.getValue());
    
    private final SliderSetting[] yawSettings = new SliderSetting[]{
        this.yaw05, this.yaw510, this.yaw1015, this.yaw1520, this.yaw2025, 
        this.yaw2530, this.yaw3035, this.yaw3540, this.yaw4045
    };
    private final SliderSetting[] pitchSettings = new SliderSetting[]{
        this.pitch05, this.pitch510, this.pitch1015, this.pitch1520, this.pitch2025, 
        this.pitch2530, this.pitch3035, this.pitch3540, this.pitch4045
    };

    public BoostValues computeBoost(float yaw, float pitch) {
        if (!this.boost.getValue() || this.mc.player == null) {
            return new BoostValues(1.5f, 1.5f);
        }
        KillAura aura = Instance.get(KillAura.class);
        if (aura != null && aura.isEnabled()) {
            yaw = MathHelper.wrapDegrees((float)aura.lastYaw);
            pitch = aura.lastPitch;
        }
        if (pitch < -75.0f) {
            return new BoostValues(1.5f, 1.5f);
        }
        if (this.mode.is("Кастомный")) {
            return handleCustomMode(yaw, pitch);
        }
        if (this.mode.is("Bravo")) {
            return new BoostValues(1.8f, 1.8f);
        }
        if (this.mode.is("ReallyWorld")) {
            return handleReallyWorldMode(yaw, pitch);
        }
        return new BoostValues(1.5f, 1.5f);
    }
    
    private BoostValues handleCustomMode(float yaw, float pitch) {
        if (!maxspeed.getValue()) {
            return new BoostValues(speedxz.getFloatValue(), speedy.getFloatValue());
        }
        
        float convertedYaw = convertAngleToRange(MathHelper.wrapDegrees(yaw));
        float convertedPitch = convertAngleToRange(Math.abs(pitch));
        
        float xzSpeed = getSpeedForYaw(convertedYaw);
        float ySpeed = getSpeedForPitch(convertedPitch);
        
        if (ySpeed > xzSpeed) {
            xzSpeed = ySpeed;
        }
        
        return new BoostValues(xzSpeed, ySpeed);
    }
    
    private BoostValues handleReallyWorldMode(float yaw, float pitch) {
        float speed = getAiBoost(pitch, yaw, false, true);
        return new BoostValues(speed, speed);
    }
    
    private float getSpeedForYaw(float yaw) {
        int index = (int) (yaw / 5.0f);
        if (index >= yawSettings.length) index = yawSettings.length - 1;
        if (index < 0) index = 0;
        return yawSettings[index].getFloatValue();
    }
    
    private float getSpeedForPitch(float pitch) {
        int index = (int) (pitch / 5.0f);
        if (index >= pitchSettings.length) index = pitchSettings.length - 1;
        if (index < 0) index = 0;
        return pitchSettings[index].getFloatValue();
    }
    
    private float convertAngleToRange(float angle) {
        float absAngle = Math.abs(angle);
        if (absAngle > 90.0f) absAngle = 180.0f - absAngle;
        if (absAngle > 45.0f) absAngle = 90.0f - absAngle;
        return absAngle;
    }
    
    private float getAiBoost(float pitch, float yaw, boolean isBravo, boolean applyRwCap) {
        if (Math.abs(pitch) > 55.0f) return 1.55f;
        float boost = adjustBoostForYaw(yaw, applyRwCap);
        boost = adjustBoostForPitch(pitch, boost);
        boost = Math.max(isBravo ? 1.65f : 1.6f, boost);
        return Math.min(boost, isBravo ? 1.9f : 2.2f);
    }
    
    private float adjustBoostForYaw(float yaw, boolean applyRwCap) {
        int idx = findClosestVector(yaw, AI_YAW_VECTORS);
        if (idx == -1) return 1.6f;
        float dist = Math.abs(MathHelper.wrapDegrees(yaw) - AI_YAW_VECTORS[idx]);
        float maxBoost = 2.2f, minBoostVal = 1.6f, maxDistance = 12f, smartBoost = 0.0f;
        if (dist <= maxDistance) {
            float ratio = dist / maxDistance;
            smartBoost = maxBoost - (maxBoost - minBoostVal) * ratio;
        }
        float variableSpeed = getVariableSpeed(dist);
        float finalSpeed = Math.max(smartBoost, variableSpeed);
        return applyRwCap ? Math.min(finalSpeed, 1.8f) : finalSpeed;
    }
    
    private float adjustBoostForPitch(float pitch, float boost) {
        int idx = findClosestVector(pitch, AI_PITCH_VECTORS);
        if (idx == -1) return boost;
        float dist = Math.abs(Math.abs(pitch) - Math.abs(AI_PITCH_VECTORS[idx]));
        if (dist < 30.0f) boost += 0.4f * (1.0f - dist / 30.0f);
        return boost;
    }
    
    private static float getVariableSpeed(float dist) {
        float[] thresholds = {4, 8, 11, 15, 21, 28};
        float[] speeds = {2.2f, 2.1f, 2.0f, 1.9f, 1.8f, 1.7f, 1.6f};
        int level = 0;
        while (level < thresholds.length && dist >= thresholds[level]) level++;
        return speeds[level];
    }
    
    private static int findClosestVector(float angle, int[] vectors) {
        int minIdx = -1;
        float minDist = Float.MAX_VALUE;
        for (int i = 0; i < vectors.length; i++) {
            float d = Math.abs(MathHelper.wrapDegrees(angle) - vectors[i]);
            if (d < minDist) { 
                minDist = d; 
                minIdx = i; 
            }
        }
        return minIdx;
    }



    @Subscribe
    private void onHud(EventHUD event) {
        if (!this.showOverlay.getValue() || this.mc.player == null) {
            return;
        }
        if (!this.mc.player.isGliding()) {
            return;
        }
        float yaw = this.mc.player.getYaw();
        float pitch = this.mc.player.getPitch();
        KillAura aura = Instance.get(KillAura.class);
        if (aura != null && aura.isEnabled()) {
            yaw = MathHelper.wrapDegrees((float)aura.lastYaw);
            pitch = aura.lastPitch;
        }
        
        String pitchText = String.format(Locale.US, "Y: %.1f°", pitch);
        String yawText = String.format(Locale.US, "XZ: %.1f°", MathHelper.wrapDegrees(yaw));
        String text = pitchText + "  ·  " + yawText;
        
        int color = ColorProvider.getColorClient();
        int sw = this.mc.getWindow().getScaledWidth();
        int sh = this.mc.getWindow().getScaledHeight();
        float w = Fonts.SFBOLD.get().getWidth(text, 8.0f);
        float x = (float)sw / 2.0f - w / 2.0f;
        float y = (float)sh / 2.0f + 32.0f;
        DrawUtil.drawText(Fonts.SFBOLD.get(), text, x, y, ColorProvider.setAlpha(color, 230), 8.0f);
    }

    @Subscribe
    private void onFirework(FireworkEvent event) {
        if (this.mc.player == null || !this.boost.getValue() || event.getBoostedEntity() != this.mc.player) {
            return;
        }
        float yaw = this.mc.player.getYaw();
        float pitch = this.mc.player.getPitch();
        KillAura aura = Instance.get(KillAura.class);
        if (aura != null && aura.isEnabled()) {
            yaw = MathHelper.wrapDegrees((float)aura.lastYaw);
            pitch = aura.lastPitch;
        }
        BoostValues values = this.computeBoost(yaw, pitch);
        event.setSpeed(Math.max(values.speedXZ(), values.speedY()));
    }

    public record BoostValues(float speedXZ, float speedY) {
    }
}
