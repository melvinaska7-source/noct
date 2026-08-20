package polar.ru.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={LivingEntityRenderer.class})
public interface LivingEntityRendererAccessor {
    @Invoker(value="addFeature")
    public boolean polar$addFeature(FeatureRenderer<?, ?> var1);

    @Invoker(value="setupTransforms")
    public void polar$setupTransforms(LivingEntityRenderState var1, MatrixStack var2, float var3, float var4);

    @Invoker(value="scale")
    public void polar$scale(LivingEntityRenderState var1, MatrixStack var2);
}

