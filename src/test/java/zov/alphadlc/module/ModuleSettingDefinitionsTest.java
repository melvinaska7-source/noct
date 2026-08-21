package zov.alphadlc.module;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleSettingDefinitionsTest {
    @Test
    void killAuraExposesOnlySupportedRotationModes() {
        var setting = ModuleSettingDefinitions.killAuraRotation();

        assertEquals("ReallyWorld", setting.getValue());
        assertEquals(List.of("ReallyWorld", "Smooth"), setting.getModes());
    }

    @Test
    void killAuraUsesRequestedBooleanDefaults() {
        assertFalse(ModuleSettingDefinitions.killAuraOnlySpace().getValue());
        assertFalse(ModuleSettingDefinitions.killAuraClientLook().getValue());
    }

    @Test
    void killAuraSprintResetHasNewNameAndDefault() {
        var setting = ModuleSettingDefinitions.killAuraSprintReset();

        assertEquals("Сброс спринта", setting.getName());
        assertEquals("Перед ударом", setting.getValue());
        assertTrue(setting.getModes().containsAll(List.of("По радиусу", "Перед ударом")));
    }

    @Test
    void autoSwapSettingsHaveDistinctConfigKeys() {
        var from = ModuleSettingDefinitions.autoSwapFrom();
        var to = ModuleSettingDefinitions.autoSwapTo();

        assertEquals("Свапать с", from.getName());
        assertEquals("Свапать на", to.getName());
        assertNotEquals(from.getName(), to.getName());
    }

    @Test
    void triggerBotUsesSmartCriticalsNameAndKeepsDefault() {
        var setting = ModuleSettingDefinitions.triggerBotSmartCriticals();

        assertEquals("Умные криты", setting.getName());
        assertFalse(setting.getValue());
    }

    @Test
    void clientSoundsVolumeDefaultsToFullPercentRange() {
        var setting = ModuleSettingDefinitions.clientSoundsVolume();

        assertEquals("Громкость", setting.getName());
        assertEquals(100.0, setting.getValue());
        assertEquals(0.0, setting.getMin());
        assertEquals(100.0, setting.getMax());
    }

    @Test
    void autoTpFriendsOnlyDefaultsToEnabled() {
        var setting = ModuleSettingDefinitions.autoTpOnlyFriends();

        assertEquals("Только друзья", setting.getName());
        assertTrue(setting.getValue());
    }
}
