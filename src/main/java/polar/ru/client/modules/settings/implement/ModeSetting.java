package polar.ru.client.modules.settings.implement;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import polar.ru.client.modules.settings.Setting;
import polar.ru.polar;

public class ModeSetting
extends Setting {
    private List<String> mods;
    private String current;
    private int index;

    public ModeSetting(String name, String current, String ... modes) {
        super(name);
        this.mods = Arrays.asList(modes);
        this.index = this.mods.indexOf(current);
        if (this.index < 0) {
            this.index = 0;
        }
        this.current = this.mods.get(this.index);
    }

    public void set(String selected) {
        int newIndex = this.mods.indexOf(selected);
        if (newIndex < 0) {
            return;
        }
        this.current = selected;
        this.index = newIndex;
    }

    public boolean is(String mode) {
        return this.current.equals(mode);
    }

    public String displayMode(String mode) {
        return polar.INSTANCE.localizationStorage == null ? mode : polar.INSTANCE.localizationStorage.translate(mode);
    }

    public String displayCurrent() {
        return this.displayMode(this.current);
    }

    public ModeSetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    public List<String> getMods() {
        return this.mods;
    }
    public String getCurrent() {
        return this.current;
    }
    public int getIndex() {
        return this.index;
    }
    public void setMods(List<String> mods) {
        this.mods = mods;
    }
    public void setCurrent(String current) {
        this.current = current;
    }
    public void setIndex(int index) {
        this.index = index;
    }
}

