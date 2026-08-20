package polar.ru.api.utils.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.packet.Packet;
import polar.ru.api.QClient;

public final class NetworkUtils
implements QClient {
    private static final List<Packet<?>> silentPackets = new ArrayList();

    public static void sendSilentPacket(Packet<?> packet) {
        silentPackets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }

    public static void sendPacket(Packet<?> packet) {
        mc.getNetworkHandler().sendPacket(packet);
    }
    private NetworkUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    public static List<Packet<?>> getSilentPackets() {
        return silentPackets;
    }
}

