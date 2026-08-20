package polar.ru.mixin;

import java.util.Random;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.state.property.Property;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.impl.render.SwingAnimations;
import polar.ru.client.modules.impl.render.ViewModel;
import polar.ru.mixin.HeldItemRendererAccessor;
import polar.ru.polar;

@Mixin(value={HeldItemRenderer.class})
public abstract class HeldItemRendererHmiMixin {
    private boolean repPower = false;
    private float prevAge = 0.0f;
    private double previousRotation = 0.0;
    private float swingAngleY = 0.0f;
    private float swingAngleX = 0.0f;
    private float swingVelocityY = 0.0f;
    private float swingVelocityX = 0.0f;
    private float swingVelocityZ = 0.0f;
    private static final float GRAVITY = 0.1f;
    private static final float DAMPING = 0.88f;
    private static final float SENSITIVITY = 0.015f;
    private float vertAngleY = 0.0f;
    private float vertVelocityY = 0.0f;
    private float vertVelocityYSlime = 0.0f;
    private float vertAngleYSlime = 0.0f;
    private float riptideCounter = 0.0f;
    private float netherCounter = 0.0f;
    @Shadow
    private ItemStack mainHand;
    @Shadow
    @Final
    private MinecraftClient client;
    private float fallCounter = 0.0f;
    private float inWaterCounter = 0.0f;
    private float inspect = 0.0f;
    private float tilt = 0.0f;
    private float freezeCounter = 0.0f;
    private float clCount = 0.0f;
    private float crawlCount = 0.0f;
    private float directionalCrawlCount = 0.0f;
    private float climbCount = 0.0f;
    private float mouseHolding = 1.0f;
    private boolean isSwinging = false;
    private float swingProgress = 0.0f;
    private boolean isForward = false;
    private boolean isAttacking = false;
    private boolean left = false;
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

    private float easeInOutBack(float x2) {
        float c1 = 1.70158f;
        float c2 = c1 * 1.525f;
        return (float)((double)x2 < 0.5 ? Math.pow(2.0f * x2, 2.0) * (double)((c2 + 1.0f) * 2.0f * x2 - c2) / 2.0 : (Math.pow(2.0f * x2 - 2.0f, 2.0) * (double)((c2 + 1.0f) * (x2 * 2.0f - 2.0f) + c2) + 2.0) / 2.0);
    }

