package polar.ru.client.figura;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import polar.ru.client.figura.FiguraConfigBootstrap;
import polar.ru.client.modules.impl.misc.FiguraAvatarInstaller;

public final class FiguraAvatarManager {
    private static final long LOAD_TIMEOUT_MS = 20000L;
    private static final AtomicBoolean APPLYING = new AtomicBoolean(false);

    private FiguraAvatarManager() {
    }

    public static boolean isFiguraLoaded() {
        return FabricLoader.getInstance().isModLoaded("figura");
    }

    public static boolean isApplying() {
        return APPLYING.get();
    }

    public static List<AvatarEntry> listInstalledAvatars() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.runDirectory == null) {
            return List.of();
        }
        Path avatarsDir = mc.runDirectory.toPath().resolve("figura").resolve("avatars").normalize();
        if (!Files.isDirectory(avatarsDir, new LinkOption[0])) {
            return List.of();
        }
        ArrayList<AvatarEntry> entries = new ArrayList<AvatarEntry>();
        try (Stream<Path> stream = Files.list(avatarsDir);){
            stream.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).forEach(path -> {
                if (!Files.exists(path.resolve("avatar.json"), new LinkOption[0])) {
                    return;
                }
                String folder = path.getFileName().toString();
                entries.add(new AvatarEntry(FiguraAvatarManager.cleanName(folder), folder, (Path)path));
            });
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        entries.sort(Comparator.comparing(entry -> entry.displayName().toLowerCase(Locale.ROOT)));
        return entries;
    }

    public static void applyAvatarAsync(AvatarEntry entry, Consumer<String> onStatus) {
        if (entry == null) {
            onStatus.accept("Модель не выбрана.");
            return;
        }
        if (!FiguraAvatarManager.isFiguraLoaded()) {
            onStatus.accept("Figura не загружена.");
            return;
        }
        if (!APPLYING.compareAndSet(false, true)) {
            onStatus.accept("Уже применяется...");
            return;
        }
        onStatus.accept("Установка: " + entry.displayName());
        Thread thread = new Thread(() -> {
            try {
                FiguraAvatarInstaller.installBlocking();
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc == null) {
                    onStatus.accept("Клиент недоступен.");
                    return;
                }
                CompletableFuture result = new CompletableFuture();
                mc.execute(() -> FiguraAvatarManager.startAvatarApply(mc, entry, result));
                String message = (String)result.get(25000L, TimeUnit.MILLISECONDS);
                onStatus.accept(message);
            }
            catch (Throwable t2) {
                onStatus.accept("Ошибка: " + t2.getClass().getSimpleName());
            }
            finally {
                APPLYING.set(false);
            }
        }, "Polar-Figura-Apply");
        thread.setDaemon(true);
        thread.start();
    }

    public static String applyAvatar(AvatarEntry entry) throws Exception {
        if (entry == null) {
            return "Модель не выбрана.";
        }
        if (!FiguraAvatarManager.isFiguraLoaded()) {
            return "Figura не загружена.";
        }
        FiguraAvatarInstaller.installBlocking();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return "Клиент недоступен.";
        }
        CompletableFuture result = new CompletableFuture();
        mc.execute(() -> FiguraAvatarManager.startAvatarApply(mc, entry, result));
        return (String)result.get(25000L, TimeUnit.MILLISECONDS);
    }

    public static void removeAvatar() {
        if (!FiguraAvatarManager.isFiguraLoaded()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return;
        }
        Runnable action = () -> {
            try {
                Class<?> manager = Class.forName("org.figuramc.figura.avatar.AvatarManager");
                FiguraAvatarManager.resetPanic(manager);
                FiguraAvatarManager.resetWatchKeys();
                UUID uuid = FiguraAvatarManager.getLocalPlayerUuid();
                if (uuid == null && mc.player != null) {
                    uuid = mc.player.getUuid();
                }
                if (uuid == null) {
                    return;
                }
                Method clearAll = FiguraAvatarManager.findExactMethod(manager, "clearAvatars", UUID.class);
                if (clearAll != null) {
                    FiguraAvatarManager.invoke(clearAll, uuid);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        };
        if (mc.isOnThread()) {
            action.run();
        } else {
            mc.execute(action);
        }
    }

    public static void repairAvatarMetadata(Path avatarDir) {
        if (avatarDir == null || !Files.isDirectory(avatarDir, new LinkOption[0])) {
            return;
        }
        try {
            Path jsonPath;
            Path script = avatarDir.resolve("script.lua");
            Path scriptCap = avatarDir.resolve("Script.lua");
            if (Files.isRegularFile(scriptCap, new LinkOption[0]) && !Files.isRegularFile(script, new LinkOption[0])) {
                Files.copy(scriptCap, script, new CopyOption[0]);
            }
            if (!Files.isRegularFile(jsonPath = avatarDir.resolve("avatar.json"), new LinkOption[0])) {
                return;
            }
            JsonObject root = JsonParser.parseString((String)Files.readString(jsonPath, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray autoScripts = root.has("autoScripts") && root.get("autoScripts").isJsonArray() ? root.getAsJsonArray("autoScripts") : new JsonArray();
            HashSet<String> existing = new HashSet<String>();
            for (Object element : autoScripts) {
                if (!((com.google.gson.JsonElement)element).isJsonPrimitive()) continue;
                existing.add(((com.google.gson.JsonElement)element).getAsString());
            }
            ArrayList<String> additions = new ArrayList<String>();
            FiguraAvatarManager.addScriptIfPresent(avatarDir, "scripts/main.lua", existing, additions);
            FiguraAvatarManager.addScriptIfPresent(avatarDir, "scripts/index.lua", existing, additions);
            FiguraAvatarManager.addScriptIfPresent(avatarDir, "scripts/avatar.lua", existing, additions);
            if (Files.isRegularFile(script, new LinkOption[0]) && autoScripts.isEmpty() && additions.isEmpty()) {
                FiguraAvatarManager.addScriptIfPresent(avatarDir, "script.lua", existing, additions);
            }
            FiguraAvatarManager.addScriptIfPresent(avatarDir, "soggyscript.lua", existing, additions);
            if (additions.isEmpty()) {
                return;
            }
            for (String addition : additions) {
                autoScripts.add(addition);
            }
            root.add("autoScripts", (JsonElement)autoScripts);
            String repaired = new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement)root);
            Files.writeString(jsonPath, (CharSequence)repaired, StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static void startAvatarApply(MinecraftClient mc, AvatarEntry entry, CompletableFuture<String> result) {
        try {
            if (mc.player == null) {
                result.complete("Зайди в мир, чтобы применить модель.");
                return;
            }
            FiguraAvatarManager.repairAvatarMetadata(entry.path());
            Class<?> manager = Class.forName("org.figuramc.figura.avatar.AvatarManager");
            FiguraAvatarManager.resetPanic(manager);
            FiguraAvatarManager.resetWatchKeys();
            FiguraConfigBootstrap.ensureAvatarNetworking();
            if (!FiguraAvatarManager.loadLocalAvatar(entry.path())) {
                result.complete("Не удалось запустить загрузку: " + entry.displayName());
                return;
            }
            long deadline = System.currentTimeMillis() + 20000L;
            FiguraAvatarManager.pollAvatarLoad(mc, entry.path(), entry.displayName(), deadline, error -> {
                if (error != null) {
                    result.complete((String)error);
                } else {
                    result.complete("Применено: " + entry.displayName());
                }
            });
        }
        catch (Throwable t2) {
            result.complete("Ошибка Figura: " + t2.getClass().getSimpleName());
        }
    }

    private static void pollAvatarLoad(MinecraftClient mc, Path expectedPath, String displayName, long deadline, Consumer<String> onDone) {
        String error = FiguraAvatarManager.evaluateLoadState(expectedPath);
        if (error != null) {
            onDone.accept(error);
            return;
        }
        if (FiguraAvatarManager.isAvatarLoadedSuccessfully()) {
            onDone.accept(null);
            return;
        }
        Object avatar = FiguraAvatarManager.getLoadedAvatar();
        if (avatar != null) {
            try {
                Field loaded = avatar.getClass().getField("loaded");
                Field scriptError = avatar.getClass().getField("scriptError");
                if (loaded.getBoolean(avatar) && scriptError.getBoolean(avatar)) {
                    onDone.accept("Ошибка скрипта модели.");
                    return;
                }
            }
            catch (Throwable loaded) {
                // empty catch block
            }
        }
        if (System.currentTimeMillis() >= deadline) {
            String loadError = FiguraAvatarManager.getLoadError();
            if (loadError != null && !loadError.isBlank()) {
                onDone.accept(FiguraAvatarManager.trimError(loadError));
                return;
            }
            onDone.accept("Таймаут загрузки: " + displayName);
            return;
        }
        mc.execute(() -> FiguraAvatarManager.pollAvatarLoad(mc, expectedPath, displayName, deadline, onDone));
    }

    private static String evaluateLoadState(Path expectedPath) {
        String lateError;
        String loadError = FiguraAvatarManager.getLoadError();
        if (loadError != null && !loadError.isBlank()) {
            return FiguraAvatarManager.trimError(loadError);
        }
        Path lastLoaded = FiguraAvatarManager.getLastLoadedPath();
        String state = FiguraAvatarManager.getLoadState();
        if (lastLoaded != null && lastLoaded.normalize().equals(expectedPath.normalize()) && ("unknown".equals(state) || state == null || state.isBlank()) && FiguraAvatarManager.getLoadedAvatar() == null && (lateError = FiguraAvatarManager.getLoadError()) != null && !lateError.isBlank()) {
            return FiguraAvatarManager.trimError(lateError);
        }
        return null;
    }

    private static boolean isAvatarLoadedSuccessfully() {
        Object avatar = FiguraAvatarManager.getLoadedAvatar();
        if (avatar == null) {
            return false;
        }
        try {
            Field loaded = avatar.getClass().getField("loaded");
            if (!loaded.getBoolean(avatar)) {
                return false;
            }
            Field scriptError = avatar.getClass().getField("scriptError");
            return !scriptError.getBoolean(avatar);
        }
        catch (Throwable ignored) {
            return true;
        }
    }

    private static String trimError(String error) {
        String trimmed = error.trim();
        if (trimmed.length() <= 120) {
            return "Ошибка: " + trimmed;
        }
        return "Ошибка: " + trimmed.substring(0, 117) + "...";
    }

    private static void addScriptIfPresent(Path avatarDir, String relative, Set<String> existing, List<String> additions) {
        if (existing.contains(relative) || additions.contains(relative)) {
            return;
        }
        if (Files.isRegularFile(avatarDir.resolve(relative), new LinkOption[0])) {
            additions.add(relative);
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

    private static Object getLoadedAvatar() {
        try {
            UUID uuid = FiguraAvatarManager.getLocalPlayerUuid();
            if (uuid == null) {
                return null;
            }
            Class<?> manager = Class.forName("org.figuramc.figura.avatar.AvatarManager");
            Method getter = FiguraAvatarManager.findExactMethod(manager, "getLoadedAvatar", UUID.class);
            if (getter == null) {
                getter = FiguraAvatarManager.findExactMethod(manager, "getAvatarForPlayer", UUID.class);
            }
            if (getter == null) {
                return null;
            }
            return getter.invoke(null, uuid);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static String getLoadError() {
        try {
            String string;
            Class<?> loader = Class.forName("org.figuramc.figura.avatar.local.LocalAvatarLoader");
            Method method = loader.getDeclaredMethod("getLoadError", new Class[0]);
            Object value = method.invoke(null, new Object[0]);
            return value instanceof String ? (string = (String)value) : null;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static String getLoadState() {
        try {
            String string;
            Class<?> loader = Class.forName("org.figuramc.figura.avatar.local.LocalAvatarLoader");
            Method method = loader.getDeclaredMethod("getLoadState", new Class[0]);
            Object value = method.invoke(null, new Object[0]);
            return value instanceof String ? (string = (String)value) : null;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static Path getLastLoadedPath() {
        try {
            Path path;
            Class<?> loader = Class.forName("org.figuramc.figura.avatar.local.LocalAvatarLoader");
            Method method = loader.getDeclaredMethod("getLastLoadedPath", new Class[0]);
            Object value = method.invoke(null, new Object[0]);
            return value instanceof Path ? (path = (Path)value) : null;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static void resetWatchKeys() {
        try {
            Class<?> loader = Class.forName("org.figuramc.figura.avatar.local.LocalAvatarLoader");
            Method method = loader.getDeclaredMethod("resetWatchKeys", new Class[0]);
            method.invoke(null, new Object[0]);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static boolean loadLocalAvatar(Path path) {
        try {
            Class<?> manager = Class.forName("org.figuramc.figura.avatar.AvatarManager");
            FiguraAvatarManager.resetPanic(manager);
            Method exactPath = FiguraAvatarManager.findExactMethod(manager, "loadLocalAvatar", Path.class);
            if (exactPath != null && FiguraAvatarManager.invoke(exactPath, path)) {
                return true;
            }
            Method exactStringPath = FiguraAvatarManager.findExactMethod(manager, "loadLocalAvatar", String.class, Path.class);
            if (exactStringPath != null && FiguraAvatarManager.invoke(exactStringPath, path.getFileName().toString(), path)) {
                return true;
            }
            Method exactPathBoolean = FiguraAvatarManager.findExactMethod(manager, "loadLocalAvatar", Path.class, Boolean.TYPE);
            if (exactPathBoolean != null && FiguraAvatarManager.invoke(exactPathBoolean, path, true)) {
                return true;
            }
            for (Method method : manager.getDeclaredMethods()) {
                if (!method.getName().equals("loadLocalAvatar") && !method.getName().toLowerCase(Locale.ROOT).contains("loadlocalavatar") || !Modifier.isStatic(method.getModifiers())) continue;
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && Path.class.isAssignableFrom(params[0]) && FiguraAvatarManager.invoke(method, path)) {
                    return true;
                }
                if (params.length == 2 && params[0] == String.class && Path.class.isAssignableFrom(params[1]) && FiguraAvatarManager.invoke(method, path.getFileName().toString(), path)) {
                    return true;
                }
                if (params.length != 2 || !Path.class.isAssignableFrom(params[0]) || params[1] != Boolean.TYPE && params[1] != Boolean.class || !FiguraAvatarManager.invoke(method, path, true)) continue;
                return true;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return false;
    }

    private static void resetPanic(Class<?> manager) throws Exception {
        Field panic = manager.getDeclaredField("panic");
        panic.setAccessible(true);
        if (!Modifier.isStatic(panic.getModifiers())) {
            return;
        }
        Class<?> type = panic.getType();
        if (type == Boolean.TYPE || type == Boolean.class) {
            panic.setBoolean(null, false);
        }
    }

    private static boolean invoke(Method method, Object ... args) {
        try {
            Boolean booleanResult;
            method.setAccessible(true);
            Object result = method.invoke(null, args);
            return !(result instanceof Boolean) || (booleanResult = (Boolean)result) != false;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private static Method findExactMethod(Class<?> clazz, String name, Class<?> ... params) {
        try {
            Method method = clazz.getDeclaredMethod(name, params);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static String cleanName(String folder) {
        String name = folder.replace('_', ' ').replace('-', ' ');
        name = name.replaceAll("^\\s*\\d{1,4}[a-zA-Z]?\\s*", "");
        name = name.replaceAll("\\s+\\d+$", "");
        return (name = name.replaceAll("\\s+", " ").trim()).isEmpty() ? folder : name;
    }

    public record AvatarEntry(String displayName, String folder, Path path) {
    }
}

