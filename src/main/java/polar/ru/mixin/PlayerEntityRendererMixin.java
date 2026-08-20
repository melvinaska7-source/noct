package polar.ru.mixin;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.client.modules.impl.render.SatelliteFeatureRenderer;
import polar.ru.mixin.LivingEntityRendererAccessor;

@Mixin(value={PlayerEntityRenderer.class})
public abstract class PlayerEntityRendererMixin {
    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    private void polar$addShoulderPetFeature(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        FeatureRendererContext rendererContext = (FeatureRendererContext)(Object)this;
        ((LivingEntityRendererAccessor)((Object)this)).polar$addFeature(new SatelliteFeatureRenderer((FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>)rendererContext, context));
    }
}

