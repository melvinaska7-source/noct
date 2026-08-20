package polar.ru.client.modules.settings.implement;

import java.util.function.Supplier;
import polar.ru.client.modules.settings.Setting;

public class BooleanSetting
extends Setting {
    private boolean state;

    public BooleanSetting(String name, boolean state) {
        super(name);
        this.state = state;
    }

    public static BooleanSetting of(String name, boolean state) {
        return new BooleanSetting(name, state);
    }

    public BooleanSetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    public boolean isState() {
        return this.state;
    }
    public void setState(boolean state) {
        this.state = state;
    }
}