    private float getAttackDamage(ItemStack stack) {
        AttributeModifiersComponent modifiers = (AttributeModifiersComponent)stack.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0.0f;
        }
        float totalDamage = 0.0f;
        for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() != EntityAttributes.ATTACK_DAMAGE.value()) continue;
            totalDamage += (float)entry.modifier().value();
        }
        return totalDamage;
    }

    private boolean isSharpAnimation(SwingAnimations config) {
        return config != null && config.hmiAnimationType.is("Шарп");
    }

    private void altSwing(MatrixStack matrices, Arm arm, float swingProgress, ItemStack item) {
        int i2 = arm == Arm.RIGHT ? 1 : -1;
        float f2 = MathHelper.sin((float)(swingProgress * 3.14f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i2 * (45.0f + f2 * 0.0f)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i2 * -45.0f));
    }

    @Inject(method={"renderFirstPersonItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        float sideFactor;
        SwingAnimations swings = ModuleClass.swingAnimations;
        if (!swings.isEnable() || !swings.hmiEnable.isState()) {
            return;
        }
        boolean isMainHand = hand == Hand.MAIN_HAND;
        Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        float f2 = sideFactor = isMainHand ? 1.0f : -1.0f;
        if (swings.swapHands.isState()) {
            arm = arm.getOpposite();
            sideFactor *= -1.0f;
        }
        this.renderCustomFirstPersonItem(player, tickDelta, pitch, hand, arm, sideFactor, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
        ci.cancel();
    }

    private void renderCustomFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, Arm arm, float sideFactor, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        SwingAnimations swings = ModuleClass.swingAnimations;
        if (swings.isEnable() && swings.hmiEnable.isState() && !player.isUsingSpyglass()) {
            HeldItemRendererAccessor acc;
            boolean sharpSword;
            ViewModel viewModel;
            SwingAnimations config = ModuleClass.swingAnimations;
            float yaw = player.getYaw();
            double radians = Math.toRadians(yaw);
            double forwardX = -Math.sin(radians);
            double forwardZ = Math.cos(radians);
            Vec3d horizontalVelocity = player.getVelocity();
            double dotProduct = horizontalVelocity.x * forwardX + horizontalVelocity.z * forwardZ;
            double crossProduct = player.getVelocity().getHorizontal().x * forwardZ - horizontalVelocity.z * forwardX;
            float al2 = player.getPitch() != 0.0f ? 90.0f / player.getPitch() / 10.0f : 1.0f;
            if (al2 > 1.0f) {
                al2 = 1.0f;
            }
            if (al2 < 0.0f) {
                al2 = 1.0f;
            }
            boolean bl = hand == Hand.MAIN_HAND;
            matrices.push();
            matrices.push();
            ViewModel viewModel2 = viewModel = ModuleClass.INSTANCE != null ? ModuleClass.viewModel : null;
            if (viewModel != null && viewModel.isEnable()) {
                viewModel.applyHandPosition(matrices, arm);
            }
            double tt = polar.deltaTime * 30.0;
            float smoothness = MathHelper.clamp((float)config.hmiSmoothness.get(), (float)0.35f, (float)2.5f);
            float hmiProgress = (float)Math.pow(MathHelper.clamp((float)swingProgress, (float)0.0f, (float)1.0f), smoothness);
            float swing_rot = (double)hmiProgress < 0.6 ? MathHelper.sin((float)(MathHelper.clamp((float)hmiProgress, (float)0.0f, (float)0.12506f) * 12.56f)) : MathHelper.sin((float)(MathHelper.clamp((float)hmiProgress, (float)0.62532f, (float)0.75038f) * 12.56f));
            float swing = MathHelper.sin((float)(hmiProgress * 3.14f));
            swing = this.easeInOutBack(swing);
            boolean bl2 = sharpSword = item.isIn(ItemTags.SWORDS) && this.isSharpAnimation(config);
            if ((item.isOf(Items.EXPERIENCE_BOTTLE) || item.isOf(Items.WIND_CHARGE) || item.isOf(Items.EGG) || item.isOf(Items.ENDER_EYE) || item.isOf(Items.SNOWBALL) || item.getItem() instanceof SplashPotionItem || item.getItem() instanceof LingeringPotionItem) && player.getOffHandStack().isEmpty() && item.getUseAction() != UseAction.SPEAR && !item.isOf(Items.FIRE_CHARGE) && !player.isSwimming() && !player.isCrawling() && !player.isClimbing()) {
                if (player.getMainArm() == Arm.LEFT) {
                    bl = !bl;
                }
                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25.0f * sideFactor));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0f * sideFactor * swing));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f * swing));
                matrices.translate(-0.15 * (double)sideFactor, 0.1, 0.1);
                matrices.translate(0.0, -0.55 * (double)swing, 0.4 * (double)swing * (double)3.14f);
                acc = (HeldItemRendererAccessor)((Object)this);
                acc.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, 0.0f, arm.getOpposite());
                matrices.pop();
            }
            if (this.client.options.attackKey.isPressed() && !this.isAttacking && (double)swingProgress == 0.0) {
                boolean bl3 = this.left = !this.left;
            }
            if (!item.isEmpty()) {
                if (player.getMainArm() == Arm.LEFT) {
                    boolean bl4 = bl = !bl;
                }
                if ((this.left || item.isIn(ItemTags.AXES) || item.getUseAction() == UseAction.SPEAR || item.getUseAction() == UseAction.BLOCK) && !item.isIn(ItemTags.SHOVELS)) {
                    if (sharpSword) {
                        matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.5 * (double)swing);
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0f * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0f * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0f * swing));
                    } else if (!item.isIn(ItemTags.SWORDS) && !item.isIn(ItemTags.AXES)) {
                        if (item.getUseAction() == UseAction.SPEAR) {
                            matrices.translate(0.0, 0.0, 0.45 * (double)swing_rot);
                            matrices.translate(-0.25 * (double)sideFactor * (double)swing, -0.35 * (double)swing_rot, -0.6 * (double)swing);
                            matrices.translate(0.0, 0.1 * (double)swing, 0.0);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0f * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0f * swing_rot * sideFactor));
                        } else if (item.isIn(ConventionalItemTags.TOOLS) && item.getUseAction() != UseAction.BLOCK && !item.isIn(ItemTags.SHOVELS)) {
                            matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.5 * (double)swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0f * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0f * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0f * swing));
                        } else if (item.getUseAction() != UseAction.BLOCK) {
                            matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.1 * (double)swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0f * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0f * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0f * swing));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0f * swing * sideFactor));
                        } else {
                            matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.2 * (double)swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-10.0f * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0f * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(20.0f * swing));
                        }
                    } else {
                        matrices.translate(0.8 * (double)sideFactor * (double)swing_rot, 0.3 * (double)swing_rot, -0.5 * (double)swing);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0f * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-20.0f * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0f * swing_rot * sideFactor));
                        if (item.isIn(ItemTags.SWORDS)) {
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0f * swing));
                        } else {
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(30.0f * swing));
                        }
                    }
                } else if (!item.isIn(ItemTags.SHOVELS)) {
                    if (sharpSword) {
                        matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.5 * (double)swing);
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0f * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0f * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0f * swing));
                    } else if (item.isIn(ItemTags.SWORDS)) {
                        matrices.translate(-0.55 * (double)sideFactor * (double)swing_rot, -0.8 * (double)swing_rot, -0.77 * (double)swing);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5.0f * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0f * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(70.0f * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(50.0f * swing));
                    } else if (item.isIn(ConventionalItemTags.TOOLS) && !item.isIn(ItemTags.SHOVELS)) {
                        matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.5 * (double)swing);
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0f * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0f * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0f * swing));
                    } else {
                        matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.1 * (double)swing);
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0f * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0f * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0f * swing));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0f * swing * sideFactor));
                    }
                } else if (item.isIn(ItemTags.SHOVELS)) {
                    matrices.translate(0.0, 0.15 * (double)swing_rot, -0.25 * (double)swing_rot);
                    matrices.translate(0.0, 0.0, -0.2 * (double)swing);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0f * swing_rot));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0f * swing_rot));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f * swing));
                }
            } else if (!(Block.getBlockFromItem((Item)item.getItem()) == Blocks.AIR || item.isIn(ConventionalItemTags.TOOLS) && !item.isIn(ItemTags.TRIMMABLE_ARMOR) && !item.isIn(ItemTags.BOOKSHELF_BOOKS) && item.getUseAction() != UseAction.EAT && item.isEnchantable() || item.getUseAction() == UseAction.BOW || item.getUseAction() == UseAction.SPYGLASS || this.getAttackDamage(item) != 0.0f || item.getUseAction() == UseAction.BLOCK || item.isOf(Items.WARPED_FUNGUS_ON_A_STICK) || item.isOf(Items.CARROT_ON_A_STICK) || item.isOf(Items.FISHING_ROD) || item.isOf(Items.SHEARS))) {
                if ((swingProgress = (float)((double)swingProgress * 1.2)) > 1.0f) {
                    swingProgress = 0.0f;
                }
            } else if (!item.isIn(ItemTags.SHOVELS) && (swingProgress = (float)((double)swingProgress * 1.5)) > 1.0f) {
                swingProgress = 0.0f;
            }
            if (player.getVelocity().length() >= 0.08) {
                this.crawlCount = (float)((double)this.crawlCount + 0.1 * player.getVelocity().length() * 2.0 * tt);
                this.directionalCrawlCount = (float)((double)this.directionalCrawlCount + 0.1 * dotProduct * 4.0 * tt);
                this.directionalCrawlCount = (float)((double)this.directionalCrawlCount + (dotProduct > 0.0 ? 0.1 * Math.abs(crossProduct) * 4.0 * tt : 0.1 * Math.abs(crossProduct) * -1.0 * 4.0 * tt));
            }
            if (player.getVelocity().getY() > 0.0) {
                this.climbCount = (float)((double)this.climbCount + 0.1 * tt);
            }
            if (player.getVelocity().getY() < 0.0) {
                this.climbCount = (float)((double)this.climbCount - 0.1 * tt);
            }
            if ((player.isCrawling() && config.climbAndCrawl || player.isClimbing() && !player.isOnGround() && Math.abs(player.getVelocity().getY()) > 0.0 && config.climbAndCrawl) && !player.isUsingItem() && swingProgress == 0.0f) {
                this.clCount = (float)((double)this.clCount + 0.1 * tt);
                if (this.clCount > 1.0f) {
                    this.clCount = 1.0f;
                }
                if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0f * this.clCount));
                }
            } else {
                this.clCount = (float)((double)this.clCount * Math.pow(0.88f, tt));
            }
            if (swingProgress == 0.0f) {
                matrices.translate(bl ? player.getPitch() / 650.0f * this.clCount * -1.0f : player.getPitch() / 650.0f * this.clCount, 0.0f, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(player.getPitch() * this.clCount));
            }
            if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                matrices.translate(0.0f, 0.0f, player.getPitch() / 120.0f * this.clCount);
            } else if (swingProgress == 0.0f) {
                matrices.translate(0.0f, 0.0f, player.getPitch() / 80.0f * this.clCount);
            }
            if (player.isClimbing() && config.climbAndCrawl && !player.isOnGround() && !item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !player.isUsingItem()) {
                matrices.translate(0.0, 0.1, -0.2);
            }
            if ((player.isInFluid() || player.inPowderSnow) && !player.isSwimming() && !player.isSubmergedInWater()) {
                this.inWaterCounter = (float)((double)this.inWaterCounter + 0.1 * tt);
                if (this.inWaterCounter >= 1.0f) {
                    this.inWaterCounter = 1.0f;
                }
            } else {
                this.inWaterCounter = (float)((double)this.inWaterCounter * Math.pow(0.88f, tt));
            }
            this.freezeCounter = player.inPowderSnow && (double)player.getFreezingScale() > 0.1 ? (float)((double)this.freezeCounter + 0.1 * tt) : (float)((double)this.freezeCounter * Math.pow(0.88f, tt));
            matrices.translate(0.0, 0.02 * (double)this.inWaterCounter, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0f * sideFactor * this.inWaterCounter));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.3f * MathHelper.sin((float)(this.freezeCounter * 5.0f))));
            if (player.getVelocity().getY() < -0.85 && item.isOf(Items.MACE) && player.getMainHandStack() == item) {
                this.fallCounter = (float)((double)this.fallCounter + 0.1 * tt);
                if (this.fallCounter >= 1.0f) {
                    this.fallCounter = 1.0f;
                }
            } else {
                this.fallCounter = (float)((double)this.fallCounter * Math.pow(0.88f, tt));
            }
            if (bl) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0f * this.fallCounter));
                matrices.translate(0.0, -0.2 * (double)this.fallCounter, 0.0);
            }
            this.vertAngleY = (float)((double)this.vertAngleY + player.getVelocity().getY() * (double)0.015f * tt);
            this.vertAngleY = (float)((double)this.vertAngleY - (double)(0.1f * this.vertAngleY) * tt);
            this.vertAngleY = (float)((double)this.vertAngleY * Math.pow(0.88f, tt));
            this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime + player.getVelocity().getY() * (double)0.015f * tt);
            this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime - (double)(0.1f * this.vertAngleYSlime) * tt);
            this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime * Math.pow(0.88f, tt));
            this.vertAngleYSlime = (float)((double)this.vertAngleYSlime + (double)this.vertVelocityYSlime * tt);
            matrices.translate(0.0f, this.vertAngleY * -1.0f, 0.0f);
            matrices.translate(0.0, Math.sin((double)player.age * 0.1) * 0.007 * (double)sideFactor, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.15f * MathHelper.sin((float)((float)player.age * 0.15f)) * sideFactor));
            if (!item.isEmpty() || player.isCrawling() || player.isClimbing() && !player.isOnGround() || player.isSwimming()) {
                if (player.getMainArm() == Arm.LEFT) {
                    boolean bl5 = bl = !bl;
                }
                if (item.getUseAction() == UseAction.BLOCK) {
                    matrices.translate(0.0f, 0.0f, 0.0f);
                } else {
                    matrices.translate(0.0, -0.1, 0.1);
                }
            }
            if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN) || item.isIn(ItemTags.HANGING_SIGNS)) {
                matrices.translate(0.0, 0.1, 0.0);
                if (player.isSwimming()) {
                    matrices.translate(0.0, -0.1, 0.1);
                }
            }
            if (player.isSwimming() && swingProgress == 0.0f && config.swimmingAnimation) {
                double a2;
                double distance = this.crawlCount;
                double swingAmplitude = 1.5;
                double frequency = 2.0;
                double s2 = distance * frequency;
                double handRotation = Math.sin(s2) * swingAmplitude;
                double smoothRotation = handRotation * 0.8 + this.previousRotation * 0.2;
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(bl ? smoothRotation : -smoothRotation)));
                matrices.translate(0.0, 0.0, smoothRotation * (double)0.2f);
                double k2 = this.crawlCount * 2.0f;
                double b2 = a2 = Math.cos(k2);
                if (a2 <= 0.0) {
                    b2 = a2 * 0.5;
                }
                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(bl ? b2 * 30.0 : b2 * 30.0 * -1.0)));
                matrices.translate(0.0, 0.0, a2 * (double)0.2f);
                if (item.isEmpty() && !bl && !player.isInvisible()) {
                    matrices.translate((double)(1.0f * sideFactor), 0.0 - (double)equipProgress * 0.3, 0.3);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0f * sideFactor));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0f * sideFactor));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f));
                    this.altSwing(matrices, arm, swingProgress, item);
                    float c2 = MathHelper.sin((float)(equipProgress * 3.14f));
                    matrices.scale(0.9f, 0.9f, 0.9f);
                    HeldItemRendererAccessor acc2 = (HeldItemRendererAccessor)((Object)this);
                    acc2.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, arm);
                }
                this.previousRotation = smoothRotation;
            }
            if ((player.isClimbing() && !player.isOnGround() || player.isCrawling() && swingProgress == 0.0f) && !player.isUsingItem()) {
                double s3 = this.climbCount;
                float v2 = (float)player.getVelocity().getY();
                float a3 = MathHelper.cos((float)((float)s3 * 2.0f));
                if (player.isClimbing()) {
                    if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0f * a3 * sideFactor));
                    } else {
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(1.0f * a3 * sideFactor));
                    }
                }
                if (player.isCrawling() && !player.isUsingItem() && swingProgress == 0.0f) {
                    float crawlProgress = MathHelper.sin((float)(this.directionalCrawlCount * 4.0f * this.mouseHolding));
                    float upAndDown = MathHelper.cos((float)(this.directionalCrawlCount * 4.0f * this.mouseHolding));
                    if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN)) {
                        crawlProgress *= 0.14f;
                        upAndDown *= 0.14f;
                    }
                    matrices.translate(0.2 * (double)crawlProgress, 0.3 * (double)crawlProgress * (double)sideFactor, -0.2 * (double)crawlProgress * (double)sideFactor * (double)al2);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0f * crawlProgress));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp((float)(20.0f * upAndDown * sideFactor), (float)0.0f, (float)20.0f)));
                }
                if (item.isEmpty() && !bl && !player.isInvisible() && (!player.isOnGround() && player.isClimbing() || player.isCrawling())) {
                    matrices.translate((double)(1.0f * sideFactor), 0.0 - (double)equipProgress * 0.3, 0.3);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0f * sideFactor));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0f * sideFactor));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f));
                    this.altSwing(matrices, arm, swingProgress, item);
                    matrices.scale(0.9f, 0.9f, 0.9f);
                    HeldItemRendererAccessor acc3 = (HeldItemRendererAccessor)((Object)this);
                    acc3.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, arm);
                }
            }
            if (item.isEmpty()) {
                if (bl && !player.isInvisible()) {
                    if (!(!player.isOnGround() && player.isClimbing() || player.isSwimming() || player.isCrawling())) {
                        if (player.getMainArm() == Arm.LEFT) {
                            bl = !bl;
                        }
                        matrices.translate(0.0, 0.2 * (double)swing_rot, 0.15 * (double)swing_rot);
                        matrices.translate(0.1 * (double)sideFactor * (double)swing, 0.15 * (double)swing, -0.45 * (double)swing);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0f * swing * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0f * swing));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0f * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0f * swing_rot));
                        acc = (HeldItemRendererAccessor)((Object)this);
                        acc.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, arm);
                    } else {
                        matrices.translate((double)(1.0f * sideFactor), 0.0 - (double)equipProgress * 0.3, 0.3);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0f * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0f * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f));
                        this.altSwing(matrices, arm, swingProgress, item);
                        float c3 = MathHelper.sin((float)(equipProgress * 3.14f));
                        matrices.scale(0.9f, 0.9f, 0.9f);
                        HeldItemRendererAccessor acc4 = (HeldItemRendererAccessor)((Object)this);
                        acc4.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, arm);
                    }
                }
            } else if (item.contains(DataComponentTypes.MAP_ID)) {
                if (bl && this.mainHand.isEmpty()) {
                    matrices.translate(0.0, 0.1, 0.0);
                    acc = (HeldItemRendererAccessor)((Object)this);
                    acc.invokeRenderMapInBothHands(matrices, vertexConsumers, light, pitch, equipProgress, swingProgress);
                } else {
                    matrices.translate(bl ? -0.1 : 0.1, 0.1, 0.0);
                    acc = (HeldItemRendererAccessor)((Object)this);
                    acc.invokeRenderMapInOneHand(matrices, vertexConsumers, light, equipProgress, arm, swingProgress, item);
                }
            } else if (item.getUseAction() == UseAction.CROSSBOW) {
                int i2;
                matrices.push();
                boolean bl22 = CrossbowItem.isCharged((ItemStack)item);
                boolean bl3 = arm == Arm.RIGHT;
                int n2 = i2 = bl3 ? 1 : -1;
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    HeldItemRendererAccessor acc5 = (HeldItemRendererAccessor)((Object)this);
                    acc5.invokeApplyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate((float)i2 * -0.4785682f, -0.24387f, 0.05731531f);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935f));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i2 * 65.3f));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i2 * 9.785f));
                    float f2 = (float)item.getMaxUseTime((LivingEntity)player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0f);
                    float g2 = f2 / (float)CrossbowItem.getPullTime((ItemStack)item, (LivingEntity)player);
                    if (g2 > 1.0f) {
                        g2 = 1.0f;
                    }
                    if (g2 > 0.1f) {
                        float h2 = MathHelper.sin((float)((f2 - 0.1f) * 1.3f));
                        float j2 = g2 - 0.1f;
                        float k3 = h2 * j2;
                        matrices.translate(k3 * 0.0f, k3 * 0.004f, k3 * 0.0f);
                    }
                    matrices.translate(g2 * 0.0f, g2 * 0.0f, g2 * 0.04f);
                    matrices.scale(1.0f, 1.0f, 1.0f);
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)i2 * 45.0f));
                } else {
                    ((HeldItemRendererAccessor)((Object)this)).invokeSwingArm(swingProgress, equipProgress, matrices, i2, arm);
                    if (bl22 && swingProgress < 0.001f && bl) {
                        matrices.translate((float)i2 * -0.341864f, 0.0f, 0.0f);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i2 * 10.0f));
                    }
                }
                matrices.translate(0.0f, 0.0f, -1.0f);
                matrices.translate(-0.45 * (double)i2, 0.45, 1.7);
                matrices.translate((double)(1.0f * sideFactor), 0.0 - (double)equipProgress * 0.3, 0.3);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0f * sideFactor));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0f * sideFactor));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f));
                this.altSwing(matrices, arm, swingProgress, item);
                float c4 = MathHelper.sin((float)(equipProgress * 3.14f));
                matrices.scale(0.9f, 0.9f, 0.9f);
                HeldItemRendererAccessor acc6 = (HeldItemRendererAccessor)((Object)this);
                acc6.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, arm);
                matrices.translate(-0.25 * (double)i2, 1.25, 0.05);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-90 * i2)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(77.0f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(85 * i2)));
                matrices.scale(1.2f, 1.2f, 1.2f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0f));
                matrices.translate(0.0, -0.15, 0.15);
                acc6.invokeRenderItem((LivingEntity)player, item, bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
                matrices.pop();
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    float f3 = (float)item.getMaxUseTime((LivingEntity)player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0f);
                    float g3 = f3 / (float)CrossbowItem.getPullTime((ItemStack)item, (LivingEntity)player);
                    if (g3 > 1.0f) {
                        g3 = 1.0f;
                    }
                    if (g3 > 0.1f) {
                        float h3 = MathHelper.sin((float)((f3 - 0.1f) * 1.3f));
                        float j3 = g3 - 0.1f;
                        float k4 = h3 * j3;
                        matrices.translate(k4 * 0.0f, k4 * 0.004f, k4 * 0.0f);
                    }
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((double)g3 <= 0.2 ? 75.0f * g3 * 5.0f * (float)i2 : (float)(75 * i2)));
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(10.0f * g3 * 1.5f));
                    matrices.translate(-0.37 * (double)i2, 0.0, 0.6);
                    matrices.translate(0.15 * (double)g3 * (double)i2, 0.0, 0.0);
                    acc6.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                }
            } else {
                int l2;
                boolean bl23 = arm == Arm.RIGHT;
                int n3 = l2 = bl23 ? 1 : -1;
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    switch (item.getUseAction()) {
                        case NONE: {
                            HeldItemRendererAccessor acc7 = (HeldItemRendererAccessor)((Object)this);
                            acc7.invokeApplyEquipOffset(matrices, arm, equipProgress);
                            break;
                        }
                        case EAT: 
                        case DRINK: {
                            float u2 = (float)item.getMaxUseTime((LivingEntity)player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0f);
                            float y2 = u2 / 5.0f;
                            if (y2 > 1.0f) {
                                y2 = 1.0f;
                            }
                            float q2 = MathHelper.sin((float)(u2 / 2.0f * 3.14f));
                            matrices.translate((double)(1 * l2), 0.1, 0.3);
                            matrices.translate(0.2 * (double)l2 * (double)y2, -0.7 * (double)y2, -0.2 * (double)y2);
                            matrices.translate(0.0, -0.2 * (double)(q2 /= 10.0f), -0.2 * (double)q2);
                            matrices.translate(0.0, 0.1 * (double)this.easeInOutBack(MathHelper.sin((float)(y2 * 3.14f))), 0.0);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l2)));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l2)));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f));
                            this.altSwing(matrices, arm, swingProgress, item);
                            float c5 = MathHelper.sin((float)(equipProgress * 3.14f));
                            matrices.scale(0.9f, 0.9f, 0.9f);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0f * y2 * (float)l2));
                            HeldItemRendererAccessor acc4 = (HeldItemRendererAccessor)((Object)this);
                            acc4.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, swingProgress, arm);
                            break;
                        }
                        case BLOCK: {
                            float k5 = (float)item.getMaxUseTime((LivingEntity)player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0f);
                            float s4 = k5 / 4.0f;
                            float s2 = k5 / 6.0f;
                            if (s4 > 1.0f) {
                                s4 = 1.0f;
                            }
                            if (s2 > 1.0f) {
                                s2 = 1.0f;
                            }
                            matrices.translate(0.0, -0.2, 0.0);
                            matrices.translate((double)(1 * l2), 0.0, 0.3);
                            matrices.translate(0.7 * (double)s4 * (double)l2, 0.0, -1.3 * (double)s4);
                            matrices.translate(-0.2 * (double)l2 * (double)s2, 0.0, 0.0);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(10.0 * Math.sin((double)s2 * 3.14))));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0f * s4 * (float)l2));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l2)));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l2)));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(5 * l2) * s4));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0f * s4));
                            matrices.translate(0.0, 0.0, -0.2 * (double)s4);
                            this.altSwing(matrices, arm, swingProgress, item);
                            matrices.scale(0.9f, 0.9f, 0.9f);
                            HeldItemRendererAccessor acc5 = (HeldItemRendererAccessor)((Object)this);
                            acc5.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, swingProgress, arm);
                            matrices.translate(0.35 * (double)l2, -0.13, -0.12);
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(10.0f * (float)l2));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0f * (float)l2));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.0f));
                            matrices.translate(-0.2 * (double)l2, -0.04, 0.15);
                            matrices.scale(1.0f, 1.0f, 1.0f);
                            break;
                        }
                        case BOW: {
                            matrices.push();
                            if (player.getMainArm() == Arm.LEFT) {
                                bl = !bl;
                            }
                            float m1 = (float)item.getMaxUseTime((LivingEntity)player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0f);
                            float f1 = m1 / 20.0f;
                            float f4 = (f1 * f1 + f1 * 2.0f) / 3.0f;
                            if (f1 > 1.0f) {
                                f1 = 1.0f;
                            }
                            if (f1 > 0.1f) {
                                float g1 = MathHelper.sin((float)((m1 - 0.1f) * 1.3f));
                                float j1 = g1 * f1;
                                matrices.translate(j1 * 0.0f, j1 * 0.004f, j1 * 0.0f);
                            }
                            matrices.translate(bl ? -0.1 : 0.1, 0.0, (double)f1 * 0.15);
                            HeldItemRendererAccessor acc1 = (HeldItemRendererAccessor)((Object)this);
                            acc1.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                            matrices.pop();
                            matrices.translate(bl ? -0.5 : 0.5, -0.45, 0.1);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotation(0.3f));
                            if (bl) {
                                matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(-0.3f));
                                matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(1.0f));
                            } else {
                                matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-0.3f));
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(1.0f));
                            }
                            acc1.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                            if (bl) {
                                matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(2.5f));
                            } else {
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(2.5f));
                            }
                            matrices.translate(bl ? -0.65 : 0.65, -0.35, 0.27);
                            if (f1 > 1.0f) {
                                f1 = 1.0f;
                            }
                            matrices.pop();
                            if (config.mb3DCompat) {
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(10 * l2)));
                            }
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(75.0f));
                            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees((float)(-15 * l2)));
                            matrices.translate(0.8 * (double)l2, (double)(0.0f - equipProgress * 0.3f), -0.1);
                            if (f4 > 0.1f) {
                                float g1 = MathHelper.sin((float)((m1 - 0.1f) * 1.3f));
                                float h1 = f1 - 0.1f;
                                float j1 = g1 * h1;
                                matrices.translate(j1 * 0.0f, j1 * 0.004f, j1 * 0.0f);
                            }
                            matrices.push();
                            break;
                        }
                        case SPEAR: {
                            float m2;
                            float f5;
                            if (player.getOffHandStack().isEmpty() && !player.isCrawling() && !player.isSwimming() && !player.isClimbing()) {
                                matrices.push();
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-25 * l2)));
                                matrices.translate(-0.15 * (double)l2, 0.1, 0.1);
                                HeldItemRendererAccessor acc8 = (HeldItemRendererAccessor)((Object)this);
                                acc8.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                                matrices.pop();
                            }
                            if ((f5 = (m2 = (float)item.getMaxUseTime((LivingEntity)player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0f)) / 10.0f) > 1.0f) {
                                f5 = 1.0f;
                            }
                            if (f5 > 0.1f) {
                                float g4 = MathHelper.sin((float)((m2 - 0.1f) * 1.3f));
                                float h4 = f5 - 0.1f;
                                float j4 = g4 * h4;
                                matrices.translate(j4 * 0.0f, j4 * 0.004f, j4 * 0.0f);
                            }
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0f));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(25 * l2)));
                            matrices.translate(0.2 * (double)l2, 0.0, 0.8);
                            HeldItemRendererAccessor acc0 = (HeldItemRendererAccessor)((Object)this);
                            acc0.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0f));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-65 * l2)));
                            matrices.translate((double)(0.65f * (float)l2), -1.0, -0.6);
                            break;
                        }
                        case BRUSH: {
                            float f5 = player.getItemUseTimeLeft() % 10;
                            float g5 = f5 - tickDelta + 1.0f;
                            float h5 = 1.0f - g5 / 10.0f;
                            float n4 = -15.0f + 75.0f * MathHelper.cos((float)(h5 * 2.0f * (float)Math.PI));
                            float z2 = (float)item.getMaxUseTime((LivingEntity)player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0f);
                            float x2 = z2 / 4.0f;
                            if (x2 > 1.0f) {
                                x2 = 1.0f;
                            }
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(25 * l2) * x2));
                            matrices.translate((double)(0.3f * (float)l2 * x2), 0.3 * (double)x2, 0.1 * (double)x2);
                            if (x2 == 1.0f) {
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n4 / 20.0f));
                            }
                            HeldItemRendererAccessor acc78 = (HeldItemRendererAccessor)((Object)this);
                            acc78.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                            break;
                        }
                        case BUNDLE: {
                            matrices.translate((double)(1 * l2), 0.0 - (double)equipProgress * 0.3, 0.3);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l2)));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l2)));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f));
                            this.altSwing(matrices, arm, swingProgress, item);
                            matrices.scale(0.9f, 0.9f, 0.9f);
                            HeldItemRendererAccessor acc67 = (HeldItemRendererAccessor)((Object)this);
                            acc67.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, arm);
                        }
                    }
                } else if (player.isUsingRiptide() && item.getUseAction() == UseAction.SPEAR) {
                    this.riptideCounter = (float)((double)this.riptideCounter + 0.15 * tt);
                    float m3 = (float)item.getMaxUseTime((LivingEntity)player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0f);
                    float f6 = m3 / 10.0f;
                    if (f6 > 1.0f) {
                        f6 = 1.0f;
                    }
                    if (f6 > 0.1f) {
                        float g5 = MathHelper.sin((float)((m3 - 0.1f) * 1.3f));
                        float h5 = f6 - 0.1f;
                        float j5 = g5 * h5;
                        matrices.translate(j5 * 0.0f, j5 * 0.004f, j5 * 0.0f);
                    }
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0f - this.riptideCounter * 2.0f));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(25 * l2)));
                    matrices.translate(0.2 * (double)l2, 0.0, 0.75);
                    matrices.translate(0.0, 0.0, 0.01 * (double)MathHelper.sin((float)(this.riptideCounter * 6.28f)));
                    HeldItemRendererAccessor acc8 = (HeldItemRendererAccessor)((Object)this);
                    acc8.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0f));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-65 * l2)));
                    matrices.translate((double)(0.65f * (float)l2), -1.0, -0.6);
                } else {
                    this.riptideCounter = 0.0f;
                    if (!(item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN) || item.isIn(ItemTags.HANGING_SIGNS))) {
                        if (item.getUseAction() == UseAction.BLOCK) {
                            matrices.translate(0.0, -0.2, 0.0);
                        }
                    } else {
                        matrices.translate(0.1 * (double)l2, 0.0, -0.1);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0f));
                    }
                    matrices.translate((double)(1 * l2), 0.0 - (double)equipProgress * 0.3, 0.3);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l2)));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l2)));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f));
                    this.altSwing(matrices, arm, swingProgress, item);
                    matrices.scale(0.9f, 0.9f, 0.9f);
                    HeldItemRendererAccessor acc9 = (HeldItemRendererAccessor)((Object)this);
                    acc9.invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, arm);
                }
                matrices.translate(-0.3 * (double)l2, 0.65, -0.1);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-65 * l2)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0f));
                if (item.isIn(ItemTags.WOOL_CARPETS)) {
                    matrices.translate(0.2 * (double)l2, -0.1, 0.0);
                }
                if (Block.getBlockFromItem((Item)item.getItem()) != Blocks.AIR && item.getUseAction() != UseAction.EAT && !item.isIn(ConventionalItemTags.BUCKETS)) {
                    if (item.getName().toString().toLowerCase().contains("TORCH".toLowerCase())) {
                        matrices.scale(1.5f, 1.5f, 1.5f);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(25 * l2)));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0f));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l2)));
                        matrices.translate(0.2 * (double)l2, 0.2, 0.05);
                    } else if ((item.isOf(Items.STRING) || item.isOf(Items.REDSTONE) || item.isOf(Items.LEVER) || item.isOf(Items.TRIPWIRE_HOOK) || Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(ConventionalBlockTags.GLASS_PANES) || Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.RAILS) || Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.CLIMBABLE) || item.isIn(ItemTags.DOORS)) && !Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.LEAVES) && !Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS) && !Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.BANNERS)) {
                        matrices.translate(0.0, 0.0, -0.1);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(5 * l2)));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0f));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l2)));
                    } else if (!(item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN) || item.isIn(ItemTags.HANGING_SIGNS))) {
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(25 * l2)));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0f));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l2)));
                        matrices.translate(0.2 * (double)l2, 0.2, 0.05);
                        if (Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.BANNERS)) {
                            matrices.translate(-0.2 * (double)l2, 0.0, 0.0);
                            matrices.scale(1.1f, 1.1f, 1.1f);
                        }
                    } else {
                        float dt = (float)(polar.deltaTime * 30.0);
                        float yawDelta = player.prevHeadYaw - player.getHeadYaw();
                        float pitchDelta = player.prevPitch - player.getPitch();
                        this.swingVelocityY += yawDelta * 0.015f * dt;
                        this.swingVelocityY += swingProgress * 2.0f * dt;
                        this.swingVelocityX += pitchDelta * 0.015f * dt;
                        this.swingVelocityY -= 0.1f * this.swingAngleY * dt;
                        this.swingVelocityX -= 0.1f * this.swingAngleX * dt;
                        this.swingVelocityY = (float)((double)this.swingVelocityY * Math.pow(0.88f, dt));
                        this.swingVelocityX = (float)((double)this.swingVelocityX * Math.pow(0.88f, dt));
                        this.swingAngleY += this.swingVelocityY * dt;
                        this.swingAngleX += this.swingVelocityX * dt;
                        double currentSpeed = player.getVelocity().length();
                        this.swingVelocityZ = (float)((double)this.swingVelocityZ + (bl ? (currentSpeed * -1.0 * 15.0 - (double)this.swingVelocityZ) * (double)0.1f * (double)dt : (currentSpeed * 15.0 - (double)this.swingVelocityZ) * (double)0.1f * (double)dt));
                        if ((currentSpeed > 0.09 && player.isOnGround() || player.isSwimming() || player.isClimbing() && !player.isOnGround()) && ((Boolean)(Object)this.client.options.getBobView().getValue()).booleanValue()) {
                            Random random = new Random();
                            boolean randomBoolean = random.nextBoolean();
                            this.swingVelocityY += (float)(randomBoolean ? -5.5 * currentSpeed * (double)dt : 5.5 * currentSpeed * (double)dt);
                        }
                        matrices.translate(0.0, 0.0, -0.1);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(35 * l2) + this.swingAngleY));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0f + this.swingAngleX));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l2) + this.swingVelocityZ));
                        if (item.isIn(ItemTags.HANGING_SIGNS)) {
                            matrices.translate(0.0, -0.1, 0.0);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-45 * l2)));
                        }
                        matrices.translate(0.3 * (double)l2, -0.35, 0.0);
                        matrices.translate(0.0, 0.0, 0.1);
                        matrices.scale(1.5f, 1.5f, 1.5f);
                    }
                } else {
                    if (!(item.isIn(ConventionalItemTags.TOOLS) && !item.isIn(ItemTags.TRIMMABLE_ARMOR) && !item.isIn(ItemTags.BOOKSHELF_BOOKS) && item.getUseAction() != UseAction.EAT && item.isEnchantable() || item.getUseAction() == UseAction.BOW || item.getUseAction() == UseAction.SPYGLASS || this.getAttackDamage(item) != 0.0f || item.getUseAction() == UseAction.BLOCK || item.isOf(Items.WARPED_FUNGUS_ON_A_STICK) || item.isOf(Items.CARROT_ON_A_STICK) || item.isOf(Items.FISHING_ROD) || item.isOf(Items.SHEARS) || item.isIn(ItemTags.HOES) || config.mb3DCompat)) {
                        if (item.getUseAction() == UseAction.BRUSH) {
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(25.0f));
                            matrices.translate(bl ? 0.0 : 0.35, bl ? 0.0 : 0.25, bl ? 0.0 : 0.37);
                            if (!bl) {
                                matrices.scale(0.75f, 0.75f, 0.75f);
                            }
                            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees((float)(-75 * l2)));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(35.0f));
                            matrices.translate(bl ? -0.05 : 0.85, bl ? 0.0 : 0.05, bl ? 0.08 : -0.2);
                        } else {
                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(5 * l2)));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0f));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l2)));
                            matrices.translate(0.0, -0.05, -0.1);
                            matrices.scale(0.7f, 0.7f, 0.7f);
                        }
                        if (item.isOf(Items.FEATHER) || item.isOf(Items.SLIME_BALL) || item.isOf(Items.PUFFERFISH)) {
                            this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime + (double)swingProgress * 0.03 * polar.deltaTime * 30.0);
                            if ((player.getVelocity().length() > 0.09 && player.isOnGround() || player.isSwimming() || player.isCrawling() || player.isClimbing() && !player.isOnGround()) && ((Boolean)(Object)this.client.options.getBobView().getValue()).booleanValue()) {
                                Random random = new Random();
                                boolean randomBoolean = random.nextBoolean();
                                this.vertVelocityYSlime += (float)(-0.05 * player.getVelocity().length() * polar.deltaTime * 30.0);
                            }
                            matrices.scale(1.0f, 1.0f + this.vertAngleYSlime * -2.0f, 1.0f);
                        }
                    } else if (item.getUseAction() == UseAction.BLOCK && item.getUseAction() != UseAction.SPEAR) {
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(160 * l2)));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-60 * l2)));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0f));
                        matrices.scale(0.75f, 0.75f, 0.75f);
                        matrices.translate(0.15 * (double)l2, bl ? 0.35 : 0.45, bl ? -0.15 : -0.1);
                        matrices.translate(0.17 * (double)l2, 0.0, 0.3);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-90 * l2)));
                    } else if (item.getUseAction() == UseAction.SPEAR) {
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(75 * l2)));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(45 * l2)));
                        matrices.translate(-0.3f * (float)l2, 0.0f, 0.0f);
                    } else if (item.getUseAction() != UseAction.SPEAR) {
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(75 * l2)));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0f));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(45 * l2)));
                    }
                    if (item.getUseAction() != UseAction.BLOCK) {
                        matrices.scale(1.2f, 1.2f, 1.2f);
                    }
                    if (item.getUseAction() == UseAction.BOW && !player.isUsingItem()) {
                        matrices.translate(-0.1 * (double)l2, -0.2, 0.0);
                    }
                    if (item.isOf(Items.MACE)) {
                        if (config.mb3DCompat) {
                            matrices.translate(-0.08, 0.17, 0.0);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0f));
                        }
                        matrices.translate(0.1 * (double)l2, 0.0, 0.0);
                        matrices.scale(0.9f, 0.9f, 0.9f);
                    }
                }
                if (!(!(item.getItem() instanceof BlockItem) || (item.isIn(ConventionalItemTags.BUCKETS) || item.getUseAction() == UseAction.EAT || item.isIn(ItemTags.BANNERS) || item.isOf(Items.STRING) || item.isOf(Items.REDSTONE) || item.isOf(Items.LEVER) || item.isOf(Items.TRIPWIRE_HOOK) || Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(ConventionalBlockTags.GLASS_PANES) || Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.RAILS) || Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.CLIMBABLE) || item.isIn(ItemTags.DOORS)) && !Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.LEAVES) || Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS))) {
                    BlockItem blockItem = (BlockItem)item.getItem();
                    BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
                    blockRenderManager.getModel(blockItem.getBlock().getDefaultState());
                    matrices.push();
                    if (!bl23) {
                        matrices.translate(-0.4f, 0.0f, 0.0f);
                    }
                    matrices.scale(0.4f, 0.4f, 0.4f);
                    matrices.translate(-0.9 * (double)l2, -0.45, -0.5);
                    if (Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.BUTTONS)) {
                        matrices.translate(0.2 * (double)l2, -0.15, -0.2);
                    }
                    if (Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.PRESSURE_PLATES)) {
                        matrices.translate(0.0, 0.1, 0.0);
                    }
                    if (item.isOf(Items.SLIME_BLOCK) || item.isOf(Items.HONEY_BLOCK) || Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.FLOWERS) || Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.LEAVES) || Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.SAPLINGS) || Block.getBlockFromItem((Item)item.getItem()).getDefaultState().isIn(BlockTags.SWORD_EFFICIENT)) {
                        this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime + (double)swingProgress * 0.03 * polar.deltaTime * 30.0);
                        if ((player.getVelocity().length() > 0.09 && player.isOnGround() || player.isSwimming() || player.isCrawling() || player.isClimbing() && !player.isOnGround()) && ((Boolean)(Object)this.client.options.getBobView().getValue()).booleanValue()) {
                            Random random = new Random();
                            boolean randomBoolean = random.nextBoolean();
                            this.vertVelocityYSlime += (float)(-0.05 * player.getVelocity().length() * polar.deltaTime * 30.0);
                        }
                        matrices.scale(1.0f, 1.0f + this.vertAngleYSlime * -2.0f, 1.0f);
                    }
                    BlockState blockState = blockItem.getBlock().getDefaultState();
                    if ((float)player.age - this.prevAge >= 100.0f) {
                        this.repPower = !this.repPower;
                        this.prevAge = player.age;
                    }
                    if (blockItem.getBlock() == Blocks.REPEATER && this.repPower) {
                        blockState = (BlockState)blockState.with((Property)RepeaterBlock.POWERED, (Comparable)Boolean.valueOf(true));
                    }
                    if (blockItem.getBlock() == Blocks.COMPARATOR && this.repPower) {
                        blockState = (BlockState)blockState.with((Property)ComparatorBlock.POWERED, (Comparable)Boolean.valueOf(true));
                    }
                    if (blockItem.getBlock() == Blocks.REDSTONE_TORCH && player.isSubmergedInWater()) {
                        blockState = (BlockState)blockState.with((Property)RedstoneTorchBlock.LIT, (Comparable)Boolean.valueOf(false));
                    }
                    if ((blockItem.getBlock() == Blocks.CAMPFIRE || blockItem.getBlock() == Blocks.SOUL_CAMPFIRE) && player.isSubmergedInWater()) {
                        blockState = (BlockState)blockState.with((Property)CampfireBlock.LIT, (Comparable)Boolean.valueOf(false));
                    }
                    if (item.isIn(ItemTags.BEDS)) {
                        if (bl) {
                            matrices.translate(0.9, 0.0, 0.8);
                        }
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(90 * l2)));
                    }
                    blockRenderManager.renderBlockAsEntity(blockState, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
                    matrices.pop();
                } else {
                    if (item.isIn(ConventionalItemTags.TOOLS) && !item.isIn(ItemTags.TRIMMABLE_ARMOR) && !item.isIn(ItemTags.BOOKSHELF_BOOKS) && item.getUseAction() != UseAction.EAT && item.isEnchantable() || item.getUseAction() == UseAction.BOW || item.getUseAction() == UseAction.SPYGLASS || this.getAttackDamage(item) != 0.0f || item.getUseAction() == UseAction.BLOCK || item.isOf(Items.WARPED_FUNGUS_ON_A_STICK) || item.isOf(Items.CARROT_ON_A_STICK) || item.isOf(Items.FISHING_ROD) || item.isOf(Items.SHEARS)) {
                        if (item.isIn(ItemTags.SWORDS) && !sharpSword) {
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0f * swing));
                            matrices.translate(0.0, 0.1 * (double)swing, -0.1 * (double)swing);
                        }
                        if (item.isIn(ItemTags.SHOVELS)) {
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0f * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f * swing));
                        } else if (item.getUseAction() == UseAction.SPEAR) {
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0f * swing_rot));
                            matrices.translate(0.0, 0.1 * (double)swing_rot, -0.1 * (double)swing_rot);
                        } else if (item.getUseAction() != UseAction.BLOCK) {
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0f * swing));
                            matrices.translate(0.0, 0.05 * (double)swing, -0.05 * (double)swing);
                        }
                    }
                    if (!(item.isOf(Items.NETHER_STAR) || item.isOf(Items.END_CRYSTAL) && config.mb3DCompat)) {
                        this.netherCounter = 0.0f;
                    } else {
                        this.netherCounter = (float)((double)this.netherCounter + 0.9 * tt);
                        matrices.translate(0.0, 0.25 + 0.02 * (double)MathHelper.sin((float)(this.netherCounter * 0.1f)), 0.0);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(3.0f * MathHelper.sin((float)(this.netherCounter * 0.2f))));
                        matrices.scale(1.0f + 0.01f * MathHelper.sin((float)this.netherCounter), 1.0f + 0.01f * MathHelper.sin((float)this.netherCounter), 1.0f + 0.01f * MathHelper.sin((float)this.netherCounter));
                    }
                    if (config.mb3DCompat) {
                        if (item.isIn(ItemTags.SWORDS)) {
                            matrices.translate(0.0, 0.2, 0.0);
                        }
                        if (item.isOf(Items.FEATHER) || item.isOf(Items.SLIME_BALL) || item.isOf(Items.PUFFERFISH)) {
                            this.vertVelocityYSlime = (float)((double)this.vertVelocityYSlime + (double)swingProgress * 0.03 * polar.deltaTime * 30.0);
                            if ((player.getVelocity().length() > 0.09 && player.isOnGround() || player.isSwimming() || player.isCrawling() || player.isClimbing() && !player.isOnGround()) && ((Boolean)(Object)this.client.options.getBobView().getValue()).booleanValue()) {
                                Random random = new Random();
                                boolean randomBoolean = random.nextBoolean();
                                this.vertVelocityYSlime += (float)(-0.05 * player.getVelocity().length() * polar.deltaTime * 30.0);
                            }
                            matrices.scale(1.0f, 1.0f + this.vertAngleYSlime * -2.0f, 1.0f);
                        }
                    }
                    if (item.isIn(ItemTags.SHOVELS)) {
                        matrices.translate(0.07 * (double)l2, 0.0, 0.05);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(90 * l2)));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0f));
                    }
                    if (item.isOf(Items.TORCH)) {
                        player.getWorld().addParticle((ParticleEffect)ParticleTypes.ITEM_SLIME, player.getPos().getX(), player.getPos().getY(), player.getPos().getZ(), 0.1, 0.1, 0.1);
                    }
                    HeldItemRendererAccessor acc10 = (HeldItemRendererAccessor)((Object)this);
                    acc10.invokeRenderItem((LivingEntity)player, item, bl23 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl23, matrices, vertexConsumers, light);
                }
            }
            matrices.pop();
            matrices.pop();
            this.isAttacking = this.client.options.attackKey.isPressed();
        }
    }

    @Shadow
    protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity var1, float var2, float var3, Hand var4, float var5, ItemStack var6, float var7, MatrixStack var8, VertexConsumerProvider var9, int var10);

    @Shadow
    protected abstract void swingArm(float var1, float var2, MatrixStack var3, int var4, Arm var5);

    @Shadow
    private static HeldItemRenderer.HandRenderType getHandRenderType(ClientPlayerEntity player) {
        throw new AssertionError();
    }
}

