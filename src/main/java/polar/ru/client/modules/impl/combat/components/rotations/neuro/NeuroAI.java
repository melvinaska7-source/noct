package polar.ru.client.modules.impl.combat.components.rotations.neuro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.QClient;
import polar.ru.client.modules.impl.combat.components.rotations.neuro.NeuroPatternData;

public class NeuroAI
implements QClient {
    private final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    private NeuroPatternData currentPattern;
    private List<NeuroPatternData.RotationSnapshot> learningData;
    private Map<String, List<NeuroPatternData.RotationSnapshot>> contextMap;
    private String currentContext = "idle";
    private float lastContextSwitch = 0.0f;
    private float adaptationFactor = 1.0f;
    private float randomnessFactor = 0.15f;

    public void loadPattern(NeuroPatternData pattern) {
        if (pattern == null) {
            return;
        }
        this.currentPattern = pattern;
        this.learningData = new ArrayList<NeuroPatternData.RotationSnapshot>(pattern.getSnapshots());
        this.contextMap = new HashMap<String, List<NeuroPatternData.RotationSnapshot>>();
        for (NeuroPatternData.RotationSnapshot snapshot : this.learningData) {
            this.contextMap.computeIfAbsent(snapshot.getContext(), k2 -> new ArrayList()).add(snapshot);
        }
    }

    public boolean hasPattern() {
        return this.currentPattern != null && this.learningData != null && !this.learningData.isEmpty();
    }

    public String getPatternName() {
        return this.currentPattern != null ? this.currentPattern.getName() : "none";
    }

    public void clearPattern() {
        this.currentPattern = null;
        this.learningData = null;
        this.contextMap = null;
        this.currentContext = "idle";
    }

    public float[] getNextRotation(LivingEntity target, float currentYaw, float currentPitch, boolean isAttacking, float distanceToTarget) {
        if (!this.hasPattern()) {
            return new float[]{currentYaw, currentPitch};
        }
        try {
            String newContext = this.determineContext(target, isAttacking, distanceToTarget);
            if (!newContext.equals(this.currentContext)) {
                this.currentContext = newContext;
                this.lastContextSwitch = 0.0f;
            }
            this.lastContextSwitch += 1.0f;
            List<NeuroPatternData.RotationSnapshot> contextSnapshots = this.contextMap.getOrDefault(this.currentContext, this.learningData);
            if (contextSnapshots.isEmpty()) {
                contextSnapshots = this.learningData;
            }
            int sampleSize = Math.min(5, contextSnapshots.size());
            List<NeuroPatternData.RotationSnapshot> samples = this.getRandomSamples(contextSnapshots, sampleSize);
            float avgDeltaYaw = 0.0f;
            float avgDeltaPitch = 0.0f;
            for (NeuroPatternData.RotationSnapshot sample : samples) {
                avgDeltaYaw += sample.getDeltaYaw();
                avgDeltaPitch += sample.getDeltaPitch();
            }
            float adaptedDeltaYaw = (avgDeltaYaw /= (float)sampleSize) * this.adaptationFactor;
            float adaptedDeltaPitch = (avgDeltaPitch /= (float)sampleSize) * this.adaptationFactor;
            float newYaw = currentYaw + (adaptedDeltaYaw += (this.rnd.nextFloat() - 0.5f) * this.randomnessFactor * 2.0f);
            float newPitch = currentPitch + (adaptedDeltaPitch += (this.rnd.nextFloat() - 0.5f) * this.randomnessFactor * 1.5f);
            newPitch = MathHelper.clamp((float)newPitch, (float)-89.0f, (float)89.0f);
            return new float[]{newYaw, newPitch};
        }
        catch (Exception e2) {
            e2.printStackTrace();
            return new float[]{currentYaw, currentPitch};
        }
    }

    private String determineContext(LivingEntity target, boolean isAttacking, float distanceToTarget) {
        if (target == null) {
            return "idle";
        }
        if (isAttacking) {
            if (distanceToTarget < 0.5f) {
                return "tracking";
            }
            return "aiming";
        }
        return this.rnd.nextFloat() < 0.7f ? "aiming" : "jerking";
    }

    private List<NeuroPatternData.RotationSnapshot> getRandomSamples(List<NeuroPatternData.RotationSnapshot> source, int count) {
        if (source.size() <= count) {
            return new ArrayList<NeuroPatternData.RotationSnapshot>(source);
        }
        ArrayList<NeuroPatternData.RotationSnapshot> samples = new ArrayList<NeuroPatternData.RotationSnapshot>();
        HashSet<Integer> usedIndices = new HashSet<Integer>();
        while (samples.size() < count) {
            int index = this.rnd.nextInt(source.size());
            if (!usedIndices.add(index)) continue;
            samples.add(source.get(index));
        }
        return samples;
    }

    public float[] getPredictedRotation(float currentYaw, float currentPitch, Queue<float[]> recentMovements, int lookback) {
        if (!this.hasPattern() || recentMovements.size() < lookback) {
            return this.getNextRotation(null, currentYaw, currentPitch, false, 999.0f);
        }
        float[] totalDelta = new float[]{0.0f, 0.0f};
        int count = 0;
        for (float[] movement : recentMovements) {
            totalDelta[0] = totalDelta[0] + movement[0];
            totalDelta[1] = totalDelta[1] + movement[1];
            ++count;
        }
        float avgRecentDeltaYaw = totalDelta[0] / (float)count;
        float avgRecentDeltaPitch = totalDelta[1] / (float)count;
        List<NeuroPatternData.RotationSnapshot> similarPatterns = this.findSimilarPatterns(avgRecentDeltaYaw, avgRecentDeltaPitch);
        if (similarPatterns.isEmpty()) {
            return new float[]{currentYaw, currentPitch};
        }
        float predictedDeltaYaw = 0.0f;
        float predictedDeltaPitch = 0.0f;
        for (NeuroPatternData.RotationSnapshot snapshot : similarPatterns) {
            predictedDeltaYaw += snapshot.getDeltaYaw();
            predictedDeltaPitch += snapshot.getDeltaPitch();
        }
        predictedDeltaYaw /= (float)similarPatterns.size();
        predictedDeltaPitch /= (float)similarPatterns.size();
        float newYaw = currentYaw + (predictedDeltaYaw += (this.rnd.nextFloat() - 0.5f) * this.randomnessFactor);
        float newPitch = MathHelper.clamp((float)(currentPitch + (predictedDeltaPitch += (this.rnd.nextFloat() - 0.5f) * this.randomnessFactor * 0.8f)), (float)-89.0f, (float)89.0f);
        return new float[]{newYaw, newPitch};
    }

    private List<NeuroPatternData.RotationSnapshot> findSimilarPatterns(float targetDeltaYaw, float targetDeltaPitch) {
        ArrayList<NeuroPatternData.RotationSnapshot> similar = new ArrayList<NeuroPatternData.RotationSnapshot>();
        float threshold = 3.0f;
        for (NeuroPatternData.RotationSnapshot snapshot : this.learningData) {
            float diffYaw = Math.abs(snapshot.getDeltaYaw() - targetDeltaYaw);
            float diffPitch = Math.abs(snapshot.getDeltaPitch() - targetDeltaPitch);
            if (!(diffYaw < threshold) || !(diffPitch < threshold)) continue;
            similar.add(snapshot);
        }
        if (similar.isEmpty() && !this.learningData.isEmpty()) {
            return this.getRandomSamples(this.learningData, Math.min(3, this.learningData.size()));
        }
        return similar;
    }

    public void setAdaptationFactor(float factor) {
        this.adaptationFactor = MathHelper.clamp((float)factor, (float)0.1f, (float)2.0f);
    }

    public void setRandomnessFactor(float factor) {
        this.randomnessFactor = MathHelper.clamp((float)factor, (float)0.0f, (float)1.0f);
    }

    public String getContextStats() {
        if (this.contextMap == null || this.contextMap.isEmpty()) {
            return "No data";
        }
        StringBuilder stats = new StringBuilder();
        for (Map.Entry<String, List<NeuroPatternData.RotationSnapshot>> entry : this.contextMap.entrySet()) {
            stats.append(entry.getKey()).append(": ").append(entry.getValue().size()).append(" | ");
        }
        return stats.toString();
    }
}

