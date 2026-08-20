package polar.ru.api.utils.math;

import polar.ru.api.QClient;

public class Timer
implements QClient {
    private long startTime = System.currentTimeMillis();
    private long millis;

    public Timer() {
        this.reset();
    }

    public static Timer create() {
        return new Timer();
    }

    public boolean finished(long delay) {
        return System.currentTimeMillis() - delay >= this.millis;
    }

    public void reset() {
        this.millis = System.currentTimeMillis();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.millis;
    }

    public double deltaTime() {
        return mc.getCurrentFps() > 0 ? 1.0 / (double)mc.getCurrentFps() : 1.0;
    }

    public boolean every(long ms) {
        boolean passed;
        boolean bl = passed = this.getMillis(System.nanoTime() - this.millis) >= ms;
        if (passed) {
            this.reset();
        }
        return passed;
    }

    public boolean passed(long time) {
        return System.currentTimeMillis() - this.startTime > time;
    }

    public long getMillis(long time) {
        return time / 1000000L;
    }

    public long getTime() {
        return System.currentTimeMillis() - this.startTime;
    }

    public void setTime(long time) {
        this.startTime = time;
    }
    public long getStartTime() {
        return this.startTime;
    }
    public long getMillis() {
        return this.millis;
    }
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    public void setMillis(long millis) {
        this.millis = millis;
    }
}

