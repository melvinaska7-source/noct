package polar.ru.mods.particular.particles;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteProvider;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;

public class WaterSplashParticle
extends SpriteBillboardParticle {
    private final SpriteProvider sprites;
    private final float startScale;

    public WaterSplashParticle(ClientWorld world, double x2, double y2, double z2, SpriteProvider sprites, double velocityX, double velocityY, double velocityZ) {
        super(world, x2, y2, z2, velocityX, velocityY, velocityZ);
        this.sprites = sprites;
        this.startScale = 0.08f + this.random.nextFloat() * 0.05f;
        this.maxAge = 18 + this.random.nextInt(8);
        this.collidesWithWorld = false;
        this.scale = this.startScale;
        this.setColor(0.86f, 0.94f, 1.0f);
        this.setAlpha(0.95f);
        this.setSpriteForAge(sprites);
    }

    public void tick() {
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        this.velocityY -= 0.02;
        this.velocityX *= 0.92;
        this.velocityY *= 0.88;
        this.velocityZ *= 0.92;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        float life = (float)this.age / (float)this.maxAge;
        this.scale = this.startScale * (0.9f + life * 0.7f);
        this.setAlpha(MathHelper.clamp((float)(1.0f - life), (float)0.0f, (float)1.0f));
        this.setSpriteForAge(this.sprites);
    }

    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Factory
    implements ParticleFactory<SimpleParticleType> {
        private final FabricSpriteProvider sprites;

        public Factory(FabricSpriteProvider sprites) {
            this.sprites = sprites;
        }

        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x2, double y2, double z2, double velocityX, double velocityY, double velocityZ) {
            return new WaterSplashParticle(world, x2, y2, z2, (SpriteProvider)this.sprites, velocityX, velocityY, velocityZ);
        }
    }
}

