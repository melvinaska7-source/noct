package polar.ru.client.figura;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import polar.ru.client.figura.FiguraAvatarManager;
import polar.ru.client.figura.FiguraConfigBootstrap;

public final class FiguraOverlaySuppressor {
    private static boolean keybindsApplied;
    private static Method popupSetEnabled;
    private static Method actionWheelSetEnabled;

    private FiguraOverlaySuppressor() {
    }

    public static void tick() {
        if (!FiguraAvatarManager.isFiguraLoaded()) {
            return;
        }
        FiguraOverlaySuppressor.ensureKeybindsDisabled();
    }

    public static void afterInput() {
        if (!FiguraAvatarManager.isFiguraLoaded()) {
            return;
        }
        FiguraOverlaySuppressor.ensureKeybindsDisabled();
        FiguraOverlaySuppressor.suppressOverlays();
        FiguraOverlaySuppressor.clearScriptCursorUnlock();
        FiguraOverlaySuppressor.relockGameplayCursor();
    }

    private static void ensureKeybindsDisabled() {
        if (keybindsApplied) {
            return;
        }
        try {
            Class<?> configsClass = Class.forName("org.figuramc.figura.config.Configs");
            FiguraConfigBootstrap.disableOverlayKeybinds(configsClass);
            keybindsApplied = true;
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static void suppressOverlays() {
        FiguraOverlaySuppressor.setOverlayEnabled(FiguraOverlaySuppressor.popupSetEnabled(), false);
        FiguraOverlaySuppressor.setOverlayEnabled(FiguraOverlaySuppressor.actionWheelSetEnabled(), false);
    }

    private static void clearScriptCursorUnlock() {
        try {
            Class<?> figuraMod = Class.forName("org.figuramc.figura.FiguraMod");
            Method getLocal = figuraMod.getDeclaredMethod("getLocalPlayerUUID", new Class[0]);
            Object uuid = getLocal.invoke(null, new Object[0]);
            if (uuid == null) {
                return;
            }
            Class<?> avatarManager = Class.forName("org.figuramc.figura.avatar.AvatarManager");
            Method getAvatar = avatarManager.getMethod("getAvatarForPlayer", uuid.getClass());
            Object avatar = getAvatar.invoke(null, uuid);
            if (avatar == null) {
                return;
            }
            Field luaRuntimeField = avatar.getClass().getField("luaRuntime");
            Object luaRuntime = luaRuntimeField.get(avatar);
            if (luaRuntime == null) {
                return;
            }
            Field hostField = luaRuntime.getClass().getField("host");
            Object host = hostField.get(luaRuntime);
            if (host == null) {
                return;
            }
            Field unlockCursor = host.getClass().getField("unlockCursor");
            unlockCursor.setBoolean(host, false);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static void relockGameplayCursor() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || mc.currentScreen != null || !mc.isWindowFocused()) {
            return;
        }
        Mouse mouse = mc.mouse;
        if (mouse != null) {
            mouse.lockCursor();
        }
    }

    private static Method popupSetEnabled() {
        if (popupSetEnabled == null) {
            popupSetEnabled = FiguraOverlaySuppressor.findSetEnabled("org.figuramc.figura.gui.PopupMenu");
        }
        return popupSetEnabled;
    }

    private static Method actionWheelSetEnabled() {
        if (actionWheelSetEnabled == null) {
            actionWheelSetEnabled = FiguraOverlaySuppressor.findSetEnabled("org.figuramc.figura.gui.ActionWheel");
        }
        return actionWheelSetEnabled;
    }

    private static Method findSetEnabled(String className) {
        try {
            return Class.forName(className).getMethod("setEnabled", Boolean.TYPE);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static void setOverlayEnabled(Method method, boolean enabled) {
        if (method == null) {
            return;
        }
        try {
            method.invoke(null, enabled);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}

