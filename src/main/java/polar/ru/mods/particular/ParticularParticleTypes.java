package polar.ru.mods.particular;

import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ParticularParticleTypes {
    public static final SimpleParticleType WATER_SPLASH_EMITTER = new SimpleParticleType(false){};
    public static final SimpleParticleType WATER_SPLASH = new SimpleParticleType(false){};
    public static final SimpleParticleType WATER_SPLASH_FOAM = new SimpleParticleType(false){};
    public static final SimpleParticleType WATER_SPLASH_RING = new SimpleParticleType(false){};

    private ParticularParticleTypes() {
    }

    public static void register() {
        Registry.register((Registry)Registries.PARTICLE_TYPE, (Identifier)Identifier.of((String)"polar", (String)"water_splash_emitter"), (Object)WATER_SPLASH_EMITTER);
        Registry.register((Registry)Registries.PARTICLE_TYPE, (Identifier)Identifier.of((String)"polar", (String)"water_splash"), (Object)WATER_SPLASH);
        Registry.register((Registry)Registries.PARTICLE_TYPE, (Identifier)Identifier.of((String)"polar", (String)"water_splash_foam"), (Object)WATER_SPLASH_FOAM);
        Registry.register((Registry)Registries.PARTICLE_TYPE, (Identifier)Identifier.of((String)"polar", (String)"water_splash_ring"), (Object)WATER_SPLASH_RING);
    }
}

