package zov.alphadlc.module.settings.impl;

public class ThemeManager {
    private static ThemeManager instance;

    // Создаем стандартную тему по умолчанию (Название, Цвет 1, Цвет 2 в HEX формате ARGB)
    // Дефолтная синяя тема
    private final Theme defaultTheme = new Theme("Default", 0xFF3C6EF5, 0xFF0D0D10);

    private ThemeManager() {
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public Theme getCurrentTheme() {
        return defaultTheme; // Просто всегда возвращаем эту дефолтную тему
    }

}