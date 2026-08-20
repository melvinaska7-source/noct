package polar.ru.client.modules.settings.implement;

import java.util.function.Supplier;
import polar.ru.client.modules.settings.Setting;

public class BindSetting
extends Setting {
    private int key;

    public BindSetting(String name, int keyDefault) {
        super(name);
        this.key = keyDefault;
    }

    public BindSetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    public int getKey() {
        return this.key;
    }
    public void setKey(int key) {
        this.key = key;
    }
}

