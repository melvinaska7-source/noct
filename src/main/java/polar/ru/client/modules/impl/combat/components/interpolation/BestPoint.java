package polar.ru.client.modules.impl.combat.components.interpolation;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import polar.ru.api.QClient;
import polar.ru.api.utils.combat.RayTraceUtil;
import polar.ru.api.utils.math.MathUtils;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;

public final class BestPoint
implements QClient {
    private static Vec3d rotationPoint = Vec3d.ZERO;
    private static Vec3d rotationMotion = Vec3d.ZERO;

    public static Vec3d getRotationPoint() {
        return rotationPoint;
    }

    public static Vec3d getNearestPoint(Entity entity) {
        Box box = entity.getBoundingBox();
        double step = 0.085;
        Vec3d bestVec = null;
        double closestDistance = Double.MAX_VALUE;
        for (double x2 = box.minX; x2 <= box.maxX; x2 += step) {
            for (double y2 = box.minY; y2 <= box.maxY; y2 += step) {
                for (double z2 = box.minZ; z2 <= box.maxZ; z2 += step) {
                    Vec3d sample = new Vec3d(x2, y2, z2);
                    double dist = BestPoint.mc.player.getEyePos().distanceTo(sample);
                    if (!(dist < closestDistance)) continue;
                    closestDistance = dist;
                    bestVec = sample;
                }
            }
        }
        return bestVec;
    }

    public static Vec3d getPoint(Entity target) {
        Box box = target.getBoundingBox();
        double width = box.maxX - box.minX;
        double height = box.maxY - box.minY;
        double depth = box.maxZ - box.minZ;
        double baseX = box.minX + width / 2.0;
        double baseY = box.minY + height * 0.72;
        double baseZ = box.minZ + depth / 2.0;
        double time = (double)System.currentTimeMillis() / 42.15;
        int id = target.getId();
        double noise = ThreadLocalRandom.current().nextDouble(-0.015, 0.015);
        double offsetX = Math.sin(time * 0.9 + (double)id) * (width * 0.42) + noise;
        double offsetY = Math.cos(time * 0.75 + (double)id) * (height * 0.12);
        double offsetZ = Math.cos(time * 1.15 + (double)id) * (depth * 0.42) + noise;
        return new Vec3d(baseX + offsetX, baseY + offsetY, baseZ + offsetZ);
    }

    public static Vec3d getPoint2(Entity target) {
        Box box = target.getBoundingBox();
        double width = box.maxX - box.minX;
        double height = box.maxY - box.minY;
        double depth = box.maxZ - box.minZ;
        double baseX = box.minX + width / 2.0;
        double baseY = box.minY + height * 0.62;
        double baseZ = box.minZ + depth / 2.0;
        double time = (double)System.currentTimeMillis() / 58.4;
        int id = target.getId();
        double offsetX = Math.sin(time * 1.1 + (double)id) * (width * 0.65);
        double offsetY = Math.cos(time * 0.95 + (double)id) * (height * 0.35);
        double offsetZ = Math.cos(time * 1.35 + (double)id) * (depth * 0.65);
        return new Vec3d(baseX + offsetX, baseY + offsetY, baseZ + offsetZ);
    }

    public static Vec3d getNearestVisiblePoint(Entity target, Vec3d preferredPoint, double range) {
        if (preferredPoint == null || BestPoint.mc.player == null || BestPoint.mc.world == null) {
            return preferredPoint;
        }
        if (BestPoint.isPointVisible(target, preferredPoint, range)) {
            return preferredPoint;
        }
        Box box = target.getBoundingBox();
        double step = 0.11;
        Vec3d bestPoint = null;
        double bestDistance = Double.MAX_VALUE;
        for (double x2 = box.minX; x2 <= box.maxX; x2 += step) {
            for (double y2 = box.minY; y2 <= box.maxY; y2 += step) {
                for (double z2 = box.minZ; z2 <= box.maxZ; z2 += step) {
                    double distanceToCurrent;
                    Vec3d sample = new Vec3d(x2, y2, z2);
                    if (!BestPoint.isPointVisible(target, sample, range) || !((distanceToCurrent = sample.squaredDistanceTo(preferredPoint)) < bestDistance)) continue;
                    bestDistance = distanceToCurrent;
                    bestPoint = sample;
                }
            }
        }
        return bestPoint != null ? bestPoint : preferredPoint;
    }

    private static boolean isPointVisible(Entity target, Vec3d point, double range) {
        Vec3d eyePos = BestPoint.mc.player.getEyePos();
        double distance = eyePos.distanceTo(point);
        if (distance > range) {
            return false;
        }
        Vec3d direction = point.subtract(eyePos).normalize();
        if (!RayTraceUtil.rayTrace(direction, distance + 0.15, target.getBoundingBox())) {
            return false;
        }
        BlockHitResult blockHit = RayTraceUtil.raycast(eyePos, point, RaycastContext.ShapeType.COLLIDER, (Entity)BestPoint.mc.player);
        return blockHit.getType() == HitResult.Type.MISS || eyePos.squaredDistanceTo(blockHit.getPos()) >= eyePos.squaredDistanceTo(point) - 1.0E-4;
    }

    public static Vec3d getMultipoint(Entity target, double distance) {
        float minMotionXZ = 0.0062f;
        float maxMotionXZ = 0.0185f;
        float minMotionY = 0.0022f;
        float maxMotionY = 0.0185f;
        double lengthX = target.getBoundingBox().getLengthX();
        double lengthY = target.getBoundingBox().getLengthY();
        double lengthZ = target.getBoundingBox().getLengthZ();
        if (rotationMotion.equals((Object)Vec3d.ZERO)) {
            rotationMotion = new Vec3d(MathUtils.randomBest(-0.025f, 0.025f), MathUtils.randomBest(-0.025f, 0.025f), MathUtils.randomBest(-0.025f, 0.025f));
        }
        if (rotationPoint.equals((Object)Vec3d.ZERO)) {
            rotationPoint = new Vec3d(0.0, lengthY * 0.52, 0.0);
        }
        rotationPoint = rotationPoint.add(rotationMotion);
        double safeX = (lengthX - 0.08) / 2.0;
        double safeZ = (lengthZ - 0.08) / 2.0;
        if (Math.abs(BestPoint.rotationPoint.x) >= safeX) {
            rotationMotion = new Vec3d((double)(BestPoint.rotationPoint.x > 0.0 ? -1 : 1) * MathUtils.randomBest(minMotionXZ, maxMotionXZ), rotationMotion.getY(), rotationMotion.getZ());
        }
        if (BestPoint.rotationPoint.y >= lengthY * 0.82) {
            rotationMotion = new Vec3d(rotationMotion.getX(), -MathUtils.randomBest(minMotionY, maxMotionY), rotationMotion.getZ());
        } else if (BestPoint.rotationPoint.y <= lengthY * 0.22) {
            rotationMotion = new Vec3d(rotationMotion.getX(), MathUtils.randomBest(minMotionY, maxMotionY), rotationMotion.getZ());
        }
        if (Math.abs(BestPoint.rotationPoint.z) >= safeZ) {
            rotationMotion = new Vec3d(rotationMotion.getX(), rotationMotion.getY(), (double)(BestPoint.rotationPoint.z > 0.0 ? -1 : 1) * MathUtils.randomBest(minMotionXZ, maxMotionXZ));
        }
        rotationPoint = rotationPoint.add(MathUtils.randomBest(-0.035f, 0.035f), 0.0, MathUtils.randomBest(-0.035f, 0.035f));
        if (!RayTraceUtil.rayTrace(BestPoint.mc.player.getRotationVector(), distance, target.getBoundingBox())) {
            float halfBox = (float)(lengthX / 2.0) * 0.85f;
            for (float x1 = -halfBox; x1 <= halfBox; x1 += 0.09f) {
                for (float z1 = -halfBox; z1 <= halfBox; z1 += 0.09f) {
                    float y1 = (float)(lengthY * 0.88);
                    while ((double)y1 >= lengthY * 0.25) {
                        Vec3d v1 = new Vec3d(target.getX() + (double)x1, target.getY() + (double)y1, target.getZ() + (double)z1);
                        Rotation rotation = RotationUtils.fromVec3d(v1);
                        if (RayTraceUtil.rayTrace(rotation.toVector(), distance, target.getBoundingBox())) {
                            rotationPoint = new Vec3d((double)x1, (double)y1, (double)z1);
                            return target.getPos().add(rotationPoint);
                        }
                        y1 -= 0.09f;
                    }
                }
            }
        }
        return target.getPos().add(rotationPoint);
    }
    private BestPoint() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

