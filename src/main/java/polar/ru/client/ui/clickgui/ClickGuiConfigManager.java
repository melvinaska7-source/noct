package polar.ru.client.ui.clickgui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.Setting;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.client.modules.settings.implement.TextSetting;

public class ClickGuiConfigManager {
    private static final String EXTENSION = ".cfg";

    public static Path getDirectory() {
        Path base = MinecraftClient.getInstance().runDirectory != null ? MinecraftClient.getInstance().runDirectory.toPath() : Path.of(".", new String[0]);
        return base.resolve("polar").resolve("configs");
    }

    private static void ensureDirectory() {
        try {
            Files.createDirectories(ClickGuiConfigManager.getDirectory(), new FileAttribute[0]);
        }
        catch (IOException iOException) {
        }
    }

    public static List<String> listConfigs() {
        ClickGuiConfigManager.ensureDirectory();
        ArrayList<String> names = new ArrayList<String>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(ClickGuiConfigManager.getDirectory(), "*.cfg")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                names.add(fileName.substring(0, fileName.length() - EXTENSION.length()));
            }
        }
        catch (IOException iOException) {
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static boolean exists(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return Files.exists(ClickGuiConfigManager.getDirectory().resolve(ClickGuiConfigManager.sanitize(name) + EXTENSION), new LinkOption[0]);
    }

    public static String sanitize(String name) {
        StringBuilder builder = new StringBuilder();
        for (char chr : name.trim().toCharArray()) {
            if (!Character.isLetterOrDigit(chr) && chr != '_' && chr != '-' && chr != ' ') continue;
            builder.append(chr);
        }
        return builder.toString().trim();
    }

    public static boolean save(String name) {
        String safe = ClickGuiConfigManager.sanitize(name);
        if (safe.isEmpty()) {
            return false;
        }
        ClickGuiConfigManager.ensureDirectory();
        ArrayList<String> lines = new ArrayList<String>();
        for (Module module : ModuleClass.INSTANCE.getObject()) {
            lines.add("module:" + module.getName() + "=" + module.isEnable());
            List<Setting> settings = module.getSettings();
            if (settings == null) continue;
            for (Setting setting : settings) {
                if (setting == null) continue;
                String base = "setting:" + module.getName() + ":" + setting.name() + "=";
                if (setting instanceof BooleanSetting) {
                    BooleanSetting booleanSetting = (BooleanSetting)setting;
                    lines.add(base + booleanSetting.isState());
                    continue;
                }
                if (setting instanceof FloatSetting) {
                    FloatSetting floatSetting = (FloatSetting)setting;
                    lines.add(base + String.format(Locale.ROOT, "%.4f", Float.valueOf(floatSetting.get())));
                    continue;
                }
                if (setting instanceof ModeSetting) {
                    ModeSetting modeSetting = (ModeSetting)setting;
                    lines.add(base + modeSetting.getCurrent());
                    continue;
                }
                if (setting instanceof TextSetting) {
                    TextSetting textSetting = (TextSetting)setting;
                    String value = textSetting.get();
                    lines.add(base + (value == null ? "" : value));
                    continue;
                }
                if (setting instanceof BindSetting) {
                    BindSetting bindSetting = (BindSetting)setting;
                    lines.add(base + bindSetting.getKey());
                    continue;
                }
                if (!(setting instanceof ListSetting)) continue;
                ListSetting listSetting = (ListSetting)setting;
                for (BooleanSetting entry : listSetting.getSettings()) {
                    lines.add("list:" + module.getName() + ":" + listSetting.name() + ":" + entry.name() + "=" + entry.isState());
                }
            }
        }
        try {
            Files.write(ClickGuiConfigManager.getDirectory().resolve(safe + EXTENSION), lines, StandardCharsets.UTF_8, new OpenOption[0]);
            return true;
        }
        catch (IOException exception) {
            return false;
        }
    }

    public static boolean load(String name) {
        String safe = ClickGuiConfigManager.sanitize(name);
        Path path = ClickGuiConfigManager.getDirectory().resolve(safe + EXTENSION);
        if (!Files.exists(path, new LinkOption[0])) {
            return false;
        }
        HashMap<String, String> values = new HashMap<String, String>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                int index = line.indexOf(61);
                if (index <= 0) continue;
                values.put(line.substring(0, index), line.substring(index + 1));
            }
        }
        catch (IOException exception) {
            return false;
        }
        for (Module module : ModuleClass.INSTANCE.getObject()) {
            List<Setting> settings;
            String enabled = (String)values.get("module:" + module.getName());
            if (enabled != null) {
                boolean target = Boolean.parseBoolean(enabled);
                if (module.isEnable() != target) {
                    module.toggle();
                }
            }
            if ((settings = module.getSettings()) == null) continue;
            for (Setting setting : settings) {
                if (setting == null) continue;
                String key = "setting:" + module.getName() + ":" + setting.name();
                String value = (String)values.get(key);
                if (setting instanceof ListSetting) {
                    ListSetting listSetting = (ListSetting)setting;
                    for (BooleanSetting entry : listSetting.getSettings()) {
                        String entryValue = (String)values.get("list:" + module.getName() + ":" + listSetting.name() + ":" + entry.name());
                        if (entryValue == null) continue;
                        entry.setState(Boolean.parseBoolean(entryValue));
                    }
                    continue;
                }
                if (value == null) continue;
                try {
                    if (setting instanceof BooleanSetting) {
                        BooleanSetting booleanSetting = (BooleanSetting)setting;
                        booleanSetting.setState(Boolean.parseBoolean(value));
                        continue;
                    }
                    if (setting instanceof FloatSetting) {
                        FloatSetting floatSetting = (FloatSetting)setting;
                        floatSetting.setValue(Float.parseFloat(value));
                        continue;
                    }
                    if (setting instanceof ModeSetting) {
                        ModeSetting modeSetting = (ModeSetting)setting;
                        if (!modeSetting.getMods().contains(value)) continue;
                        modeSetting.set(value);
                        continue;
                    }
                    if (setting instanceof TextSetting) {
                        TextSetting textSetting = (TextSetting)setting;
                        textSetting.setText(value);
                        continue;
                    }
                    if (!(setting instanceof BindSetting)) continue;
                    BindSetting bindSetting = (BindSetting)setting;
                    bindSetting.setKey(Integer.parseInt(value));
                }
                catch (Exception exception) {
                }
            }
        }
        return true;
    }

    public static boolean delete(String name) {
        try {
            return Files.deleteIfExists(ClickGuiConfigManager.getDirectory().resolve(ClickGuiConfigManager.sanitize(name) + EXTENSION));
        }
        catch (IOException exception) {
            return false;
        }
    }
}
