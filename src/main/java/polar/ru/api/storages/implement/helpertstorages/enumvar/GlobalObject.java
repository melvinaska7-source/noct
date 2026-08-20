package polar.ru.api.storages.implement.helpertstorages.enumvar;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GlobalObject<T> {
    private final ObjectArrayList<T> object = new ObjectArrayList();
    public ObjectArrayList<T> getObject() {
        return this.object;
    }
}

