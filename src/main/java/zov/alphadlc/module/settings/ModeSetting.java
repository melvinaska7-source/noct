package zov.alphadlc.module.settings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModeSetting extends Setting {
    private String value;
    private final List<String> modes;
    private final Map<String, String> aliases = new HashMap<>();

    public ModeSetting(String name, String defaultValue, String... modes) {
        super(name);
        this.value = defaultValue;
        this.modes = Arrays.asList(modes);
    }

    /** Псевдоним значения при загрузке старых конфигов (from -> to). */
    public ModeSetting addAlias(String from, String to) {
        aliases.put(from, to);
        return this;
    }

    private String resolve(String value) {
        return aliases.getOrDefault(value, value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        String resolved = resolve(value);
        if (modes.contains(resolved)) {
            this.value = resolved;
        }
    }

    public boolean is(String equalsIgnoreCase) {
        return value.equalsIgnoreCase(equalsIgnoreCase);
    }

    public List<String> getModes() {
        return modes;
    }

    public int getIndex() {
        return modes.indexOf(value);
    }

    public void setIndex(int index) {
        if (index >= 0 && index < modes.size()) {
            this.value = modes.get(index);
        }
    }

    public void cycle() {
        int nextIndex = (getIndex() + 1) % modes.size();
        setValue(modes.get(nextIndex));
    }

    @Override
    public String getValueAsString() {
        return value;
    }

    @Override
    public void setValueFromString(String value) {
        String resolved = resolve(value);
        if (modes.contains(resolved)) {
            this.value = resolved;
        }
    }

    @Override
    public ModeSetting setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}