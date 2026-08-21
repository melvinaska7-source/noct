package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.event.list.EventPlayerUpdate;
import zov.alphadlc.event.list.MoveInputEvent;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.util.player.move.MoveUtil;

@ModuleInformation(moduleName = "Speed", moduleDesc = "Увеличение скорости передвижения", moduleCategory = ModuleCategory.MOVEMENT)
public class Speed extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "RW", "RW", "Collision");
    private int ticks;
    private int groundTicks;
    final Map<Entity, Vec3d> previousPositions = new HashMap<Entity, Vec3d>();

    @Subscribe
    public void onPlayerUpdate(EventPlayerUpdate event) {
        if (this.mode.is("Collision")) {
            this.tickCollision();
        } else if (this.mode.is("RW")) {
            this.tickRW();
        }
    }
    
    private void tickRW() {
        if (!this.canUseRW()) {
            this.resetRWState(true);
            return;
        }
        
        if (this.ticks > 3) {
            double boost = 0.03;
            if (this.ticks % 2 == 0) {
                this.mc.player.addVelocity(0.0, 0.03, 0.0);
                boost = this.mc.player.isOnGround() ? 0.085 : 0.03;
            }
            double yaw = Math.toRadians(MoveUtil.getDirection());
            double x2 = -Math.sin(yaw);
            double z2 = Math.cos(yaw);
            if (this.mc.player.input.movementForward == -1.0f) {
                x2 = 0.0;
                z2 = 0.0;
            }
            this.mc.player.addVelocity(x2 * boost, 0.0, z2 * boost);
        }
        ++this.ticks;
        
        if (this.ticks % 2 == 0) {
            this.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(this.mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
    }

    private void tickCollision() {
        if (!MoveUtil.isMoving()) {
            return;
        }
        double scanRadius = 1.25;
        Box searchBox = this.mc.player.getBoundingBox().expand(scanRadius);
        List<Entity> nearbyEntities = this.mc.world.getOtherEntities(this.mc.player, searchBox, entity -> entity instanceof PlayerEntity);
        for (Entity entity2 : nearbyEntities) {
            if (!(entity2 instanceof PlayerEntity)) continue;
            double distanceX = Math.abs(this.mc.player.getX() - entity2.getX());
            double distanceZ = Math.abs(this.mc.player.getZ() - entity2.getZ());
            if (distanceX > 2.1 || distanceZ > 1.3) continue;
            double entitySpeed = this.getEntitySpeed(entity2);
            if (entitySpeed < 5.0) {
                double boostAmount = 0.02;
                Box collisionBox = this.mc.player.getBoundingBox().expand(0.1);
                List<Entity> collisionEntities = this.mc.world.getOtherEntities(this.mc.player, collisionBox, e2 -> e2 instanceof PlayerEntity);
                if (collisionEntities.isEmpty()) break;
                double[] motion = this.forward(boostAmount);
                this.mc.player.addVelocity(motion[0], 0.0, motion[1]);
                break;
            }
            double boostAmount = 0.032;
            Box checkBox = this.mc.player.getBoundingBox().expand(1.25);
            List<Entity> potentialCollisions = this.mc.world.getOtherEntities(this.mc.player, checkBox, e2 -> e2 instanceof PlayerEntity);
            int collisions = 0;
            for (Entity collisionEntity : potentialCollisions) {
                double distToCollision = this.mc.player.distanceTo(collisionEntity);
                if (!(distToCollision <= 1.25)) continue;
                ++collisions;
            }
            if (collisions <= 0) break;
            double[] motion = this.forward(boostAmount);
            this.mc.player.addVelocity(motion[0], 0.0, motion[1]);
            break;
        }
    }

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        if (!this.mode.is("RW") || this.mc.player == null || !MoveUtil.isMoving()) {
            this.groundTicks = 0;
            return;
        }
        this.groundTicks = this.mc.player.verticalCollision ? ++this.groundTicks : 0;
        if (this.groundTicks >= 1) {
            this.mc.player.jump();
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!this.mode.is("RW") || this.mc.player == null || event.getType() != EventPacket.Type.RECEIVE) {
            return;
        }
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            if (this.ticks % 2 == 1) {
                ++this.ticks;
            }
        }
    }

    public double getEntitySpeed(Entity entity) {
        Vec3d currentPos = entity.getPos();
        Vec3d previousPos = this.previousPositions.getOrDefault(entity, currentPos);
        double dx = currentPos.x - previousPos.x;
        double dz = currentPos.z - previousPos.z;
        double speed = Math.sqrt(dx * dx + dz * dz) * 20.0;
        this.previousPositions.put(entity, currentPos);
        return speed;
    }

    private double[] forward(double speed) {
        float forward = this.mc.player.input.movementForward;
        float strafe = this.mc.player.input.movementSideways;
        float yaw = this.mc.player.getYaw();
        if (forward != 0.0f) {
            if (strafe > 0.0f) {
                yaw += forward > 0.0f ? -45.0f : 45.0f;
            } else if (strafe < 0.0f) {
                yaw += forward > 0.0f ? 45.0f : -45.0f;
            }
            strafe = 0.0f;
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }
        double sin = Math.sin(Math.toRadians(yaw + 90.0f));
        double cos = Math.cos(Math.toRadians(yaw + 90.0f));
        double posX = (double)forward * speed * cos + (double)strafe * speed * sin;
        double posZ = (double)forward * speed * sin - (double)strafe * speed * cos;
        return new double[]{posX, posZ};
    }

    private boolean canUseRW() {
        return this.mc.player != null && this.mc.world != null && this.mc.player.networkHandler != null && MoveUtil.isMoving() && !this.mc.player.hasVehicle() && !this.mc.player.getAbilities().flying;
    }

    private void resetRWState(boolean resetTimer) {
        this.ticks = 0;
        this.groundTicks = 0;
    }

    @Override
    public void onEnable() {
        this.resetRWState(true);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.resetRWState(true);
        super.onDisable();
    }
}
