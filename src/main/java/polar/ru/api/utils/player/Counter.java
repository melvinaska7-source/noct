package polar.ru.api.utils.player;

import net.minecraft.util.math.MathHelper;
import polar.ru.api.QClient;

public final class Counter
implements QClient {
    private static int currentFPS;

    public static void updateFPS() {
        int prevFPS = mc.getCurrentFps();
        currentFPS = MathHelper.lerp((float)0.5f, (int)prevFPS, (int)currentFPS);
    }
    private Counter() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    public static int getCurrentFPS() {
        return currentFPS;
    }
}

