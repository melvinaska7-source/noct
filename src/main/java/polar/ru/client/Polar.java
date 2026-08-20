package polar.ru.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.particle.ParticleType;
import polar.ru.client.figura.FiguraConfigBootstrap;
import polar.ru.client.modules.impl.misc.FiguraAvatarInstaller;
import polar.ru.mods.maseffects.MaseffectsParticleTypes;
import polar.ru.mods.maseffects.particles.ReviveParticle;
import polar.ru.mods.maseffects.particles.ReviveSparkParticle;
import polar.ru.mods.particular.ParticularParticleTypes;
import polar.ru.mods.particular.particles.WaterSplashEmitterParticle;
import polar.ru.mods.particular.particles.WaterSplashFoamParticle;
import polar.ru.mods.particular.particles.WaterSplashParticle;
import polar.ru.mods.particular.particles.WaterSplashRingParticle;

public class Polar
implements ClientModInitializer {
    public void onInitializeClient() {
        FiguraAvatarInstaller.installAsync();
        FiguraConfigBootstrap.ensureAvatarNetworking();
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register((ParticleType)MaseffectsParticleTypes.REVIVE, ReviveParticle.Factory::new);
        registry.register((ParticleType)MaseffectsParticleTypes.REVIVE_SPARK, ReviveSparkParticle.Factory::new);
        registry.register((ParticleType)ParticularParticleTypes.WATER_SPLASH, WaterSplashParticle.Factory::new);
        registry.register((ParticleType)ParticularParticleTypes.WATER_SPLASH_FOAM, WaterSplashFoamParticle.Factory::new);
        registry.register((ParticleType)ParticularParticleTypes.WATER_SPLASH_RING, WaterSplashRingParticle.Factory::new);
        registry.register((ParticleType)ParticularParticleTypes.WATER_SPLASH_EMITTER, (type, world, x2, y2, z2, velocityX, velocityY, velocityZ) -> new WaterSplashEmitterParticle(world, x2, y2, z2, velocityX, velocityY, velocityZ));
    }
}

