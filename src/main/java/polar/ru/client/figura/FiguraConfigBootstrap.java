package polar.ru.client.figura;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import polar.ru.client.figura.FiguraAvatarManager;

public final class FiguraConfigBootstrap {
    static final String KEY_UNKNOWN = "key.keyboard.unknown";
    private static final String[] REQUIRED_DOMAINS = new String[]{"raw.githubusercontent.com", "api.github.com"};

    private FiguraConfigBootstrap() {
    }

    public static void ensureAvatarNetworking() {
        if (!FiguraAvatarManager.isFiguraLoaded()) {
            return;
        }
        FiguraConfigBootstrap.patchConfigFile();
        FiguraConfigBootstrap.applyRuntimeConfig();
        FiguraConfigBootstrap.suppressLuaChatOutput();
        FiguraConfigBootstrap.grantLocalMaxPermissions();
    }

    private static void patchConfigFile() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.runDirectory == null) {
                return;
            }
            Path configPath = mc.runDirectory.toPath().resolve("config").resolve("figura.json");
            Files.createDirectories(configPath.getParent(), new FileAttribute[0]);
            JsonObject root = Files.isRegularFile(configPath, new LinkOption[0]) ? JsonParser.parseString((String)Files.readString(configPath, StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject();
            if (!root.has("CONFIG_VERSION")) {
                root.addProperty("CONFIG_VERSION", (Number)1);
            }
            root.addProperty("allow_networking", Boolean.valueOf(true));
            root.addProperty("default_permission_level", (Number)4);
            root.addProperty("log_location", (Number)1);
            root.addProperty("action_wheel_button", KEY_UNKNOWN);
            root.addProperty("popup_button", KEY_UNKNOWN);
            JsonArray filter = root.has("network_filter") && root.get("network_filter").isJsonArray() ? root.getAsJsonArray("network_filter") : new JsonArray();
            LinkedHashSet<String> domains = new LinkedHashSet<String>();
            for (JsonElement element : filter) {
                if (element.isJsonPrimitive()) {
                    domains.add(element.getAsString());
                    continue;
                }
                if (!element.isJsonObject() || !element.getAsJsonObject().has("source")) continue;
                domains.add(element.getAsJsonObject().get("source").getAsString());
            }
            for (String domain : REQUIRED_DOMAINS) {
                domains.add(domain);
            }
            JsonArray merged = new JsonArray();
            for (String domain : domains) {
                merged.add(domain);
            }
            root.add("network_filter", (JsonElement)merged);
            Files.writeString(configPath, (CharSequence)new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement)root), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static void applyRuntimeConfig() {
        try {
            Class<?> configsClass = Class.forName("org.figuramc.figura.config.Configs");
            Class<?> configManagerClass = Class.forName("org.figuramc.figura.config.ConfigManager");
            FiguraConfigBootstrap.setBooleanConfig(configsClass, "ALLOW_NETWORKING", true);
            FiguraConfigBootstrap.setMaxDefaultPermissionLevel(configsClass);
            FiguraConfigBootstrap.setLogLocationToConsole(configsClass);
            FiguraConfigBootstrap.ensureNetworkFilters(configsClass);
            FiguraConfigBootstrap.disableOverlayKeybinds(configsClass);
            Method saveConfig = configManagerClass.getMethod("saveConfig", new Class[0]);
            saveConfig.invoke(null, new Object[0]);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    static void disableOverlayKeybinds(Class<?> configsClass) throws Exception {
        FiguraConfigBootstrap.disableKeybindConfig(configsClass, "ACTION_WHEEL_BUTTON");
        FiguraConfigBootstrap.disableKeybindConfig(configsClass, "POPUP_BUTTON");
        FiguraConfigBootstrap.resetKeyMappings();
    }

    private static void disableKeybindConfig(Class<?> configsClass, String fieldName) throws Exception {
        Object keybindConfig = configsClass.getField(fieldName).get(null);
        Field disabledField = keybindConfig.getClass().getField("disabled");
        disabledField.setBoolean(keybindConfig, true);
        Field valueField = keybindConfig.getClass().getField("value");
        valueField.set(keybindConfig, KEY_UNKNOWN);
        Field tempValueField = keybindConfig.getClass().getField("tempValue");
        tempValueField.set(keybindConfig, KEY_UNKNOWN);
        Method setValue = keybindConfig.getClass().getMethod("setValue", String.class);
        setValue.invoke(keybindConfig, KEY_UNKNOWN);
        Object keyBind = keybindConfig.getClass().getField("keyBind").get(keybindConfig);
        if (keyBind instanceof KeyBinding) {
            KeyBinding binding = (KeyBinding)keyBind;
            binding.setBoundKey(InputUtil.UNKNOWN_KEY);
        } else {
            Class<?> inputConstants = Class.forName("com.mojang.blaze3d.platform.InputConstants");
            Object unknownKey = inputConstants.getMethod("getKey", String.class).invoke(null, KEY_UNKNOWN);
            for (Method method : keyBind.getClass().getMethods()) {
                if (!"setKey".equals(method.getName()) || method.getParameterCount() != 1) continue;
                method.invoke(keyBind, unknownKey);
                break;
            }
        }
    }

    private static void resetKeyMappings() {
        try {
            Class<?> keyBinding = Class.forName("net.minecraft.client.option.KeyBinding");
            for (String methodName : new String[]{"resetMapping", "updateKeysByCode"}) {
                try {
                    keyBinding.getMethod(methodName, new Class[0]).invoke(null, new Object[0]);
                    return;
                }
                catch (NoSuchMethodException noSuchMethodException) {
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static void setBooleanConfig(Class<?> configsClass, String fieldName, boolean value) throws Exception {
        Object config = configsClass.getField(fieldName).get(null);
        Field valueField = config.getClass().getField("value");
        valueField.set(config, value);
    }

    public static void suppressLuaChatOutput() {
        if (!FiguraAvatarManager.isFiguraLoaded()) {
            return;
        }
        try {
            Class<?> printer = Class.forName("org.figuramc.figura.lua.FiguraLuaPrinter");
            Method clearQueue = printer.getMethod("clearPrintQueue", new Class[0]);
            clearQueue.invoke(null, new Object[0]);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static void setLogLocationToConsole(Class<?> configsClass) throws Exception {
        Object logLocation = configsClass.getField("LOG_LOCATION").get(null);
        logLocation.getClass().getField("value").set(logLocation, 1);
    }

    private static void setMaxDefaultPermissionLevel(Class<?> configsClass) throws Exception {
        Class<?> categoryClass = Class.forName("org.figuramc.figura.permissions.Permissions$Category");
        Object max = categoryClass.getField("MAX").get(null);
        int maxIndex = categoryClass.getField("index").getInt(max);
        Object permissionConfig = configsClass.getField("DEFAULT_PERMISSION_LEVEL").get(null);
        permissionConfig.getClass().getField("value").set(permissionConfig, maxIndex);
    }

    private static void ensureNetworkFilters(Class<?> configsClass) throws Exception {
        Object networkFilterConfig = configsClass.getField("NETWORK_FILTER").get(null);
        Method getFilters = networkFilterConfig.getClass().getMethod("getFilters", new Class[0]);
        List filters = (List)getFilters.invoke(networkFilterConfig, new Object[0]);
        Class<?> filterClass = Class.forName("org.figuramc.figura.lua.api.net.NetworkingAPI$Filter");
        Method getSource = filterClass.getMethod("getSource", new Class[0]);
        Constructor<?> filterConstructor = filterClass.getConstructor(String.class);
        ArrayList<String> existing = new ArrayList<String>();
        for (Object filter : filters) {
            Object source = getSource.invoke(filter, new Object[0]);
            if (!(source instanceof String)) continue;
            String string = (String)source;
            existing.add(string);
        }
        for (String domain : REQUIRED_DOMAINS) {
            if (existing.contains(domain)) continue;
            filters.add(filterConstructor.newInstance(domain));
        }
    }

    private static void grantLocalMaxPermissions() {
        try {
            UUID uuid = FiguraConfigBootstrap.getLocalPlayerUuid();
            if (uuid == null) {
                return;
            }
            Class<?> categoryClass = Class.forName("org.figuramc.figura.permissions.Permissions$Category");
            Object max = categoryClass.getField("MAX").get(null);
            Class<?> permissionManager = Class.forName("org.figuramc.figura.permissions.PermissionManager");
            Method setDefaultFor = permissionManager.getDeclaredMethod("setDefaultFor", UUID.class, categoryClass);
            setDefaultFor.setAccessible(true);
            setDefaultFor.invoke(null, uuid, max);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static UUID getLocalPlayerUuid() {
        try {
            Class<?> figuraMod = Class.forName("org.figuramc.figura.FiguraMod");
            Method getLocal = figuraMod.getDeclaredMethod("getLocalPlayerUUID", new Class[0]);
            Object value = getLocal.invoke(null, new Object[0]);
            if (value instanceof UUID) {
                UUID uuid = (UUID)value;
                return uuid;
            }
        }
        catch (Throwable figuraMod) {
            // empty catch block
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.player != null ? mc.player.getUuid() : null;
    }
}

