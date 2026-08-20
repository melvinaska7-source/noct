package polar.ru.client.modules.impl.render;

import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;

public class FriendMarkers
extends Module {
    public static final FriendMarkers INSTANCE = new FriendMarkers();
    private final BooleanSetting heads = new BooleanSetting("Увеличить голову", true);

    public FriendMarkers() {
        super("Friend Markers", "Выделяет друзей", Module.ModuleCategory.RENDER);
        this.addSettings(this.heads);
    }

    public boolean shouldScaleHead() {
        return this.isEnable() && this.heads.isState();
    }
}

