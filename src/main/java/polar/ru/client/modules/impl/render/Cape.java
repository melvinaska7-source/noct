package polar.ru.client.modules.impl.render;

import polar.ru.client.modules.Module;

public class Cape
extends Module {
    public static Cape INSTANCE = new Cape();

    public Cape() {
        super("Cape", "Cape", Module.ModuleCategory.RENDER);
    }

    public boolean isCustomCapeEnabled() {
        return this.isEnable();
    }
}

