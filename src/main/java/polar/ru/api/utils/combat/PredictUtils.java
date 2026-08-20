package polar.ru.api.utils.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.client.modules.impl.combat.ElytraTarget;

public class PredictUtils
implements QClient {
    private static final Map<UUID, PositionData> positionCache = new ConcurrentHashMap<UUID, PositionData>();

    public static void updateEntity(LivingEntity entity) {
        PositionData data = positionCache.computeIfAbsent(entity.getUuid(), k2 -> new PositionData());
        data.update(entity.getX(), entity.getY(), entity.getZ());
    }

    public static PositionData getData(LivingEntity entity) {
        return positionCache.get(entity.getUuid());
    }

    public static Vec3d predict(LivingEntity entity, int ticks, float extraForward, boolean isMeFlying) {
        PositionData data = PredictUtils.getData(entity);
        Vec3d pos = new Vec3d(entity.getX(), entity.getY() + (double)(entity.getStandingEyeHeight() / 2.0f), entity.getZ());
        if (data == null) {
            return PredictUtils.predictElytraPhysics(entity, pos, ticks);
        }
        Vec3d forward = data.getResolvedForward();
        double speed = data.getLastSpeed();
        boolean isHighSpeed = data.isSpeedChanged();
        if (entity.isGliding()) {
            double horizontalSpeed = Math.hypot(forward.x, forward.z) * 20.0;
            double verticalSpeed = Math.abs(forward.y) * 20.0;
            if (horizontalSpeed <= 5.0 && verticalSpeed <= 5.0) {
                return pos;
            }
            boolean shouldPredict = isMeFlying && isHighSpeed;
            float predictMultiplier = shouldPredict ? (float)ticks + 2.0f + extraForward : (float)ticks;
            Vec3d linearPredict = pos.add(forward.multiply((double)predictMultiplier, (double)predictMultiplier, (double)predictMultiplier));
            Vec3d physicsPredict = PredictUtils.predictElytraPhysics(entity, pos, ticks);
            double weight = MathHelper.clamp((double)(speed / 50.0), (double)0.3, (double)0.9);
            return new Vec3d(MathHelper.lerp((double)weight, (double)physicsPredict.x, (double)linearPredict.x), MathHelper.lerp((double)weight, (double)physicsPredict.y, (double)linearPredict.y), MathHelper.lerp((double)weight, (double)physicsPredict.z, (double)linearPredict.z));
        }
        if (speed > 1.0) {
            return pos.add(forward.multiply((double)ticks, (double)ticks, (double)ticks));
        }
        return pos;
    }

    public static Vec3d predict(LivingEntity entity, Vec3d pos, int ticks) {
        PositionData data = PredictUtils.getData(entity);
        if (data != null && entity.isGliding()) {
            Vec3d forward = data.getResolvedForward();
            double horizontalSpeed = Math.hypot(forward.x, forward.z) * 20.0;
            double verticalSpeed = Math.abs(forward.y) * 20.0;
            if (horizontalSpeed <= 5.0 && verticalSpeed <= 5.0) {
                return pos;
            }
            return pos.add(forward.multiply((double)ticks, (double)ticks, (double)ticks));
        }
        return PredictUtils.predictElytraPhysics(entity, pos, ticks);
    }

    public static Vec3d predictElytraPhysics(LivingEntity entity, Vec3d pos, int ticks) {
        Vec3d velocity = entity.getVelocity();
        if (!entity.isGliding()) {
            return pos.add(velocity.multiply((double)ticks, (double)ticks, (double)ticks));
        }
        double horizontalDelta = Math.hypot(entity.prevX - entity.getX(), entity.prevZ - entity.getZ()) * 20.0;
        double verticalDelta = Math.abs(entity.getY() - entity.prevY) * 20.0;
        if (horizontalDelta <= 5.0 && verticalDelta <= 5.0) {
            return pos;
        }
        for (int i2 = 0; i2 < ticks; ++i2) {
            double lift;
            Vec3d rotation = entity.getRotationVector();
            float pitchRad = (float)Math.toRadians(entity.getPitch());
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            double velocityLength = velocity.length();
            float cos = MathHelper.cos((float)pitchRad);
            cos = (float)((double)(cos * cos) * Math.min(1.0, rotation.length() / 0.4));
            velocity = velocity.add(0.0, -0.08 * (-1.0 + (double)cos * 0.75), 0.0);
            if (velocity.y < 0.0 && horizontalSpeed > 0.0) {
                lift = velocity.y * -0.1 * (double)cos;
                velocity = velocity.add(rotation.x * lift / horizontalSpeed, lift, rotation.z * lift / horizontalSpeed);
            }
            if (pitchRad < 0.0f && horizontalSpeed > 0.0) {
                lift = velocityLength * (double)(-MathHelper.sin((float)pitchRad)) * 0.04;
                velocity = velocity.add(-rotation.x * lift / horizontalSpeed, lift * 3.2, -rotation.z * lift / horizontalSpeed);
            }
            if (horizontalSpeed > 0.0) {
                velocity = velocity.add((rotation.x / horizontalSpeed * velocityLength - velocity.x) * 0.1, 0.0, (rotation.z / horizontalSpeed * velocityLength - velocity.z) * 0.1);
            }
            velocity = velocity.multiply(0.99, 0.98, 0.99);
            pos = pos.add(velocity);
        }
        return pos;
    }

    public static Vec3d bypasselytrahacking(LivingEntity target, float forwardTicks) {
        Vec3d look = Vec3d.fromPolar((float)target.getPitch(), (float)target.getYaw());
        Vec3d rotation = target.getRotationVector();
        Vec3d deltaToPlayer = target.getPos().add(0.0, (double)(target.getHeight() * 0.6f), 0.0).subtract(PredictUtils.mc.player.getEyePos());
        Vec3d blendedDirection = look.normalize().lerp(rotation, look.length());
        return deltaToPlayer.add(blendedDirection.normalize().multiply((double)forwardTicks));
    }

    public static Vec3d bypasselytrahacking(LivingEntity target) {
        ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
        float forwardTicks = elytraTarget != null ? (float)elytraTarget.getForwardTicks() : 0.0f;
        return PredictUtils.bypasselytrahacking(target, forwardTicks);
    }

    public static void cleanup() {
        long now = System.currentTimeMillis();
        positionCache.entrySet().removeIf(e2 -> now - ((PositionData)e2.getValue()).getLastUpdate() > 10000L);
    }

    public static void clear() {
        positionCache.clear();
    }

    public static class PositionData {
        private double serverX;
        private double serverY;
        private double serverZ;
        private double prevServerX;
        private double prevServerY;
        private double prevServerZ;
        private double backUpX;
        private double backUpY;
        private double backUpZ;
        private double lastSpeed;
        private double prevSpeed;
        private long lastUpdate;

        public Vec3d getResolvedPos() {
            return new Vec3d(this.serverX, this.serverY, this.serverZ);
        }

        public Vec3d getResolvedForward() {
            return new Vec3d(this.serverX - this.prevServerX, this.serverY - this.prevServerY, this.serverZ - this.prevServerZ);
        }

        public void update(double x2, double y2, double z2) {
            this.backUpX = this.prevServerX;
            this.backUpY = this.prevServerY;
            this.backUpZ = this.prevServerZ;
            this.prevServerX = this.serverX;
            this.prevServerY = this.serverY;
            this.prevServerZ = this.serverZ;
            this.serverX = x2;
            this.serverY = y2;
            this.serverZ = z2;
            this.prevSpeed = this.lastSpeed;
            this.lastSpeed = this.getResolvedForward().length() * 20.0;
            this.lastUpdate = System.currentTimeMillis();
        }

        public boolean isSpeedChanged() {
            return this.lastSpeed >= 20.0 || this.lastSpeed != this.prevSpeed && this.lastSpeed == 0.0;
        }
        public double getServerX() {
            return this.serverX;
        }
        public double getServerY() {
            return this.serverY;
        }
        public double getServerZ() {
            return this.serverZ;
        }
        public double getPrevServerX() {
            return this.prevServerX;
        }
        public double getPrevServerY() {
            return this.prevServerY;
        }
        public double getPrevServerZ() {
            return this.prevServerZ;
        }
        public double getBackUpX() {
            return this.backUpX;
        }
        public double getBackUpY() {
            return this.backUpY;
        }
        public double getBackUpZ() {
            return this.backUpZ;
        }
        public double getLastSpeed() {
            return this.lastSpeed;
        }
        public double getPrevSpeed() {
            return this.prevSpeed;
        }
        public long getLastUpdate() {
            return this.lastUpdate;
        }
    }
}

