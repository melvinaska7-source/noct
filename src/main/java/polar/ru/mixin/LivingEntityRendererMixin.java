package polar.ru.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.impl.render.Chams;
import polar.ru.client.modules.impl.render.FriendMarkers;
import polar.ru.client.modules.impl.render.SeeInvisibles;
import polar.ru.client.modules.impl.render.SeeInvisiblesRenderState;
import polar.ru.polar;

@Mixin(value={LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
implements QClient {
    @Inject(method={"updateRenderState"}, at={@At(value="TAIL")})
    private void polar$updateSeeInvisiblesState(T entity, S state, float tickDelta, CallbackInfo ci) {
        boolean shouldRenderInvisible = this.polar$shouldRenderInvisible(entity);
        ((SeeInvisiblesRenderState)state).polar$setSeeInvisiblesTarget(shouldRenderInvisible);
        if (shouldRenderInvisible) {
            ((LivingEntityRenderState)state).invisible = true;
            ((LivingEntityRenderState)state).invisibleToPlayer = false;
        }
    }

    @ModifyConstant(method={"render"}, constant={@Constant(intValue=0x26FFFFFF)})
    private int polar$changeInvisibleAlpha(int original, S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        return ((SeeInvisiblesRenderState)state).polar$isSeeInvisiblesTarget() ? SeeInvisibles.INVISIBLE_COLOR : original;
    }

    @Inject(method={"getRenderLayer"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$hideOriginalModel(S state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderLayer> cir) {
        Chams chams;
        Chams chams2 = chams = ModuleClass.INSTANCE != null ? ModuleClass.chams : null;
        if (chams == null || !chams.isEnable()) {
            return;
        }
        PlayerEntity player = this.polar$resolvePlayer(state);
        if (player != null && chams.shouldHideBaseModel(player)) {
            cir.setReturnValue(null);
        }
    }

    @Unique
    private boolean polar$shouldRenderInvisible(T entity) {
        PlayerEntity player;
        block3: {
            block2: {
                if (!(entity instanceof PlayerEntity)) break block2;
                player = (PlayerEntity)entity;
                if (ModuleClass.INSTANCE != null) break block3;
            }
            return false;
        }
        SeeInvisibles seeInvisibles = ModuleClass.seeInvisibles;
        return seeInvisibles != null && seeInvisibles.shouldRenderInvisible(player);
    }

    @Unique
    private PlayerEntity polar$resolvePlayer(S state) {
        PlayerEntity player;
        PlayerEntityRenderState playerState;
        block3: {
            block2: {
                if (!(state instanceof PlayerEntityRenderState)) break block2;
                playerState = (PlayerEntityRenderState)state;
                if (LivingEntityRendererMixin.mc.world != null) break block3;
            }
            return null;
        }
        Entity entity = LivingEntityRendererMixin.mc.world.getEntityById(playerState.id);
        return entity instanceof PlayerEntity ? (player = (PlayerEntity)entity) : null;
    }

    /*
     * Enabled aggressive block sorting
     */
    @WrapOperation(method={"render"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V")})
    private void polar$modifyFriendHead(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int light, int overlay, int color, Operation<Void> original, @Local(argsOnly=true) S livingEntityRenderState) {
        PlayerEntity player = this.polar$resolvePlayer(livingEntityRenderState);
        if (player != null && instance instanceof BipedEntityModel) {
            BipedEntityModel model = (BipedEntityModel)instance;
            if (FriendMarkers.INSTANCE.shouldScaleHead() && polar.INSTANCE.friendStorage.isFriend(player.getName().getString())) {
                float scale = 1.09f;
                model.head.scale(new Vector3f(scale, scale, scale));
                original.call(new Object[]{instance, matrixStack, vertexConsumer, light, overlay, color});
                float resetScale = 1.0f / scale;
                model.head.scale(new Vector3f(resetScale, resetScale, resetScale));
                return;
            }
        }
        original.call(new Object[]{instance, matrixStack, vertexConsumer, light, overlay, color});
    }
}

