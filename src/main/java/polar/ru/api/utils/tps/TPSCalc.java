package polar.ru.api.utils.tps;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventPacket;

public class TPSCalc {
    private float TPS = 20.0f;
    private float adjustTicks = 0.0f;
    private long timestamp;
    private long lastPacketTime;
    private static final int SAMPLE_SIZE = 20;
    private final float[] tpsSamples = new float[20];
    private int sampleIndex = 0;

    @EventLink
    public void onPacket(EventPacket e2) {
        if (e2.getType() == EventPacket.Type.RECEIVE && e2.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            this.updateTPS();
        }
    }

    public float getTPS() {
        if (this.lastPacketTime == 0L) {
            return this.TPS;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getNetworkHandler() == null || System.currentTimeMillis() - this.lastPacketTime > 3500L) {
            return 20.0f;
        }
        return this.TPS;
    }

    private void updateTPS() {
        float boundedTPS;
        long now = System.nanoTime();
        this.lastPacketTime = System.currentTimeMillis();
        if (this.timestamp == 0L) {
            this.timestamp = now;
            return;
        }
        long delay = now - this.timestamp;
        this.timestamp = now;
        if (delay <= 0L) {
            return;
        }
        float maxTPS = 20.0f;
        float rawTPS = maxTPS * (1.0E9f / (float)delay);
        this.tpsSamples[this.sampleIndex % 20] = boundedTPS = MathHelper.clamp((float)rawTPS, (float)0.0f, (float)maxTPS);
        ++this.sampleIndex;
        int sampleCount = Math.min(this.sampleIndex, 20);
        float sum = 0.0f;
        for (int i2 = 0; i2 < sampleCount; ++i2) {
            float sample = this.tpsSamples[i2];
            sum += sample;
        }
        this.TPS = (float)this.round(sum / (float)sampleCount);
        this.adjustTicks = this.TPS - maxTPS;
    }

    public double round(double input) {
        return (double)Math.round(input * 10.0) / 10.0;
    }
    public float getAdjustTicks() {
        return this.adjustTicks;
    }
    public long getTimestamp() {
        return this.timestamp;
    }
    public long getLastPacketTime() {
        return this.lastPacketTime;
    }
    public float[] getTpsSamples() {
        return this.tpsSamples;
    }
    public int getSampleIndex() {
        return this.sampleIndex;
    }
}

