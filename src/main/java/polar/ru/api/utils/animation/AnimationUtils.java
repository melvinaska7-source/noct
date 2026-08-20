package polar.ru.api.utils.animation;

import net.minecraft.util.math.MathHelper;
import polar.ru.api.utils.animation.Easing;
import polar.ru.api.utils.animation.Easings;

public class AnimationUtils {
    private float currentValue;
    private float targetValue;
    private float fromValue;
    private float speed;
    private Easing easing;
    private long startTime;
    private double duration;
    private boolean isRunning;

    public AnimationUtils(float initialValue, float speed, Easing easing) {
        this.currentValue = initialValue;
        this.targetValue = initialValue;
        this.fromValue = initialValue;
        this.speed = speed;
        this.easing = easing != null ? easing : Easings.LINEAR;
        this.startTime = 0L;
        this.duration = 0.0;
        this.isRunning = false;
    }

    public AnimationUtils(float initialValue, float speed) {
        this(initialValue, speed, Easings.LINEAR);
    }

    public void update(float target) {
        if (this.targetValue != target) {
            this.targetValue = target;
            this.fromValue = this.currentValue;
            this.startTime = System.nanoTime();
            this.duration = this.speed <= 0.0f ? 0.0 : 1.0 / (double)this.speed;
            this.isRunning = true;
        }
        if (!this.isRunning || this.isDone()) {
            this.currentValue = this.targetValue;
            this.isRunning = false;
            return;
        }
        double part = this.calculatePart();
        float easedPart = (float)this.easing.ease(part);
        this.currentValue = MathHelper.lerp((float)easedPart, (float)this.fromValue, (float)this.targetValue);
    }

    private double calculatePart() {
        if (!this.isRunning) {
            return 1.0;
        }
        long now = System.nanoTime();
        double elapsed = (double)(now - this.startTime) / 1.0E9;
        return MathHelper.clamp((double)(elapsed / this.duration), (double)0.0, (double)1.0);
    }

    public float getValue() {
        return this.currentValue;
    }

    public void setValue(float value) {
        this.currentValue = value;
        this.targetValue = value;
        this.fromValue = value;
        this.isRunning = false;
    }

    public float getTarget() {
        return this.targetValue;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
        this.duration = 1.0 / (double)speed;
    }

    public void setEasing(Easing easing) {
        this.easing = easing != null ? easing : Easings.LINEAR;
    }

    public boolean isDone() {
        return this.calculatePart() >= 1.0;
    }

    public boolean isAlive() {
        return !this.isDone();
    }
}

