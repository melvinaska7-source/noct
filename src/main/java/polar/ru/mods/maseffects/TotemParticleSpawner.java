package polar.ru.mods.maseffects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.world.World;
import polar.ru.mods.maseffects.MaseffectsParticleTypes;

public final class TotemParticleSpawner {
    private TotemParticleSpawner() {
    }

    public static void spawn(LivingEntity entity) {
        int i2;
        if (entity == null) {
            return;
        }
        World world = entity.getWorld();
        if (world == null || !world.isClient) {
            return;
        }
        double x2 = entity.getX();
        double y2 = entity.getY() + (double)entity.getHeight() * 0.5;
        double z2 = entity.getZ();
        float scale = Math.max(0.4f, entity.getWidth());
        int entityId = entity.getId();
        for (i2 = 0; i2 < 17; ++i2) {
            double offsetX = (Math.random() - 0.5) * (double)entity.getWidth();
            double offsetY = Math.random() * (double)entity.getHeight();
            double offsetZ = (Math.random() - 0.5) * (double)entity.getWidth();
            world.addParticle((ParticleEffect)MaseffectsParticleTypes.REVIVE, x2 + offsetX, y2 + offsetY, z2 + offsetZ, (double)scale, (double)entityId, 0.0);
        }
        for (i2 = 0; i2 < 99; ++i2) {
            double spread = 0.32;
            double vx = (Math.random() - 0.5) * spread;
            double vy = Math.random() * 0.22;
            double vz = (Math.random() - 0.5) * spread;
            world.addParticle((ParticleEffect)MaseffectsParticleTypes.REVIVE_SPARK, x2, y2, z2, (double)entityId, vx, vy);
        }
    }
}

