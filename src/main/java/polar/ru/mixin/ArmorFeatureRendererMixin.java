package polar.ru.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.impl.render.Chams;

@Mixin(value={ArmorFeatureRenderer.class})
public class ArmorFeatureRendererMixin
implements QClient {
    @Inject(method={"render"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$hideArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        PlayerEntity player;
        PlayerEntityRenderState playerState;
        block6: {
            block5: {
                if (!(state instanceof PlayerEntityRenderState)) break block5;
                playerState = (PlayerEntityRenderState)state;
                if (ModuleClass.INSTANCE != null && ArmorFeatureRendererMixin.mc.world != null) break block6;
            }
            return;
        }
        Chams chams = ModuleClass.chams;
        if (chams == null || !chams.isEnable()) {
            return;
        }
        Entity entity = ArmorFeatureRendererMixin.mc.world.getEntityById(playerState.id);
        if (entity instanceof PlayerEntity && chams.shouldHideItemsAndCape(player = (PlayerEntity)entity)) {
            ci.cancel();
        }
    }
}

