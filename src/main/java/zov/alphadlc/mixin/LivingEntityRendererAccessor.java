package zov.alphadlc.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {

    @Invoker("setupTransforms")
    void alphadlc$setupTransforms(LivingEntityRenderState state, MatrixStack matrices, float bodyYaw, float scale);

    @Invoker("scale")
    void alphadlc$scale(LivingEntityRenderState state, MatrixStack matrices);
}
