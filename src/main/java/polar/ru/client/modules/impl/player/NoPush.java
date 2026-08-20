package polar.ru.client.modules.impl.player;

import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.ListSetting;

public class NoPush
extends Module {
    public static NoPush INSTANCE = new NoPush();
    private ListSetting collisionList = new ListSetting("Коллизия", new BooleanSetting("Блоки", true), new BooleanSetting("Вода", false), new BooleanSetting("Удочик", true), new BooleanSetting("Игроки", true));

    public NoPush() {
        super("NoPush", "Отключает коллизию", Module.ModuleCategory.MISC);
        this.addSettings(this.collisionList);
    }
    public ListSetting getCollisionList() {
        return this.collisionList;
    }
    public void setCollisionList(ListSetting collisionList) {
        this.collisionList = collisionList;
    }
}

