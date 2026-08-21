package zov.alphadlc.mixin;

import java.util.List;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zov.alphadlc.module.list.render.Wings;
import zov.alphadlc.util.base.Instance;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void renderWingsBeforeEntities(MatrixStack matrices,
                                           VertexConsumerProvider.Immediate vertexConsumers,
                                           Camera camera, RenderTickCounter tickCounter,
                                           List<Entity> entities, CallbackInfo ci) {
        Wings wings = Instance.get(Wings.class);
        if (wings != null && wings.isEnabled()) {
            wings.renderBeforeEntities(matrices, camera, tickCounter.getTickDelta(false), entities);
        }
    }
}
