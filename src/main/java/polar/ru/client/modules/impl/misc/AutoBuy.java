package polar.ru.client.modules.impl.misc;

import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BindSetting;

public class AutoBuy
extends Module {
    public static AutoBuy INSTANCE = new AutoBuy();
    public BindSetting openKey = new BindSetting("Бинд гуи", -1);

    public AutoBuy() {
        super("AutoBuy", "Автоматическая покупка предметов на аукционе", Module.ModuleCategory.MISC);
        this.addSettings(this.openKey);
    }
}

