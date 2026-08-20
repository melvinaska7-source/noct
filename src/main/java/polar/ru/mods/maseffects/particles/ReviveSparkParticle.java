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

public class ReviveSparkParticle
extends SpriteBillboardParticle {
    private final SpriteProvider sprites;
    private final int targetEntityId;
    private final float startScale;

    public ReviveSparkParticle(ClientWorld world, double x2, double y2, double z2, SpriteProvider sprites, double targetEntityId, double velocityX, double velocityY) {
        super(world, x2, y2, z2, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        this.targetEntityId = MathHelper.floor((double)targetEntityId);
        this.startScale = 0.16f + this.random.nextFloat() * 0.08f;
        this.maxAge = 18 + this.random.nextInt(8);
        this.collidesWithWorld = false;
        this.setColor(1.0f, 1.0f, 1.0f);
        this.setAlpha(0.0f);
        this.scale = this.startScale;
        this.velocityX = velocityX * 0.08 + (this.random.nextDouble() - 0.5) * 0.02;
        this.velocityY = velocityY * 0.08 + (this.random.nextDouble() - 0.5) * 0.02 + 0.02;
        this.velocityZ = (this.random.nextDouble() - 0.5) * 0.02;
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
            double pull = this.age > 6 ? 0.075 : 0.025;
            this.velocityX += dx / distance * pull;
            this.velocityY += dy / distance * pull;
            this.velocityZ += dz / distance * pull;
        }
        this.velocityX *= 0.94;
        this.velocityY *= 0.94;
        this.velocityZ *= 0.94;
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.setSpriteForAge(this.sprites);
        float life = (float)this.age / (float)this.maxAge;
        this.setAlpha(MathHelper.clamp((float)(1.0f - life * 1.25f), (float)0.0f, (float)1.0f));
        this.scale = this.startScale * (1.0f - life * 0.45f);
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
            return new ReviveSparkParticle(world, x2, y2, z2, (SpriteProvider)this.sprites, velocityX, velocityY, velocityZ);
        }
    }
}

