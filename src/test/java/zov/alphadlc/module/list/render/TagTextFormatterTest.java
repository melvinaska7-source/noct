package zov.alphadlc.module.list.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagTextFormatterTest {
    @Test
    void formatsPotionAmplifierAsArabicNumber() {
        assertEquals("Сила 2 0:30", TagTextFormatter.potionRow("Сила", 2, "0:30"));
        assertEquals("Скорость 21 1:05", TagTextFormatter.potionRow("Скорость", 21, "1:05"));
    }
}
