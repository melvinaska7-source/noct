package polar.ru.mods.particular;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import polar.ru.mods.particular.ParticularParticleTypes;

public final class ParticularWaterSplash {
    private final Deque<Float> velocities = new ArrayDeque<Float>(4);

    public void trackVelocity(ClientPlayerEntity player) {
        if (player == null) {
            return;
        }
        this.velocities.addLast(Float.valueOf((float)Math.abs(player.getVelocity().y)));
        if (this.velocities.size() > 4) {
            this.velocities.removeFirst();
        }
    }

    public void trySpawnOnWaterEntry(ClientPlayerEntity player) {
        if (player == null) {
            return;
        }
        World world = player.getWorld();
        if (world == null || !world.isClient) {
            return;
        }
        double surfaceY = this.findWaterSurfaceY(player);
        float speed = this.velocities.isEmpty() ? 0.0f : Collections.max(this.velocities).floatValue();
        ParticularWaterSplash.spawnEmitter(world, player.getX(), surfaceY, player.getZ(), player.getWidth() * 2.0f, speed);
    }

    private double findWaterSurfaceY(ClientPlayerEntity player) {
        World world = player.getWorld();
        BlockPos basePos = BlockPos.ofFloored((double)player.getX(), (double)player.getY(), (double)player.getZ());
        for (int i2 = 0; i2 < 5; ++i2) {
            BlockPos pos = basePos.up(i2);
            FluidState fluidState = world.getFluidState(pos);
            if (!fluidState.isIn(FluidTags.WATER)) continue;
            return (double)((float)pos.getY() + fluidState.getHeight((BlockView)world, pos)) - 0.01;
        }
        return player.getY();
    }

    public static void spawnEmitter(World world, double x2, double y2, double z2, float width, float speed) {
        if (world == null || !world.isClient) {
            return;
        }
        world.addParticle((ParticleEffect)ParticularParticleTypes.WATER_SPLASH_EMITTER, x2, y2, z2, (double)width, (double)speed, 0.0);
    }
}

