package polar.ru.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public interface QClient {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final Window mw = mc.getWindow();
}

