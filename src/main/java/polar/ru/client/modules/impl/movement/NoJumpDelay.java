package polar.ru.client.modules.impl.movement;

import polar.ru.client.modules.Module;

public class NoJumpDelay
extends Module {
    public static NoJumpDelay INSTANCE = new NoJumpDelay();

    public NoJumpDelay() {
        super("NoJumpDelay", "Убирает задержку между прыжками", Module.ModuleCategory.MOVEMENT);
    }
}

