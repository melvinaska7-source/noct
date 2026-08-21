package zov.alphadlc.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zov.alphadlc.module.list.render.NoRender;
import zov.alphadlc.util.base.Instance;

@Mixin(WeatherRendering.class)
public class WeatherRenderingMixin {

    @Inject(method = "renderPrecipitation(Lnet/minecraft/world/World;Lnet/minecraft/client/render/VertexConsumerProvider;IFLnet/minecraft/util/math/Vec3d;)V",
            at = @At("HEAD"), cancellable = true)
    private void cancelPrecipitation(World world, VertexConsumerProvider vertexConsumers, int ticks,
                                     float tickDelta, Vec3d cameraPos, CallbackInfo ci) {
        if (shouldRemovePrecipitation()) ci.cancel();
    }

    @Inject(method = "addParticlesAndSound", at = @At("HEAD"), cancellable = true)
    private void cancelPrecipitationEffects(ClientWorld world, Camera camera, int ticks,
                                            ParticlesMode particlesMode, CallbackInfo ci) {
        if (shouldRemovePrecipitation()) ci.cancel();
    }

    private static boolean shouldRemovePrecipitation() {
        NoRender removals = Instance.get(NoRender.class);
        return removals != null && removals.isEnabled() && removals.elements.isEnabled("Дождь");
    }
}
