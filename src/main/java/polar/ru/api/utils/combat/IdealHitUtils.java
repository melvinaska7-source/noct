package polar.ru.api.utils.combat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.mixin.IEntity;

public final class IdealHitUtils
implements QClient {
    private static final int WATER_CRIT_INTENT_TICKS = 8;
    private static final int WATER_CRIT_CONTACT_TICKS = 10;
    private static final double WATER_CRIT_MIN_UPWARD_VELOCITY = 0.05;
    private static int lastWaterContactAge = Integer.MIN_VALUE;
    private static int lastWaterCritIntentAge = Integer.MIN_VALUE;

    public static float getAICooldown() {
        if (IdealHitUtils.mc.player.getMainHandStack().getItem() == Items.AIR) {
            return 0.9f;
        }
        if (IdealHitUtils.mc.player.getMainHandStack().getItem() instanceof AxeItem || IdealHitUtils.mc.player.getMainHandStack().getItem() instanceof ShovelItem) {
            return 0.95f;
        }
        return 0.93f;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static boolean canAIFall() {
        BlockPos posWater = BlockPos.ofFloored((Position)IdealHitUtils.mc.player.getPos().add(0.0, (double)-0.4f, 0.0));
        if (IdealHitUtils.mc.world.getBlockState(posWater).isOf(Blocks.WATER)) {
            return true;
        }
        if (IdealHitUtils.getBlock(0.0, 3.0, 0.0) == Blocks.AIR && IdealHitUtils.getBlock(0.0, 2.0, 0.0) == Blocks.AIR) {
            if (IdealHitUtils.getBlock(0.0, 1.0, 0.0) == Blocks.AIR) return true;
        }
        if (IdealHitUtils.mc.player.fallDistance < (IdealHitUtils.getBlock(0.0, 2.0, 0.0) != Blocks.AIR ? 0.08f : 0.6f)) return true;
        if (!(IdealHitUtils.mc.player.fallDistance > 1.2f)) return false;
        return true;
    }

    public static boolean canCritical(LivingEntity target) {
        boolean isCritPossible;
        IdealHitUtils.updateWaterCritState();
        boolean packetCrits = ModuleClass.packetCriticals.isEnable();
        boolean hasSlowFalling = IdealHitUtils.mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
        boolean inCobweb = IdealHitUtils.isInCobweb();
        boolean smartCrit = ModuleClass.aura.smartCrit.isState();
        if (packetCrits && inCobweb) {
            return true;
        }
        if (packetCrits && hasSlowFalling) {
            return IdealHitUtils.mc.player.getVelocity().y < 0.0 && IdealHitUtils.mc.player.fallDistance > 0.0f;
        }
        if (IdealHitUtils.isTryingWaterCrit()) {
            return IdealHitUtils.isWaterCritWindow();
        }
        boolean bl = isCritPossible = !IdealHitUtils.mc.player.isOnGround() && IdealHitUtils.mc.player.getVelocity().y < 0.0 && IdealHitUtils.mc.player.fallDistance > 0.0f;
        if (IdealHitUtils.isNoJumpDelayCeilingCritIntent()) {
            return IdealHitUtils.isNoJumpDelayCeilingCritWindow();
        }
        if (IdealHitUtils.isNoJumpDelayJumpCritIntent()) {
            return IdealHitUtils.isNoJumpDelayJumpCritWindow();
        }
        if (IdealHitUtils.cannotPerformCrit()) {
            return true;
        }
        if (smartCrit) {
            return IdealHitUtils.mc.player.isOnGround() || isCritPossible;
        }
        return isCritPossible;
    }

    private static boolean isNoJumpDelayCeilingCritIntent() {
        return ModuleClass.noJumpDelay.isEnable() && IdealHitUtils.mc.options != null && IdealHitUtils.mc.options.jumpKey.isPressed() && IdealHitUtils.hasLowCeilingForJumpCrit();
    }

    private static boolean isNoJumpDelayJumpCritIntent() {
        return ModuleClass.noJumpDelay.isEnable() && IdealHitUtils.mc.options != null && IdealHitUtils.mc.options.jumpKey.isPressed();
    }

    private static boolean isNoJumpDelayCeilingCritWindow() {
        return IdealHitUtils.mc.player != null && !IdealHitUtils.mc.player.isOnGround() && IdealHitUtils.mc.player.getVelocity().y <= 0.01 && !IdealHitUtils.mc.player.isTouchingWater() && !IdealHitUtils.mc.player.isSubmergedInWater() && !IdealHitUtils.mc.player.isInLava() && !IdealHitUtils.mc.player.isClimbing() && !IdealHitUtils.mc.player.hasVehicle() && !IdealHitUtils.mc.player.getAbilities().flying;
    }

    public static boolean isNoJumpDelayJumpCritWindow() {
        return IdealHitUtils.mc.player != null && IdealHitUtils.mc.world != null && ModuleClass.noJumpDelay.isEnable() && IdealHitUtils.mc.options != null && IdealHitUtils.mc.options.jumpKey.isPressed() && !IdealHitUtils.mc.player.isOnGround() && IdealHitUtils.mc.player.getVelocity().y < 0.0 && !IdealHitUtils.mc.player.isTouchingWater() && !IdealHitUtils.mc.player.isSubmergedInWater() && !IdealHitUtils.mc.player.isInLava() && !IdealHitUtils.mc.player.isClimbing() && !IdealHitUtils.mc.player.hasVehicle() && !IdealHitUtils.mc.player.getAbilities().flying && !IdealHitUtils.mc.player.hasStatusEffect(StatusEffects.LEVITATION) && !IdealHitUtils.mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) && !IdealHitUtils.mc.player.hasStatusEffect(StatusEffects.BLINDNESS) && !IdealHitUtils.mc.player.isGliding() && !IdealHitUtils.isInCobweb();
    }

    private static boolean hasLowCeilingForJumpCrit() {
        if (IdealHitUtils.mc.player == null || IdealHitUtils.mc.world == null) {
            return false;
        }
        Box box = IdealHitUtils.mc.player.getBoundingBox().contract(0.03);
        Box headBox = new Box(box.minX, box.maxY, box.minZ, box.maxX, box.maxY + 0.32, box.maxZ);
        for (BlockPos pos : BlockPos.iterate((int)MathHelper.floor((double)headBox.minX), (int)MathHelper.floor((double)headBox.minY), (int)MathHelper.floor((double)headBox.minZ), (int)MathHelper.floor((double)headBox.maxX), (int)MathHelper.floor((double)headBox.maxY), (int)MathHelper.floor((double)headBox.maxZ))) {
            BlockState state = IdealHitUtils.mc.world.getBlockState(pos);
            if (state.isAir() || state.getCollisionShape((BlockView)IdealHitUtils.mc.world, pos).isEmpty()) continue;
            return true;
        }
        return false;
    }

    public static boolean canPacketCrit() {
        return IdealHitUtils.isInCobweb() || IdealHitUtils.mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
    }

    private static void updateWaterCritState() {
        if (IdealHitUtils.mc.player == null || IdealHitUtils.mc.world == null) {
            lastWaterContactAge = Integer.MIN_VALUE;
            lastWaterCritIntentAge = Integer.MIN_VALUE;
            return;
        }
        boolean nearWaterSurface = IdealHitUtils.isNearWaterSurface();
        if (!nearWaterSurface) {
            return;
        }
        lastWaterContactAge = IdealHitUtils.mc.player.age;
        if (IdealHitUtils.isWaterCritIntentState()) {
            lastWaterCritIntentAge = IdealHitUtils.mc.player.age;
        }
    }

    private static boolean isWaterCritIntentState() {
        if (IdealHitUtils.mc.player == null || IdealHitUtils.mc.options == null) {
            return false;
        }
        return IdealHitUtils.mc.options.jumpKey.isPressed() && !IdealHitUtils.mc.player.isOnGround() && !IdealHitUtils.mc.player.isSubmergedInWater() && IdealHitUtils.mc.player.getVelocity().y > 0.05;
    }

    private static boolean isTryingWaterCrit() {
        if (IdealHitUtils.mc.player == null || IdealHitUtils.mc.options == null || !IdealHitUtils.mc.options.jumpKey.isPressed()) {
            return false;
        }
        return IdealHitUtils.mc.player.age - lastWaterCritIntentAge <= 8 && IdealHitUtils.mc.player.age - lastWaterContactAge <= 10;
    }

    private static boolean isWaterCritWindow() {
        return IdealHitUtils.mc.player != null && !IdealHitUtils.mc.player.isOnGround() && !IdealHitUtils.mc.player.isTouchingWater() && !IdealHitUtils.mc.player.isSubmergedInWater() && IdealHitUtils.mc.player.fallDistance > 0.0f && IdealHitUtils.mc.player.getVelocity().y < 0.0;
    }

    private static boolean isNearWaterSurface() {
        if (IdealHitUtils.mc.player == null || IdealHitUtils.mc.world == null) {
            return false;
        }
        BlockPos below = BlockPos.ofFloored((Position)IdealHitUtils.mc.player.getPos().add(0.0, (double)-0.4f, 0.0));
        return IdealHitUtils.mc.player.isTouchingWater() || IdealHitUtils.mc.player.isSubmergedInWater() || IdealHitUtils.mc.world.getBlockState(below).isOf(Blocks.WATER);
    }

    private static boolean cannotPerformCrit() {
        double effectiveJumpHeight = IdealHitUtils.mc.player.getStepHeight();
        Vec3d jumpVec = new Vec3d(0.0, effectiveJumpHeight, 0.0);
        Vec3d allowedMovement = ((IEntity)IdealHitUtils.mc.player).invokeAdjustMovementForCollisions(jumpVec);
        boolean cobweb = IdealHitUtils.isInCobweb();
        BlockPos posWater = BlockPos.ofFloored((Position)IdealHitUtils.mc.player.getPos().add(0.0, (double)(IdealHitUtils.mc.player.getHeight() / 2.0f), 0.0));
        return IdealHitUtils.mc.player.isInLava() || IdealHitUtils.mc.player.isClimbing() || IdealHitUtils.mc.world.getBlockState(posWater).isOf(Blocks.WATER) || IdealHitUtils.mc.player.hasStatusEffect(StatusEffects.LEVITATION) || IdealHitUtils.mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) || IdealHitUtils.mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || cobweb || IdealHitUtils.mc.player.isGliding() || IdealHitUtils.mc.player.hasVehicle() || IdealHitUtils.mc.player.getAbilities().flying || IdealHitUtils.mc.player.isTouchingWater() || allowedMovement.y < (double)IdealHitUtils.mc.player.getStepHeight() - 0.5 && IdealHitUtils.mc.player.isOnGround();
    }

    public static boolean isInCobweb() {
        Box box = IdealHitUtils.mc.player.getBoundingBox();
        for (BlockPos pos : BlockPos.iterate((int)MathHelper.floor((double)box.minX), (int)MathHelper.floor((double)box.minY), (int)MathHelper.floor((double)box.minZ), (int)MathHelper.floor((double)box.maxX), (int)MathHelper.floor((double)box.maxY), (int)MathHelper.floor((double)box.maxZ))) {
            if (!IdealHitUtils.mc.world.getBlockState(pos).isOf(Blocks.COBWEB)) continue;
            return true;
        }
        return false;
    }

    public static Block getBlock(double x2, double y2, double z2) {
        return IdealHitUtils.mc.world.getBlockState(IdealHitUtils.mc.player.getBlockPos().add((int)x2, (int)y2, (int)z2)).getBlock();
    }

    public static boolean findFall(float fallDistance) {
        Vec3d rotationVec = IdealHitUtils.mc.player.getRotationVector();
        double tempVelocityX = IdealHitUtils.mc.player.getVelocity().x;
        double tempVelocityY = IdealHitUtils.mc.player.getVelocity().y;
        double tempVelocityZ = IdealHitUtils.mc.player.getVelocity().z;
        float n2 = MathHelper.cos((float)(IdealHitUtils.mc.player.getPitch() * ((float)Math.PI / 180)));
        n2 = (float)((double)(n2 * n2) * Math.min(rotationVec.length() / 0.4, 1.0));
        Vec3d vec3d = new Vec3d(tempVelocityX, tempVelocityY, tempVelocityZ).add(0.0, 0.08 * (-1.0 + (double)n2 * 0.75), 0.0);
        tempVelocityY = vec3d.y * (double)0.98f;
        return tempVelocityY < (double)fallDistance;
    }
    private IdealHitUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

