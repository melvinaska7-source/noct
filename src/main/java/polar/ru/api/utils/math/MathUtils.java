package polar.ru.api.utils.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.utils.math.FastRandom;

public class MathUtils
implements QClient {
    public static FastRandom fastRandomize = new FastRandom();

    public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0.0) {
            rotationYaw += 180.0f;
        }
        float forward = 1.0f;
        if (moveForward < 0.0) {
            forward = -0.5f;
        } else if (moveForward > 0.0) {
            forward = 0.5f;
        }
        if (moveStrafing > 0.0) {
            rotationYaw -= 90.0f * forward;
        }
        if (moveStrafing < 0.0) {
            rotationYaw += 90.0f * forward;
        }
        return Math.toRadians(rotationYaw);
    }

    public static float randomNew(double min, double max) {
        if (min > max) {
            return (float)((double)fastRandomize.nextFloat() * (min - max) + max);
        }
        return (float)((double)fastRandomize.nextFloat() * (max - min) + min);
    }

    public static double getBps(Entity player) {
        double dx = player.getX() - player.prevX;
        double dy = player.getY() - player.prevY;
        double dz = player.getZ() - player.prevZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance * 20.0;
    }

    public static float calculateBPS() {
        if (MathUtils.mc.player == null) {
            return 0.0f;
        }
        double dx = MathUtils.mc.player.getX() - MathUtils.mc.player.prevX;
        double dy = MathUtils.mc.player.getY() - MathUtils.mc.player.prevY;
        double dz = MathUtils.mc.player.getZ() - MathUtils.mc.player.prevZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float timerSpeed = 1.0f;
        float bps = (float)(distance * (double)timerSpeed * 20.0);
        return (float)Math.round(bps * 10.0f) / 10.0f;
    }

    public static double getTargetCompensatedSpeed(Entity target) {
        double baseSpeed = 1.5;
        if (target == null) {
            return 1.5;
        }
        double targetBps = MathUtils.calculateBPS();
        double speedFactor = 0.00342;
        double bonusSpeed = targetBps * 0.00342;
        return 1.5 + bonusSpeed;
    }

    public static float random(float min, float max) {
        SecureRandom secureRandom = new SecureRandom();
        double randA = secureRandom.nextDouble();
        double randB = secureRandom.nextDouble();
        double randC = secureRandom.nextGaussian() * (double)0.02f;
        double smoothFactor = Math.pow(randA, 1.0 + secureRandom.nextDouble() * 0.7);
        double mixFactor = (randB * 0.8 + 0.1) * (Math.log1p(randA * 3.0) * 0.5 + 0.5);
        return (float)((double)min + (double)(max - min) * smoothFactor * mixFactor + randC);
    }

    public static double randomBest(double min, double max) {
        return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
    }

    public static boolean isHovered(double x2, double y2, double width, double height, double mouseX, double mouseY) {
        return mouseX >= x2 && mouseY >= y2 && mouseX < x2 + width && mouseY < y2 + height;
    }

    public static float interpolate(float prev, float to, float value) {
        return prev + (to - prev) * value;
    }

    public static Vec3d interpolate(Vec3d end, Vec3d start, float multiple) {
        return new Vec3d(MathUtils.interpolate(end.getX(), start.getX(), (double)multiple), MathUtils.interpolate(end.getY(), start.getY(), (double)multiple), MathUtils.interpolate(end.getZ(), start.getZ(), (double)multiple));
    }

    public static Vec3d interpolate(Entity entity, float partialTicks) {
        double posX = MathHelper.lerp((double)partialTicks, (double)entity.prevX, (double)entity.getX());
        double posY = MathHelper.lerp((double)partialTicks, (double)entity.prevY, (double)entity.getY());
        double posZ = MathHelper.lerp((double)partialTicks, (double)entity.prevZ, (double)entity.getZ());
        return new Vec3d(posX, posY, posZ);
    }

    public static double interpolate(double current, double old, double scale) {
        return old + (current - old) * scale;
    }

    public static float round(float number) {
        return (float)Math.round(number * 10.0f) / 10.0f;
    }

    public static double round(double num, double increment) {
        double v2 = (double)Math.round(num / increment) * increment;
        BigDecimal bd2 = new BigDecimal(v2);
        bd2 = bd2.setScale(2, RoundingMode.HALF_UP);
        return bd2.doubleValue();
    }

    public static float lerp(float current, float old, float scale) {
        return current + (old - current) * MathUtils.clamp(scale, 0.0f, 1.0f);
    }

    public static float clamp(float value, float min, float max) {
        if (value <= min) {
            return min;
        }
        return Math.min(value, max);
    }

    public static double clamp(double min, double max, double n2) {
        return Math.max(min, Math.min(max, n2));
    }

    public static <T extends Number> T ler1p(T input, T target, double step) {
        double start = input.doubleValue();
        double end = target.doubleValue();
        double result = start + step * (end - start);
        if (input instanceof Integer) {
            return (T)Integer.valueOf((int)Math.round(result));
        }
        if (input instanceof Double) {
            return (T)Double.valueOf(result);
        }
        if (input instanceof Float) {
            return (T)Float.valueOf((float)result);
        }
        if (input instanceof Long) {
            return (T)Long.valueOf(Math.round(result));
        }
        if (input instanceof Short) {
            return (T)Short.valueOf((short)Math.round(result));
        }
        if (input instanceof Byte) {
            return (T)Byte.valueOf((byte)Math.round(result));
        }
        throw new IllegalArgumentException("Unsupported type: " + input.getClass().getSimpleName());
    }
}

