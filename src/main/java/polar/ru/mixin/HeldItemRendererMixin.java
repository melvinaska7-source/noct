package polar.ru.mixin;

import com.google.common.base.MoreObjects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.events.implement.EventHandsRender;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.render.hands.ShaderHandsRenderer;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.render.ShaderHands;
import polar.ru.client.modules.impl.render.SwingAnimations;
import polar.ru.client.modules.impl.render.ViewModel;
import polar.ru.mixin.HeldItemRendererInvoker;

@Mixin(value={HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
    @Shadow
    private ItemStack mainHand;
    @Shadow
    private float equipProgressMainHand;
    @Shadow
    private float prevEquipProgressMainHand;
    @Shadow
    private float prevEquipProgressOffHand;
    @Shadow
    private float equipProgressOffHand;
    @Shadow
    private ItemStack offHand;

    @Shadow
    protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity var1, float var2, float var3, Hand var4, float var5, ItemStack var6, float var7, MatrixStack var8, VertexConsumerProvider var9, int var10);

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"))
    private void onRenderItemHead(float tickProgress, MatrixStack matrices, VertexConsumerProvider.Immediate immediate, ClientPlayerEntity player, int light, CallbackInfo ci) {
        EventInvoker.invoke(new EventHandsRender.Pre());
        ShaderHands shaderHands = this.getShaderHands();
        if (shaderHands == null || !shaderHands.isEnable()) {
            return;
        }
        ShaderHandsRenderer.getInstance().captureBeforeHands();
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("TAIL"))
    private void onRenderItemTail(float tickProgress, MatrixStack matrices, VertexConsumerProvider.Immediate immediate, ClientPlayerEntity player, int light, CallbackInfo ci) {
        EventInvoker.invoke(new EventHandsRender.Post());
        ShaderHands shaderHands = this.getShaderHands();
        if (shaderHands == null || !shaderHands.isEnable()) {
            return;
        }
        ShaderHandsRenderer.getInstance().captureAfterHands();
    }

    @Redirect(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void onRenderFirstPersonItemCall(HeldItemRenderer instance, AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        Hand renderHand = hand;
        SwingAnimations tweaks = this.getTweaks();
        if (tweaks != null && tweaks.isEnable() && !tweaks.hmiEnable.isState() && tweaks.swapHands.isState()) {
            renderHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        }
        ((HeldItemRendererInvoker)instance).whylol$callRenderFirstPersonItem(player, tickDelta, pitch, renderHand, swingProgress, stack, equipProgress, matrices, vertexConsumers, light);
    }

    @ModifyArg(method={"renderFirstPersonItem"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/render/item/HeldItemRenderer;renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IFFLnet/minecraft/util/Arm;)V"), index=5)
    private Arm swapEmptyHandArm(Arm arm) {
        SwingAnimations tweaks = this.getTweaks();
        if (tweaks != null && tweaks.isEnable() && !tweaks.hmiEnable.isState() && tweaks.swapHands.isState()) {
            return arm == Arm.RIGHT ? Arm.LEFT : Arm.RIGHT;
        }
        return arm;
    }

    @Inject(method={"renderFirstPersonItem"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/util/math/MatrixStack;push()V", shift=At.Shift.AFTER)})
    private void onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ViewModel viewModel = this.getViewModel();
        if (viewModel == null || !viewModel.isEnable()) {
            return;
        }
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        viewModel.applyHandPosition(matrices, arm);
    }

    @Redirect(method={"renderFirstPersonItem"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V", ordinal=2))
    private void onSwingArm(HeldItemRenderer instance, float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm) {
        Aura aura;
        SwingAnimations tweaks = this.getTweaks();
        if (tweaks == null || !tweaks.isEnable() || tweaks.hmiEnable.isState() || !tweaks.swingEnabled.isState()) {
            this.callSwingArm(instance, swingProgress, equipProgress, matrices, armX, arm);
            return;
        }
        Aura aura2 = aura = ModuleClass.INSTANCE != null ? ModuleClass.aura : null;
        if (!(!tweaks.auraTargetOnly.isState() || aura != null && aura.isEnable() && aura.getTarget() != null && aura.getTarget().isAlive())) {
            this.callSwingArm(instance, swingProgress, equipProgress, matrices, armX, arm);
            return;
        }
        if (MinecraftClient.getInstance().player != null) {
            Arm expectedSwingArm = MinecraftClient.getInstance().player.getMainArm();
            if (tweaks.swapHands.isState()) {
                Arm var_1306_2 = expectedSwingArm = expectedSwingArm == Arm.RIGHT ? Arm.LEFT : Arm.RIGHT;
            }
            if (arm != expectedSwingArm) {
                this.callSwingArm(instance, swingProgress, equipProgress, matrices, armX, arm);
                return;
            }
        }
        int i2 = arm == Arm.RIGHT ? 1 : -1;
        float strength = tweaks.swingStrength.get();
        float sin1 = MathHelper.sin((float)(swingProgress * swingProgress * (float)Math.PI));
        float sin2 = MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * (float)Math.PI));
        switch (tweaks.swingType.getCurrent()) {
            case "Down": {
                matrices.translate((float)i2 * 0.56f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(76 * i2)));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0f * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0f));
                break;
            }
            case "Poke": {
                float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966 * 2.0);
                float tilt = strength / 3.0f;
                matrices.translate((float)i2 * 0.56f, -0.52f, -0.72f);
                matrices.translate(0.0f, 0.0f, tilt * -anim);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(75.0f * (float)i2));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-75.0f * (strength / 4.0f) * anim - 60.0f) * (float)i2));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-75.0f));
                break;
            }
            case "Static": {
                matrices.translate((float)i2 * 0.56f, -0.42f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -60.0f * strength));
                matrices.translate(0.0, -0.1, 0.0);
                break;
            }
            case "Feast": {
                matrices.translate((float)i2 * 0.56f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(30 * i2)));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0f * (float)i2 * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -65.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(30 * i2)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(35 * i2)));
                break;
            }
            case "Akrien": {
                matrices.translate((float)i2 * 0.65f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(76 * i2)));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0f * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 25.0f * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -25.0f * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin1 * 15.0f * strength));
                matrices.translate(sin2 * 0.18f * strength, sin2 * 0.59f * strength, 0.0f);
                break;
            }
            case "Smooth": {
                this.applySwingOffset(matrices, i2, swingProgress, strength);
                break;
            }
            case "Block": {
                if (swingProgress > 0.0f) {
                    float g2 = MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * (float)Math.PI));
                    matrices.translate(0.56f * (float)i2, equipProgress * -0.2f - 0.5f, -0.7f);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * i2)));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g2 * -85.0f * strength));
                    matrices.translate(-0.1f * (float)i2, 0.28f, 0.2f);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85.0f));
                    break;
                }
                float n2 = -0.4f * MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * (float)Math.PI));
                float m2 = 0.2f * MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * ((float)Math.PI * 2)));
                float f1 = -0.2f * MathHelper.sin((float)(swingProgress * (float)Math.PI));
                matrices.translate(n2 * (float)i2 * strength, m2 * strength, f1 * strength);
                this.applyEquipOffset(matrices, i2, equipProgress);
                this.applySwingOffset(matrices, i2, swingProgress, strength);
                break;
            }
            case "ToBack": {
                float g3 = MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * (float)Math.PI));
                matrices.translate(0.65f * (float)i2, -0.45f, -0.9f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((-30.0f * (1.0f - g3 * strength) - 30.0f) * (float)i2));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110.0f * (float)i2));
                break;
            }
            case "SelfBack": {
                float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966 * 2.0);
                matrices.translate(0.65f * (float)i2, -0.3f, -0.8f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(90 * i2)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-70 * i2)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0f - 60.0f * strength * anim));
                break;
            }
            case "Break": 
            case "Брик": {
                matrices.translate(0.66f * (float)i2, -0.3f, -0.38f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(270 * i2)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * 10.0f * strength));
                matrices.scale(0.5f, 0.5f, 0.5f);
                matrices.translate(-0.1f * (float)i2, 0.2f, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0f * (float)i2));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-105.0f * (float)i2));
                break;
            }
            case "DropDown": {
                float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966 * 2.0);
                this.applyEquipOffset(matrices, i2, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tweaks.corner.get()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-tweaks.slant.get() * anim * strength));
                break;
            }
            case "Pander": {
                float panderAnim = MathHelper.sin((float)(swingProgress * (float)Math.PI));
                float panderF = 1.0f - equipProgress;
                matrices.translate((float)i2 * 0.56f, -0.52f, -0.72f);
                matrices.translate((0.3f - panderAnim * 0.15f) * (float)i2, 0.2f - panderF * 0.12f, -0.15f - panderAnim * 0.13f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((76.0f - 10.0f * panderAnim) * (float)i2));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-16.0f - 8.0f * panderAnim) * (float)i2));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83.0f - 26.0f * panderAnim));
                break;
            }
            case "Slant": {
                float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966 * 2.0);
                float rotate = 35.0f * strength;
                matrices.translate((float)i2 * 0.56f, -0.52f, -0.72f);
                matrices.translate(0.0f, 0.0f, -0.3f * anim * strength);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(anim * -rotate));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim * rotate));
                break;
            }
            default: {
                this.callSwingArm(instance, swingProgress, equipProgress, matrices, armX, arm);
            }
        }
    }

    @Overwrite
    public void renderItem(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light) {
        float f2 = player.getHandSwingProgress(tickDelta);
        Hand hand = (Hand)MoreObjects.firstNonNull((Object)player.preferredHand, (Object)Hand.MAIN_HAND);
        float g2 = player.getLerpedPitch(tickDelta);
        HeldItemRenderer.HandRenderType handRenderType = HeldItemRenderer.getHandRenderType(player);
        float h2 = MathHelper.lerp(tickDelta, player.lastRenderPitch, player.renderPitch);
        float i2 = MathHelper.lerp(tickDelta, player.lastRenderYaw, player.renderYaw);
        float j2;
        float k2;
        if (handRenderType.renderMainHand) {
            j2 = hand == Hand.MAIN_HAND ? f2 : 0.0f;
            k2 = 1.0f - MathHelper.lerp(tickDelta, this.prevEquipProgressMainHand, this.equipProgressMainHand);
            this.renderFirstPersonItem(player, tickDelta, g2, Hand.MAIN_HAND, j2, this.mainHand, k2, matrices, (VertexConsumerProvider)vertexConsumers, light);
        }
        if (handRenderType.renderOffHand) {
            j2 = hand == Hand.OFF_HAND ? f2 : 0.0f;
            k2 = 1.0f - MathHelper.lerp(tickDelta, this.prevEquipProgressOffHand, this.equipProgressOffHand);
            this.renderFirstPersonItem(player, tickDelta, g2, Hand.OFF_HAND, j2, this.offHand, k2, matrices, (VertexConsumerProvider)vertexConsumers, light);
        }
        vertexConsumers.draw();
    }

    @Inject(method={"applyEatOrDrinkTransformation"}, at={@At(value="HEAD")}, cancellable=true)
    private void onApplyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, PlayerEntity player, CallbackInfo ci) {
        SwingAnimations tweaks = this.getTweaks();
        if (tweaks == null || !tweaks.isEnable() || tweaks.hmiEnable.isState() || !tweaks.eatAnim.isState() || !player.isUsingItem()) {
            return;
        }
        this.applyEatOrDrinkTransformationCustom(matrices, tickDelta, arm, stack);
        ci.cancel();
    }

    private void applyEatOrDrinkTransformationCustom(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack) {
        float h2;
        if (MinecraftClient.getInstance().player == null) {
            return;
        }
        float f2 = (float)MinecraftClient.getInstance().player.getItemUseTimeLeft() - tickDelta + 1.0f;
        float g2 = f2 / (float)stack.getMaxUseTime((LivingEntity)MinecraftClient.getInstance().player);
        if (g2 < 0.8f) {
            h2 = MathHelper.abs((float)(MathHelper.cos((float)(f2 / 4.0f * (float)Math.PI)) * 0.005f));
            matrices.translate(0.0f, h2, 0.0f);
        }
        h2 = 1.0f - (float)Math.pow(g2, 27.0);
        int i2 = arm == Arm.RIGHT ? 1 : -1;
        float offsetX = 0.0f;
        float offsetY = 0.0f;
        float offsetZ = 0.0f;
        float scale = 1.0f;
        ViewModel viewModel = this.getViewModel();
        if (viewModel != null && viewModel.isEnable()) {
            if (arm == Arm.RIGHT) {
                offsetX = viewModel.mainHandX.get();
                offsetY = viewModel.mainHandY.get();
                offsetZ = viewModel.mainHandZ.get();
                scale = viewModel.mainHandScale.get();
            } else {
                offsetX = viewModel.offHandX.get();
                offsetY = viewModel.offHandY.get();
                offsetZ = viewModel.offHandZ.get();
                scale = viewModel.offHandScale.get();
            }
        }
        matrices.translate(h2 * 0.6f * (float)i2 + offsetX, h2 * -0.5f + offsetY, offsetZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i2 * h2 * 90.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h2 * 10.0f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i2 * h2 * 30.0f));
        matrices.scale(scale, scale, scale);
    }

    private void applyEquipOffset(MatrixStack matrices, int i2, float equipProgress) {
        matrices.translate((float)i2 * 0.56f, -0.52f + equipProgress * -0.6f, -0.72f);
    }

    private void applySwingOffset(MatrixStack matrices, int i2, float swingProgress, float strength) {
        float f2 = MathHelper.sin((float)(swingProgress * swingProgress * (float)Math.PI));
        matrices.translate(0.56f * (float)i2, -0.52f, -0.72f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i2 * (45.0f + f2 * -20.0f * strength)));
        float g2 = MathHelper.sin((float)(MathHelper.sqrt((float)swingProgress) * (float)Math.PI));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i2 * g2 * -20.0f * strength));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g2 * -80.0f * strength));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i2 * -45.0f));
    }

    private void callSwingArm(HeldItemRenderer instance, float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm) {
        ((HeldItemRendererInvoker)instance).whylol$callSwingArm(swingProgress, equipProgress, matrices, armX, arm);
    }

    private SwingAnimations getTweaks() {
        if (ModuleClass.INSTANCE == null) {
            return null;
        }
        return ModuleClass.swingAnimations;
    }

    private ViewModel getViewModel() {
        if (ModuleClass.INSTANCE == null) {
            return null;
        }
        return ModuleClass.viewModel;
    }

    private ShaderHands getShaderHands() {
        if (ModuleClass.INSTANCE == null) {
            return null;
        }
        return ModuleClass.shaderHands;
    }
}

