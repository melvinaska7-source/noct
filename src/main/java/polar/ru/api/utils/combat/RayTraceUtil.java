package polar.ru.api.utils.combat;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;
import polar.ru.api.QClient;
import polar.ru.api.utils.combat.PredictUtils;
import polar.ru.client.modules.impl.combat.ElytraTarget;

public final class RayTraceUtil
implements QClient {
    public static HitResult rayTrace(double rayTraceDistance, float yaw, float pitch, Entity entity) {
        Vec3d startVec = RayTraceUtil.mc.player.getEyePos();
        Vec3d directionVec = RayTraceUtil.getVectorForRotation(pitch, yaw);
        Vec3d endVec = startVec.add(directionVec.x * rayTraceDistance, directionVec.y * rayTraceDistance, directionVec.z * rayTraceDistance);
        return RayTraceUtil.mc.world.raycast(new RaycastContext(startVec, endVec, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, entity));
    }

    public static BlockHitResult raycast(Vec3d start, Vec3d end, RaycastContext.ShapeType shapeType) {
        return RayTraceUtil.raycast(start, end, shapeType, (Entity)RayTraceUtil.mc.player);
    }

    public static BlockHitResult raycast(Vec3d start, Vec3d end, RaycastContext.ShapeType shapeType, Entity entity) {
        return RayTraceUtil.mc.world.raycast(new RaycastContext(start, end, shapeType, RaycastContext.FluidHandling.NONE, entity));
    }

    public static boolean rayTrace(Vec3d clientVec, double range, Box box) {
        Vec3d cameraVec = Objects.requireNonNull(RayTraceUtil.mc.player).getEyePos();
        return box.contains(cameraVec) || box.raycast(cameraVec, cameraVec.add(clientVec.multiply(range))).isPresent();
    }

    public static boolean isViewEntity(LivingEntity target, float yaw, float pitch, float distance, boolean ignoreWalls) {
        if (target == null) {
            return false;
        }
        if (RayTraceUtil.mc.player != null && (RayTraceUtil.mc.player.isGliding() || target.isGliding())) {
            return RayTraceUtil.rayTraceEntity(yaw, pitch, distance, (Entity)target, ignoreWalls);
        }
        Entity entity = mc.getCameraEntity();
        if (entity == null || RayTraceUtil.mc.world == null) {
            return false;
        }
        double reachDistanceSquared = distance * distance;
        Vec3d startVec = entity.getEyePos();
        Vector3f directionVec = RayTraceUtil.calculateViewVector(yaw, pitch);
        directionVec.mul(distance, distance, distance);
        Vec3d endVec = startVec.add((double)directionVec.x, (double)directionVec.y, (double)directionVec.z);
        Box aabb = target.getBoundingBox();
        EntityHitResult result = ProjectileUtil.raycast((Entity)entity, (Vec3d)startVec, (Vec3d)endVec, (Box)aabb, entityIn -> !entityIn.isSpectator() && entityIn.isAlive() && entityIn == target, (double)reachDistanceSquared);
        return result != null;
    }

    public static boolean rayTraceEntity(float yaw, float pitch, double distance, Entity entity, boolean raytraceBlock) {
        if (RayTraceUtil.mc.player == null || RayTraceUtil.mc.world == null || entity == null) {
            return false;
        }
        Vec3d eyeVec = RayTraceUtil.mc.player.getEyePos();
        Vec3d lookVec = RayTraceUtil.getVectorForRotation(pitch, yaw).normalize();
        Vec3d endVec = eyeVec.add(lookVec.multiply(distance));
        RayHit hit = RayTraceUtil.traceBox(RayTraceUtil.getStrictBox(entity), eyeVec, endVec);
        if (!hit.hit && RayTraceUtil.shouldUseElytraTrace(entity)) {
            hit = RayTraceUtil.traceElytraBox(entity, eyeVec, endVec);
        }
        if (!hit.hit) {
            return false;
        }
        return !raytraceBlock || RayTraceUtil.canSeeHitPoint(eyeVec, hit.point);
    }

    public static Vector3f calculateViewVector(float yaw, float pitch) {
        float pitchRad = pitch * ((float)Math.PI / 180);
        float yawRad = -yaw * ((float)Math.PI / 180);
        float cosYaw = MathHelper.cos((float)yawRad);
        float sinYaw = MathHelper.sin((float)yawRad);
        float cosPitch = MathHelper.cos((float)pitchRad);
        float sinPitch = MathHelper.sin((float)pitchRad);
        return new Vector3f(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }

    public static Vec3d getVectorForRotation(float pitch, float yaw) {
        float yawRadians = -yaw * ((float)Math.PI / 180) - (float)Math.PI;
        float pitchRadians = -pitch * ((float)Math.PI / 180);
        float cosYaw = MathHelper.cos((float)yawRadians);
        float sinYaw = MathHelper.sin((float)yawRadians);
        float cosPitch = -MathHelper.cos((float)pitchRadians);
        float sinPitch = MathHelper.sin((float)pitchRadians);
        return new Vec3d((double)(sinYaw * cosPitch), (double)sinPitch, (double)(cosYaw * cosPitch));
    }

    public static boolean rayTraceSingleEntity(float yaw, float pitch, double distance, Entity entity) {
        Vec3d eyeVec = RayTraceUtil.mc.player.getEyePos();
        Vec3d lookVec = RayTraceUtil.mc.player.getRotationVector(pitch, yaw);
        Vec3d extendedVec = eyeVec.add(lookVec.multiply(distance));
        Box aabb = entity.getBoundingBox();
        return aabb.contains(eyeVec) || aabb.raycast(eyeVec, extendedVec).isPresent();
    }

    private static Box getStrictBox(Entity entity) {
        return entity.getBoundingBox();
    }

    private static boolean shouldUseElytraTrace(Entity entity) {
        LivingEntity livingEntity;
        return RayTraceUtil.mc.player.isGliding() || entity instanceof LivingEntity && (livingEntity = (LivingEntity)entity).isGliding();
    }

    private static RayHit traceElytraBox(Entity entity, Vec3d eyeVec, Vec3d endVec) {
        Box baseBox = RayTraceUtil.getStrictBox(entity);
        Box sweptBox = RayTraceUtil.buildElytraSweptBox(entity, baseBox);
        RayHit boxHit = RayTraceUtil.traceBox(sweptBox, eyeVec, endVec);
        if (boxHit.hit) {
            return boxHit;
        }
        return RayTraceUtil.traceElytraCorridor(entity, baseBox, eyeVec, endVec);
    }

    private static Box buildElytraSweptBox(Entity entity, Box baseBox) {
        LivingEntity livingEntity;
        Vec3d playerMotion = RayTraceUtil.mc.player.getVelocity();
        Vec3d entityMotion = entity.getVelocity();
        Vec3d relativeMotion = entityMotion.subtract(playerMotion);
        double entityHorizontalSpeed = Math.hypot(entityMotion.x, entityMotion.z);
        double playerHorizontalSpeed = Math.hypot(playerMotion.x, playerMotion.z);
        double relativeHorizontalSpeed = Math.hypot(relativeMotion.x, relativeMotion.z);
        double predictTicks = MathHelper.clamp((double)(1.25 + entityHorizontalSpeed * 1.15 + playerHorizontalSpeed * 0.35), (double)1.25, (double)4.25);
        Box sweptBox = RayTraceUtil.union(baseBox, baseBox.offset(entity.prevX - entity.getX(), entity.prevY - entity.getY(), entity.prevZ - entity.getZ()));
        for (int i2 = 1; i2 <= 4; ++i2) {
            double scale = predictTicks * (double)i2 / 4.0;
            sweptBox = RayTraceUtil.union(sweptBox, baseBox.offset(entityMotion.x * scale, entityMotion.y * scale, entityMotion.z * scale));
        }
        Vec3d forward = RayTraceUtil.getEntityForward(entity);
        if (forward.lengthSquared() > 1.0E-4) {
            double forwardPredict = RayTraceUtil.getElytraForwardTraceDistance(entity, 1.2 + entityHorizontalSpeed * 2.4 + playerHorizontalSpeed * 0.45);
            sweptBox = RayTraceUtil.union(sweptBox, baseBox.offset(forward.normalize().multiply(forwardPredict)));
        }
        double growXZ = MathHelper.clamp((double)(0.12 + relativeHorizontalSpeed * 0.85 + (RayTraceUtil.mc.player.isGliding() ? 0.22 : 0.0)), (double)0.18, (double)1.35);
        double growY = MathHelper.clamp((double)(0.1 + Math.abs(relativeMotion.y) * 1.15 + (entity instanceof LivingEntity && (livingEntity = (LivingEntity)entity).isGliding() ? 0.18 : 0.0)), (double)0.14, (double)0.9);
        return sweptBox.expand(growXZ, growY, growXZ);
    }

    private static RayHit traceElytraCorridor(Entity entity, Box baseBox, Vec3d eyeVec, Vec3d endVec) {
        Vec3d VanillaChestLootTableGenerator;
        Vec3d forward = RayTraceUtil.getEntityForward(entity);
        if (forward.lengthSquared() <= 1.0E-4) {
            return RayHit.MISS;
        }
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            VanillaChestLootTableGenerator = livingEntity.getEyePos();
        } else {
            VanillaChestLootTableGenerator = baseBox.getCenter();
        }
        Vec3d anchor = VanillaChestLootTableGenerator;
        Vec3d corridorEnd = anchor.add(forward.normalize().multiply(RayTraceUtil.getElytraForwardTraceDistance(entity, 6.0)));
        SegmentHit segmentHit = RayTraceUtil.closestSegmentHit(eyeVec, endVec, anchor, corridorEnd);
        double boxLengthX = baseBox.maxX - baseBox.minX;
        double boxLengthY = baseBox.maxY - baseBox.minY;
        double boxLengthZ = baseBox.maxZ - baseBox.minZ;
        double radius = MathHelper.clamp((double)(Math.max(boxLengthX, boxLengthZ) * 0.5 + 0.75), (double)0.95, (double)1.65);
        double verticalRadius = MathHelper.clamp((double)(boxLengthY * 0.5 + 0.35), (double)0.95, (double)1.45);
        if (segmentHit.distance <= radius && Math.abs(segmentHit.targetPoint.y - segmentHit.rayPoint.y) <= verticalRadius) {
            return new RayHit(true, segmentHit.rayPoint);
        }
        return RayHit.MISS;
    }

    private static double getElytraForwardTraceDistance(Entity entity, double fallback) {
        double maxDistance = 14.0;
        double distance = MathHelper.clamp((double)fallback, (double)1.2, (double)maxDistance);
        ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
        if (elytraTarget != null && elytraTarget.isAuraActive() && entity instanceof LivingEntity) {
            distance = Math.max(distance, MathHelper.clamp((double)(elytraTarget.forwardValue.getValue().doubleValue() + 3.0), (double)3.0, (double)maxDistance));
        }
        return distance;
    }

    private static Vec3d getEntityForward(Entity entity) {
        Vec3d resolvedForward;
        LivingEntity livingEntity;
        PredictUtils.PositionData data;
        if (entity instanceof LivingEntity && (data = PredictUtils.getData(livingEntity = (LivingEntity)entity)) != null && (resolvedForward = data.getResolvedForward()).lengthSquared() > 1.0E-4) {
            return resolvedForward;
        }
        Vec3d motion = entity.getVelocity();
        Vec3d horizontalMotion = new Vec3d(motion.x, 0.0, motion.z);
        if (horizontalMotion.lengthSquared() > 1.0E-4) {
            return horizontalMotion;
        }
        return entity.getRotationVector();
    }

    private static RayHit traceBox(Box box, Vec3d eyeVec, Vec3d endVec) {
        if (box.contains(eyeVec)) {
            return new RayHit(true, eyeVec);
        }
        Optional hit = box.raycast(eyeVec, endVec);
        return (RayHit)hit.map(vec3d -> new RayHit(true, (Vec3d)vec3d)).orElse(RayHit.MISS);
    }

    private static boolean canSeeHitPoint(Vec3d eyeVec, Vec3d hitPoint) {
        RaycastContext context = new RaycastContext(eyeVec, hitPoint, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, (Entity)RayTraceUtil.mc.player);
        return RayTraceUtil.mc.world.raycast(context).getType() == HitResult.Type.MISS;
    }

    private static SegmentHit closestSegmentHit(Vec3d a0, Vec3d a1, Vec3d b0, Vec3d b1) {
        double tc;
        double sc;
        Vec3d u2 = a1.subtract(a0);
        Vec3d v2 = b1.subtract(b0);
        Vec3d w2 = a0.subtract(b0);
        double a2 = u2.dotProduct(u2);
        double b2 = u2.dotProduct(v2);
        double c2 = v2.dotProduct(v2);
        double d2 = u2.dotProduct(w2);
        double e2 = v2.dotProduct(w2);
        double denominator = a2 * c2 - b2 * b2;
        if (denominator < 1.0E-7) {
            sc = 0.0;
            tc = c2 > 1.0E-7 ? MathHelper.clamp((double)(e2 / c2), (double)0.0, (double)1.0) : 0.0;
        } else {
            sc = MathHelper.clamp((double)((b2 * e2 - c2 * d2) / denominator), (double)0.0, (double)1.0);
            tc = c2 > 1.0E-7 ? MathHelper.clamp((double)((a2 * e2 - b2 * d2) / denominator), (double)0.0, (double)1.0) : 0.0;
        }
        Vec3d rayPoint = a0.add(u2.multiply(sc));
        Vec3d targetPoint = b0.add(v2.multiply(tc));
        return new SegmentHit(rayPoint.distanceTo(targetPoint), rayPoint, targetPoint);
    }

    private static Box union(Box first, Box second) {
        return new Box(Math.min(first.minX, second.minX), Math.min(first.minY, second.minY), Math.min(first.minZ, second.minZ), Math.max(first.maxX, second.maxX), Math.max(first.maxY, second.maxY), Math.max(first.maxZ, second.maxZ));
    }
    private RayTraceUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    private static class RayHit {
        private static final RayHit MISS = new RayHit(false, Vec3d.ZERO);
        private final boolean hit;
        private final Vec3d point;

        private RayHit(boolean hit, Vec3d point) {
            this.hit = hit;
            this.point = point;
        }
    }

    private static class SegmentHit {
        private final double distance;
        private final Vec3d rayPoint;
        private final Vec3d targetPoint;

        private SegmentHit(double distance, Vec3d rayPoint, Vec3d targetPoint) {
            this.distance = distance;
            this.rayPoint = rayPoint;
            this.targetPoint = targetPoint;
        }
    }
}

