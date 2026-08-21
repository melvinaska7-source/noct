package zov.alphadlc.util.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;

public final class NetworkUtils {
    private static boolean sendingSilent;

    private NetworkUtils() {
    }

    public static void sendSilentPacket(Packet<?> packet) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.player == null || minecraft.getNetworkHandler() == null) {
            return;
        }

        try {
            sendingSilent = true;
            minecraft.getNetworkHandler().sendPacket(packet);
        } finally {
            sendingSilent = false;
        }
    }

    public static void sendPacket(Packet<?> packet) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.player == null || minecraft.getNetworkHandler() == null) {
            return;
        }

        minecraft.getNetworkHandler().sendPacket(packet);
    }

    public static boolean isSendingSilent() {
        return sendingSilent;
    }
}

