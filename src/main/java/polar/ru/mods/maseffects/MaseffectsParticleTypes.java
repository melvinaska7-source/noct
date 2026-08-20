package polar.ru.mods.maseffects;

import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class MaseffectsParticleTypes {
    public static final SimpleParticleType REVIVE = new SimpleParticleType(false){};
    public static final SimpleParticleType REVIVE_SPARK = new SimpleParticleType(false){};

    private MaseffectsParticleTypes() {
    }

    public static void register() {
        Registry.register((Registry)Registries.PARTICLE_TYPE, (Identifier)Identifier.of((String)"polar", (String)"revive"), (Object)REVIVE);
        Registry.register((Registry)Registries.PARTICLE_TYPE, (Identifier)Identifier.of((String)"polar", (String)"revive_spark"), (Object)REVIVE_SPARK);
    }
}

