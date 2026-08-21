package zov.alphadlc.module.list.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.item.ModelTransformationMode;

public class HMIRenderer {

    private float getEffectivePitch(AbstractClientPlayerEntity player) {
        float pitch = player.getPitch();
        if (player.isSwimming() || player.isCrawling() || player.isClimbing()) return pitch;

        boolean nearGround = player.isOnGround();
        if (!nearGround) {
            double feetY = player.getY();
            for (int i = 0; i <= 2; i++) {
                BlockPos checkPos = BlockPos.ofFloored(player.getX(), feetY - i, player.getZ());
                if (player.getWorld().getBlockState(checkPos).isSolid()) {
                    double dist = feetY - checkPos.getY() - 1.0;
                    if (dist <= 1.5) { nearGround = true; }
                    break;
                }
            }
        }
        if (!nearGround) return pitch;

        for (Entity entity : player.getWorld().getOtherEntities(player, player.getBoundingBox().expand(6.0))) {
            double yDiff = entity.getY() - player.getY();
            if (yDiff < -1.9 || yDiff > 2.5) return pitch;
        }

        return MathHelper.clamp(pitch, 35.0F, 48.0F);
    }

    private static double prevTime = 0.0;
    public static double deltaTime = 0.0;

    private boolean repPower = false;
    private float prevAge = 0.0F;
    private double previousRotation = 0.0;
    private float swingAngleY = 0.0F;
    private float swingAngleX = 0.0F;
    private float swingVelocityY = 0.0F;
    private float swingVelocityX = 0.0F;
    private float swingVelocityZ = 0.0F;
    private float vertAngleY = 0.0F;
    private float vertVelocityYSlime = 0.0F;
    private float vertAngleYSlime = 0.0F;
    private float riptideCounter = 0.0F;
    private float netherCounter = 0.0F;
    private float fallCounter = 0.0F;
    private float inWaterCounter = 0.0F;
    private float freezeCounter = 0.0F;
    private float clCount = 0.0F;
    private float crawlCount = 0.0F;
    private float directionalCrawlCount = 0.0F;
    private float climbCount = 0.0F;
    private float mouseHolding = 1.0F;
    private boolean isAttacking = false;
    private boolean left = false;
    // Per-hand return animation state (0=main, 1=off)
    private final boolean[] wasSwinging = {false, false};
    private final float[] swingReturnProgress = {0.0F, 0.0F};
    private final float[] lastSwing = {0.0F, 0.0F};
    private final float[] lastSwingRot = {0.0F, 0.0F};

    private SwingAnimations swingAnimations;

    public void setSwingAnimations(SwingAnimations sa) {
        this.swingAnimations = sa;
    }
    private static final boolean ENABLE_SWIMMING_ANIM = true;
    private static final boolean ENABLE_CLIMB_AND_CRAWL = true;
    private static final boolean ENABLE_PUNCHING = true;
    private static final boolean MB3D_COMPAT = false;

    public void updateDeltaTime() {
        long currentTime = System.nanoTime();
        double currentSec = currentTime / 1_000_000_000.0;
        deltaTime = currentSec - prevTime;
        prevTime = currentSec;
        if (MinecraftClient.getInstance().isPaused()) {
            deltaTime = 0.0;
        } else {
            deltaTime = Math.min(0.05, deltaTime);
        }
    }

    private float easeInOutBack(float x) {
        float c1 = 1.70158F;
        float c2 = c1 * 1.525F;
        return (float) ((double) x < 0.5
                ? Math.pow(2.0 * x, 2.0) * ((c2 + 1.0) * 2.0 * x - c2) / 2.0
                : (Math.pow(2.0 * x - 2.0, 2.0) * ((c2 + 1.0) * (x * 2.0F - 2.0F) + c2) + 2.0) / 2.0);
    }

