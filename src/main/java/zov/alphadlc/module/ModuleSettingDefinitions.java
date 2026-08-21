package zov.alphadlc.module;

import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;

public final class ModuleSettingDefinitions {
    private ModuleSettingDefinitions() {
    }

    public static ModeSetting killAuraRotation() {
        return new ModeSetting("Ротация", "ReallyWorld", "ReallyWorld", "Smooth");
    }

    public static BooleanSetting killAuraOnlySpace() {
        return new BooleanSetting("Только с пробелом", false);
    }

    public static BooleanSetting killAuraClientLook() {
        return new BooleanSetting("Клиент лук", false);
    }

    public static ModeSetting killAuraSprintReset() {
        return new ModeSetting("Сброс спринта", "Перед ударом", "По радиусу", "Перед ударом");
    }

    public static ModeSetting autoSwapFrom() {
        return new ModeSetting("Свапать с", "Шар", "Гепл", "Щит", "Талисман", "Шар");
    }

    public static ModeSetting autoSwapTo() {
        return new ModeSetting("Свапать на", "Шар", "Гепл", "Щит", "Талисман", "Шар");
    }

    public static BooleanSetting triggerBotSmartCriticals() {
        return new BooleanSetting("Умные криты", false);
    }

    public static SliderSetting clientSoundsVolume() {
        return new SliderSetting("Громкость", 100, 0, 100, 1);
    }

    public static BooleanSetting autoTpOnlyFriends() {
        return new BooleanSetting("Только друзья", true);
    }
}
