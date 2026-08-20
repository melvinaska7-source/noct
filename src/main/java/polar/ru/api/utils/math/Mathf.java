package polar.ru.api.utils.math;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.MathHelper;

public final class Mathf {
    public static float clamp01(float x2) {
        return (float)Mathf.clamp(0.0, 1.0, x2);
    }

    public static double getRandom(double min, double max) {
        if (min == max) {
            return min;
        }
        if (min > max) {
            double d2 = min;
            min = max;
            max = d2;
        }
        return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
    }

    public static float calculateDelta(float a2, float b2) {
        return a2 - b2;
    }

    public static double round(double target, int decimal) {
        double p2 = Math.pow(10.0, decimal);
        return (double)Math.round(target * p2) / p2;
    }

    public static Number round(double num, double increment) {
        if (increment <= 0.0) {
            throw new IllegalArgumentException("Increment must be greater than zero");
        }
        double roundedValue = (double)Math.round(num / increment) * increment;
        BigDecimal bigDecimal = BigDecimal.valueOf(roundedValue);
        bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }

    public static String formatTime(long millis) {
        long hours = millis / 3600000L;
        long minutes = millis % 3600000L / 60000L;
        long seconds = millis % 360000L % 60000L / 1000L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static float slerp(float start, float end, float t2) {
        t2 = Math.max(0.0f, Math.min(1.0f, t2));
        float startRadians = (float)Math.toRadians(start);
        float endRadians = (float)Math.toRadians(end);
        float dotProduct = (float)Math.cos(startRadians) * (float)Math.cos(endRadians) + (float)Math.sin(startRadians) * (float)Math.sin(endRadians);
        float angle = (float)Math.acos(dotProduct);
        if (Math.abs(angle) < 0.001f) {
            return start;
        }
        float factorStart = (float)(Math.sin((1.0f - t2) * angle) / Math.sin(angle));
        float factorEnd = (float)(Math.sin(t2 * angle) / Math.sin(angle));
        float interpolatedValue = start * factorStart + end * factorEnd;
        return (float)MathHelper.clamp((double)MathHelper.wrapDegrees((double)Math.toDegrees(interpolatedValue)), (double)start, (double)end);
    }

    public static double round(double value, int scale, double inc) {
        double halfOfInc = inc / 2.0;
        double floored = Math.floor(value / inc) * inc;
        if (value >= floored + halfOfInc) {
            return new BigDecimal(Math.ceil(value / inc) * inc).setScale(scale, RoundingMode.HALF_UP).doubleValue();
        }
        return new BigDecimal(floored).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    public static double step(double value, double steps) {
        double a2 = (double)Math.round(value / steps) * steps;
        a2 *= 1000.0;
        a2 = (int)a2;
        return a2 /= 1000.0;
    }

    public static double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        double deltaZ = z2 - z1;
        return MathHelper.sqrt((float)((float)(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)));
    }

    public static double clamp(double min, double max, double n2) {
        return Math.max(min, Math.min(max, n2));
    }

    public static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }

    public static float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    public static double interporate(double p_219803_0_, double p_219803_2_, double p_219803_4_) {
        return p_219803_2_ + p_219803_0_ * (p_219803_4_ - p_219803_2_);
    }

    public static float lerp(float min, float max, float delta) {
        return min + (max - min) * delta;
    }

    public static float easeOutExpo(float x2) {
        return x2 == 1.0f ? 1.0f : (float)(1.0 - Math.pow(2.0, -10.0f * x2));
    }
    private Mathf() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

