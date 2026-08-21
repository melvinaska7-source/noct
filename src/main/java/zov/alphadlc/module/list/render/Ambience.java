package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.event.list.EventWorldRender;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ColorSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.module.settings.impl.ThemeManager;
import zov.alphadlc.util.render.ambience.SkyShaderRenderer;


@ModuleInformation(moduleName = "Ambience", moduleDesc = "Кастомное время и туман", moduleCategory = ModuleCategory.RENDER)
public class Ambience extends Module {

    public final ModeSetting timePreset = new ModeSetting("Время", "День",
            "День", "Рассвет", "Закат", "Сумерки", "Ночь", "Полночь");

    public final BooleanSetting customFog = new BooleanSetting("Туман", false);
    public final SliderSetting fogDistance = new SliderSetting("Дальность тумана", 100, 10, 500, 10)
            .setVisible(() -> customFog.getValue());
    public final SliderSetting fogDensity = new SliderSetting("Плотность тумана", 0.75, 0.0, 2.0, 0.05)
            .setVisible(() -> customFog.getValue());
    public final BooleanSetting themeFogColor = new BooleanSetting("Цвет тумана от темы", false)
            .setVisible(() -> customFog.getValue());
    public final ColorSetting fogColor = new ColorSetting("Цвет тумана", 0xFFA0A0A0)
            .setVisible(() -> customFog.getValue() && !themeFogColor.getValue());

    public final BooleanSetting skyShader = new BooleanSetting("Шейдер неба", false);
    public final ModeSetting skyShaderMode = new ModeSetting("Режим", "Aurora", 
            "Aurora", "Energy", "Nebula", "Cosmic Veil", "Deep Space", "Matrix", "Void", "Plasma")
            .setVisible(() -> skyShader.getValue());
    
    public final SliderSetting plasmaScale = new SliderSetting("Плазма: масштаб", 1.0, 0.2, 3.0, 0.05)
            .setVisible(() -> skyShader.getValue() && skyShaderMode.getValue().equals("Plasma"));
    public final SliderSetting plasmaSpeed = new SliderSetting("Плазма: скорость", 1.0, 0.0, 3.0, 0.05)
            .setVisible(() -> skyShader.getValue() && skyShaderMode.getValue().equals("Plasma"));
    
    public final BooleanSetting showStars = new BooleanSetting("Звёзды", true)
            .setVisible(() -> skyShader.getValue());
    
    public final BooleanSetting syncWithTheme = new BooleanSetting("Цвет шейдера от темы", true)
            .setVisible(() -> skyShader.getValue());
    public final ColorSetting shaderColor = new ColorSetting("Цвет шейдера", 0xFF6C63D2)
            .setVisible(() -> skyShader.getValue() && !syncWithTheme.getValue());
    
    public final BooleanSetting removeFog = new BooleanSetting("Удалять туман", true)
            .setVisible(() -> skyShader.getValue());

    public final BooleanSetting worldSaturation = new BooleanSetting("Насыщенность мира", false);
    public final SliderSetting saturationValue = new SliderSetting("Насыщенность", 1.0, 0.0, 3.0, 0.05)
            .setVisible(() -> worldSaturation.getValue());

    private long shaderStartTime = 0;

    @Override
    public void onEnable() {
        super.onEnable();
        shaderStartTime = System.currentTimeMillis();
        if (skyShader.getValue()) {
            updateSkyShaderConfig();
        }
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (skyShader.getValue()) {
            updateSkyShaderConfig();
        }
    }

    @Subscribe
    public void onWorldRender(EventWorldRender event) {
        if (skyShader.getValue()) updateSkyShaderConfig();
    }

    private void updateSkyShaderConfig() {
        SkyShaderRenderer.updateConfig(this);
    }

    public long getShaderStartTime() {
        return shaderStartTime;
    }

    public long getCustomTime() {
        return switch (timePreset.getValue()) {
            case "Рассвет" -> 23000;
            case "День" -> 6000;
            case "Закат" -> 12500;
            case "Сумерки" -> 13500;
            case "Ночь" -> 16000;
            case "Полночь" -> 18000;
            default -> 6000;
        };
    }

    public boolean isFogEnabled() {
        return customFog.getValue();
    }

    public float getFogDistance() {
        return (float) fogDistance.getValue();
    }

    public float getFogDensity() {
        return fogDensity.getFloatValue();
    }

    public boolean isThemeFogColorEnabled() {
        return themeFogColor.getValue();
    }

    public int getFogColor() {
        if (themeFogColor.getValue()) {
            return ThemeManager.getInstance().getCurrentTheme().getColorFirst();
        }
        return fogColor.getValue();
    }

    public int getSkyShaderColor() {
        if (syncWithTheme.getValue()) {
            return ThemeManager.getInstance().getCurrentTheme().getColorFirst();
        }
        return shaderColor.getValue();
    }

    public boolean isSkyShaderEnabled() {
        return skyShader.getValue();
    }

    public String getSkyShaderMode() {
        return skyShaderMode.getValue();
    }

    public float getStarDensity() {
        return 0.7f;
    }

    public float getNebulaStrength() {
        return 0.8f;
    }

    public float getNebulaSpeed() {
        return 0.5f;
    }

    public float getPlasmaScale() {
        return plasmaScale.getFloatValue();
    }

    public float getPlasmaSpeed() {
        return plasmaSpeed.getFloatValue();
    }

    public boolean isRemoveFogEnabled() {
        return removeFog.getValue();
    }
}
