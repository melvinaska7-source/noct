package polar.ru.client.modules.impl.render;

import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.storages.implement.helpertstorages.Theme;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.polar;

public class WorldTweaks
extends Module {
    public static WorldTweaks INSTANCE = new WorldTweaks();
    private final ListSetting worldSettings = new ListSetting("World Settings", new BooleanSetting("Time", true), new BooleanSetting("Fog", true));
    private final FloatSetting timeSetting = new FloatSetting("Time", 12.0f, 0.0f, 24.0f, 1.0f).visible(() -> this.worldSettings.is("Time"));
    private final FloatSetting fogDistanceSetting = new FloatSetting("Fog Distance", 100.0f, 20.0f, 200.0f, 1.0f).visible(() -> this.worldSettings.is("Fog"));

    public WorldTweaks() {
        super("CustomWorld", "Настройка времени и тумана", Module.ModuleCategory.RENDER);
        this.addSettings(this.worldSettings, this.timeSetting, this.fogDistanceSetting);
    }

    public boolean isTimeEnabled() {
        return this.isEnable() && this.worldSettings.is("Time");
    }

    public boolean isFogEnabled() {
        return this.isEnable() && this.worldSettings.is("Fog");
    }

    public long getForcedTime() {
        return (long)(this.timeSetting.get() * 1000.0f);
    }

    public float getFogDistance() {
        return this.fogDistanceSetting.get();
    }

    public int getFogColor() {
        return this.getThemeBaseColor();
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (!this.isTimeEnabled() || WorldTweaks.mc.world == null) {
            return;
        }
        WorldTweaks.mc.world.getLevelProperties().setTimeOfDay(this.getForcedTime());
    }

    private int getThemeBaseColor() {
        if (polar.INSTANCE == null || polar.INSTANCE.themeStorage == null || polar.INSTANCE.themeStorage.getThemes() == null || polar.INSTANCE.themeStorage.getThemes().getTheme() == null) {
            return ColorUtils.getThemeColor();
        }
        Theme theme = polar.INSTANCE.themeStorage.getThemes().getTheme();
        if (!"Rainbow".equals(theme.getName()) && theme.color != null && theme.color.length > 0) {
            return theme.color[0];
        }
        return ColorUtils.getThemeColor();
    }
}

