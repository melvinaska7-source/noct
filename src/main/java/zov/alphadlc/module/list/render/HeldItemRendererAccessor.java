package zov.alphadlc.module.list.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;

public interface HeldItemRendererAccessor {
    void invokeRenderPlayerArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm);
    
    void invokeApplyItemArmTransform(MatrixStack matrices, Arm arm, float equipProgress);
    
    void invokeSwingArm(float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm);
    
    void invokeRenderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);
    
    void invokeRenderTwoHandedMap(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float pitch, float equipProgress, float swingProgress);
    
    void invokeRenderOneHandedMap(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, Arm arm, float swingProgress, ItemStack stack);
    
    ItemStack getOffHand();
}
