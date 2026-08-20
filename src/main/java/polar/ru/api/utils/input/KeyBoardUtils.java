package polar.ru.api.utils.input;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import polar.ru.api.QClient;
import polar.ru.api.events.implement.EventBinding;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.client.ClientSoundPlayer;
import polar.ru.client.modules.Module;
import polar.ru.client.ui.MenuPanel;
import polar.ru.client.ui.autobuy.AutoBuy;

public final class KeyBoardUtils
implements QClient {
    public static final int MOUSE_BUTTON_OFFSET = 1000;

    public static void call(int key, int action) {
        if (key <= -1) {
            return;
        }
        if (action == 1) {
            if (key == 344) {
                ClientSoundPlayer.playSound("opengui.wav", 0.6, 1.0f);
                mc.setScreen((Screen)new MenuPanel());
            }
            if (key == ModuleClass.autoBuy.openKey.getKey() && ModuleClass.autoBuy.isEnable()) {
                mc.setScreen((Screen)new AutoBuy());
            }
            if (key == ModuleClass.searchHelper.bind.getKey()) {
                ModuleClass.searchHelper.onBindPressed();
            }
            new EventBinding(key, EventBinding.BindType.KEYBOARD).call();
            ObjectArrayList modules = ModuleClass.INSTANCE.getObject();
            int size = modules.size();
            for (int i2 = 0; i2 < size; ++i2) {
                Module module = (Module)modules.get(i2);
                if (module.getKey() != key) continue;
                module.toggle();
            }
        }
    }

    public static String getKeyName(int keyCode) {
        if (keyCode == -1) {
            return "None";
        }
        String name = GLFW.glfwGetKeyName((int)keyCode, (int)0);
        if (name != null) {
            return name.toUpperCase();
        }
        return switch (keyCode) {
            case 256 -> "ESC";
            case 32 -> "SPACE";
            case 340 -> "LSHIFT";
            case 344 -> "RSHIFT";
            case 341 -> "LCTRL";
            case 345 -> "RCTRL";
            default -> "KEY" + keyCode;
        };
    }

    public static void callMouse(int button, int action) {
        if (KeyBoardUtils.mc.currentScreen != null) {
            return;
        }
        if (button < 0) {
            return;
        }
        if (action == 1) {
            int mouseKey = 1000 + button;
            if (mouseKey == ModuleClass.searchHelper.bind.getKey()) {
                ModuleClass.searchHelper.onBindPressed();
            }
            new EventBinding(mouseKey, EventBinding.BindType.MOUSE).call();
            ObjectArrayList modules = ModuleClass.INSTANCE.getObject();
            int size = modules.size();
            for (int i2 = 0; i2 < size; ++i2) {
                Module module = (Module)modules.get(i2);
                if (module.getKey() != mouseKey) continue;
                module.toggle();
            }
        }
    }

    public static boolean isBindHeld(int key) {
        if (key == -1) {
            return false;
        }
        long window = mc.getWindow().getHandle();
        if (key >= 1000) {
            int mouseButton = key - 1000;
            return GLFW.glfwGetMouseButton((long)window, (int)mouseButton) == 1;
        }
        return GLFW.glfwGetKey((long)window, (int)key) == 1;
    }

    public static boolean isBindPressed(int key) {
        return KeyBoardUtils.isBindHeld(key);
    }

    public static String getBindName(int key) {
        if (key == -1) {
            return "n/a";
        }
        if (key >= 1000) {
            int mouseButton = key - 1000;
            return switch (mouseButton) {
                case 0 -> "ЛКМ";
                case 1 -> "ПКМ";
                case 2 -> "СКМ";
                case 3 -> "MOUSE4";
                case 4 -> "MOUSE5";
                default -> "MOUSE" + (mouseButton + 1);
            };
        }
        if (key >= 65 && key <= 90) {
            return String.valueOf((char)(65 + (key - 65)));
        }
        if (key >= 48 && key <= 57) {
            return String.valueOf((char)(48 + (key - 48)));
        }
        String symbol = switch (key) {
            case 96 -> "`";
            case 45 -> "-";
            case 61 -> "=";
            case 91 -> "[";
            case 93 -> "]";
            case 92 -> "\\";
            case 59 -> ";";
            case 39 -> "'";
            case 44 -> ",";
            case 46 -> ".";
            case 47 -> "/";
            default -> null;
        };
        if (symbol != null) {
            return symbol;
        }
        return switch (key) {
            case 32 -> "SPACE";
            case 340 -> "LSHIFT";
            case 344 -> "RSHIFT";
            case 341 -> "LCTRL";
            case 345 -> "RCTRL";
            case 342 -> "LALT";
            case 346 -> "RALT";
            case 258 -> "TAB";
            case 257 -> "ENTER";
            case 256 -> "ESC";
            case 259 -> "BACKSPACE";
            case 261 -> "DELETE";
            case 260 -> "INSERT";
            case 268 -> "HOME";
            case 269 -> "END";
            case 266 -> "PAGEUP";
            case 267 -> "PAGEDOWN";
            case 265 -> "UP";
            case 264 -> "DOWN";
            case 263 -> "LEFT";
            case 262 -> "RIGHT";
            case 280 -> "CAPS";
            case 290 -> "F1";
            case 291 -> "F2";
            case 292 -> "F3";
            case 293 -> "F4";
            case 294 -> "F5";
            case 295 -> "F6";
            case 296 -> "F7";
            case 297 -> "F8";
            case 298 -> "F9";
            case 299 -> "F10";
            case 300 -> "F11";
            case 301 -> "F12";
            case 320 -> "NUM0";
            case 321 -> "NUM1";
            case 322 -> "NUM2";
            case 323 -> "NUM3";
            case 324 -> "NUM4";
            case 325 -> "NUM5";
            case 326 -> "NUM6";
            case 327 -> "NUM7";
            case 328 -> "NUM8";
            case 329 -> "NUM9";
            case 330 -> "NUM.";
            case 331 -> "NUM/";
            case 332 -> "NUM*";
            case 333 -> "NUM-";
            case 334 -> "NUM+";
            case 335 -> "NUMENTER";
            default -> "KEY" + key;
        };
    }

    public static boolean isMouseButton(int key) {
        return key >= 1000;
    }

    public static int getMouseButtonFromKey(int key) {
        if (KeyBoardUtils.isMouseButton(key)) {
            return key - 1000;
        }
        return -1;
    }

    public static int createMouseBind(int mouseButton) {
        return 1000 + mouseButton;
    }
    private KeyBoardUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

