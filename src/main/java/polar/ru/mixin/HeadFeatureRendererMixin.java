package polar.ru.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.client.modules.impl.render.Chams;

@Mixin(value={HeadFeatureRenderer.class})
public abstract class HeadFeatureRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<S>>
extends FeatureRenderer<S, M> {
    public HeadFeatureRendererMixin(FeatureRendererContext<S, M> context) {
        super(context);
    }

    @Inject(method={"render"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderHead(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i2, S livingEntityRenderState, float f2, float g2, CallbackInfo ci) {
        if (!(livingEntityRenderState instanceof PlayerEntityRenderState)) {
            return;
        }
        PlayerEntityRenderState playerState = (PlayerEntityRenderState)livingEntityRenderState;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) {
            return;
        }
        Entity entity = mc.world.getEntityById(playerState.id);
        if (!(entity instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity)entity;
        if (Chams.INSTANCE != null && Chams.INSTANCE.shouldHideItemsAndCape(player)) {
            ci.cancel();
        }
    }
}

