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
import zov.alphadlc.util.math.GCDFixer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Утилиты для KillAura / LegitAura / AttackAura.
 * Адаптировано из Aethereal под Nocturne.
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
        return opt.filter(vec3d -> throughWalls || mc.world.raycast(
            new RaycastContext(rayOrigin, vec3d, RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS).isPresent();
    }

    public static boolean isVisible(Vec3d from, LivingEntity entity, double reach) {
        Box box = entity.getBoundingBox();
        double[] steps = {0.0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0};
        int lastIndex = steps.length - 1;
        double reachSq = reach * reach;
        for (int i = 0; i <= lastIndex; i++) {
            for (int j = 0; j <= lastIndex; j++) {
                for (int k = 0; k <= lastIndex; k++) {
                    if (i <= 0 || i >= lastIndex || j <= 0 || j >= lastIndex || k <= 0 || k >= lastIndex) {
                        Vec3d point = new Vec3d(
                            MathHelper.lerp(steps[i], box.minX, box.maxX),
                            MathHelper.lerp(steps[j], box.minY, box.maxY),
                            MathHelper.lerp(steps[k], box.minZ, box.maxZ));
                        double distSq = from.squaredDistanceTo(point);
                        if (distSq <= reachSq) {
                            Vec3d end = point.add(from.subtract(point).multiply(0.05 / Math.sqrt(distSq)));
                            if (mc.world.raycast(new RaycastContext(from, end,
                                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player))
                                .getType() == HitResult.Type.MISS) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean canCrit() {
        if (mc.player == null || mc.world == null) return false;
        double nextVy = (mc.player.getVelocity().y - 0.08) * 0.98;
        if (nextVy >= 0.0) return false;
        Box moved = mc.player.getBoundingBox().offset(0.0, nextVy, 0.0);
        Box feet = new Box(moved.minX, moved.minY - 0.01, moved.minZ, moved.maxX, moved.minY, moved.maxZ);
        return mc.world.isBlockSpaceEmpty(mc.player, feet);
    }

    public static boolean isCritReady() {
        if (mc.player == null || mc.player.getWorld() == null) return false;
        World world = mc.player.getWorld();
        BlockPos eye = BlockPos.ofFloored(mc.player.getEyePos());
        FluidState fluid = world.getFluidState(eye);
        return !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
            && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            && !fluid.isIn(FluidTags.WATER)
            && !fluid.isIn(FluidTags.LAVA)
            && !mc.player.getAbilities().flying
            && !mc.player.isGliding()
            && !mc.player.isClimbing()
            && !mc.player.hasVehicle();
    }

    public static boolean isFallingForCrit() {
        return mc.player != null && isCritReady() && mc.player.fallDistance > 0.0f && !mc.player.isOnGround();
    }

    public static float smoothRotation(float start, float end, float amount) {
        float amountClamped = MathHelper.clamp(amount, 0.0f, 1.0f);
        float delta = MathHelper.wrapDegrees(end - start);
        if (Math.abs(delta) < 0.5f) return end;
        float stepped = MathHelper.wrapDegrees(start + (delta * amountClamped));
        float patched = GCDFixer.fix(start, stepped);
        float remaining = MathHelper.wrapDegrees(end - patched);
        return Math.abs(remaining) < 0.5f ? end : patched;
    }

    public static Vec3d findAimPoint(Vec3d eye, LivingEntity target, double reach, boolean throughWalls) {
        Box bb = target.getBoundingBox();
        double mx = (bb.minX + bb.maxX) * 0.5;
        double mz = (bb.minZ + bb.maxZ) * 0.5;
        Vec3d targetEye = target.getPos().add(0.0, target.getStandingEyeHeight(), 0.0);
        double distToTargetEye = eye.distanceTo(targetEye);

        double aimHeight = MathHelper.lerp(
            MathHelper.clamp(distToTargetEye / 3.0, 0.0, 1.0),
            bb.minY, MathHelper.clamp(eye.y, bb.minY, bb.maxY));

        Vec3d ideal = new Vec3d(mx, aimHeight, mz);
        List<Vec3d> pts = new ArrayList<>();
        pts.add(ideal);

        double[] t = {0.0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0};
        int last = t.length - 1;
        for (int a = 0; a < t.length; a++) {
            for (int b = 0; b < t.length; b++) {
                for (int c = 0; c < t.length; c++) {
                    if (a == 0 || a == last || b == 0 || b == last || c == 0 || c == last) {
                        pts.add(new Vec3d(
                            MathHelper.lerp(t[a], bb.minX, bb.maxX),
                            MathHelper.lerp(t[b], bb.minY, bb.maxY),
                            MathHelper.lerp(t[c], bb.minZ, bb.maxZ)));
                    }
                }
            }
        }

        for (double pad : new double[]{0.0, 0.2}) {
            List<Vec3d> visible = new ArrayList<>();
            for (Vec3d p : pts) {
                Vec3d delta = p.subtract(eye);
                double len = delta.length();
                double limit = reach + pad;
                if (len <= limit) {
                    float traceDist = (float) limit;
                    if (canAttack(eye,
                        (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0),
                        (float) (-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))),
                        traceDist, target, false)) {
                        visible.add(p);
                    }
                }
            }
            if (!visible.isEmpty()) {
                Vec3d centroid = visible.stream().reduce(Vec3d.ZERO, Vec3d::add)
                    .multiply(1.0 / visible.size());
                return visible.stream()
                    .min(Comparator.comparingDouble(pt -> pt.squaredDistanceTo(centroid)))
                    .get().subtract(eye);
            }
            if (throughWalls) {
                List<Vec3d> through = new ArrayList<>();
                for (Vec3d p2 : pts) {
                    Vec3d delta2 = p2.subtract(eye);
                    double len2 = delta2.length();
                    double limit2 = reach + pad;
                    if (len2 <= limit2) {
                        float traceDist2 = (float) limit2;
                        if (canAttack(eye,
                            (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(delta2.z, delta2.x)) - 90.0),
                            (float) (-Math.toDegrees(Math.atan2(delta2.y, Math.hypot(delta2.x, delta2.z)))),
                            traceDist2, target, true)) {
                            through.add(p2);
                        }
                    }
                }
                if (!through.isEmpty()) {
                    Vec3d centroid2 = through.stream().reduce(Vec3d.ZERO, Vec3d::add)
                        .multiply(1.0 / through.size());
                    return through.stream()
                        .min(Comparator.comparingDouble(pt2 -> pt2.squaredDistanceTo(centroid2)))
                        .get().subtract(eye);
                }
            }
        }
        return Vec3d.ZERO;
    }

    public static boolean shouldAttackWithCooldown(int attackCooldown, LivingEntity target, boolean checks) {
        if (!checks && attackCooldown >= 7 && isInReach(target, 3.0)
            && mc.player.getAttackCooldownProgress(0.5f) > 0.7f) {
            return canCrit();
        }
        return false;
    }
}
