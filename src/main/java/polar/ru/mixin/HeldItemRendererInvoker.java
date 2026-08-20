package polar.ru.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={HeldItemRenderer.class})
public interface HeldItemRendererInvoker {
    @Accessor(value="mainHand")
    public ItemStack whylol$getMainHand();

    @Accessor(value="offHand")
    public ItemStack whylol$getOffHand();

    @Invoker(value="renderFirstPersonItem")
    public void whylol$callRenderFirstPersonItem(AbstractClientPlayerEntity var1, float var2, float var3, Hand var4, float var5, ItemStack var6, float var7, MatrixStack var8, VertexConsumerProvider var9, int var10);

    @Invoker(value="applyEquipOffset")
    public void whylol$applyEquipOffset(MatrixStack var1, Arm var2, float var3);

    @Invoker(value="swingArm")
    public void whylol$callSwingArm(float var1, float var2, MatrixStack var3, int var4, Arm var5);

    @Invoker(value="renderArmHoldingItem")
    public void whylol$renderArmHoldingItem(MatrixStack var1, VertexConsumerProvider var2, int var3, float var4, float var5, Arm var6);

    @Invoker(value="renderMapInBothHands")
    public void whylol$renderMapInBothHands(MatrixStack var1, VertexConsumerProvider var2, int var3, float var4, float var5, float var6);

    @Invoker(value="renderMapInOneHand")
    public void whylol$renderMapInOneHand(MatrixStack var1, VertexConsumerProvider var2, int var3, float var4, Arm var5, float var6, ItemStack var7);

    @Invoker(value="renderItem")
    public void whylol$renderItem(LivingEntity var1, ItemStack var2, ModelTransformationMode var3, boolean var4, MatrixStack var5, VertexConsumerProvider var6, int var7);
}

