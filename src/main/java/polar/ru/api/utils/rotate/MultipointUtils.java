package polar.ru.api.utils.rotate;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;

public final class MultipointUtils
implements QClient {
    public static Vec3d getClosestPoint(Entity entity) {
        Vec3d eyePos = MultipointUtils.mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        double step = 0.1;
        Vec3d bestVec = null;
        double closestDistance = Double.MAX_VALUE;
        for (double x2 = box.minX; x2 <= box.maxX; x2 += step) {
            for (double y2 = box.minY; y2 <= box.maxY; y2 += step) {
                for (double z2 = box.minZ; z2 <= box.maxZ; z2 += step) {
                    Vec3d sample = new Vec3d(x2, y2, z2);
                    double dist = eyePos.distanceTo(sample);
                    if (!(dist < closestDistance)) continue;
                    closestDistance = dist;
                    bestVec = sample;
                }
            }
        }
        return bestVec;
    }
    private MultipointUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

