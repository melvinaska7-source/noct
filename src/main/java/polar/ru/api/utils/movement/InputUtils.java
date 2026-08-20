package polar.ru.api.utils.movement;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import polar.ru.api.QClient;

public final class InputUtils
implements QClient {
    private static boolean movementLocked;

    public static void lockMovement() {
        movementLocked = true;
        InputUtils.unpressMovementKeys();
    }

    public static void unlockMovement() {
        movementLocked = false;
    }

    public static boolean isMovementLocked() {
        return movementLocked;
    }

    private static void unpressMovementKeys() {
        if (mc == null || InputUtils.mc.options == null) {
            return;
        }
        InputUtils.mc.options.forwardKey.setPressed(false);
        InputUtils.mc.options.backKey.setPressed(false);
        InputUtils.mc.options.leftKey.setPressed(false);
        InputUtils.mc.options.rightKey.setPressed(false);
        InputUtils.mc.options.jumpKey.setPressed(false);
        InputUtils.mc.options.sneakKey.setPressed(false);
        InputUtils.mc.options.sprintKey.setPressed(false);
    }

    public static void syncMovementKeys(KeyBinding[] bindings) {
        if (mc == null || mc.getWindow() == null || InputUtils.mc.options == null || movementLocked) {
            return;
        }
        for (KeyBinding binding : bindings) {
            binding.setPressed(InputUtil.isKeyPressed((long)mc.getWindow().getHandle(), (int)binding.getDefaultKey().getCode()));
        }
    }
    private InputUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

