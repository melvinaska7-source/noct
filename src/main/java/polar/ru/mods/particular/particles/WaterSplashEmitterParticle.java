package polar.ru.mods.particular.particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import polar.ru.mods.particular.ParticularParticleTypes;

public class WaterSplashEmitterParticle
extends SpriteBillboardParticle {
    private final float width;
    private final float speed;
    private boolean spawned;

    public WaterSplashEmitterParticle(ClientWorld world, double x2, double y2, double z2, double width, double speed, double ignored) {
        super(world, x2, y2, z2, 0.0, 0.0, 0.0);
        this.width = Math.max(0.2f, (float)width);
        this.speed = Math.max(0.0f, (float)speed);
        this.maxAge = 4;
        this.collidesWithWorld = false;
        this.setAlpha(0.0f);
        this.scale = 0.0f;
    }

    public void tick() {
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        if (!this.spawned) {
            this.spawned = true;
            this.spawnChildren();
        }
    }

    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.NO_RENDER;
    }

    private void spawnChildren() {
        int ringCount = Math.max(8, MathHelper.ceil((float)(this.width * 10.0f)));
        double radius = Math.max(0.18, (double)this.width * 0.25);
        double splashUp = 0.06 + (double)this.speed * 0.05;
        for (int i2 = 0; i2 < ringCount; ++i2) {
            double angle = Math.PI * 2 * (double)i2 / (double)ringCount;
            double px = this.x + Math.cos(angle) * radius;
            double pz = this.z + Math.sin(angle) * radius;
            double vx = Math.cos(angle) * (0.02 + (double)this.speed * 0.02);
            double vz = Math.sin(angle) * (0.02 + (double)this.speed * 0.02);
            this.world.addParticle((ParticleEffect)ParticularParticleTypes.WATER_SPLASH, px, this.y, pz, vx, splashUp, vz);
            this.world.addParticle((ParticleEffect)ParticularParticleTypes.WATER_SPLASH_FOAM, px, this.y + 0.03, pz, vx * 0.55, splashUp * 0.35, vz * 0.55);
        }
        this.world.addParticle((ParticleEffect)ParticularParticleTypes.WATER_SPLASH_RING, this.x, this.y, this.z, 0.0, 0.02 + (double)this.speed * 0.01, 0.0);
        int droplets = Math.max(3, MathHelper.ceil((float)(this.width * 2.0f)));
        for (int i3 = 0; i3 < droplets; ++i3) {
            double vx = (this.random.nextDouble() - 0.5) * 0.04;
            double vy = 0.05 + this.random.nextDouble() * 0.05 + (double)this.speed * 0.03;
            double vz = (this.random.nextDouble() - 0.5) * 0.04;
            this.world.addParticle((ParticleEffect)ParticleTypes.FALLING_WATER, this.x, this.y + 0.02, this.z, vx, vy, vz);
        }
    }

    public static final class Factory
    implements ParticleFactory<SimpleParticleType> {
        public Particle createParticle(SimpleParticleType parameters, ClientWorld world, double x2, double y2, double z2, double velocityX, double velocityY, double velocityZ) {
            return new WaterSplashEmitterParticle(world, x2, y2, z2, velocityX, velocityY, velocityZ);
        }
    }
}

