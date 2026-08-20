package polar.ru.client.modules.settings;

import java.awt.Color;
import java.util.function.Supplier;
import polar.ru.api.QClient;
import polar.ru.polar;

public abstract class Setting
implements QClient {
    private final String name;
    public Supplier<Boolean> visible = () -> true;
    public Color color = Color.WHITE;

    public Setting(String name) {
        this.name = name;
    }

    public Boolean visible() {
        return this.visible.get();
    }

    public String displayName() {
        return polar.INSTANCE.localizationStorage == null ? this.name : polar.INSTANCE.localizationStorage.translate(this.name);
    }
    public String name() {
        return this.name;
    }
    public Color color() {
        return this.color;
    }
}

