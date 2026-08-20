package polar.ru.client.modules.settings.implement;

import java.util.List;
import java.util.function.Supplier;
import polar.ru.client.modules.settings.Setting;
import polar.ru.client.modules.settings.implement.BooleanSetting;

public class ListSetting
extends Setting {
    public List<BooleanSetting> settings;

    public ListSetting(String name, BooleanSetting ... settings) {
        super(name);
        this.settings = List.of(settings);
    }

    public ListSetting of(String name, BooleanSetting ... settings) {
        return new ListSetting(name, settings);
    }

    public boolean is(String name) {
        return this.requireSetting(name).isState();
    }

    public void set(String name, boolean value) {
        if (value) {
            for (BooleanSetting option : this.settings) {
                if (option.name().equalsIgnoreCase(name)) continue;
                option.setState(false);
            }
        }
        this.requireSetting(name).setState(value);
    }

    public ListSetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }

    private BooleanSetting requireSetting(String name) {
        for (BooleanSetting option : this.settings) {
            if (!option.name().equalsIgnoreCase(name)) continue;
            return option;
        }
        throw new NullPointerException("Unknown list setting entry: " + name);
    }
    public List<BooleanSetting> getSettings() {
        return this.settings;
    }
    public void setSettings(List<BooleanSetting> settings) {
        this.settings = settings;
    }
}

