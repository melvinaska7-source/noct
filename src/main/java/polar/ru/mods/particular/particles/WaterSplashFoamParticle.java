package polar.ru.mods.particular.particles;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteProvider;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import polar.ru.mods.particular.particles.WaterSplashParticle;

public class WaterSplashFoamParticle
extends WaterSplashParticle {
    public WaterSplashFoamParticle(ClientWorld world, double x2, double y2, double z2, SpriteProvider sprites, double velocityX, double velocityY, double velocityZ) {
        super(world, x2, y2, z2, sprites, velocityX, velocityY, velocityZ);
        this.setColor(1.0f, 1.0f, 1.0f);
        this.setAlpha(0.85f);
    }

    public static final class Factory
    implements ParticleFactory<SimpleParticleType> {
        private final FabricSpriteProvider sprites;

        public Factory(FabricSpriteProvider sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x2, double y2, double z2, double velocityX, double velocityY, double velocityZ) {
            return new WaterSplashFoamParticle(world, x2, y2, z2, (SpriteProvider)this.sprites, velocityX, velocityY, velocityZ);
        }
    }
}

