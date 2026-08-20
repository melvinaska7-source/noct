package polar.ru.api.storages.implement;

import java.lang.reflect.InvocationTargetException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.world.World;
import polar.ru.api.QClient;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.events.implement.EventPopTotem;
import polar.ru.api.events.implement.EventTickPre;

public class ServerStorage
implements QClient {
    private int serverSlot;
    private float serverYaw;
    private float serverPitch;
    private float fallDistance;
    private double serverX;
    private double serverY;
    private double serverZ;
    private boolean serverOnGround;
    private boolean serverSprinting;
    private boolean serverSneaking;
    private boolean serverHorizontalCollision;

    public void ServerManager() {
        EventInvoker.register(this);
    }

    @EventLink
    public void onTick(EventTickPre e2) {
        if (ServerStorage.mc.player == null || ServerStorage.mc.world == null) {
            return;
        }
        double y2 = ServerStorage.mc.player.prevY - ServerStorage.mc.player.getY();
        if (ServerStorage.mc.player.isOnGround()) {
            this.fallDistance = 0.0f;
        } else if (y2 > 0.0) {
            this.fallDistance += (float)y2;
        }
    }

    @EventLink
    public void onPacketSend(EventPacket e2) {
        PlayerMoveC2SPacket packet;
        if (ServerStorage.mc.player == null || ServerStorage.mc.world == null) {
            return;
        }
        Packet<?> var_2596_2 = e2.getPacket();
        if (var_2596_2 instanceof PlayerMoveC2SPacket) {
            packet = (PlayerMoveC2SPacket)var_2596_2;
            if (packet.changesPosition()) {
                this.serverX = packet.getX(ServerStorage.mc.player.getX());
                this.serverY = packet.getY(ServerStorage.mc.player.getY());
                this.serverZ = packet.getZ(ServerStorage.mc.player.getZ());
            }
            if (packet.changesLook()) {
                this.serverYaw = packet.getYaw(ServerStorage.mc.player.getYaw());
                this.serverPitch = packet.getPitch(ServerStorage.mc.player.getPitch());
            }
            this.serverOnGround = packet.isOnGround();
            this.serverHorizontalCollision = packet.horizontalCollision();
        }
        if ((var_2596_2 = e2.getPacket()) instanceof UpdateSelectedSlotC2SPacket) {
            UpdateSelectedSlotC2SPacket slotPacket = (UpdateSelectedSlotC2SPacket)var_2596_2;
            this.serverSlot = slotPacket.getSelectedSlot();
        }
        if ((var_2596_2 = e2.getPacket()) instanceof ClientCommandC2SPacket) {
            ClientCommandC2SPacket actPacket = (ClientCommandC2SPacket)var_2596_2;
            switch (actPacket.getMode()) {
                case START_SPRINTING: {
                    e2.setCancelled(this.serverSprinting);
                    if (e2.isCancelled()) break;
                    this.serverSprinting = true;
                    break;
                }
                case STOP_SPRINTING: {
                    e2.setCancelled(!this.serverSprinting);
                    if (e2.isCancelled()) break;
                    this.serverSprinting = false;
                    break;
                }
                case PRESS_SHIFT_KEY: {
                    this.serverSneaking = true;
                    break;
                }
                case RELEASE_SHIFT_KEY: {
                    this.serverSneaking = false;
                }
            }
        }
    }

    @EventLink
    public void onPacketReceive(EventPacket e2) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        EntityStatusS2CPacket packet;
        if (ServerStorage.mc.player == null || ServerStorage.mc.world == null) {
            return;
        }
        Packet<?> var_2596_2 = e2.getPacket();
        if (var_2596_2 instanceof EntityStatusS2CPacket && (packet = (EntityStatusS2CPacket)var_2596_2).getStatus() == 35) {
            Entity var_1297_2 = packet.getEntity((World)ServerStorage.mc.world);
            if (!(var_1297_2 instanceof PlayerEntity)) {
                return;
            }
            PlayerEntity player = (PlayerEntity)var_1297_2;
            EventInvoker.invoke(new EventPopTotem(player));
        }
    }
    public int getServerSlot() {
        return this.serverSlot;
    }
    public float getServerYaw() {
        return this.serverYaw;
    }
    public float getServerPitch() {
        return this.serverPitch;
    }
    public float getFallDistance() {
        return this.fallDistance;
    }
    public double getServerX() {
        return this.serverX;
    }
    public double getServerY() {
        return this.serverY;
    }
    public double getServerZ() {
        return this.serverZ;
    }
    public boolean isServerOnGround() {
        return this.serverOnGround;
    }
    public boolean isServerSprinting() {
        return this.serverSprinting;
    }
    public boolean isServerSneaking() {
        return this.serverSneaking;
    }
    public boolean isServerHorizontalCollision() {
        return this.serverHorizontalCollision;
    }
}

