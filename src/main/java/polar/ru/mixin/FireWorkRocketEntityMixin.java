package polar.ru.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.events.implement.EventFireWork;
import polar.ru.client.modules.impl.movement.ElytraBoost;

@Mixin(value={FireworkRocketEntity.class})
public abstract class FireWorkRocketEntityMixin
extends ProjectileEntity {
    @Unique
    private Vec3d rotation;
    @Shadow
    private LivingEntity shooter;

    public FireWorkRocketEntityMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method={"tick"}, at={@At(value="HEAD")})
    public void tick(CallbackInfo ci) {
        new EventFireWork((FireworkRocketEntity)(Object)this).call();
        MinecraftClient mc = MinecraftClient.getInstance();
        ElytraBoost elytraBoost = ElytraBoost.INSTANCE;
        if (mc != null && mc.player != null && elytraBoost != null && elytraBoost.isEnable()) {
            elytraBoost.saveLastPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        }
    }

    @ModifyExpressionValue(method={"tick"}, at={@At(value="INVOKE", target="Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;")})
    public Vec3d captureRotation(Vec3d original) {
        this.rotation = original;
        return this.rotation;
    }

    @Redirect(method={"tick"}, at=@At(value="INVOKE", target="Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;", ordinal=0))
    public Vec3d modifyBoost(Vec3d velocity, double x2, double y2, double z2) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ElytraBoost elytraBoost = ElytraBoost.INSTANCE;
        if (mc == null || mc.player == null || !mc.player.isGliding()) {
            return this.defaultBoost(velocity);
        }
        if (elytraBoost == null || !elytraBoost.isEnable()) {
            return this.defaultBoost(velocity);
        }
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        Vec2f boost = elytraBoost.computeBoost(yaw, pitch);
        float xzBoost = boost.x;
        float yBoost = boost.y;
        return velocity.add(this.rotation.x * 0.1 + (this.rotation.x * (double)xzBoost - velocity.x) * 0.5, this.rotation.y * 0.1 + (this.rotation.y * (double)yBoost - velocity.y) * 0.5, this.rotation.z * 0.1 + (this.rotation.z * (double)xzBoost - velocity.z) * 0.5);
    }

    @Unique
    private Vec3d defaultBoost(Vec3d velocity) {
        return velocity.add(this.rotation.x * 0.1 + (this.rotation.x * 1.5 - velocity.x) * 0.5, this.rotation.y * 0.1 + (this.rotation.y * 1.5 - velocity.y) * 0.5, this.rotation.z * 0.1 + (this.rotation.z * 1.5 - velocity.z) * 0.5);
    }
}

