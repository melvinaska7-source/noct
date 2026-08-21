package zov.alphadlc.mixin;

import com.google.common.base.MoreObjects;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zov.alphadlc.module.list.render.GlassHands;
import zov.alphadlc.module.list.render.HMIRenderer;
import zov.alphadlc.module.list.render.HeldItemRendererAccessor;
import zov.alphadlc.module.list.render.SwingAnimations;
import zov.alphadlc.module.list.render.ViewModel;
import zov.alphadlc.util.base.Instance;
import zov.alphadlc.util.render.hands.GlassHandsRenderer;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin implements HeldItemRendererAccessor {

    @Shadow private ItemStack mainHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float prevEquipProgressOffHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private ItemStack offHand;

    @Shadow
    protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Shadow
    protected abstract void swingArm(float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm);

    @Shadow
    private static HeldItemRenderer.HandRenderType getHandRenderType(ClientPlayerEntity player) {
        throw new AssertionError();
    }

    @Invoker("renderArmHoldingItem")
    @Override
    public abstract void invokeRenderPlayerArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm);

    @Invoker("applyEquipOffset")
    @Override
    public abstract void invokeApplyItemArmTransform(MatrixStack matrices, Arm arm, float equipProgress);

    @Invoker("swingArm")
    @Override
    public abstract void invokeSwingArm(float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm);

    @Invoker("renderItem")
    @Override
    public abstract void invokeRenderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Invoker("renderMapInBothHands")
    @Override
    public abstract void invokeRenderTwoHandedMap(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float pitch, float equipProgress, float swingProgress);

    @Invoker("renderMapInOneHand")
    @Override
    public abstract void invokeRenderOneHandedMap(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, Arm arm, float swingProgress, ItemStack stack);

    @Accessor("offHand")
    @Override
    public abstract ItemStack getOffHand();

    @Inject(
            method = "renderFirstPersonItem",
            at = @At("HEAD"),
            cancellable = true
    )
    public void injectHMIHead(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        var swingAnimation = Instance.get(SwingAnimations.class);
        if (swingAnimation != null && swingAnimation.isEnabled() && swingAnimation.isHMI()) {
            HMIRenderer hmi = swingAnimation.getHMIRenderer();
            hmi.renderHMI((HeldItemRenderer)(Object)this, this, player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
            ci.cancel();
        }
    }

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    public void injectAfterMatrixPushHandPosition(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        var viewModel = Instance.get(ViewModel.class);
        if (viewModel != null && viewModel.isEnabled() && !item.isEmpty() && !item.contains(DataComponentTypes.MAP_ID)) {
            var isMainHand = hand == Hand.MAIN_HAND;
            var arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            viewModel.applyHandPosition(matrices, arm);
        }
    }

    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
                    ordinal = 2
            )
    )
    public void redirectSwingArmForCustomAnim(HeldItemRenderer instance, float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm) {
        var swingAnimation = Instance.get(SwingAnimations.class);
        if (swingAnimation != null && swingAnimation.isEnabled()) {
            if (arm == Arm.RIGHT) {
                swingAnimation.renderSwordAnimation(matrices, swingProgress, equipProgress, arm);
            } else {
                swingArm(swingProgress, equipProgress, matrices, armX, arm);
            }
        } else {
            swingArm(swingProgress, equipProgress, matrices, armX, arm);
        }
    }

    @Overwrite
    public void renderItem(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light) {
        GlassHands glassHands = Instance.get(GlassHands.class);
        boolean glassActive = glassHands != null && glassHands.isEnabled();

        if (glassActive) {
            GlassHandsRenderer.getInstance().captureBeforeHands(glassHands);
        }

        float f = player.getHandSwingProgress(tickDelta);
        Hand hand = (Hand) MoreObjects.firstNonNull(player.preferredHand, Hand.MAIN_HAND);
        float g = player.getLerpedPitch(tickDelta);
        HeldItemRenderer.HandRenderType handRenderType = this.getHandRenderType(player);

        float j;
        float k;
        if (handRenderType.renderMainHand) {
            j = hand == Hand.MAIN_HAND ? f : 0.0F;
            k = 1.0F - MathHelper.lerp(tickDelta, this.prevEquipProgressMainHand, this.equipProgressMainHand);
            this.renderFirstPersonItem(player, tickDelta, g, Hand.MAIN_HAND, j, this.mainHand, k, matrices, vertexConsumers, light);
        }

        if (handRenderType.renderOffHand) {
            j = hand == Hand.OFF_HAND ? f : 0.0F;
            k = 1.0F - MathHelper.lerp(tickDelta, this.prevEquipProgressOffHand, this.equipProgressOffHand);
            this.renderFirstPersonItem(player, tickDelta, g, Hand.OFF_HAND, j, this.offHand, k, matrices, vertexConsumers, light);
        }

        vertexConsumers.draw();

        if (glassActive) {
            GlassHandsRenderer.getInstance().captureAfterHands(glassHands);
        }
    }
}
