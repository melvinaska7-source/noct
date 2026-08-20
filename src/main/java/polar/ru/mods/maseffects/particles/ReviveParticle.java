package polar.ru.mods.maseffects.particles;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteProvider;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;

public class ReviveParticle
extends SpriteBillboardParticle {
    private final SpriteProvider sprites;
    private final int targetEntityId;
    private final float baseScale;

    public ReviveParticle(ClientWorld world, double x2, double y2, double z2, SpriteProvider sprites, double scale, double targetEntityId, double ignored) {
        super(world, x2, y2, z2, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.targetEntityId = MathHelper.floor((double)targetEntityId);
        this.baseScale = 0.5f + (float)Math.max(0.0, scale) * 0.2f;
        this.maxAge = 24 + this.random.nextInt(10);
        this.collidesWithWorld = false;
        this.setColor(0.82f, 0.67f, 1.0f);
        this.setAlpha(0.0f);
        this.scale = this.baseScale;
        this.setSpriteForAge(sprites);
    }

    public void tick() {
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        Entity entity = this.world.getEntityById(this.targetEntityId);
        if (entity != null) {
            double tx = entity.getX();
            double ty = entity.getY() + (double)entity.getHeight() * 0.5;
            double tz = entity.getZ();
            double dx = tx - this.x;
            double dy = ty - this.y;
            double dz = tz - this.z;
            double distance = Math.max(0.001, Math.sqrt(dx * dx + dy * dy + dz * dz));
            double pull = 0.045 + Math.min(0.08, distance * 0.012);
            this.velocityX += dx / distance * pull;
            this.velocityY += dy / distance * pull;
            this.velocityZ += dz / distance * pull;
        }
        this.velocityX *= 0.91;
        this.velocityY *= 0.91;
        this.velocityZ *= 0.91;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.setSpriteForAge(this.sprites);
        float life = (float)this.age / (float)this.maxAge;
        float fadeIn = MathHelper.clamp((float)(life / 0.18f), (float)0.0f, (float)1.0f);
        float fadeOut = MathHelper.clamp((float)((1.0f - life) / 0.22f), (float)0.0f, (float)1.0f);
        this.setAlpha(Math.min(fadeIn, fadeOut));
        this.scale = this.baseScale * (0.85f + MathHelper.sin((float)(life * (float)Math.PI)) * 0.35f);
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
            return new ReviveParticle(world, x2, y2, z2, (SpriteProvider)this.sprites, velocityX, velocityY, velocityZ);
        }
    }
}

