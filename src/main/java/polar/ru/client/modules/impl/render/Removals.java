package polar.ru.client.modules.impl.render;

import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.ListSetting;

public class Removals
extends Module {
    public static Removals INSTANCE = new Removals();
    private final ListSetting elements = new ListSetting("Элементы", new BooleanSetting("Огонь", false), new BooleanSetting("Плохие эффекты", false), new BooleanSetting("Оверлей в блоке", false), new BooleanSetting("Частицы", false), new BooleanSetting("Погода", false), new BooleanSetting("Облака", false), new BooleanSetting("Блок-сущности", false), new BooleanSetting("Тени", false), new BooleanSetting("Анимацию тотема", false), new BooleanSetting("Тряску при уроне", false), new BooleanSetting("Тряску экрана", false));

    public Removals() {
        super("Removals", "Убирает выбранные элементы рендера", Module.ModuleCategory.RENDER);
        this.addSettings(this.elements);
    }

    public boolean isEnabled(String element) {
        return this.isEnable() && this.elements.is(element);
    }

    public boolean isTotemAnimationDisabled() {
        return this.isEnabled("Анимацию тотема");
    }
}

