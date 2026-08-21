package zov.alphadlc.util.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigKeyAliasesTest {
    @Test
    void resolvesRenamedModuleInBothDirections() {
        assertEquals(List.of("Dog Fly", "Bib Fly"), ConfigKeyAliases.candidates("Dog Fly"));
        assertEquals(List.of("Bib Fly", "Dog Fly"), ConfigKeyAliases.candidates("Bib Fly"));
    }

    @Test
    void resolvesSmartCritsSettingInBothDirections() {
        assertEquals(List.of("Только с пробелом", "Умные криты"), ConfigKeyAliases.candidates("Только с пробелом"));
        assertEquals(List.of("Умные криты", "Только с пробелом"), ConfigKeyAliases.candidates("Умные криты"));
    }

    @Test
    void resolvesActualSlowdownModeKeyAndHistoricalAlias() {
        assertEquals(
                List.of("Режим замедления", "Сброс спринта", "Замедление"),
                ConfigKeyAliases.candidates("Режим замедления"));
        assertEquals(
                List.of("Сброс спринта", "Режим замедления", "Замедление"),
                ConfigKeyAliases.candidates("Сброс спринта"));
        assertEquals(
                List.of("Замедление", "Режим замедления", "Сброс спринта"),
                ConfigKeyAliases.candidates("Замедление"));
    }

    @Test
    void resolvesAutoSwapKeysInBothDirections() {
        assertEquals(List.of("Свапать с", "Свапать на"), ConfigKeyAliases.candidates("Свапать с"));
        assertEquals(List.of("Свапать на", "Свапать с"), ConfigKeyAliases.candidates("Свапать на"));
    }

    @Test
    void leavesUnknownKeyUnchanged() {
        assertEquals(List.of("Без alias"), ConfigKeyAliases.candidates("Без alias"));
    }
}
