package zov.alphadlc.util.player.combat;

import lombok.experimental.UtilityClass;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import zov.alphadlc.util.IMinecraft;

import java.util.Optional;

@UtilityClass
public class MaceUtil implements IMinecraft {

    public static boolean isHoldingMace() {
        return mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE);
    }

    public static boolean hasMace() {
        if (mc.player == null) return false;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) return true;
        }
        return false;
    }

    public static int findMaceSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) return i;
        }
        return -1;
    }

    public static Optional<Vec3d> predictLanding(ClientPlayerEntity player, World world) {
        if (world == null || player.isOnGround()) return Optional.empty();
        if (player.getAbilities().flying || player.isGliding() || player.hasVehicle() || player.isClimbing())
            return Optional.empty();
        if (player.hasStatusEffect(StatusEffects.LEVITATION)) return Optional.empty();
        if (player.isTouchingWater() || player.isInLava()) return Optional.empty();

        Box baseBox = player.getBoundingBox();
        double ox = 0.0, oy = 0.0, oz = 0.0;
        Vec3d velocity = player.getVelocity();
        double gravity = 0.08;

        for (int tick = 0; tick < 400; tick++) {
            double vx = velocity.x * 0.98;
            double vy = (velocity.y - gravity) * 0.98;
            double vz = velocity.z * 0.98;

            Vec3d step = new Vec3d(vx, vy, vz);
            Box simBox = baseBox.offset(ox, oy, oz);

            if (wouldCollideVertically(world, simBox, step.y)) {
                return Optional.of(player.getPos().add(ox, oy + step.y, oz));
            }

            ox += step.x;
            oy += step.y;
            oz += step.z;

            if (isInFluid(world, baseBox.offset(ox, oy, oz))) {
                return Optional.of(player.getPos().add(ox, oy, oz));
            }

            velocity = new Vec3d(vx, vy, vz);
            if (velocity.lengthSquared() < 1e-12 && step.y >= -1e-4) break;
        }
        return Optional.empty();
    }

    public static boolean willLandSoon() {
        if (mc.world == null || mc.player == null) return false;
        ClientPlayerEntity player = mc.player;

        double offsetY = 0.0;
        Vec3d velocity = player.getVelocity();
        double gravity = 0.08;

        for (int tick = 0; tick < 2; tick++) {
            double nextVy = (velocity.y - gravity) * 0.98;
            Vec3d step = new Vec3d(0.0, nextVy, 0.0);
            Box box = player.getBoundingBox().offset(0.0, offsetY, 0.0);

            if (nextVy < 0.0 && wouldCollideVertically(mc.world, box.offset(step.x, step.y, step.z), 0)) {
                return true;
            }
            offsetY += step.y;
            velocity = new Vec3d(velocity.x, step.y, velocity.z);
        }
        return false;
    }

    private static boolean wouldCollideVertically(World world, Box box, double dy) {
        Box moved = box.offset(0.0, dy, 0.0);
        BlockPos min = BlockPos.ofFloored(moved.minX, moved.minY, moved.minZ);
        BlockPos max = BlockPos.ofFloored(moved.maxX, moved.maxY, moved.maxZ);
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getCollisionShape(world, pos) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInFluid(World world, Box box) {
        for (BlockPos pos : BlockPos.iterate(
            BlockPos.ofFloored(box.minX, box.minY, box.minZ),
            BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ))) {
            FluidState fluid = world.getBlockState(pos).getFluidState();
            if (!fluid.isEmpty() && (fluid.isIn(FluidTags.WATER) || fluid.isIn(FluidTags.LAVA))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInSlowBlock() {
        if (mc.world == null || mc.player == null) return false;
        Box box = mc.player.getBoundingBox();
        BlockPos min = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ);
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.isOf(Blocks.COBWEB) || state.isOf(Blocks.SWEET_BERRY_BUSH)) return true;
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty() && (fluid.isIn(FluidTags.WATER) || fluid.isIn(FluidTags.LAVA))) return true;
        }
        return false;
    }
}
