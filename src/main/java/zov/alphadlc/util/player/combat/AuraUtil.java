package zov.alphadlc.util.player.combat;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.render.math.GCDFixer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Утилиты для KillAura / LegitAura / AttackAura.
 */
@UtilityClass
public class AuraUtil implements IMinecraft {

    public static double distanceSqToEntity(Vec3d eye, Entity entity) {
        Box box = entity.getBoundingBox();
        double cx = MathHelper.clamp(eye.x, box.minX, box.maxX);
        double cy = MathHelper.clamp(eye.y, box.minY, box.maxY);
        double cz = MathHelper.clamp(eye.z, box.minZ, box.maxZ);
        double dx = cx - eye.x;
        double dy = cy - eye.y;
        double dz = cz - eye.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public static double distanceSqToEntity(Entity entity) {
        if (mc.player == null) return Double.POSITIVE_INFINITY;
        return distanceSqToEntity(mc.player.getEyePos(), entity);
    }

    public static boolean isInReach(Entity entity, double maxReach) {
        return distanceSqToEntity(entity) <= maxReach * maxReach;
    }

    public static boolean canAttack(float yaw, float pitch, double distance, Entity entity, boolean throughWalls) {
        if (mc.player == null || mc.world == null) return false;
        return canAttack(mc.player.getEyePos(), yaw, pitch, distance, entity, throughWalls);
    }

    public static boolean canAttack(Vec3d rayOrigin, float yaw, float pitch, double distance, Entity entity, boolean throughWalls) {
        if (mc.player == null || mc.world == null) return false;
        Vec3d dir = Vec3d.fromPolar(pitch, yaw).multiply(distance);
        Optional<Vec3d> opt = entity.getBoundingBox().contains(rayOrigin)
            ? Optional.of(rayOrigin)
            : entity.getBoundingBox().raycast(rayOrigin, rayOrigin.add(dir));
        if (opt.isEmpty()) return false;
        if (throughWalls) return true;
        return isVisible(rayOrigin, entity, distance);
    }

    public static boolean isVisible(Vec3d from, Entity entity, double distance) {
        if (mc.world == null) return false;
        Vec3d to = entity.getBoundingBox().getCenter();
        return mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
    }

    public static boolean isCritPossible() {
        if (mc.player == null) return false;
        return mc.player.isOnGround() && !mc.player.isSprinting() && !mc.player.isTouchingWater()
            && !mc.player.isInLava() && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            && mc.player.getVehicle() == null && !mc.player.isClimbing()
            && !mc.player.getAbilities().flying;
    }

    public static boolean shouldWaitForCrit() {
        return isCritPossible() && mc.player.getAttackCooldownProgress(0.5f) > 0.9f;
    }

    public static List<Vec3d> getAimPoints(Entity entity) {
        List<Vec3d> points = new ArrayList<>();
        Box box = entity.getBoundingBox();
        double cx = (box.minX + box.maxX) / 2.0;
        double cz = (box.minZ + box.maxZ) / 2.0;
        double height = box.maxY - box.minY;

        points.add(new Vec3d(cx, box.minY + height * 0.5, cz)); // center
        points.add(new Vec3d(cx, box.minY + height * 0.85, cz)); // head
        points.add(new Vec3d(cx, box.minY + 0.1, cz)); // feet

        return points;
    }

    public static Optional<Vec3d> getBestAimPoint(Entity entity, float yaw, float pitch, double reach, boolean throughWalls) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = Vec3d.fromPolar(pitch, yaw);

        return getAimPoints(entity).stream()
            .filter(p -> {
                if (throughWalls) return true;
                return mc.world.raycast(new RaycastContext(eye, p, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
            })
            .filter(p -> p.squaredDistanceTo(eye) <= reach * reach)
            .min(Comparator.comparingDouble(p -> {
                Vec3d toP = p.subtract(eye).normalize();
                return -look.dotProduct(toP);
            }));
    }

    public static float[] smoothRotate(float currentYaw, float currentPitch, float targetYaw, float targetPitch, float speed) {
        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float dist = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        if (dist < 1.0f) return new float[]{targetYaw, targetPitch};

        float factor = Math.min(speed / dist, 1.0f);
        float newYaw = currentYaw + yawDiff * factor;
        float newPitch = currentPitch + pitchDiff * factor;

        newYaw = currentYaw + GCDFixer.getFixRotate(MathHelper.wrapDegrees(newYaw - currentYaw));
        newPitch = currentPitch + GCDFixer.getFixRotate(newPitch - currentPitch);
        newPitch = MathHelper.clamp(newPitch, -90f, 90f);

        return new float[]{newYaw, newPitch};
    }

    public static float calculateFov(Vec3d from, Vec3d to, float yaw, float pitch) {
        Vec3d look = Vec3d.fromPolar(pitch, yaw).normalize();
        Vec3d dir = to.subtract(from).normalize();
        double dot = MathHelper.clamp(look.dotProduct(dir), -1.0, 1.0);
        return (float) Math.toDegrees(Math.acos(dot));
    }

    public static boolean isInFluid(Entity entity) {
        if (mc.world == null) return false;
        Box box = entity.getBoundingBox();
        for (BlockPos pos : BlockPos.iterate(
            BlockPos.ofFloored(box.minX, box.minY, box.minZ),
            BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ))) {
            FluidState fluid = mc.world.getBlockState(pos).getFluidState();
            if (!fluid.isEmpty() && (fluid.isIn(FluidTags.WATER) || fluid.isIn(FluidTags.LAVA))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFallingForCrit() {
        if (mc.player == null) return false;
        return mc.player.fallDistance > 0.5f;
    }

    public static boolean canCrit() {
        return isCritPossible();
    }
}
