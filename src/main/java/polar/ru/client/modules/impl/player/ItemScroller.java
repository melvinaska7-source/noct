package polar.ru.client.modules.impl.player;

import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class ItemScroller
extends Module {
    public static ItemScroller INSTANCE = new ItemScroller();
    public final FloatSetting delay = new FloatSetting("Задержка", 50.0f, 0.0f, 200.0f, 1.0f);
    private long lastQuickMoveAt;

    public ItemScroller() {
        super("ItemScroller", "Убирает задержку перемещения предметов", Module.ModuleCategory.PLAYER);
        this.addSettings(this.delay);
    }

    public boolean canQuickMove() {
        long now = System.currentTimeMillis();
        if (now - this.lastQuickMoveAt < (long)this.delay.get()) {
            return false;
        }
        this.lastQuickMoveAt = now;
        return true;
    }

    public void resetTimer() {
        this.lastQuickMoveAt = 0L;
    }

    @Override
    public void onDisable() {
        this.resetTimer();
        super.onDisable();
    }
}

