package polar.ru.api.storages.implement;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import polar.ru.api.storages.implement.DragStorage;
import polar.ru.api.storages.implement.LocalizationStorage;
import polar.ru.api.storages.implement.ThemeStorage;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.cmd.macro.Macro;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.api.utils.namespaced.FileUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.render.Interface;
import polar.ru.client.modules.impl.render.base.InterfaceProcessing;
import polar.ru.client.modules.impl.render.base.implement.WaterMark;
import polar.ru.client.modules.settings.Setting;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.client.modules.settings.implement.TextSetting;
import polar.ru.polar;

public class ConfigStorage {
    public String currentConfig = "default";
    private final String extension = ".polar";

    public ConfigStorage() {
        this.loadAll();
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveAll));
    }

    private void loadAll() {
        try {
            this.loadGlobals();
            this.loadConfig(this.currentConfig);
        }
        catch (Exception e2) {
            e2.printStackTrace(System.err);
        }
    }

    private void saveAll() {
        try {
            this.saveGlobals();
        }
        catch (Exception e2) {
            e2.printStackTrace(System.err);
        }
    }

    public void saveConfig(String config) throws Exception {
        File file = new File(polar.INSTANCE.configsDir, config + ".polar");
        JsonObject object = new JsonObject();
        object.add("config", (JsonElement)new JsonPrimitive(config));
        object.add("theme", (JsonElement)new JsonPrimitive(polar.INSTANCE.themeStorage.getThemes().name()));
        object.add("language", (JsonElement)new JsonPrimitive(polar.INSTANCE.localizationStorage.getLanguage().name()));
        object.add("modules", (JsonElement)this.serializeModules());
        object.add("draggables", (JsonElement)this.serializeDraggables());
        object.add("hud", (JsonElement)this.serializeHudState());
        object.add("figura", (JsonElement)this.serializeFiguraState());
        try (OutputStreamWriter writer = new OutputStreamWriter((OutputStream)new FileOutputStream(file, false), StandardCharsets.UTF_8);){
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement)object));
        }
        this.currentConfig = config;
    }

    public void loadConfig(String config) throws Exception {
        JsonObject object;
        if (!FileUtils.exists(String.valueOf(polar.INSTANCE.configsDir) + "/" + config + ".polar")) {
            return;
        }
        try (InputStream stream = Files.newInputStream(Paths.get(String.valueOf(polar.INSTANCE.configsDir) + "/" + config + ".polar", new String[0]), new OpenOption[0]);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);){
            object = JsonParser.parseReader((Reader)reader).getAsJsonObject();
        }
        if (object.has("theme")) {
            String themeName = object.get("theme").getAsString();
            for (ThemeStorage.Themes theme : ThemeStorage.Themes.values()) {
                if (!theme.name().equals(themeName)) continue;
                polar.INSTANCE.themeStorage.setThemes(theme);
                break;
            }
        }
        if (object.has("language")) {
            try {
                polar.INSTANCE.localizationStorage.setLanguage(LocalizationStorage.Language.valueOf(object.get("language").getAsString()));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (object.has("modules")) {
            this.deserializeModules(object.get("modules").getAsJsonObject());
        }
        if (object.has("draggables")) {
            this.deserializeDraggables(object.get("draggables").getAsJsonObject());
        }
        if (object.has("hud")) {
            this.deserializeHudState(object.get("hud").getAsJsonObject());
        }
        if (object.has("figura")) {
            this.deserializeFiguraState(object.get("figura").getAsJsonObject());
        }
        this.currentConfig = config;
    }

    public void saveGlobals() throws Exception {
        File file = new File(polar.INSTANCE.globalsDir, "globals.polar");
        JsonObject object = new JsonObject();
        object.add("config", (JsonElement)new JsonPrimitive(this.currentConfig));
        object.add("theme", (JsonElement)new JsonPrimitive(polar.INSTANCE.themeStorage.getThemes().name()));
        object.add("language", (JsonElement)new JsonPrimitive(polar.INSTANCE.localizationStorage.getLanguage().name()));
        JsonArray friendsArray = new JsonArray();
        polar.INSTANCE.friendStorage.getFriends().forEach(arg_0 -> ((JsonArray)friendsArray).add(arg_0));
        object.add("friends", (JsonElement)friendsArray);
        JsonArray staffsArray = new JsonArray();
        polar.INSTANCE.staffStorage.getStaffs().forEach(arg_0 -> ((JsonArray)staffsArray).add(arg_0));
        object.add("staffs", (JsonElement)staffsArray);
        JsonArray macrosArray = new JsonArray();
        polar.INSTANCE.macroStorage.getMacros().forEach(macro -> {
            JsonObject macroObject = new JsonObject();
            macroObject.addProperty("name", macro.getName());
            macroObject.addProperty("command", macro.getCommand());
            macroObject.addProperty("key", (Number)macro.getBind().getKey());
            macrosArray.add((JsonElement)macroObject);
        });
        object.add("macros", (JsonElement)macrosArray);
        try (OutputStreamWriter writer = new OutputStreamWriter((OutputStream)new FileOutputStream(file, false), StandardCharsets.UTF_8);){
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement)object));
        }
    }

    public void loadGlobals() throws Exception {
        JsonObject object;
        if (!FileUtils.exists(String.valueOf(polar.INSTANCE.globalsDir) + "/globals.polar")) {
            return;
        }
        try (InputStream stream = Files.newInputStream(Paths.get(String.valueOf(polar.INSTANCE.globalsDir) + "/globals.polar", new String[0]), new OpenOption[0]);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);){
            object = JsonParser.parseReader((Reader)reader).getAsJsonObject();
        }
        if (object.has("config")) {
            this.currentConfig = object.get("config").getAsString();
        }
        if (object.has("theme")) {
            String themeName = object.get("theme").getAsString();
            for (ThemeStorage.Themes theme : ThemeStorage.Themes.values()) {
                if (!theme.name().equals(themeName)) continue;
                polar.INSTANCE.themeStorage.setThemes(theme);
                break;
            }
        }
        if (object.has("language")) {
            try {
                polar.INSTANCE.localizationStorage.setLanguage(LocalizationStorage.Language.valueOf(object.get("language").getAsString()));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (object.has("friends")) {
            for (JsonElement element : object.get("friends").getAsJsonArray()) {
                if (polar.INSTANCE.friendStorage.isFriend(element.getAsString())) continue;
                polar.INSTANCE.friendStorage.add(element.getAsString());
            }
        }
        if (object.has("staffs")) {
            for (JsonElement element : object.get("staffs").getAsJsonArray()) {
                if (polar.INSTANCE.staffStorage.isStaff(element.getAsString())) continue;
                polar.INSTANCE.staffStorage.add(element.getAsString());
            }
        }
        if (object.has("macros")) {
            for (JsonElement element : object.get("macros").getAsJsonArray()) {
                try {
                    int key;
                    String command;
                    String name;
                    if (element.isJsonObject()) {
                        JsonObject macroObject = element.getAsJsonObject();
                        name = macroObject.has("name") ? macroObject.get("name").getAsString() : "";
                        command = macroObject.has("command") ? macroObject.get("command").getAsString() : "";
                        key = macroObject.has("key") ? macroObject.get("key").getAsInt() : -1;
                    } else {
                        String[] split = element.getAsString().split(":", 3);
                        if (split.length < 3) continue;
                        name = split[0];
                        command = split[1];
                        key = Integer.parseInt(split[2]);
                    }
                    if (name.isBlank() || polar.INSTANCE.macroStorage.getMacro(name) != null) continue;
                    polar.INSTANCE.macroStorage.add(new Macro(name, command, new BindSetting("bind", key)));
                }
                catch (Exception exception) {}
            }
        }
    }

    private JsonObject serializeModules() {
        JsonObject modules = new JsonObject();
        for (Module module : ModuleClass.INSTANCE.getObject()) {
            try {
                JsonObject object = new JsonObject();
                object.add("toggled", (JsonElement)new JsonPrimitive(Boolean.valueOf(module.isEnable())));
                object.add("bind", (JsonElement)new JsonPrimitive((Number)module.getKey()));
                JsonObject settings = new JsonObject();
                for (Setting s2 : module.getSettings()) {
                    try {
                        if (s2 instanceof BooleanSetting) {
                            BooleanSetting bool = (BooleanSetting)s2;
                            settings.add(s2.name(), (JsonElement)new JsonPrimitive(Boolean.valueOf(bool.isState())));
                            continue;
                        }
                        if (s2 instanceof FloatSetting) {
                            FloatSetting num = (FloatSetting)s2;
                            settings.add(s2.name(), (JsonElement)new JsonPrimitive((Number)Float.valueOf(num.getValue().floatValue())));
                            continue;
                        }
                        if (s2 instanceof ModeSetting) {
                            ModeSetting mode = (ModeSetting)s2;
                            settings.add(s2.name(), (JsonElement)new JsonPrimitive(mode.getCurrent()));
                            continue;
                        }
                        if (s2 instanceof TextSetting) {
                            TextSetting text = (TextSetting)s2;
                            settings.add(s2.name(), (JsonElement)new JsonPrimitive(text.get()));
                            continue;
                        }
                        if (s2 instanceof BindSetting) {
                            BindSetting bind = (BindSetting)s2;
                            settings.add(s2.name(), (JsonElement)new JsonPrimitive((Number)bind.getKey()));
                            continue;
                        }
                        if (!(s2 instanceof ListSetting)) continue;
                        ListSetting list = (ListSetting)s2;
                        JsonObject listObj = new JsonObject();
                        for (BooleanSetting setting : list.getSettings()) {
                            listObj.add(setting.name(), (JsonElement)new JsonPrimitive(Boolean.valueOf(setting.isState())));
                        }
                        settings.add(list.name(), (JsonElement)listObj);
                    }
                    catch (Exception exception) {}
                }
                object.add("settings", (JsonElement)settings);
                modules.add(module.getName(), (JsonElement)object);
            }
            catch (Exception exception) {}
        }
        return modules;
    }

    private void deserializeModules(JsonObject modules) {
        JsonObject object;
        LinkedHashMap<Module, Boolean> targetStates = new LinkedHashMap<Module, Boolean>();
        for (Module module : ModuleClass.INSTANCE.getObject()) {
            try {
                object = modules.has(module.getName()) ? modules.get(module.getName()).getAsJsonObject() : null;
                boolean toggled = object != null && object.has("toggled") && object.get("toggled").getAsBoolean();
                targetStates.put(module, toggled);
                if (!module.isEnable()) continue;
                module.setEnabled(false);
            }
            catch (Exception ignored) {
                targetStates.put(module, false);
            }
        }
        for (Module module : ModuleClass.INSTANCE.getObject()) {
            try {
                if (!modules.has(module.getName())) continue;
                object = modules.get(module.getName()).getAsJsonObject();
                if (object.has("bind")) {
                    module.setKey(object.get("bind").getAsInt());
                }
                if (!object.has("settings")) continue;
                JsonObject settings = object.get("settings").getAsJsonObject();
                for (Setting s2 : module.getSettings()) {
                    try {
                        if (!settings.has(s2.name())) continue;
                        JsonElement element = settings.get(s2.name());
                        if (s2 instanceof BooleanSetting) {
                            BooleanSetting bool = (BooleanSetting)s2;
                            bool.setState(element.getAsBoolean());
                            continue;
                        }
                        if (s2 instanceof FloatSetting) {
                            FloatSetting num = (FloatSetting)s2;
                            num.setValue(element.getAsFloat());
                            continue;
                        }
                        if (s2 instanceof ModeSetting) {
                            ModeSetting mode = (ModeSetting)s2;
                            mode.set(element.getAsString());
                            continue;
                        }
                        if (s2 instanceof TextSetting) {
                            TextSetting text = (TextSetting)s2;
                            text.setText(element.getAsString());
                            continue;
                        }
                        if (s2 instanceof BindSetting) {
                            BindSetting bind = (BindSetting)s2;
                            bind.setKey(element.getAsInt());
                            continue;
                        }
                        if (!(s2 instanceof ListSetting)) continue;
                        ListSetting list = (ListSetting)s2;
                        JsonObject listObj = element.getAsJsonObject();
                        for (BooleanSetting setting : list.getSettings()) {
                            if (!listObj.has(setting.name())) continue;
                            setting.setState(listObj.get(setting.name()).getAsBoolean());
                        }
                    }
                    catch (Exception exception) {
                    }
                }
            }
            catch (Exception exception) {
            }
        }
        for (Map.Entry entry : targetStates.entrySet()) {
            try {
                ((Module)entry.getKey()).setEnabled((Boolean)entry.getValue());
            }
            catch (Exception exception) {}
        }
    }

    private JsonObject serializeHudState() {
        JsonObject hud = new JsonObject();
        Interface interfaceModule = ModuleClass.interfaceModule;
        if (interfaceModule == null) {
            return hud;
        }
        for (Map.Entry<String, InterfaceProcessing> entry : interfaceModule.getConfigurableHudElements().entrySet()) {
            InterfaceProcessing element = entry.getValue();
            if (element == null) continue;
            JsonObject object = new JsonObject();
            object.add("unusualRectType", (JsonElement)new JsonPrimitive(Boolean.valueOf(element.isUnusualRectType())));
            if (element instanceof WaterMark) {
                WaterMark waterMark = (WaterMark)element;
                object.add("showFps", (JsonElement)new JsonPrimitive(Boolean.valueOf(waterMark.isShowFps())));
                object.add("showMs", (JsonElement)new JsonPrimitive(Boolean.valueOf(waterMark.isShowMs())));
            }
            hud.add(entry.getKey(), (JsonElement)object);
        }
        return hud;
    }

    private void deserializeHudState(JsonObject hud) {
        Interface interfaceModule = ModuleClass.interfaceModule;
        if (interfaceModule == null) {
            return;
        }
        for (Map.Entry<String, InterfaceProcessing> entry : interfaceModule.getConfigurableHudElements().entrySet()) {
            if (!hud.has(entry.getKey())) continue;
            try {
                JsonObject object = hud.get(entry.getKey()).getAsJsonObject();
                InterfaceProcessing element = entry.getValue();
                if (object.has("unusualRectType")) {
                    element.setUnusualRectType(object.get("unusualRectType").getAsBoolean());
                }
                if (!(element instanceof WaterMark)) continue;
                WaterMark waterMark = (WaterMark)element;
                if (object.has("showFps")) {
                    waterMark.setShowFps(object.get("showFps").getAsBoolean());
                }
                if (!object.has("showMs")) continue;
                waterMark.setShowMs(object.get("showMs").getAsBoolean());
            }
            catch (Exception exception) {}
        }
    }

    private JsonObject serializeDraggables() {
        JsonObject draggables = new JsonObject();
        for (Draggable drag : DragStorage.draggables.values()) {
            JsonObject object = new JsonObject();
            object.add("x", (JsonElement)new JsonPrimitive((Number)Float.valueOf(drag.getX())));
            object.add("y", (JsonElement)new JsonPrimitive((Number)Float.valueOf(drag.getY())));
            draggables.add(drag.getName(), (JsonElement)object);
        }
        return draggables;
    }

    private void deserializeDraggables(JsonObject draggables) {
        for (String name : draggables.keySet()) {
            Draggable drag = DragStorage.draggables.get(name);
            if (drag == null) continue;
            JsonObject object = draggables.get(name).getAsJsonObject();
            if (object.has("x")) {
                drag.setX(object.get("x").getAsFloat());
            }
            if (!object.has("y")) continue;
            drag.setY(object.get("y").getAsFloat());
        }
    }

    private JsonObject serializeFiguraState() {
        JsonObject figura;
        block11: {
            figura = new JsonObject();
            try {
                Class<?> moduleCategoryClass;
                Field figuraField;
                Object figuraCategory;
                Method getComponentMethod;
                Object component;
                Class<?> menuScreenClass = Class.forName("fun.rich.display.screens.clickgui.MenuScreen");
                Field instanceField = menuScreenClass.getField("INSTANCE");
                Object menuScreen = instanceField.get(null);
                if (menuScreen == null || (component = (getComponentMethod = menuScreenClass.getMethod("getComponent", Class.forName("fun.rich.features.module.ModuleCategory"))).invoke(menuScreen, figuraCategory = (figuraField = (moduleCategoryClass = Class.forName("fun.rich.features.module.ModuleCategory")).getField("FIGURA")).get(null))) == null) break block11;
                Class<?> componentClass = component.getClass();
                try {
                    Field selectedKeyField = componentClass.getDeclaredField("selectedKey");
                    selectedKeyField.setAccessible(true);
                    String selectedKey = (String)selectedKeyField.get(component);
                    if (selectedKey != null && !selectedKey.isEmpty()) {
                        figura.add("selectedKey", (JsonElement)new JsonPrimitive(selectedKey));
                    }
                }
                catch (Exception selectedKeyField) {
                    // empty catch block
                }
                try {
                    Field appliedKeyField = componentClass.getDeclaredField("appliedKey");
                    appliedKeyField.setAccessible(true);
                    String appliedKey = (String)appliedKeyField.get(component);
                    if (appliedKey != null && !appliedKey.isEmpty()) {
                        figura.add("appliedKey", (JsonElement)new JsonPrimitive(appliedKey));
                    }
                }
                catch (Exception appliedKeyField) {
                    // empty catch block
                }
                try {
                    Field selectedNameField = componentClass.getDeclaredField("selectedName");
                    selectedNameField.setAccessible(true);
                    String selectedName = (String)selectedNameField.get(component);
                    if (selectedName != null && !selectedName.isEmpty()) {
                        figura.add("selectedName", (JsonElement)new JsonPrimitive(selectedName));
                    }
                }
                catch (Exception exception) {}
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return figura;
    }

    private void deserializeFiguraState(JsonObject figura) {
        block11: {
            try {
                Class<?> menuScreenClass = Class.forName("fun.rich.display.screens.clickgui.MenuScreen");
                Field instanceField = menuScreenClass.getField("INSTANCE");
                Object menuScreen = instanceField.get(null);
                if (menuScreen == null) break block11;
                Method getComponentMethod = menuScreenClass.getMethod("getComponent", Class.forName("fun.rich.features.module.ModuleCategory"));
                Class<?> moduleCategoryClass = Class.forName("fun.rich.features.module.ModuleCategory");
                Field figuraField = moduleCategoryClass.getField("FIGURA");
                Object figuraCategory = figuraField.get(null);
                Object component = getComponentMethod.invoke(menuScreen, figuraCategory);
                if (component == null) break block11;
                Class<?> componentClass = component.getClass();
                if (figura.has("selectedKey")) {
                    try {
                        Field selectedKeyField = componentClass.getDeclaredField("selectedKey");
                        selectedKeyField.setAccessible(true);
                        selectedKeyField.set(component, figura.get("selectedKey").getAsString());
                    }
                    catch (Exception selectedKeyField) {
                        // empty catch block
                    }
                }
                if (figura.has("appliedKey")) {
                    try {
                        Field appliedKeyField = componentClass.getDeclaredField("appliedKey");
                        appliedKeyField.setAccessible(true);
                        appliedKeyField.set(component, figura.get("appliedKey").getAsString());
                    }
                    catch (Exception appliedKeyField) {
                        // empty catch block
                    }
                }
                if (figura.has("selectedName")) {
                    try {
                        Field selectedNameField = componentClass.getDeclaredField("selectedName");
                        selectedNameField.setAccessible(true);
                        selectedNameField.set(component, figura.get("selectedName").getAsString());
                    }
                    catch (Exception exception) {}
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }
}