    private float getAttackDamage(ItemStack stack) {
        float totalDamage = 0.0F;
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return totalDamage;
        for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(EntityAttributes.ATTACK_DAMAGE)) {
                totalDamage += (float) entry.modifier().value();
            }
        }
        return totalDamage;
    }

    private void altSwing(MatrixStack matrices, Arm arm, float swingProgress) {
        int direction = arm == Arm.RIGHT ? 1 : -1;
        float swingSin = MathHelper.sin(swingProgress * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * (45.0F + swingSin * 0.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -45.0F));
    }

    private boolean isSword(ItemStack stack) {
        if (stack.isIn(ItemTags.SWORDS)) return true;
        if (stack.getItem() instanceof SwordItem) return true;
        return false;
    }

    private boolean isTool(ItemStack stack) {
        return stack.isIn(ItemTags.AXES) || stack.isIn(ItemTags.HOES) || stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.SHOVELS) || isSword(stack);
    }

    private boolean isSmallBlockItem(ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        BlockState state = block.getDefaultState();
        return stack.isOf(Items.STRING) || stack.isOf(Items.REDSTONE) || stack.isOf(Items.LEVER)
                || stack.isOf(Items.TRIPWIRE_HOOK)
                || state.isIn(net.minecraft.registry.tag.BlockTags.IMPERMEABLE)
                || state.isIn(BlockTags.RAILS)
                || state.isIn(BlockTags.CLIMBABLE)
                || stack.isIn(ItemTags.DOORS);
    }

    public void renderHMI(HeldItemRenderer renderer, HeldItemRendererAccessor acc, AbstractClientPlayerEntity player,
                          float tickDelta, float pitch, Hand hand, float swingProgress,
                          ItemStack stack, float equipProgress, MatrixStack matrices,
                          VertexConsumerProvider vertexConsumers, int light) {

        updateDeltaTime();

        // Для лука и трезубца используем упрощенную анимацию (без HMI физики)
        boolean useVanillaLike = stack.getUseAction() == UseAction.BOW || stack.getUseAction() == UseAction.SPEAR;
        
        if (useVanillaLike) {
            // Простой рендер без HMI эффектов
            boolean bl = hand == Hand.MAIN_HAND;
            Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
            float kj = bl ? 1.0F : -1.0F;
            int l = arm == Arm.RIGHT ? 1 : -1;
            
            matrices.push();
            
            // Применяем view model offsets если есть
            if (swingAnimations != null) {
                double vx = swingAnimations.hmiPosX.getValue();
                double vy = swingAnimations.hmiPosY.getValue();
                double vz = swingAnimations.hmiPosZ.getValue();
                matrices.translate(vx * kj, vy, vz);
            }
            
            // Если игрок использует предмет (зажимает ПКМ)
            if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                UseAction useAction = stack.getUseAction();
                
                if (useAction == UseAction.BOW) {
                    // Анимация натягивания лука
                    matrices.push();
                    float m1 = stack.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float f1 = m1 / 20.0F;
                    if (f1 > 1.0F) f1 = 1.0F;
                    if (f1 > 0.1F) {
                        float g1 = MathHelper.sin((m1 - 0.1F) * 1.3F);
                        float j1 = g1 * f1;
                        matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                    }
                    
                    acc.invokeApplyItemArmTransform(matrices, arm, equipProgress);
                    matrices.translate(l * -0.2785682F, 0.18312F, 0.15731531F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(l * 35.3F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(l * -9.785F));
                    
                    float h = stack.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float g = h / 20.0F;
                    g = (g * g + g * 2.0F) / 3.0F;
                    if (g > 1.0F) g = 1.0F;
                    
                    if (g > 0.1F) {
                        float i = MathHelper.sin((h - 0.1F) * 1.3F);
                        float j = g - 0.1F;
                        float k = i * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                    }
                    
                    matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                    matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(l * 45.0F));
                    
                    acc.invokeRenderItem(player, stack, arm == Arm.RIGHT ? net.minecraft.item.ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : net.minecraft.item.ModelTransformationMode.FIRST_PERSON_LEFT_HAND, arm != Arm.RIGHT, matrices, vertexConsumers, light);
                    matrices.pop();
                    
                } else if (useAction == UseAction.SPEAR) {
                    // Анимация замаха трезубцем
                    acc.invokeApplyItemArmTransform(matrices, arm, equipProgress);
                    
                    float dt = stack.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float f = dt / 10.0F;
                    if (f > 1.0F) f = 1.0F;
                    if (f > 0.1F) {
                        float g = MathHelper.sin((dt - 0.1F) * 1.3F);
                        float j = g * (f - 0.1F);
                        matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                    }
                    
                    matrices.translate(0.0F, 0.0F, 0.1F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
                    matrices.translate(0.0F, 0.0F, -0.1F);
                    
                    acc.invokeRenderItem(player, stack, arm == Arm.RIGHT ? net.minecraft.item.ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : net.minecraft.item.ModelTransformationMode.FIRST_PERSON_LEFT_HAND, arm != Arm.RIGHT, matrices, vertexConsumers, light);
                }
            } else {
                // Обычный рендер без использования
                acc.invokeSwingArm(swingProgress, equipProgress, matrices, bl ? 1 : -1, arm);
                acc.invokeRenderItem(player, stack, bl ? net.minecraft.item.ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : net.minecraft.item.ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl, matrices, vertexConsumers, light);
            }
            
            matrices.pop();
            return;
        }

        float yaw = player.getYaw();
        double radians = Math.toRadians(yaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        Vec3d vel = player.getVelocity();
        double dotProduct = vel.x * forwardX + vel.z * forwardZ;
        double crossProduct = vel.x * forwardZ - vel.z * forwardX;

        float effectivePitch = getEffectivePitch(player);
        float al = effectivePitch != 0.0F ? 90.0F / effectivePitch / 10.0F : 1.0F;
        if (al > 1.0F) al = 1.0F;
        if (al < 0.0F) al = 1.0F;

        boolean bl = hand == Hand.MAIN_HAND;
        Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
        float kj = bl ? 1.0F : -1.0F;

        // Apply view model offsets from SwingAnimations sliders
        double vx = 0.0, vy = 0.0, vz = 0.0;
        double rx = 0.0, ry = 0.0, rz = 0.0;
        double scale = 1.0;
        double animSpeed = 8.0;
        if (swingAnimations != null) {
            vx = swingAnimations.hmiPosX.getValue();
            vy = swingAnimations.hmiPosY.getValue();
            vz = swingAnimations.hmiPosZ.getValue();
            rx = swingAnimations.hmiRotX.getValue();
            ry = swingAnimations.hmiRotY.getValue();
            rz = swingAnimations.hmiRotZ.getValue();
            scale = swingAnimations.hmiScale.getValue();
            animSpeed = 8.0;
        }

        matrices.push();
        matrices.push();
        matrices.translate(vx * kj, vy, vz);
        // Apply view model rotation offsets
        if (rx != 0.0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rx));
        if (ry != 0.0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) ry * kj));
        if (rz != 0.0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rz * kj));
        // Apply view model scale
        if (scale != 1.0) matrices.scale((float) scale, (float) scale, (float) scale);

        double tt = deltaTime * animSpeed;

        // Get speed multiplier from SwingAnimations
        float speedMultiplier = 1.0f;
        if (swingAnimations != null) {
            // Speed slider is from 0-10, default 7. Convert to multiplier (0.5x to 2.5x)
            speedMultiplier = (float) (0.5 + (swingAnimations.speed.getValue() / 10.0) * 2.0);
        }

        // === SWING SPEED MODIFICATION (before all animations) ===
        if (Block.getBlockFromItem(stack.getItem()) != Blocks.AIR
                && (!isTool(stack) || stack.isIn(ItemTags.TRIMMABLE_ARMOR) || stack.isIn(ItemTags.BOOKSHELF_BOOKS)
                || stack.getUseAction() == UseAction.EAT || !stack.isEnchantable())
                && stack.getUseAction() != UseAction.BOW && stack.getUseAction() != UseAction.SPYGLASS
                && getAttackDamage(stack) == 0.0F && stack.getUseAction() != UseAction.BLOCK
                && !stack.isOf(Items.WARPED_FUNGUS_ON_A_STICK) && !stack.isOf(Items.CARROT_ON_A_STICK)
                && !(stack.getItem() instanceof FishingRodItem) && !stack.isOf(Items.SHEARS)) {
            swingProgress = (float) ((double) swingProgress * 0.45 * speedMultiplier);
            if (swingProgress > 1.0F) swingProgress = 0.0F;
        } else if (!stack.isIn(ItemTags.SHOVELS)) {
            swingProgress = (float) ((double) swingProgress * 0.45 * speedMultiplier);
            if (swingProgress > 1.0F) swingProgress = 0.0F;
        }

        float raw_swing_rot = (double) swingProgress < 0.6
                ? MathHelper.sin(MathHelper.clamp(swingProgress, 0.0F, 0.12506F) * 12.56F)
                : MathHelper.sin(MathHelper.clamp(swingProgress, 0.62532F, 0.75038F) * 12.56F);
        float raw_swing = MathHelper.sin(swingProgress * 3.14F);
        raw_swing = easeInOutBack(raw_swing);

        float swing_rot = raw_swing_rot;
        float swing = raw_swing;

        // === SWING RETURN ANIMATION (mirror recovery, per-hand) ===
        int handIdx = bl ? 0 : 1;
        if (swingProgress > 0.001F) {
            wasSwinging[handIdx] = true;
            lastSwing[handIdx] = swing;
            lastSwingRot[handIdx] = swing_rot;
            swingReturnProgress[handIdx] = 1.0F;
        } else if (wasSwinging[handIdx] && swingReturnProgress[handIdx] > 0.0F) {
            swingReturnProgress[handIdx] -= (float)(deltaTime * 5.5);
            if (swingReturnProgress[handIdx] < 0.0F) swingReturnProgress[handIdx] = 0.0F;
            float t = swingReturnProgress[handIdx] * swingReturnProgress[handIdx];
            swing = lastSwing[handIdx] * t;
            swing_rot = lastSwingRot[handIdx] * t;
        } else {
            wasSwinging[handIdx] = false;
            swingReturnProgress[handIdx] = 0.0F;
        }

        // === THROWABLE ITEMS (offhand arm visible) ===
        if ((stack.isOf(Items.EXPERIENCE_BOTTLE) || stack.isOf(Items.EGG) || stack.isOf(Items.ENDER_EYE)
                || stack.isOf(Items.SNOWBALL) || stack.isOf(Items.ENDER_PEARL)
                || stack.getItem() instanceof SplashPotionItem || stack.getItem() instanceof LingeringPotionItem)
                && player.getOffHandStack().isEmpty()
                && stack.getUseAction() != UseAction.SPEAR
                && !stack.isOf(Items.FIRE_CHARGE)
                && !player.isSwimming() && !player.isCrawling() && !player.isClimbing()) {
            if (player.getMainArm() == Arm.LEFT) bl = !bl;
            float ll = bl ? 1.0F : -1.0F;
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25.0F * ll));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * ll * swing));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
            matrices.translate(-0.15 * ll, 0.1, 0.1);
            matrices.translate(0.0F, -0.55 * swing, 0.4 * swing * 3.14F);
            acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, equipProgress, 0.0F, arm.getOpposite());
            matrices.pop();
        }

        if (wasSwinging[handIdx] && swingReturnProgress[handIdx] <= 0.0F) {
            left = !left;
        }

        // === SWING ANIMATIONS PER ITEM TYPE ===
        if (!stack.isEmpty()) {
            if (player.getMainArm() == Arm.LEFT) bl = !bl;
            float ll = bl ? 1.0F : -1.0F;

            if ((left || stack.isIn(ItemTags.AXES) || stack.getUseAction() == UseAction.SPEAR || stack.getUseAction() == UseAction.BLOCK) && !stack.isIn(ItemTags.SHOVELS)) {
                if (!isSword(stack) && !stack.isIn(ItemTags.AXES)) {
                    if (stack.getUseAction() == UseAction.SPEAR) {
                        matrices.translate(0.0F, 0.0F, 0.45 * swing_rot);
                        matrices.translate(-0.25F * kj * swing, -0.35 * swing_rot, -0.6 * swing);
                        matrices.translate(0.0F, 0.1 * swing, 0.0F);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * ll));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0F * swing_rot * ll));
                    } else if (isTool(stack) && stack.getUseAction() != UseAction.BLOCK && !stack.isIn(ItemTags.SHOVELS)) {
                        matrices.translate(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.5F * swing);
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * ll));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                    } else if (stack.getUseAction() != UseAction.BLOCK) {
                        matrices.translate(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.1 * swing);
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * ll));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * ll));
                    } else {
                        matrices.translate(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.2 * swing);
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-10.0F * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * ll));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(20.0F * swing));
                    }
                } else {
                    matrices.translate(0.8 * ll * swing_rot, 0.3 * swing_rot, -0.5F * swing);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * ll));
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-20.0F * swing_rot));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F * swing_rot * ll));
                    if (isSword(stack)) {
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                    } else {
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(30.0F * swing));
                    }
                }
            } else if (!stack.isIn(ItemTags.SHOVELS)) {
                if (isSword(stack)) {
                    matrices.translate(-0.55 * ll * swing_rot, -0.8 * swing_rot, -0.77 * swing);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5.0F * swing_rot * ll));
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(70.0F * swing_rot * ll));
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(50.0F * swing));
                } else if (isTool(stack) && !stack.isIn(ItemTags.SHOVELS)) {
                    matrices.translate(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.5F * swing);
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * ll));
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                } else {
                    matrices.translate(0.1 * ll * swing_rot, 0.1 * swing_rot, -0.1 * swing);
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * ll));
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * ll));
                }
            } else if (stack.isIn(ItemTags.SHOVELS)) {
                matrices.translate(0.0F, 0.15 * swing_rot, -0.25F * swing_rot);
                matrices.translate(0.0F, 0.0F, -0.2 * swing);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F * swing_rot));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
            }
        }

        // === MOVEMENT PHYSICS ===
        if (vel.length() >= 0.08) {
            crawlCount += 0.1 * vel.length() * 2.0F * tt;
            directionalCrawlCount += 0.1 * dotProduct * 4.0F * tt;
            directionalCrawlCount += (dotProduct > 0.0 ? 0.1 * Math.abs(crossProduct) * 4.0F * tt : 0.1 * Math.abs(crossProduct) * -1.0F * 4.0F * tt);
        }
        if (vel.y > 0.0) climbCount += 0.1 * tt;
        if (vel.y < 0.0) climbCount -= 0.1 * tt;

        // Climb/crawl
        if ((player.isCrawling() && ENABLE_CLIMB_AND_CRAWL
                || player.isClimbing() && !player.isOnGround() && Math.abs(vel.y) > 0.0 && ENABLE_CLIMB_AND_CRAWL)
                && !player.isUsingItem() && swingProgress == 0.0F) {
            clCount += 0.1 * tt;
            if (clCount > 1.0F) clCount = 1.0F;
            if (!stack.isOf(Items.LANTERN) && !stack.isOf(Items.SOUL_LANTERN)) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0F * clCount));
            }
        } else {
            clCount *= Math.pow(0.88F, tt);
        }

        if (swingProgress == 0.0F) {
            matrices.translate(bl ? effectivePitch / 650.0F * clCount * -1.0F : effectivePitch / 650.0F * clCount, 0.0F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(effectivePitch * clCount));
        }
        if (!stack.isOf(Items.LANTERN) && !stack.isOf(Items.SOUL_LANTERN)) {
            matrices.translate(0.0F, 0.0F, effectivePitch / 120.0F * clCount);
        } else if (swingProgress == 0.0F) {
            matrices.translate(0.0F, 0.0F, effectivePitch / 80.0F * clCount);
        }
        if (player.isClimbing() && ENABLE_CLIMB_AND_CRAWL && !player.isOnGround()
                && !stack.isOf(Items.LANTERN) && !stack.isOf(Items.SOUL_LANTERN) && !player.isUsingItem()) {
            matrices.translate(0.0F, 0.1, -0.2);
        }

        // Water
        if ((player.isTouchingWater() || player.isFrozen()) && !player.isSwimming() && !player.isSubmergedInWater()) {
            inWaterCounter += 0.1 * tt;
            if (inWaterCounter >= 1.0F) inWaterCounter = 1.0F;
        } else {
            inWaterCounter *= Math.pow(0.88F, tt);
        }

        // Freeze
        float freezingScale = MathHelper.clamp((float) player.getFrozenTicks() / (float) player.getMinFreezeDamageTicks(), 0.0F, 1.0F);
        if (player.isFrozen() && freezingScale > 0.1) {
            freezeCounter += 0.1 * tt;
        } else {
            freezeCounter *= Math.pow(0.88F, tt);
        }

        matrices.translate(0.0F, 0.02 * inWaterCounter, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0F * kj * inWaterCounter));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.3F * MathHelper.sin(freezeCounter * 5.0F)));

        // Fall (Mace)
        if (vel.y < -0.85 && stack.isOf(Items.MACE) && player.getMainHandStack() == stack) {
            fallCounter += 0.1 * tt;
            if (fallCounter >= 1.0F) fallCounter = 1.0F;
        } else {
            fallCounter *= Math.pow(0.88F, tt);
        }
        if (bl) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F * fallCounter));
            matrices.translate(0.0F, -0.2 * fallCounter, 0.0F);
        }

        // Vertical bob
        vertAngleY += vel.y * 0.015F * tt;
        vertAngleY -= 0.1F * vertAngleY * tt;
        vertAngleY *= Math.pow(0.88F, tt);
        vertVelocityYSlime += vel.y * 0.015F * tt;
        vertVelocityYSlime -= 0.1F * vertAngleYSlime * tt;
        vertVelocityYSlime *= Math.pow(0.88F, tt);
        vertAngleYSlime += vertVelocityYSlime * tt;

        matrices.translate(0.0F, vertAngleY * -1.0F, 0.0F);
        matrices.translate(0.0F, Math.sin(player.age * 0.1) * 0.007 * kj, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.15F * MathHelper.sin((float) player.age * 0.15F) * kj));

        // Base item position offset
        if (!stack.isEmpty() || player.isCrawling() || player.isClimbing() && !player.isOnGround() || player.isSwimming()) {
            if (player.getMainArm() == Arm.LEFT) bl = !bl;
            if (stack.getUseAction() == UseAction.BLOCK) {
                matrices.translate(0.0F, 0.0F, 0.0F);
            } else {
                matrices.translate(0.0F, -0.1, 0.1);
            }
        }

        // Lantern / Hanging signs
        if (stack.isOf(Items.LANTERN) || stack.isOf(Items.SOUL_LANTERN) || stack.isIn(ItemTags.HANGING_SIGNS)) {
            matrices.translate(0.0F, 0.1, 0.0F);
            if (player.isSwimming()) matrices.translate(0.0F, -0.1, 0.1);
        }

        // === SWIMMING ANIMATION ===
        if (player.isSwimming() && swingProgress == 0.0F && ENABLE_SWIMMING_ANIM) {
            double s = (player.age + tickDelta) * 0.1 * 2.0;
            double handRotation = Math.sin(s) * 1.5;
            double smoothRotation = handRotation * 0.8 + previousRotation * 0.2;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (bl ? smoothRotation : -smoothRotation)));
            matrices.translate(0.0F, 0.0F, (float)(smoothRotation * 0.2));
            double k = (player.age + tickDelta) * 0.2;
            double a = Math.cos(k);
            double b = a <= 0.0 ? a * 0.5 : a;
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) (bl ? b * 30.0 : b * -30.0)));
            matrices.translate(0.0F, 0.0F, (float)(a * 0.2));
            if (stack.isEmpty() && !bl && !player.isInvisible()) {
                float j1 = bl ? 1.0F : -1.0F;
                matrices.translate(j1, 0.0F - equipProgress * 0.3, 0.3);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * j1));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * j1));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                altSwing(matrices, arm, swingProgress);
                matrices.scale(0.9F, 0.9F, 0.9F);
                acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            }
            previousRotation = smoothRotation;
        }

        // === CLIMB/CRAWL ANIMATION ===
        if ((player.isClimbing() && !player.isOnGround() || player.isCrawling() && swingProgress == 0.0F) && !player.isUsingItem()) {
            double s = (player.age + tickDelta) * 0.1;
            float h = MathHelper.cos((float) s * 2.0F);
            float j = bl ? 1.0F : -1.0F;
            if (player.isClimbing()) {
                if (!stack.isOf(Items.LANTERN) && !stack.isOf(Items.SOUL_LANTERN)) {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F * h * j));
                } else {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(1.0F * h * j));
                }
            }
            if (player.isCrawling() && !player.isUsingItem() && swingProgress == 0.0F) {
                float timeValue = (player.age + tickDelta) * 0.4F;
                float l = MathHelper.sin(timeValue * mouseHolding);
                float dt = MathHelper.cos(timeValue * mouseHolding);
                if (stack.isOf(Items.LANTERN) || stack.isOf(Items.SOUL_LANTERN)) { l *= 0.14F; dt *= 0.14F; }
                matrices.translate(0.2 * l, 0.3 * l * j, -0.2 * l * j * al);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * l));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp(20.0F * dt * j, 0.0F, 20.0F)));
            }
            if (stack.isEmpty() && !bl && !player.isInvisible() && (!player.isOnGround() && player.isClimbing() || player.isCrawling())) {
                float l = bl ? 1.0F : -1.0F;
                matrices.translate(l, 0.0F - equipProgress * 0.3, 0.3);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * l));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * l));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                altSwing(matrices, arm, swingProgress);
                matrices.scale(0.9F, 0.9F, 0.9F);
                acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
            }
        }

        // === EMPTY HAND (PUNCHING) ===
        if (stack.isEmpty()) {
            if (bl && !player.isInvisible()) {
                float ll = bl ? 1.0F : -1.0F;
                if (!ENABLE_PUNCHING) { matrices.pop(); matrices.pop(); return; }
                if ((player.isOnGround() || !player.isClimbing()) && !player.isSwimming() && !player.isCrawling()) {
                    if (player.getMainArm() == Arm.LEFT) bl = !bl;
                    matrices.translate(0.0F, 0.2 * swing_rot, 0.15 * swing_rot);
                    matrices.translate(0.1 * ll * swing, 0.15 * swing, -0.45 * swing);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * swing * ll));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swing));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0F * swing_rot * ll));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F * swing_rot));
                    acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                } else {
                    matrices.translate(ll, 0.0F - equipProgress * 0.3, 0.3);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * ll));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * ll));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                    altSwing(matrices, arm, swingProgress);
                    matrices.scale(0.9F, 0.9F, 0.9F);
                    acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                }
            }
        } else if (stack.contains(DataComponentTypes.MAP_ID)) {
            if (bl && acc.getOffHand().isEmpty()) {
                matrices.translate(0.0F, 0.1, 0.0F);
                acc.invokeRenderTwoHandedMap(matrices, vertexConsumers, light, pitch, equipProgress, swingProgress);
            } else {
                matrices.translate(bl ? -0.1F : 0.1F, 0.1F, 0.0F);
                acc.invokeRenderOneHandedMap(matrices, vertexConsumers, light, equipProgress, arm, swingProgress, stack);
            }
        } else if (stack.getUseAction() == UseAction.CROSSBOW) {
            renderCrossbow(acc, player, tickDelta, pitch, hand, swingProgress, stack, equipProgress, matrices, vertexConsumers, light, bl, arm, kj);
        } else {
            renderGeneralItem(acc, player, tickDelta, pitch, hand, swingProgress, stack, equipProgress, matrices, vertexConsumers, light, bl, arm, kj, tt, swing, swing_rot);
        }

        matrices.pop();
        matrices.pop();
        isAttacking = MinecraftClient.getInstance().options.attackKey.isPressed();
    }

    private void renderCrossbow(HeldItemRendererAccessor acc, AbstractClientPlayerEntity player,
                                float tickDelta, float pitch, Hand hand, float swingProgress,
                                ItemStack stack, float equipProgress, MatrixStack matrices,
                                VertexConsumerProvider vertexConsumers, int light, boolean bl, Arm arm, float kj) {
        matrices.push();
        boolean bl2 = arm == Arm.RIGHT;
        int i = bl2 ? 1 : -1;
        if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
            acc.invokeApplyItemArmTransform(matrices, arm, equipProgress);
            matrices.translate(i * -0.4785682F, -0.24387F, 0.05731531F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 65.3F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 9.785F));
            float f = stack.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
            float g = f / CrossbowItem.getPullTime(stack, player);
            if (g > 1.0F) g = 1.0F;
            if (g > 0.1F) {
                float h = MathHelper.sin((f - 0.1F) * 1.3F);
                float yawDelta = h * (g - 0.1F);
                matrices.translate(yawDelta * 0.0F, yawDelta * 0.004F, yawDelta * 0.0F);
            }
            matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
            matrices.scale(1.0F, 1.0F, 1.0F);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(i * 45.0F));
        } else {
            acc.invokeSwingArm(swingProgress, equipProgress, matrices, i, arm);
            if (CrossbowItem.isCharged(stack) && swingProgress < 0.001F && bl) {
                matrices.translate(i * -0.341864F, 0.0F, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 10.0F));
            }
        }
        float yawDelta = bl ? 1.0F : -1.0F;
        matrices.translate(0.0F, 0.0F, -1.0F);
        matrices.translate(-0.45 * i, 0.45, 1.7);
        matrices.translate(yawDelta, 0.0F - equipProgress * 0.3, 0.3);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * yawDelta));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * yawDelta));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
        altSwing(matrices, arm, swingProgress);
        matrices.scale(0.9F, 0.9F, 0.9F);
        acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
        matrices.translate(-0.25F * i, 1.25F, 0.05);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90 * i));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(77.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(85 * i));
        matrices.scale(1.2F, 1.2F, 1.2F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
        matrices.translate(0.0F, -0.15, 0.15);
        acc.invokeRenderItem(player, stack, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
        matrices.pop();
        if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
            float f = stack.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
            float g = f / CrossbowItem.getPullTime(stack, player);
            if (g > 1.0F) g = 1.0F;
            if (g > 0.1F) {
                float h = MathHelper.sin((f - 0.1F) * 1.3F);
                float k = h * (g - 0.1F);
                matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
            }
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(g <= 0.2 ? 75.0F * g * 5.0F * i : 75 * i));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(10.0F * g * 1.5F));
            matrices.translate(-0.37 * i, 0.0F, 0.6);
            matrices.translate(0.15 * g * i, 0.0F, 0.0F);
            acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
        }
    }

    private void renderGeneralItem(HeldItemRendererAccessor acc, AbstractClientPlayerEntity player,
                                   float tickDelta, float pitch, Hand hand, float swingProgress,
                                   ItemStack stack, float equipProgress, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, int light, boolean bl, Arm arm, float kj, double tt, float swing, float swing_rot) {
        boolean bl2 = arm == Arm.RIGHT;
        int l = bl2 ? 1 : -1;

        if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
            renderUsingItem(acc, player, tickDelta, hand, swingProgress, stack, equipProgress, matrices, vertexConsumers, light, bl, arm, kj, l, tt, swing, swing_rot);
        } else if (player.isUsingRiptide() && stack.getUseAction() == UseAction.SPEAR) {
            riptideCounter += 0.15 * tt;
            float dt = stack.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
            float f = dt / 10.0F;
            if (f > 1.0F) f = 1.0F;
            if (f > 0.1F) {
                float g = MathHelper.sin((dt - 0.1F) * 1.3F);
                float j = g * (f - 0.1F);
                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
            }
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F - riptideCounter * 2.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l));
            matrices.translate(0.2 * l, 0.0F, 0.75F);
            matrices.translate(0.0F, 0.0F, 0.01 * MathHelper.sin(riptideCounter * 6.28F));
            acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-65 * l));
            matrices.translate(0.65F * l, -1.0F, -0.6);
        } else {
            riptideCounter = 0.0F;
            if (!stack.isOf(Items.LANTERN) && !stack.isOf(Items.SOUL_LANTERN) && !stack.isIn(ItemTags.HANGING_SIGNS)) {
                if (stack.getUseAction() == UseAction.BLOCK) {
                    matrices.translate(0.0F, -0.2, 0.0F);
                }
            } else {
                matrices.translate(0.1 * l, 0.0F, -0.1);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
            }
            // Arm rendering for idle items
            matrices.translate(l, 0.0F - equipProgress * 0.3, 0.3);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
            altSwing(matrices, arm, swingProgress);
            matrices.scale(0.9F, 0.9F, 0.9F);
            acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
        }

        // COMMON item positioning (runs for ALL cases: using, riptide, idle)
        // Bow has its own item positioning from the original mod
        if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand && stack.getUseAction() == UseAction.BOW) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(75.0F));
            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(-15 * l));
            matrices.translate(0.8 * l, 0.0F - equipProgress * 0.3F, -0.1);
        } else {
            matrices.translate(-0.3 * l, 0.65, -0.1);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-65 * l));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
        }

        // Block items - rendered as 3D blocks (not via renderItem)
        if (stack.getItem() instanceof BlockItem && !(stack.isIn(ItemTags.TRIMMABLE_ARMOR) || stack.isIn(ItemTags.BOOKSHELF_BOOKS))) {
            renderBlockItem(acc, player, stack, matrices, vertexConsumers, light, bl, bl2, l, arm, tt, swing, swing_rot, equipProgress, swingProgress);
        } else {
            // Non-block items - rendered via renderItem
            boolean isToolOrWeapon = isTool(stack) && !stack.isIn(ItemTags.TRIMMABLE_ARMOR) && !stack.isIn(ItemTags.BOOKSHELF_BOOKS)
                    && stack.getUseAction() != UseAction.EAT && stack.isEnchantable()
                    || stack.getUseAction() == UseAction.BOW || stack.getUseAction() == UseAction.SPYGLASS
                    || getAttackDamage(stack) != 0.0F || stack.getUseAction() == UseAction.BLOCK
                    || stack.isOf(Items.WARPED_FUNGUS_ON_A_STICK) || stack.isOf(Items.CARROT_ON_A_STICK)
                    || stack.getItem() instanceof FishingRodItem || stack.isOf(Items.SHEARS);

            if (isToolOrWeapon) {
                // Base rotations per item type (makes items face player)
                if (isSword(stack)) {
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75 * l));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * l));
                    matrices.scale(1.2F, 1.2F, 1.2F);
                    // Sword swing animation
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F * swing));
                    matrices.translate(0.0F, 0.1 * swing, -0.1 * swing);
                } else if (stack.getUseAction() == UseAction.SPEAR) {
                    // Trident idle positioning
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75 * l));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * l));
                    matrices.translate(-0.3F * l, 0.0F, 0.0F);
                    matrices.scale(1.2F, 1.2F, 1.2F);
                    // Trident swing
                    float jn = swing_rot;
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0F * jn));
                    matrices.translate(0.0F, 0.1 * jn, -0.1 * jn);
                } else if (stack.getUseAction() == UseAction.BOW) {
                    // Bow idle positioning
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(160 * l));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60 * l));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                    matrices.scale(0.75F, 0.75F, 0.75F);
                    matrices.translate(0.15 * l, bl ? 0.35F : 0.45F, bl ? -0.15F : -0.1F);
                    matrices.translate(0.17 * l, 0.0F, 0.3);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90 * l));
                } else if (stack.getUseAction() == UseAction.BLOCK) {
                    // Shield idle positioning
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(-154 * l));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90 * l));
                    matrices.translate(-0.012F * l, +0.380, -0.175);
                    matrices.scale(0.55F, 0.55F, 0.55F);
                } else if (stack.isIn(ItemTags.SHOVELS)) {
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75 * l));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * l));
                    matrices.scale(1.2F, 1.2F, 1.2F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F * swing_rot));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                    matrices.translate(0.07 * l, 0.0F, 0.05);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * l));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0F));
                } else {
                    // Other tools (axes, pickaxes, hoes)
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75 * l));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * l));
                    matrices.scale(1.2F, 1.2F, 1.2F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F * swing));
                    matrices.translate(0.0F, 0.05 * swing, -0.05 * swing);
                }
            } else {
                // Regular items (food, potions, etc.) - face player vertically
                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(5 * l));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
                matrices.translate(-0.015 * l, -0.055, -0.06);
                matrices.scale(0.7F, 0.7F, 0.7F);
            }

            // Nether star / end crystal
            if (stack.isOf(Items.NETHER_STAR) || (stack.isOf(Items.END_CRYSTAL) && MB3D_COMPAT)) {
                netherCounter += 0.9 * tt;
                matrices.translate(0.0F, 0.25F + 0.02 * MathHelper.sin(netherCounter * 0.1F), 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(3.0F * MathHelper.sin(netherCounter * 0.2F)));
                float ns = 1.0F + 0.01F * MathHelper.sin(netherCounter);
                matrices.scale(ns, ns, ns);
            } else {
                netherCounter = 0.0F;
            }

            acc.invokeRenderItem(player, stack, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
        }
    }

    private void renderUsingItem(HeldItemRendererAccessor acc, AbstractClientPlayerEntity player,
                                 float tickDelta, Hand hand, float swingProgress,
                                 ItemStack stack, float equipProgress, MatrixStack matrices,
                                 VertexConsumerProvider vertexConsumers, int light, boolean bl, Arm arm, float kj, int l, double tt, float swing, float swing_rot) {
        UseAction useAction = stack.getUseAction();

        if (useAction == UseAction.NONE) {
            acc.invokeApplyItemArmTransform(matrices, arm, equipProgress);
        } else if (useAction == UseAction.EAT || useAction == UseAction.DRINK) {
            float yawDelta = stack.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
            float pitchDelta = yawDelta / 5.0F;
            if (pitchDelta > 1.0F) pitchDelta = 1.0F;
            float k = MathHelper.sin(yawDelta / 2.0F * 3.14F) / 10.0F;
            matrices.translate(l, 0.1, 0.3);
            matrices.translate(0.2 * l * pitchDelta, -0.7 * pitchDelta, -0.2 * pitchDelta);
            matrices.translate(0.0F, -0.2 * k, -0.2 * k);
            matrices.translate(0.0F, 0.1F * easeInOutBack(MathHelper.sin(pitchDelta * 3.14F)), 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
            altSwing(matrices, arm, swingProgress);
            matrices.scale(0.9F, 0.9F, 0.9F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * pitchDelta * l));
            acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
        } else if (useAction == UseAction.BLOCK) {
            float k = stack.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
            double s = k / 4.0F;
            float s2 = k / 6.0F;
            if (s > 1.0) s = 1.0;
            if (s2 > 1.0F) s2 = 1.0F;
            matrices.translate(0.0F, -0.2, 0.0F);
            matrices.translate(l, 0.0F, 0.3);
            matrices.translate(0.7 * s * l, 0.0F, -1.3 * s);
            matrices.translate(-0.2 * l * s2, 0.0F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(15.0 * Math.sin(s2 * 3.14))));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(70.0 * s * l)));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5 * l * (float) s));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(-10.0 * s)));
            matrices.translate(0.0F, 0.0F, -0.2 * s);
            altSwing(matrices, arm, swingProgress);
            matrices.scale(0.9F, 0.9F, 0.9F);
            acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
            // Shield positioning - separate per hand for independent tuning
            if (l == 1) {
                // Right hand shield
                matrices.translate(+0.19F, +0.10, -0.240);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(4.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.4F));
                matrices.translate(-0.2, -0.04, 0.15);
            } else {
                // Left hand shield
                matrices.translate(-0.19F, +0.10, -0.180);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-4.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.4F));
                matrices.translate(+0.2, -0.04, 0.15);
            }
        } else if (useAction == UseAction.BOW) {
            matrices.push();
            float m1 = stack.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
            float f1 = m1 / 20.0F;
            if (f1 > 1.0F) f1 = 1.0F;
            if (f1 > 0.1F) {
                float g1 = MathHelper.sin((m1 - 0.1F) * 1.3F);
                float j1 = g1 * f1;
                matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
            }
            matrices.push();
            matrices.translate(bl ? -0.1F : 0.1F, 0.0F, f1 * 0.15F);
            acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
            matrices.pop();
            matrices.translate(bl ? -0.5F : 0.5F, -0.45, 0.1);
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(0.3F));
            if (bl) {
                matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(-0.3F));
                matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(1.0F));
            } else {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-0.3F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(1.0F));
            }
            acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
            if (bl) {
                matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(2.5F));
            } else {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(2.5F));
            }
            matrices.translate(bl ? -0.65 : 0.65, -0.35, 0.27);
            matrices.pop();
        } else if (useAction == UseAction.SPEAR) {
            if (player.getOffHandStack().isEmpty() && !player.isCrawling() && !player.isSwimming() && !player.isClimbing()) {
                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25 * l));
                matrices.translate(-0.15 * l, 0.1, 0.1);
                acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                matrices.pop();
            }
            float dt = stack.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
            float f = dt / 10.0F;
            if (f > 1.0F) f = 1.0F;
            if (f > 0.1F) {
                float g = MathHelper.sin((dt - 0.1F) * 1.3F);
                float j = g * (f - 0.1F);
                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
            }
            // Position arm holding trident forward
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l));
            matrices.translate(0.2 * l, 0.0F, 0.8);
            acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
            // Position trident in the hand (not under it)
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(100.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-55 * l));
            matrices.translate(0.5F * l, -0.7, -0.35);
        } else if (useAction == UseAction.SPYGLASS) {
            float g1 = player.getItemUseTimeLeft() % 10;
            float h1 = g1 - tickDelta + 1.0F;
            float j1 = 1.0F - h1 / 10.0F;
            float n = -15.0F + 75.0F * MathHelper.cos(j1 * 2.0F * (float) Math.PI);
            float z = stack.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
            float x = z / 4.0F;
            if (x > 1.0F) x = 1.0F;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l * x));
            matrices.translate(0.3F * l * x, 0.3 * x, 0.1 * x);
            if (x == 1.0F) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n / 20.0F));
            acc.invokeRenderPlayerArm(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
        }
    }

    private void renderBlockItem(HeldItemRendererAccessor acc, AbstractClientPlayerEntity player,
                                 ItemStack stack, MatrixStack matrices,
                                 VertexConsumerProvider vertexConsumers, int light, boolean bl, boolean bl2, int l, Arm arm, double tt, float swing, float swing_rot, float equipProgress, float swingProgress) {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == Blocks.AIR) return;
        BlockState state = block.getDefaultState();

        // Small items
        if (isSmallBlockItem(stack)) {
            matrices.translate(0.0F, 0.0F, -0.1);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(5 * l));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
        } else if (stack.getName().getString().toLowerCase().contains("torch")) {
            matrices.scale(1.5F, 1.5F, 1.5F);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25 * l));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
            matrices.translate(0.2 * l, 0.2, 0.05);
        } else if (!stack.isOf(Items.LANTERN) && !stack.isOf(Items.SOUL_LANTERN) && !stack.isIn(ItemTags.HANGING_SIGNS)) {
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25 * l));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
            matrices.translate(0.2 * l, 0.2, 0.05);
        } else {
            // Lantern / hanging signs with swing physics
            float dt = (float) (deltaTime * 8.0);
            float yawDelta = player.prevYaw - player.getYaw();
            float pitchDelta = player.prevPitch - player.getPitch();
            swingVelocityY += yawDelta * 0.015F * dt;
            swingVelocityY += swingProgress * 2.0F * dt;
            swingVelocityX += pitchDelta * 0.015F * dt;
            swingVelocityY -= 0.1F * swingAngleY * dt;
            swingVelocityX -= 0.1F * swingAngleX * dt;
            swingVelocityY *= Math.pow(0.88F, dt);
            swingVelocityX *= Math.pow(0.88F, dt);
            swingAngleY += swingVelocityY * dt;
            swingAngleX += swingVelocityX * dt;
            double currentSpeed = player.getVelocity().length();
            swingVelocityZ += (bl ? currentSpeed * -15.0F - swingVelocityZ : currentSpeed * 15.0F - swingVelocityZ) * 0.1F * dt;
            if ((currentSpeed > 0.09 && player.isOnGround() || player.isSwimming() || player.isClimbing() && !player.isOnGround())
                    && MinecraftClient.getInstance().options.getBobView().getValue()) {
                boolean randomBool = Math.random() > 0.5;
                swingVelocityY += randomBool ? -5.5F * currentSpeed * dt : 5.5F * currentSpeed * dt;
            }
            matrices.translate(0.0F, 0.0F, -0.1);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(35 * l + swingAngleY));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F + swingAngleX));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l + swingVelocityZ));
            if (stack.isIn(ItemTags.HANGING_SIGNS)) {
                matrices.translate(0.0F, -0.1, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45 * l));
            }
            matrices.translate(0.3 * l, -0.35, 0.0F);
            matrices.translate(0.0F, 0.0F, 0.1);
            matrices.scale(1.5F, 1.5F, 1.5F);
        }

        // Render block
        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        matrices.push();
        if (!bl2) matrices.translate(-0.4F, 0.0F, 0.0F);
        matrices.scale(0.35F, 0.35F, 0.35F);
        matrices.translate(-0.9 * l, -0.45, -0.5F);

        if (state.isIn(BlockTags.BUTTONS)) matrices.translate(0.2 * l, -0.15, -0.2);
        if (state.isIn(BlockTags.PRESSURE_PLATES)) matrices.translate(0.0F, 0.1, 0.0F);

        // Slime bounce
        if (stack.isOf(Items.SLIME_BLOCK) || stack.isOf(Items.HONEY_BLOCK)
                || state.isIn(BlockTags.FLOWERS) || state.isIn(BlockTags.LEAVES)
                || state.isIn(BlockTags.SAPLINGS) || state.isIn(BlockTags.SWORD_EFFICIENT)) {
            vertVelocityYSlime += swingProgress * 0.03 * deltaTime * 8.0;
            if ((player.getVelocity().length() > 0.09 && player.isOnGround() || player.isSwimming()
                    || player.isCrawling() || player.isClimbing() && !player.isOnGround())
                    && MinecraftClient.getInstance().options.getBobView().getValue()) {
                vertVelocityYSlime += -0.05F * player.getVelocity().length() * deltaTime * 8.0;
            }
            matrices.scale(1.0F, 1.0F + vertAngleYSlime * -2.0F, 1.0F);
        }

        if (player.age - prevAge >= 100.0F) { repPower = !repPower; prevAge = player.age; }
        if (stack.isIn(ItemTags.BEDS) && bl) {
            matrices.translate(0.9, 0.0F, 0.8);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * l));
        }

        blockRenderManager.renderBlockAsEntity(state, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}

