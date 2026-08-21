package zov.alphadlc.module;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.module.list.render.ClientSounds;
import zov.alphadlc.module.list.render.hud.Interface;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.Setting;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.QuickLogger;
import zov.alphadlc.util.base.Instance;
import zov.alphadlc.util.render.math.Animation;
import zov.alphadlc.util.render.math.Easing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class Module implements IMinecraft, QuickLogger {
    private final String name, desc;
    private final ModuleCategory category;
    private int key;
    private boolean enabled;
    private final Animation animation = new Animation(Easing.BACK_OUT, 450);

    public final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<Setting> settings = new ArrayList<>();
    private boolean settingsCached = false;

    public Module() {
        ModuleInformation information = getClass().getAnnotation(ModuleInformation.class);

        this.name = information.moduleName();
        this.desc = information.moduleDesc();
        this.category = information.moduleCategory();
        this.key = information.moduleKeybind();
    }

    public List<Setting> getSettings() {
        // Поля настроек статичны на протяжении жизни модуля, поэтому отражённый список
        // строим один раз (лениво) и переиспользуем. getSettings() вызывается каждый кадр
        // (в т.ч. по всем модулям для keybind в HUD), а reflection+stream+новый список — дорого.
        if (!settingsCached) {
            for (java.lang.reflect.Field field : this.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(this);
                    if (value instanceof Setting setting) {
                        settings.add(setting);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            settingsCached = true;
        }
        return settings;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
            Interface iface = Instance.get(Interface.class);
            if (iface != null && iface.isModuleStateNotifEnabled() && !isClickGui())
                iface.notifications.post(this.name, enabled);
        }
        if (isClickGui()) return;
        ClientSounds soundsModule = Instance.get(ClientSounds.class);
        if (soundsModule != null && (soundsModule.isEnabled() || this instanceof ClientSounds)) {
            ClientSounds.play(this.isEnabled());
        }
    }

    protected ModeSetting modeCreate() {
        return new ModeSetting("Мод", "Vanilla", "Vanilla", "Grim", "Polar");
    }

    public void onEnable() {
        AlphaDLC.getInstance().getEventBus().register(this);
    }

    public void onDisable() {
        AlphaDLC.getInstance().getEventBus().unregister(this);
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }

    // Модуль ClickGui — исключение: без уведомлений и звука вкл/выкл.
    // Проверяем по имени класса, т.к. отображаемое имя — "Click Gui" (с пробелом).
    private boolean isClickGui() {
        return this.getClass().getSimpleName().equals("ClickGui");
    }
}