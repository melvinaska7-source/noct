package polar.ru.api.storages.implement;

import java.util.HashMap;
import java.util.Map;
import polar.ru.client.modules.Module;

public class LocalizationStorage {
    private final Map<String, String> en = new HashMap<String, String>();
    private final Map<String, String> uk = new HashMap<String, String>();
    private final Map<String, String> moduleDescEn = new HashMap<String, String>();
    private final Map<String, String> moduleDescUk = new HashMap<String, String>();
    private Language language = Language.RUSSIAN;

    public LocalizationStorage() {
        this.add("Режим", "Mode", "Режим");
        this.add("Мод", "Mode", "Режим");
        this.add("Стиль", "Style", "Стиль");
        this.add("Скорость", "Speed", "Швидкість");
        this.add("Скорость анимации", "Animation Speed", "Швидкість анімації");
        this.add("Скорость вращения", "Rotation Speed", "Швидкість обертання");
        this.add("Скорость волн", "Wave Speed", "Швидкість хвиль");
        this.add("Скорость нитей", "Thread Speed", "Швидкість ниток");
        this.add("Дистанция", "Distance", "Дистанція");
        this.add("Размер", "Size", "Розмір");
        this.add("Прозрачность", "Opacity", "Прозорість");
        this.add("Свечение", "Glow", "Світіння");
        this.add("Сила свечения", "Glow Strength", "Сила світіння");
        this.add("Сила анимации", "Animation Strength", "Сила анімації");
        this.add("Плавность", "Smoothness", "Плавність");
        this.add("Анимация", "Animation", "Анімація");
        this.add("Анимация крыльев", "Wing Animation", "Анімація крил");
        this.add("Анимация свинга", "Swing Animation", "Анімація свінгу");
        this.add("Плавная анимация", "Smooth Animation", "Плавна анімація");
        this.add("Тип частиц", "Particle Type", "Тип частинок");
        this.add("Количество", "Count", "Кількість");
        this.add("Приоритет", "Priority", "Пріоритет");
        this.add("Ротация", "Rotation", "Ротація");
        this.add("Обход", "Bypass", "Обхід");
        this.add("Сервер", "Server", "Сервер");
        this.add("После лута", "After Loot", "Після луту");
        this.add("Элементы", "Elements", "Елементи");
        this.add("Ватермарка", "Watermark", "Ватермарка");
        this.add("Аррай лист", "Array List", "Список модулів");
        this.add("Горячие клавиши", "Key Binds", "Гарячі клавіші");
        this.add("Зелья", "Potions", "Зілля");
        this.add("Таргет худ", "Target HUD", "Таргет HUD");
        this.add("Уведомления", "Notifications", "Сповіщення");
        this.add("Стафф", "Staff", "Стаф");
        this.add("Сессия", "Session", "Сесія");
        this.add("КейСтроки", "Key Strokes", "Кейстроки");
        this.add("Информация", "Information", "Інформація");
        this.add("Обычный", "Default", "Звичайний");
        this.add("Красивый", "Fancy", "Гарний");
        this.add("Шейдер", "Shader", "Шейдер");
        this.add("Нитки", "Threads", "Нитки");
        this.add("Разлет", "Scatter", "Розліт");
        this.add("Падение", "Fall", "Падіння");
        this.add("Возвращаться", "Return", "Повертатися");
        this.add("Тепаться на спавн", "Teleport to Spawn", "Телепортуватись на спавн");
        this.add("Картинка 1", "Image 1", "Картинка 1");
        this.add("Картинка 2", "Image 2", "Картинка 2");
        this.add("Призраки", "Ghosts", "Привиди");
        this.add("Райдер", "Rider", "Райдер");
        this.add("Души", "Souls", "Души");
        this.add("Кристаллы", "Crystals", "Кристали");
        this.add("Коллизия", "Collision", "Колізія");
        this.add("Тест", "Test", "Тест");
        this.add("Дистанция атаки", "Attack Range", "Дистанція атаки");
        this.add("Только движение", "Only Movement", "Тільки рух");
        this.add("Только при Aura", "Only with Aura", "Тільки з Aura");
        this.add("Только с аурой", "Only with Aura", "Тільки з аурою");
        this.add("Правая рука X", "Right Hand X", "Права рука X");
        this.add("Правая рука Y", "Right Hand Y", "Права рука Y");
        this.add("Правая рука Z", "Right Hand Z", "Права рука Z");
        this.add("Левая рука X", "Left Hand X", "Ліва рука X");
        this.add("Левая рука Y", "Left Hand Y", "Ліва рука Y");
        this.add("Левая рука Z", "Left Hand Z", "Ліва рука Z");
        this.add("Авто-взлёт", "Auto Takeoff", "Авто зліт");
        this.add("Обходить Grim", "Bypass Grim", "Обходити Grim");
        this.add("Крылья", "Wings", "Крила");
        this.add("Крылья 2", "Wings 2", "Крила 2");
        this.add("Китайская шляпа", "China Hat", "Китайський капелюх");
        this.add("Figura", "Figura", "Figura");
    }

    private void add(String key, String english, String ukrainian) {
        this.en.put(key, english);
        this.uk.put(key, ukrainian);
    }

    public Language getLanguage() {
        return this.language;
    }

    public void setLanguage(Language language) {
        this.language = language == null ? Language.RUSSIAN : language;
    }

    public void cycleLanguage() {
        this.language = this.language.next();
    }

    public String translateCategory(Module.ModuleCategory category) {
        return this.translate(category.getName());
    }

    public String translate(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        return switch (this.language.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> key;
            case 1 -> this.en.getOrDefault(key, this.fallbackEnglish(key));
            case 2 -> this.uk.getOrDefault(key, this.fallbackUkrainian(key));
        };
    }

    public String translateModuleDescription(String description) {
        if (description == null || description.isBlank() || "NULLABLE".equalsIgnoreCase(description) || "desc".equalsIgnoreCase(description)) {
            return "";
        }
        return switch (this.language.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> description;
            case 1 -> this.moduleDescEn.getOrDefault(description, description);
            case 2 -> this.moduleDescUk.getOrDefault(description, description);
        };
    }

    private String fallbackEnglish(String key) {
        if (key.chars().allMatch(ch -> ch < 128)) {
            return this.humanizeAscii(key);
        }
        return key;
    }

    private String fallbackUkrainian(String key) {
        if (this.uk.containsKey(key)) {
            return this.uk.get(key);
        }
        if (key.chars().allMatch(ch -> ch < 128)) {
            return this.humanizeAscii(key);
        }
        return key;
    }

    private String humanizeAscii(String key) {
        if (key.indexOf(32) >= 0) {
            return key;
        }
        String humanized = key.replaceAll("([a-z])([A-Z])", "$1 $2");
        humanized = humanized.replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        return humanized.trim();
    }

    public static enum Language {
        RUSSIAN("Русский"),
        ENGLISH("English"),
        UKRAINIAN("Українська");

        private final String displayName;

        private Language(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public Language next() {
            Language[] values = Language.values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }
}

