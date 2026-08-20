package polar.ru.client.modules.impl.combat.components.rotations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;

public class TestRotation
extends RotationsSystem
implements QClient {
    private static final Path DATASET_PATH = Path.of(System.getProperty("user.home"), "Desktop", "data.json");
    private final List<DatasetFrame> frames = new ArrayList<DatasetFrame>();
    private LivingEntity trackedTarget;
    private LivingEntity trackedRotationTarget;
    private Vec3d currentAimPoint;
    private Vec3d targetAimPoint;
    private long lastModified = Long.MIN_VALUE;
    private long lastLoadAttempt;
    private boolean datasetReady;
    private int playbackIndex;
    private int aimPointTicks;
    private int aimPointRefreshTicks;
    private int smoothProfileTicks;
    private float smoothYawStep;
    private float smoothPitchStep;
    private float smoothYaw;
    private float smoothPitch;
    private float yawSmoothFactor = 1.0f;
    private float pitchSmoothFactor = 1.0f;
    private boolean hasRotationState;

    public void reset() {
        this.trackedTarget = null;
        this.trackedRotationTarget = null;
        this.currentAimPoint = null;
        this.targetAimPoint = null;
        this.playbackIndex = 0;
        this.aimPointTicks = 0;
        this.aimPointRefreshTicks = 0;
        this.smoothProfileTicks = 0;
        this.smoothYawStep = 0.0f;
        this.smoothPitchStep = 0.0f;
        this.smoothYaw = 0.0f;
        this.smoothPitch = 0.0f;
        this.yawSmoothFactor = 1.0f;
        this.pitchSmoothFactor = 1.0f;
        this.hasRotationState = false;
    }

    @Override
    public void updateRotations(LivingEntity target) {
        if (TestRotation.mc.player == null || target == null) {
            return;
        }
        boolean focus = this.shouldFocus();
        this.ensureDatasetLoaded();
        Vec3d aimPoint = this.selectAimPoint(target, focus);
        Vec2f rot = RotationUtils.getRotations(aimPoint);
        if (!this.datasetReady || this.frames.isEmpty()) {
            RotationStorage.update(new Rotation(rot.x, MathHelper.clamp((float)rot.y, (float)-89.0f, (float)89.0f)), 360.0f, 360.0f, 45.0f, 45.0f, 0, 1, Aura.clientLook.isState());
            return;
        }
        float currentYaw = TestRotation.mc.player.getYaw();
        float currentPitch = TestRotation.mc.player.getPitch();
        this.syncRotationState(target, currentYaw, currentPitch);
        float remainingYaw = MathHelper.wrapDegrees((float)(rot.x - this.smoothYaw));
        float remainingPitch = rot.y - this.smoothPitch;
        DatasetFrame frame = this.pickFrame(remainingYaw, remainingPitch, focus);
        this.updateSmoothProfile(frame, remainingYaw, remainingPitch, focus);
        float gcd = Math.max(GCDUtil.getGCDValue(), 1.0E-4f);
        float yawStep = this.buildAxisStep(remainingYaw, frame, true, focus);
        float pitchStep = this.buildAxisStep(remainingPitch, frame, false, focus);
        this.smoothYawStep = this.smoothAxisStep(this.smoothYawStep, yawStep += this.buildJitter(frame, remainingYaw, true, gcd), remainingYaw, true, focus);
        this.smoothPitchStep = this.smoothAxisStep(this.smoothPitchStep, pitchStep += this.buildJitter(frame, remainingPitch, false, gcd), remainingPitch, false, focus);
        float quantizedYawStep = this.quantizeDelta(this.smoothYawStep, remainingYaw, gcd, true);
        float quantizedPitchStep = this.quantizeDelta(this.smoothPitchStep, remainingPitch, gcd, false);
        this.smoothYaw = MathHelper.wrapDegrees((float)(this.smoothYaw + quantizedYawStep));
        this.smoothPitch = MathHelper.clamp((float)(this.smoothPitch + quantizedPitchStep), (float)-89.0f, (float)89.0f);
        RotationStorage.update(new Rotation(this.smoothYaw, this.smoothPitch), 360.0f, 360.0f, 45.0f, 45.0f, 0, 1, Aura.clientLook.isState());
    }

    private boolean shouldFocus() {
        float cooldown = TestRotation.mc.player.getAttackCooldownProgress(1.5f);
        boolean fallingForCrit = !TestRotation.mc.player.isOnGround() && TestRotation.mc.player.getVelocity().y < 0.0 && TestRotation.mc.player.fallDistance > 0.0f;
        return cooldown >= 0.88f || fallingForCrit;
    }

    private void ensureDatasetLoaded() {
        long now = System.currentTimeMillis();
        if (!this.shouldReload(now)) {
            return;
        }
        this.lastLoadAttempt = now;
        long modified = this.readLastModified();
        if (this.datasetReady && modified == this.lastModified) {
            return;
        }
        this.frames.clear();
        this.datasetReady = false;
        if (!Files.exists(DATASET_PATH, new LinkOption[0])) {
            this.lastModified = Long.MIN_VALUE;
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(DATASET_PATH);){
            JsonArray array = JsonParser.parseReader((Reader)reader).getAsJsonArray();
            for (JsonElement element : array) {
                DatasetFrame frame;
                if (!element.isJsonObject() || (frame = this.parseFrame(element.getAsJsonObject())) == null) continue;
                this.frames.add(frame);
            }
            this.datasetReady = !this.frames.isEmpty();
            this.lastModified = modified;
            this.reset();
        }
        catch (IOException | IllegalStateException ignored) {
            this.datasetReady = false;
            this.lastModified = Long.MIN_VALUE;
            this.reset();
        }
    }

    private boolean shouldReload(long now) {
        if (!this.datasetReady || this.frames.isEmpty()) {
            return now - this.lastLoadAttempt >= 1500L;
        }
        return now - this.lastLoadAttempt >= 3000L;
    }

    private long readLastModified() {
        try {
            return Files.exists(DATASET_PATH, new LinkOption[0]) ? Files.getLastModifiedTime(DATASET_PATH, new LinkOption[0]).toMillis() : Long.MIN_VALUE;
        }
        catch (IOException ignored) {
            return Long.MIN_VALUE;
        }
    }

    private DatasetFrame parseFrame(JsonObject object) {
        float fromYaw = this.getFloat(object, "fromYaw");
        float toYaw = this.getFloat(object, "toYaw");
        float fromPitch = this.getFloat(object, "fromPitch");
        float toPitch = this.getFloat(object, "toPitch");
        float signedYaw = MathHelper.wrapDegrees((float)(toYaw - fromYaw));
        float signedPitch = toPitch - fromPitch;
        float absYaw = Math.abs(signedYaw);
        float absPitch = Math.abs(signedPitch);
        float deltaYaw = Math.max(this.getFloat(object, "deltaYaw"), absYaw);
        float deltaPitch = Math.max(this.getFloat(object, "deltaPitch"), absPitch);
        if (deltaYaw <= 0.0f && deltaPitch <= 0.0f) {
            return null;
        }
        DatasetFrame frame = new DatasetFrame();
        frame.deltaYaw = deltaYaw;
        frame.deltaPitch = deltaPitch;
        frame.signedYaw = signedYaw != 0.0f ? signedYaw : Math.signum(this.getFloat(object, "jitterYawDir")) * deltaYaw;
        frame.signedPitch = signedPitch != 0.0f ? signedPitch : Math.signum(this.getFloat(object, "jitterPitchDir")) * deltaPitch;
        frame.rotationSpeed = Math.max(this.getFloat(object, "rotationSpeed"), 0.0f);
        frame.jitterScore = Math.max(this.getFloat(object, "jitterScore"), 0.0f);
        frame.jitterYawSpeed = Math.max(this.getFloat(object, "jitterYawSpeed"), 0.0f);
        frame.jitterPitchSpeed = Math.max(this.getFloat(object, "jitterPitchSpeed"), 0.0f);
        frame.isJittering = this.getBoolean(object, "isJittering");
        frame.attacking = this.getBoolean(object, "attacking");
        frame.combatFrame = this.getBoolean(object, "isCombatFrame");
        frame.instantSnap = this.getBoolean(object, "isInstantSnap");
        frame.timeDeltaMs = Math.max(1L, object.has("timeDeltaMs") ? object.get("timeDeltaMs").getAsLong() : 50L);
        return frame;
    }

    private DatasetFrame pickFrame(float remainingYaw, float remainingPitch, boolean focus) {
        float pressure = Math.abs(remainingYaw) + Math.abs(remainingPitch) * 0.82f;
        int size = this.frames.size();
        int window = Math.min(size, focus ? 78 : 56);
        int bestIndex = this.playbackIndex % size;
        float bestScore = Float.MAX_VALUE;
        for (int i2 = 0; i2 < window; ++i2) {
            int index = (this.playbackIndex + i2) % size;
            DatasetFrame frame = this.frames.get(index);
            float framePressure = frame.deltaYaw + frame.deltaPitch * 0.82f;
            float score = Math.abs(framePressure - pressure);
            if (focus) {
                if (!frame.isCombatLike()) {
                    score += 3.0f;
                }
                if (frame.instantSnap) {
                    score -= 0.5f;
                }
            } else if (frame.isCombatLike()) {
                score += 1.6f;
            }
            if (pressure < 10.0f && frame.isJittering) {
                score -= Math.min(frame.jitterScore, 2.6f) * 0.2f;
            }
            if (!((score += (float)i2 * 0.032f) < bestScore)) continue;
            bestScore = score;
            bestIndex = index;
        }
        this.playbackIndex = (bestIndex + 1) % size;
        return this.frames.get(bestIndex);
    }

    private void updateSmoothProfile(DatasetFrame frame, float remainingYaw, float remainingPitch, boolean focus) {
        if (this.smoothProfileTicks > 0) {
            --this.smoothProfileTicks;
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float pressure = MathHelper.clamp((float)((Math.abs(remainingYaw) + Math.abs(remainingPitch)) / 32.0f), (float)0.0f, (float)1.0f);
        float timePressure = MathHelper.clamp((float)((float)frame.timeDeltaMs / 120.0f), (float)0.0f, (float)1.0f);
        float yawMin = focus ? 0.94f : 0.86f;
        float yawMax = focus ? 1.12f : 1.04f;
        float pitchMin = focus ? 0.92f : 0.84f;
        float pitchMax = focus ? 1.08f : 1.0f;
        this.yawSmoothFactor = random.nextFloat(yawMin, yawMax + pressure * 0.08f + timePressure * 0.04f);
        this.pitchSmoothFactor = random.nextFloat(pitchMin, pitchMax + pressure * 0.06f + timePressure * 0.03f);
        if (frame.isCombatLike()) {
            this.yawSmoothFactor *= 1.02f;
            this.pitchSmoothFactor *= 1.015f;
        }
        this.smoothProfileTicks = random.nextInt(focus ? 2 : 3, focus ? 6 : 8);
    }

    private float buildAxisStep(float remaining, DatasetFrame frame, boolean yawAxis, boolean focus) {
        float finishThreshold;
        float desiredAbs = Math.abs(remaining);
        if (desiredAbs <= 1.0E-4f) {
            return 0.0f;
        }
        float template = yawAxis ? frame.deltaYaw : frame.deltaPitch;
        float speedBoost = 0.3f + MathHelper.clamp((float)(frame.rotationSpeed * (yawAxis ? 3.4f : 2.8f)), (float)0.0f, (float)(yawAxis ? 0.2f : 0.16f));
        float pressureBoost = MathHelper.clamp((float)(desiredAbs / (yawAxis ? 105.0f : 82.0f)), (float)0.09f, (float)(yawAxis ? 0.52f : 0.46f));
        float step = Math.max(template * Math.max(speedBoost, pressureBoost), yawAxis ? 0.03f : 0.024f);
        if (frame.instantSnap) {
            step = Math.max(step, desiredAbs * (yawAxis ? 0.085f : 0.065f));
        }
        if (frame.attacking || frame.combatFrame) {
            step *= yawAxis ? 1.02f : 1.015f;
        }
        step *= yawAxis ? 0.5f : 0.46f;
        float f2 = finishThreshold = yawAxis ? 6.0f : 4.0f;
        if (desiredAbs < finishThreshold) {
            float finishBoost = 1.0f + (finishThreshold - desiredAbs) / finishThreshold * 0.18f;
            step *= finishBoost;
        }
        float maxStep = yawAxis ? Math.max(0.48f, desiredAbs * (frame.instantSnap ? 0.11f : 0.065f)) : Math.max(0.34f, desiredAbs * (frame.instantSnap ? 0.09f : 0.058f));
        step = Math.min(step, maxStep);
        step = Math.min(step, desiredAbs);
        return Math.signum(remaining) * step;
    }

    private float buildJitter(DatasetFrame frame, float remaining, boolean yawAxis, float gcd) {
        float desiredAbs = Math.abs(remaining);
        float f2 = yawAxis ? 6.5f : 4.8f;
        if (desiredAbs > f2) {
            return 0.0f;
        }
        float speed = yawAxis ? frame.jitterYawSpeed : frame.jitterPitchSpeed;
        float base = gcd * MathHelper.clamp((float)(frame.jitterScore * 0.01f), (float)0.0f, (float)(yawAxis ? 0.15f : 0.11f));
        base += gcd * MathHelper.clamp((float)(speed * (yawAxis ? 1.3f : 1.0f)), (float)0.0f, (float)(yawAxis ? 0.1f : 0.07f));
        if (frame.isJittering) {
            base *= 1.05f;
        }
        if (base <= 0.0f) {
            return 0.0f;
        }
        float direction = ThreadLocalRandom.current().nextBoolean() ? 1.0f : -1.0f;
        float jitter = base * ThreadLocalRandom.current().nextFloat(0.3f, 0.95f) * direction;
        if (Math.abs(jitter) > desiredAbs && Math.signum(jitter) == Math.signum(remaining)) {
            jitter = remaining;
        }
        return jitter;
    }

    private void syncRotationState(LivingEntity target, float currentYaw, float currentPitch) {
        if (!this.hasRotationState || this.trackedRotationTarget != target) {
            this.trackedRotationTarget = target;
            this.smoothYaw = currentYaw;
            this.smoothPitch = currentPitch;
            this.smoothYawStep = 0.0f;
            this.smoothPitchStep = 0.0f;
            this.yawSmoothFactor = 1.0f;
            this.pitchSmoothFactor = 1.0f;
            this.smoothProfileTicks = 0;
            this.hasRotationState = true;
        }
    }

    private float smoothAxisStep(float currentStep, float desiredStep, float remaining, boolean yawAxis, boolean focus) {
        float finishThreshold;
        float minCap;
        float desiredAbs = Math.abs(remaining);
        if (desiredAbs <= 1.0E-4f) {
            return 0.0f;
        }
        float baseAlpha = yawAxis ? (focus ? 0.092f : 0.06f) : (focus ? 0.082f : 0.055f);
        float alpha = baseAlpha * (yawAxis ? this.yawSmoothFactor : this.pitchSmoothFactor);
        float smoothed = currentStep + (desiredStep - currentStep) * MathHelper.clamp((float)alpha, (float)0.025f, (float)0.16f);
        float f2 = minCap = yawAxis ? 0.13f : 0.1f;
        float capScale = yawAxis ? (focus ? 0.056f : 0.036f) : (focus ? 0.046f : 0.032f);
        float randomFactor = yawAxis ? this.yawSmoothFactor : this.pitchSmoothFactor;
        float maxCap = minCap + desiredAbs * capScale * MathHelper.clamp((float)randomFactor, (float)0.88f, (float)1.18f);
        float f3 = finishThreshold = yawAxis ? 5.5f : 3.8f;
        if (desiredAbs < finishThreshold) {
            maxCap *= 1.12f;
        }
        smoothed = MathHelper.clamp((float)smoothed, (float)(-maxCap), (float)maxCap);
        if (Math.abs(remaining) < Math.abs(smoothed) && Math.signum(remaining) == Math.signum(smoothed)) {
            smoothed = remaining;
        }
        return smoothed;
    }

    private float quantizeDelta(float wantedDelta, float remaining, float gcd, boolean yawAxis) {
        float quantized;
        float limited = wantedDelta;
        if (Math.abs(remaining) < Math.abs(limited) && Math.signum(remaining) == Math.signum(limited)) {
            limited = remaining;
        }
        if ((quantized = (float)Math.round(limited / gcd) * gcd) == 0.0f && Math.abs(limited) >= gcd * 0.2f) {
            quantized = Math.signum(limited) * gcd;
        }
        if (Math.abs(remaining) < Math.abs(quantized) && Math.signum(remaining) == Math.signum(quantized)) {
            quantized = remaining;
        }
        if (!yawAxis) {
            quantized = MathHelper.clamp((float)quantized, (float)-89.0f, (float)89.0f);
        }
        return quantized;
    }

    private Vec3d selectAimPoint(LivingEntity target, boolean focus) {
        if (this.trackedTarget != target || this.currentAimPoint == null || this.targetAimPoint == null) {
            this.trackedTarget = target;
            this.currentAimPoint = this.targetAimPoint = this.createAimPoint(target, focus);
            this.aimPointTicks = 0;
            this.aimPointRefreshTicks = this.randomRefreshTicks(focus);
            return this.currentAimPoint;
        }
        if (this.aimPointTicks++ >= this.aimPointRefreshTicks) {
            this.targetAimPoint = this.createAimPoint(target, focus);
            this.aimPointTicks = 0;
            this.aimPointRefreshTicks = this.randomRefreshTicks(focus);
        }
        float lerp = focus ? 0.06f : 0.04f;
        this.currentAimPoint = new Vec3d(MathHelper.lerp((double)lerp, (double)this.currentAimPoint.x, (double)this.targetAimPoint.x), MathHelper.lerp((double)lerp, (double)this.currentAimPoint.y, (double)this.targetAimPoint.y), MathHelper.lerp((double)lerp, (double)this.currentAimPoint.z, (double)this.targetAimPoint.z));
        return this.currentAimPoint;
    }

    private int randomRefreshTicks(boolean focus) {
        return ThreadLocalRandom.current().nextInt(focus ? 7 : 10, focus ? 13 : 18);
    }

    private Vec3d createAimPoint(LivingEntity target, boolean focus) {
        Box box = this.getPredictedBox(target);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double x2 = MathHelper.lerp((double)random.nextDouble(0.45, 0.55), (double)box.minX, (double)box.maxX);
        double y2 = MathHelper.lerp((double)random.nextDouble(focus ? 0.53 : 0.49, focus ? 0.7 : 0.76), (double)box.minY, (double)box.maxY);
        double z2 = MathHelper.lerp((double)random.nextDouble(0.45, 0.55), (double)box.minZ, (double)box.maxZ);
        return new Vec3d(x2, y2, z2);
    }

    private float getFloat(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsFloat() : 0.0f;
    }

    private boolean getBoolean(JsonObject object, String key) {
        return object.has(key) && object.get(key).getAsBoolean();
    }

    private static class DatasetFrame {
        float deltaYaw;
        float deltaPitch;
        float signedYaw;
        float signedPitch;
        float rotationSpeed;
        float jitterScore;
        float jitterYawSpeed;
        float jitterPitchSpeed;
        long timeDeltaMs;
        boolean isJittering;
        boolean attacking;
        boolean combatFrame;
        boolean instantSnap;

        private DatasetFrame() {
        }

        boolean isCombatLike() {
            return this.attacking || this.combatFrame || this.instantSnap;
        }
    }
}

