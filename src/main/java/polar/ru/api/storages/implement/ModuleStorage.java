package polar.ru.api.storages.implement;

import polar.ru.api.QClient;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;

public class ModuleStorage
implements QClient {
    public ModuleStorage() {
        this.initModules();
    }

    private void initModules() {
        ModuleClass.INSTANCE.initialize();
    }
}

