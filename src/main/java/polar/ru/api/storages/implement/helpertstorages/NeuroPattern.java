package polar.ru.api.storages.implement.helpertstorages;

import java.io.Serializable;

public class NeuroPattern
implements Serializable {
    private static final long serialVersionUID = 1L;
    private final float yaw;
    private final float pitch;
    private final float deltaYaw;
    private final float deltaPitch;
    private final double distance;
    private final long timestamp;
    private final boolean isCritical;
    private final double targetSpeed;
    private final String targetType;
    private final float smoothness;

    public NeuroPattern(float yaw, float pitch, float deltaYaw, float deltaPitch, double distance, boolean isCritical, double targetSpeed, String targetType, float smoothness) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
        this.distance = distance;
        this.timestamp = System.currentTimeMillis();
        this.isCritical = isCritical;
        this.targetSpeed = targetSpeed;
        this.targetType = targetType;
        this.smoothness = smoothness;
    }
    public float getYaw() {
        return this.yaw;
    }
    public float getPitch() {
        return this.pitch;
    }
    public float getDeltaYaw() {
        return this.deltaYaw;
    }
    public float getDeltaPitch() {
        return this.deltaPitch;
    }
    public double getDistance() {
        return this.distance;
    }
    public long getTimestamp() {
        return this.timestamp;
    }
    public boolean isCritical() {
        return this.isCritical;
    }
    public double getTargetSpeed() {
        return this.targetSpeed;
    }
    public String getTargetType() {
        return this.targetType;
    }
    public float getSmoothness() {
        return this.smoothness;
    }
    public boolean equals(Object o2) {
        if (o2 == this) {
            return true;
        }
        if (!(o2 instanceof NeuroPattern)) {
            return false;
        }
        NeuroPattern other = (NeuroPattern)o2;
        if (!other.canEqual(this)) {
            return false;
        }
        if (Float.compare(this.getYaw(), other.getYaw()) != 0) {
            return false;
        }
        if (Float.compare(this.getPitch(), other.getPitch()) != 0) {
            return false;
        }
        if (Float.compare(this.getDeltaYaw(), other.getDeltaYaw()) != 0) {
            return false;
        }
        if (Float.compare(this.getDeltaPitch(), other.getDeltaPitch()) != 0) {
            return false;
        }
        if (Double.compare(this.getDistance(), other.getDistance()) != 0) {
            return false;
        }
        if (this.getTimestamp() != other.getTimestamp()) {
            return false;
        }
        if (this.isCritical() != other.isCritical()) {
            return false;
        }
        if (Double.compare(this.getTargetSpeed(), other.getTargetSpeed()) != 0) {
            return false;
        }
        if (Float.compare(this.getSmoothness(), other.getSmoothness()) != 0) {
            return false;
        }
        String this$targetType = this.getTargetType();
        String other$targetType = other.getTargetType();
        return !(this$targetType == null ? other$targetType != null : !this$targetType.equals(other$targetType));
    }
    protected boolean canEqual(Object other) {
        return other instanceof NeuroPattern;
    }
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + Float.floatToIntBits(this.getYaw());
        result = result * 59 + Float.floatToIntBits(this.getPitch());
        result = result * 59 + Float.floatToIntBits(this.getDeltaYaw());
        result = result * 59 + Float.floatToIntBits(this.getDeltaPitch());
        long $distance = Double.doubleToLongBits(this.getDistance());
        result = result * 59 + (int)($distance >>> 32 ^ $distance);
        long $timestamp = this.getTimestamp();
        result = result * 59 + (int)($timestamp >>> 32 ^ $timestamp);
        result = result * 59 + (this.isCritical() ? 79 : 97);
        long $targetSpeed = Double.doubleToLongBits(this.getTargetSpeed());
        result = result * 59 + (int)($targetSpeed >>> 32 ^ $targetSpeed);
        result = result * 59 + Float.floatToIntBits(this.getSmoothness());
        String $targetType = this.getTargetType();
        result = result * 59 + ($targetType == null ? 43 : $targetType.hashCode());
        return result;
    }
    public String toString() {
        return "NeuroPattern(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ", deltaYaw=" + this.getDeltaYaw() + ", deltaPitch=" + this.getDeltaPitch() + ", distance=" + this.getDistance() + ", timestamp=" + this.getTimestamp() + ", isCritical=" + this.isCritical() + ", targetSpeed=" + this.getTargetSpeed() + ", targetType=" + this.getTargetType() + ", smoothness=" + this.getSmoothness() + ")";
    }
}

