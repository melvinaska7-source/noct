package polar.ru.api.utils.math;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class FastRandom
extends Random {
    private final ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
    private Random random = null;
    private volatile boolean seedSet;
    private volatile boolean seedUpdated;
    private volatile long seed;

    private void validateRandom() {
        if (this.random == null) {
            this.random = new Random(this.seed);
            this.seedUpdated = false;
        } else if (this.seedUpdated) {
            this.random.setSeed(this.seed);
            this.seedUpdated = false;
        }
    }

    public static long mix(long left, long right) {
        left *= left * 6364136223846793005L + 1442695040888963407L;
        return left + right;
    }

    @Override
    public void setSeed(long seed) {
        this.seed = seed;
        this.seedSet = true;
        this.seedUpdated = true;
    }

    @Override
    public void nextBytes(byte[] bytes) {
        if (this.seedSet) {
            this.validateRandom();
            this.random.nextBytes(bytes);
        } else {
            this.threadLocalRandom.nextBytes(bytes);
        }
    }

    @Override
    public int nextInt() {
        if (this.seedSet) {
            this.validateRandom();
            return this.random.nextInt();
        }
        return this.threadLocalRandom.nextInt();
    }

    @Override
    public int nextInt(int bound) {
        if (this.seedSet) {
            this.validateRandom();
            return this.random.nextInt(bound);
        }
        return this.threadLocalRandom.nextInt(bound);
    }

    @Override
    public long nextLong() {
        if (this.seedSet) {
            this.validateRandom();
            return this.random.nextLong();
        }
        return this.threadLocalRandom.nextLong();
    }

    @Override
    public boolean nextBoolean() {
        if (this.seedSet) {
            this.validateRandom();
            return this.random.nextBoolean();
        }
        return this.threadLocalRandom.nextBoolean();
    }

    @Override
    public float nextFloat() {
        if (this.seedSet) {
            this.validateRandom();
            return this.random.nextFloat();
        }
        return this.threadLocalRandom.nextFloat();
    }

    @Override
    public double nextDouble() {
        if (this.seedSet) {
            this.validateRandom();
            return this.random.nextDouble();
        }
        return this.threadLocalRandom.nextDouble();
    }

    @Override
    public double nextGaussian() {
        if (this.seedSet) {
            this.validateRandom();
            return this.random.nextGaussian();
        }
        return this.threadLocalRandom.nextGaussian();
    }
}

