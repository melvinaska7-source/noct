package polar.ru.client.modules.impl.misc;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;

public final class KtLeave
extends Module {
    public static final KtLeave INSTANCE = new KtLeave();
    private static final double SPEED = 20.0;
    private static final int PACKETS = 30;
    private static final int INTERVAL = 1;
    private int tickCounter = 0;

    public KtLeave() {
        super("KtLeave", "HolyWorld", Module.ModuleCategory.MISC);
    }

    @EventLink
    public void onTick(EventUpdate event) {
        ClientPlayerEntity player = KtLeave.mc.player;
        if (player == null) {
            return;
        }
        double rad = Math.toRadians(player.getYaw());
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);
        player.setVelocity(new Vec3d(dx * 20.0, player.getVelocity().y, dz * 20.0));
        player.setSprinting(true);
        ++this.tickCounter;
        if (this.tickCounter < 1) {
            return;
        }
        this.tickCounter = 0;
        this.applyBoost(mc.getNetworkHandler(), player);
    }

    private void applyBoost(ClientPlayNetworkHandler conn, ClientPlayerEntity player) {
        if (conn == null) {
            return;
        }
        double rad = Math.toRadians(player.getYaw());
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);
        double x2 = player.getX();
        double y2 = player.getY();
        double z2 = player.getZ();
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        for (int i2 = 1; i2 <= 30; ++i2) {
            double step = 20.0 * (double)i2 / 30.0;
            boolean ground = i2 % 2 == 0;
            conn.sendPacket((Packet)new PlayerMoveC2SPacket.PositionAndOnGround(x2 + dx * step, ground ? y2 : y2 + 0.0625, z2 + dz * step, ground, false));
        }
        conn.sendPacket((Packet)new PlayerMoveC2SPacket.PositionAndOnGround(x2 + dx * 20.0, y2, z2 + dz * 20.0, true, false));
    }
}

