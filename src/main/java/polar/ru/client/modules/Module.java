package polar.ru.client.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import polar.ru.api.QClient;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.notification.NotificationManager;
import polar.ru.client.modules.settings.Setting;
import polar.ru.polar;

public abstract class Module
implements QClient {
    private String name;
    private String description;
    private int key;
    private ModuleCategory category;
    private boolean isOpen;
    private boolean enable;
    private final List<Setting> settings = new ArrayList<Setting>();
    private final AnimationUtils animka = new AnimationUtils(60.0f, 11.0f, Easings.LINEAR);
    private final AnimationUtils arrayAnimka = new AnimationUtils(0.0f, 11.0f, Easings.LINEAR);

    public Module(String name, String description, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = -1;
    }

    public Module(String name, ModuleCategory category) {
        this.name = name;
        this.description = "NULLABLE";
        this.category = category;
        this.key = -1;
    }

    public void onEnable() {
        this.enable = true;
        EventInvoker.register(this);
        this.animka.update(1.0f);
        NotificationManager.push(this.name, this.category.getIcons(), true);
    }

    public void onDisable() {
        this.enable = false;
        EventInvoker.unregister(this);
        this.animka.update(0.0f);
        NotificationManager.push(this.name, this.category.getIcons(), false);
    }

    public void toggle() {
        boolean bl = this.enable = !this.enable;
        if (this.enable) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    public void setEnabled(boolean state) {
        boolean lastState = this.enable;
        this.enable = state;
        try {
            if (state) {
                this.onEnable();
            } else if (lastState) {
                this.onDisable();
            }
        }
        catch (Exception e2) {
            this.enable = false;
            this.onDisable();
        }
    }

    public void addSettings(Setting ... settings) {
        if (settings == null || settings.length == 0) {
            return;
        }
        Arrays.stream(settings).filter(Objects::nonNull).forEach(this.settings::add);
    }

    public String getDisplayName() {
        return polar.INSTANCE.localizationStorage == null ? this.name : polar.INSTANCE.localizationStorage.translate(this.name);
    }

    public String getDisplayDescription() {
        if (this.description == null || this.description.isBlank() || "NULLABLE".equalsIgnoreCase(this.description) || "desc".equalsIgnoreCase(this.description)) {
            return "";
        }
        return polar.INSTANCE.localizationStorage == null ? this.description : polar.INSTANCE.localizationStorage.translateModuleDescription(this.description);
    }
    public String getName() {
        return this.name;
    }
    public String getDescription() {
        return this.description;
    }
    public int getKey() {
        return this.key;
    }
    public ModuleCategory getCategory() {
        return this.category;
    }
    public boolean isOpen() {
        return this.isOpen;
    }
    public boolean isEnable() {
        return this.enable;
    }
    public List<Setting> getSettings() {
        return this.settings;
    }
    public AnimationUtils getAnimka() {
        return this.animka;
    }
    public AnimationUtils getArrayAnimka() {
        return this.arrayAnimka;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setKey(int key) {
        this.key = key;
    }
    public void setCategory(ModuleCategory category) {
        this.category = category;
    }
    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public static enum ModuleCategory {
        COMBAT("Combat", "A"),
        MOVEMENT("Movement", "B"),
        RENDER("Visuals", "D"),
        MISC("Util", "E"),
        PLAYER("Player", "C"),
        FIGURA("Figura", "F");

        private final String name;
        private final String icons;
        private ModuleCategory(String name, String icons) {
            this.name = name;
            this.icons = icons;
        }
        public String getName() {
            return this.name;
        }
        public String getIcons() {
            return this.icons;
        }
    }
}

