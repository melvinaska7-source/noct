package polar.ru.api.storages.implement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.helpertstorages.NeuroPattern;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;

public class NeuroAuraStorage
implements QClient {
    private static final long MIN_RECORD_INTERVAL = 50L;
    private static final int MAX_FRAMES = 20000;
    private static final String PATTERNS_DIRECTORY = "data_patterns";
    private static final String LEGACY_PATTERNS_DIRECTORY = "neuro_patterns";
    private static final String PRIMARY_EXTENSION = ".data";
    private static final String LEGACY_EXTENSION = ".neuro";
    private static final float SYNC_SCORE_THRESHOLD = 45.0f;
    private static final float MAX_YAW_CORRECTION = 8.0f;
    private static final float MAX_PITCH_CORRECTION = 6.0f;
    private final List<NeuroPattern> recordedPatterns = new CopyOnWriteArrayList<NeuroPattern>();
    private boolean isRecording = false;
    private boolean isUsingNeuro = false;
    private boolean showStats = true;
    private String currentPatternName = null;
    private String lastDebugMessage = "Готов!";
    private int recordedThisSession = 0;
    private long lastRecordTime = 0L;
    private float prevRecordYaw = 0.0f;
    private float prevRecordPitch = 0.0f;
    private boolean hasRecordedBefore = false;
    private final List<Frame> frames = new CopyOnWriteArrayList<Frame>();
    private int playbackIndex = -1;
    private int ticksSinceSync = 0;
    private float smoothedYawDelta = 0.0f;
    private float smoothedPitchDelta = 0.0f;
    private float smoothedOutputYaw = Float.NaN;
    private float smoothedOutputPitch = Float.NaN;
    private float yawSpeedFactor = 1.0f;
    private float pitchSpeedFactor = 1.0f;
    private int speedProfileTicks = 0;
    private Vec3d currentAimPoint = null;
    private Vec3d targetRandomPoint = null;
    private int aimPointTicks = 0;
    private LivingEntity lastAimTarget = null;
    private boolean lastWasIdle = true;
    private int attackCount = 0;
    private float randomXOffset = 0.0f;
    private float randomYRatio = 0.66f;
    private float randomZOffset = 0.0f;

    public NeuroAuraStorage() {
        this.createPatternsDirectory();
    }

    private void createPatternsDirectory() {
        try {
            Path path = Paths.get(PATTERNS_DIRECTORY, new String[0]);
            if (!Files.exists(path, new LinkOption[0])) {
                Files.createDirectories(path, new FileAttribute[0]);
            }
        }
        catch (IOException e2) {
            this.lastDebugMessage = "§cОшибка папки";
        }
    }

    public void recordTick(LivingEntity target, float currentYaw, float currentPitch) {
        boolean hasTarget;
        if (!this.isRecording || NeuroAuraStorage.mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastRecordTime < 50L) {
            return;
        }
        float deltaYaw = 0.0f;
        float deltaPitch = 0.0f;
        if (this.hasRecordedBefore) {
            deltaYaw = MathHelper.wrapDegrees((float)(currentYaw - this.prevRecordYaw));
            deltaPitch = currentPitch - this.prevRecordPitch;
        }
        float angleYaw = 0.0f;
        float anglePitch = 0.0f;
        double distance = 0.0;
        boolean bl = hasTarget = target != null;
        if (hasTarget) {
            AimData aimData = this.getAimData(target, currentYaw, currentPitch, null, true);
            angleYaw = aimData.angleYaw;
            anglePitch = aimData.anglePitch;
            distance = aimData.distance;
        }
        Frame frame = new Frame();
        frame.deltaYaw = deltaYaw;
        frame.deltaPitch = deltaPitch;
        frame.angleYaw = angleYaw;
        frame.anglePitch = anglePitch;
        frame.distance = distance;
        frame.hasTarget = hasTarget;
        frame.smoothness = this.calculateSmoothness(deltaYaw, deltaPitch);
        this.frames.add(frame);
        while (this.frames.size() > 20000) {
            this.frames.remove(0);
        }
        if (hasTarget) {
            boolean crit = NeuroAuraStorage.mc.player.fallDistance > 0.0f && !NeuroAuraStorage.mc.player.isOnGround();
            String type = target instanceof PlayerEntity ? "player" : "mob";
            this.recordedPatterns.add(new NeuroPattern(angleYaw, anglePitch, deltaYaw, deltaPitch, distance, crit, 0.0, type, frame.smoothness));
            while (this.recordedPatterns.size() > 20000) {
                this.recordedPatterns.remove(0);
            }
        }
        this.prevRecordYaw = currentYaw;
        this.prevRecordPitch = currentPitch;
        this.hasRecordedBefore = true;
        this.lastRecordTime = now;
        ++this.recordedThisSession;
        if (this.recordedThisSession % 20 == 0) {
            this.lastDebugMessage = "§aЗапись: §f" + this.frames.size();
        }
    }

    public Rotation getNeuroRotation(LivingEntity target, float currentYaw, float currentPitch, boolean idle) {
        boolean airborne;
        if (!this.isUsingNeuro || target == null || NeuroAuraStorage.mc.player == null || this.frames.isEmpty()) {
            this.resetState();
            return null;
        }
        if (!idle && this.lastWasIdle) {
            this.rollNewRandomPoint();
            ++this.attackCount;
        }
        this.lastWasIdle = idle;
        boolean needSync = this.playbackIndex < 0 || this.playbackIndex >= this.frames.size();
        AimData aimData = this.getAimData(target, currentYaw, currentPitch, null, idle);
        boolean bl = airborne = !NeuroAuraStorage.mc.player.isOnGround() || NeuroAuraStorage.mc.player.getVelocity().y != 0.0;
        if (Math.abs(aimData.angleYaw) > 110.0f) {
            needSync = true;
            this.smoothedYawDelta = 0.0f;
            this.smoothedPitchDelta = 0.0f;
            this.smoothedOutputYaw = currentYaw;
            this.smoothedOutputPitch = currentPitch;
        }
        if (!needSync && this.ticksSinceSync >= 5) {
            float distDiff;
            float pitchDiff;
            Frame currentFrame = this.frames.get(this.playbackIndex);
            float yawDiff = Math.abs(MathHelper.wrapDegrees((float)(currentFrame.angleYaw - aimData.angleYaw)));
            if (yawDiff + (pitchDiff = Math.abs(currentFrame.anglePitch - aimData.anglePitch)) + (distDiff = (float)Math.abs(currentFrame.distance - aimData.distance)) * 0.3f > 45.0f) {
                needSync = true;
            }
        }
        if (needSync) {
            this.playbackIndex = this.findBest(aimData.angleYaw, aimData.anglePitch, aimData.distance);
            this.ticksSinceSync = 0;
        }
        Frame frame = this.frames.get(this.playbackIndex);
        aimData = this.getAimData(target, currentYaw, currentPitch, frame, idle);
        float applyYaw = frame.deltaYaw;
        float applyPitch = frame.deltaPitch;
        this.updateSpeedProfile(idle, airborne, aimData);
        if (Math.abs(frame.angleYaw) > 3.0f && Math.abs(aimData.angleYaw) > 3.0f && Math.signum(frame.angleYaw) != Math.signum(aimData.angleYaw)) {
            applyYaw = -applyYaw;
        }
        if (Math.abs(frame.anglePitch) > 3.0f && Math.abs(aimData.anglePitch) > 3.0f && Math.signum(frame.anglePitch) != Math.signum(aimData.anglePitch)) {
            applyPitch = -applyPitch;
        }
        applyYaw = this.adaptRecordedDelta(applyYaw, aimData.angleYaw, frame.smoothness, idle, 8.0f);
        applyPitch = this.adaptRecordedDelta(applyPitch, aimData.anglePitch, frame.smoothness, idle, 6.0f);
        if (Math.abs(aimData.angleYaw) < 32.0f) {
            applyYaw = MathHelper.lerp((float)0.58f, (float)applyYaw, (float)aimData.angleYaw);
        }
        if (Math.abs(aimData.anglePitch) < 24.0f) {
            applyPitch = MathHelper.lerp((float)0.52f, (float)applyPitch, (float)aimData.anglePitch);
        }
        this.smoothedYawDelta = this.smoothDelta(this.smoothedYawDelta, applyYaw, frame.smoothness);
        this.smoothedPitchDelta = this.smoothDelta(this.smoothedPitchDelta, applyPitch, frame.smoothness);
        float quantizedYaw = this.quantizeToMouseStep(this.smoothedYawDelta, aimData.angleYaw);
        float quantizedPitch = this.quantizeToMouseStep(this.smoothedPitchDelta, aimData.anglePitch);
        float rawYaw = MathHelper.wrapDegrees((float)(currentYaw + (quantizedYaw += this.getMicroJitter(true, idle, airborne, aimData))));
        float rawPitch = MathHelper.clamp((float)(currentPitch + (quantizedPitch += this.getMicroJitter(false, idle, airborne, aimData))), (float)-90.0f, (float)90.0f);
        float finalYaw = this.smoothOutputRotation(rawYaw, currentYaw, frame.smoothness, idle, true);
        float finalPitch = this.smoothOutputRotation(rawPitch, currentPitch, frame.smoothness, idle, false);
        ++this.playbackIndex;
        ++this.ticksSinceSync;
        for (int skipped = 0; this.playbackIndex < this.frames.size() && !this.frames.get((int)this.playbackIndex).hasTarget && skipped < 5; ++skipped) {
            ++this.playbackIndex;
        }
        if (this.playbackIndex >= this.frames.size()) {
            float newAngleYaw = MathHelper.wrapDegrees((float)(aimData.perfectYaw - finalYaw));
            float newAnglePitch = aimData.perfectPitch - finalPitch;
            this.playbackIndex = this.findBest(newAngleYaw, newAnglePitch, aimData.distance);
            this.ticksSinceSync = 0;
        }
        this.lastDebugMessage = String.format("§a[%d/%d] dY%.2f dP%.2f", this.playbackIndex, this.frames.size(), Float.valueOf(quantizedYaw), Float.valueOf(quantizedPitch));
        return new Rotation(finalYaw, finalPitch);
    }

    private void rollNewRandomPoint() {
        ThreadLocalRandom r2 = ThreadLocalRandom.current();
        this.randomXOffset = r2.nextFloat(-0.38f, 0.38f);
        this.randomYRatio = r2.nextFloat(0.4f, 0.85f);
        this.randomZOffset = r2.nextFloat(-0.38f, 0.38f);
    }

    private AimData getAimData(LivingEntity target, float currentYaw, float currentPitch, Frame frame, boolean relaxed) {
        Vec3d eyePos = NeuroAuraStorage.mc.player.getEyePos();
        Vec3d point = this.selectAimPoint(target, relaxed);
        double distance = eyePos.distanceTo(point);
        double dx = point.x - eyePos.x;
        double dy = point.y - eyePos.y;
        double dz = point.z - eyePos.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float perfectYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        float perfectPitch = (float)Math.toDegrees(Math.atan2(-dy, distXZ));
        AimData aimData = new AimData();
        aimData.targetPoint = point;
        aimData.distance = distance;
        aimData.perfectYaw = perfectYaw;
        aimData.perfectPitch = perfectPitch;
        aimData.angleYaw = MathHelper.wrapDegrees((float)(perfectYaw - currentYaw));
        aimData.anglePitch = perfectPitch - currentPitch;
        return aimData;
    }

    private float adaptRecordedDelta(float recordedDelta, float currentAngle, float smoothness, boolean idle, float maxCorrection) {
        float correctionWeight = idle ? 0.14f : 0.045f;
        float correctionLimit = idle ? maxCorrection * 0.65f : maxCorrection * 0.3f;
        float correction = MathHelper.clamp((float)(currentAngle - recordedDelta), (float)(-correctionLimit), (float)correctionLimit);
        float result = recordedDelta + correction * correctionWeight;
        if (Math.abs(currentAngle) < Math.abs(result) && Math.signum(currentAngle) == Math.signum(result)) {
            result = currentAngle;
        }
        float preserveFactor = idle ? MathHelper.clamp((float)(1.0f - smoothness * 0.22f), (float)0.8f, (float)0.97f) : MathHelper.clamp((float)(1.0f - smoothness * 0.1f), (float)0.91f, (float)0.99f);
        result *= preserveFactor;
        if (Math.abs(currentAngle) <= GCDUtil.getGCDValue()) {
            return currentAngle;
        }
        return result;
    }

    private Vec3d selectAimPoint(LivingEntity target, boolean relaxed) {
        if (target != this.lastAimTarget) {
            this.lastAimTarget = target;
            this.currentAimPoint = null;
            this.targetRandomPoint = null;
            this.aimPointTicks = 0;
            this.rollNewRandomPoint();
        }
        Box box = target.getBoundingBox();
        Vec3d eyePos = NeuroAuraStorage.mc.player.getEyePos();
        Vec3d stablePoint = new Vec3d(box.getCenter().x, box.minY + box.getLengthY() * 0.72, box.getCenter().z);
        if (box.expand(0.12).contains(eyePos) || eyePos.squaredDistanceTo(stablePoint) <= 2.25) {
            this.currentAimPoint = stablePoint;
            this.targetRandomPoint = stablePoint;
            this.aimPointTicks = 0;
            return stablePoint;
        }
        double xCenter = (box.minX + box.maxX) * 0.5;
        double zCenter = (box.minZ + box.maxZ) * 0.5;
        double halfW = box.getLengthX() * 0.5;
        double halfD = box.getLengthZ() * 0.5;
        double height = box.getLengthY();
        Vec3d desired = new Vec3d(xCenter + halfW * (double)this.randomXOffset, box.minY + height * (double)this.randomYRatio, zCenter + halfD * (double)this.randomZOffset);
        if (this.targetRandomPoint == null) {
            this.targetRandomPoint = desired;
        } else {
            float driftLerp = relaxed ? 0.13f : 0.07f;
            this.targetRandomPoint = new Vec3d(MathHelper.lerp((double)driftLerp, (double)this.targetRandomPoint.x, (double)desired.x), MathHelper.lerp((double)driftLerp, (double)this.targetRandomPoint.y, (double)desired.y), MathHelper.lerp((double)driftLerp, (double)this.targetRandomPoint.z, (double)desired.z));
        }
        if (this.currentAimPoint == null) {
            this.currentAimPoint = this.targetRandomPoint;
            this.aimPointTicks = 0;
            return this.currentAimPoint;
        }
        float pointLerp = relaxed ? 0.11f : 0.055f;
        this.currentAimPoint = new Vec3d(MathHelper.lerp((double)pointLerp, (double)this.currentAimPoint.x, (double)this.targetRandomPoint.x), MathHelper.lerp((double)pointLerp, (double)this.currentAimPoint.y, (double)this.targetRandomPoint.y), MathHelper.lerp((double)pointLerp, (double)this.currentAimPoint.z, (double)this.targetRandomPoint.z));
        ++this.aimPointTicks;
        return this.currentAimPoint;
    }

    private float smoothDelta(float current, float target, float smoothness) {
        float lerpFactor = MathHelper.clamp((float)(0.035f + (1.0f - smoothness) * 0.12f), (float)0.035f, (float)0.15f);
        return current + (target - current) * lerpFactor;
    }

    private float smoothOutputRotation(float targetRotation, float currentRotation, float smoothness, boolean idle, boolean yawAxis) {
        float delta;
        float previous;
        float f2 = previous = yawAxis ? this.smoothedOutputYaw : this.smoothedOutputPitch;
        if (Float.isNaN(previous)) {
            previous = currentRotation;
        }
        float f3 = delta = yawAxis ? MathHelper.wrapDegrees((float)(targetRotation - previous)) : targetRotation - previous;
        float maxStep = yawAxis ? (idle ? 1.68f : 0.86f) : (idle ? 1.26f : 0.62f);
        float lerpFactor = idle ? MathHelper.clamp((float)(0.08f + (1.0f - smoothness) * 0.09f), (float)0.08f, (float)0.17f) : MathHelper.clamp((float)(0.04f + (1.0f - smoothness) * 0.055f), (float)0.04f, (float)0.095f);
        float smoothed = previous + MathHelper.clamp((float)(delta * (lerpFactor *= yawAxis ? this.yawSpeedFactor : this.pitchSpeedFactor)), (float)(-(maxStep *= yawAxis ? this.yawSpeedFactor : this.pitchSpeedFactor)), (float)maxStep);
        if (yawAxis) {
            this.smoothedOutputYaw = smoothed = MathHelper.wrapDegrees((float)smoothed);
        } else {
            this.smoothedOutputPitch = smoothed = MathHelper.clamp((float)smoothed, (float)-90.0f, (float)90.0f);
        }
        return smoothed;
    }

    private void updateSpeedProfile(boolean idle, boolean airborne, AimData aimData) {
        if (this.speedProfileTicks > 0) {
            --this.speedProfileTicks;
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float anglePressure = MathHelper.clamp((float)((Math.abs(aimData.angleYaw) + Math.abs(aimData.anglePitch)) / 35.0f), (float)0.0f, (float)1.0f);
        float baseYawMin = idle ? 1.06f : 0.96f;
        float baseYawMax = idle ? 1.34f : 1.12f;
        float basePitchMin = idle ? 1.0f : 0.9f;
        float basePitchMax = idle ? 1.24f : 1.05f;
        this.yawSpeedFactor = random.nextFloat(baseYawMin, baseYawMax + anglePressure * (idle ? 0.1f : 0.16f));
        this.pitchSpeedFactor = random.nextFloat(basePitchMin, basePitchMax + anglePressure * (idle ? 0.08f : 0.12f));
        if (!idle && anglePressure > 0.58f) {
            this.yawSpeedFactor = Math.max(this.yawSpeedFactor, 1.08f + anglePressure * 0.24f);
            this.pitchSpeedFactor = Math.max(this.pitchSpeedFactor, 1.0f + anglePressure * 0.18f);
        }
        if (airborne) {
            this.yawSpeedFactor *= 0.97f;
            this.pitchSpeedFactor *= 0.95f;
        }
        this.speedProfileTicks = random.nextInt(idle ? 3 : 2, idle ? 7 : 5);
    }

    private float getMicroJitter(boolean yawAxis, boolean idle, boolean airborne, AimData aimData) {
        float amplitude;
        float gcd = GCDUtil.getGCDValue();
        if (gcd <= 0.0f) {
            return 0.0f;
        }
        float pressure = Math.abs(yawAxis ? aimData.angleYaw : aimData.anglePitch);
        if (!idle) {
            float f2 = yawAxis ? 10.0f : 7.0f;
            if (pressure > f2) {
                return 0.0f;
            }
        }
        float f3 = amplitude = yawAxis ? gcd * 0.018f : gcd * 0.012f;
        if (airborne) {
            amplitude *= 0.35f;
        }
        float wave = (float)Math.sin(((float)NeuroAuraStorage.mc.player.age + (yawAxis ? 0.0f : 7.0f)) * (idle ? 0.42f : 0.28f));
        return wave * amplitude;
    }

    private float quantizeToMouseStep(float delta, float remainingAngle) {
        float quantized;
        float gcd = GCDUtil.getGCDValue();
        if (gcd <= 0.0f) {
            return delta;
        }
        float limited = delta;
        if (Math.abs(remainingAngle) < Math.abs(limited) && Math.signum(remainingAngle) == Math.signum(limited)) {
            limited = remainingAngle;
        }
        if ((quantized = (float)Math.round(limited / gcd) * gcd) == 0.0f && Math.abs(remainingAngle) >= gcd * 0.35f && Math.abs(limited) > 0.001f) {
            quantized = Math.signum(limited) * gcd;
        }
        if (Math.abs(remainingAngle) < Math.abs(quantized) && Math.signum(remainingAngle) == Math.signum(quantized)) {
            quantized = remainingAngle;
        }
        return quantized;
    }

    private float calculateSmoothness(float deltaYaw, float deltaPitch) {
        float magnitude = Math.abs(deltaYaw) + Math.abs(deltaPitch);
        float base = 1.0f - magnitude / 18.0f;
        float periodic = (float)Math.sin(((float)this.recordedThisSession + (float)NeuroAuraStorage.mc.player.age * 0.31f) * 0.34f) * 0.012f;
        float noise = ThreadLocalRandom.current().nextFloat(-0.008f, 0.008f);
        return MathHelper.clamp((float)(base + periodic + noise), (float)0.22f, (float)0.88f);
    }

    private int findBest(float angleYaw, float anglePitch, double distance) {
        int best = 0;
        float bestScore = Float.MAX_VALUE;
        for (int i2 = 0; i2 < this.frames.size(); ++i2) {
            float distanceDiff;
            float pitchDiff;
            float yawDiff;
            float score;
            Frame frame = this.frames.get(i2);
            if (!frame.hasTarget || !((score = (yawDiff = Math.abs(MathHelper.wrapDegrees((float)(frame.angleYaw - angleYaw)))) + (pitchDiff = Math.abs(frame.anglePitch - anglePitch)) + (distanceDiff = (float)Math.abs(frame.distance - distance)) * 0.3f) < bestScore)) continue;
            bestScore = score;
            best = i2;
        }
        return best;
    }

    public boolean savePatterns(String profileName) {
        if (this.frames.isEmpty()) {
            this.lastDebugMessage = "§cНет записей";
            return false;
        }
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("data_patterns/" + profileName + PRIMARY_EXTENSION))) {
            SaveData data = new SaveData();
            data.patterns = new ArrayList<NeuroPattern>(this.recordedPatterns);
            data.frames = new ArrayList<Frame>(this.frames);
            out.writeObject(data);
            this.currentPatternName = profileName;
            this.lastDebugMessage = "§aСохранено " + this.frames.size();
            return true;
        } catch (IOException e2) {
            this.lastDebugMessage = "§cОшибка сохранения";
            return false;
        }
    }

    public boolean loadPatterns(String profileName) {
        File file = this.resolveProfileFile(profileName);
        if (!file.exists()) {
            this.lastDebugMessage = "§eНе найдено: " + profileName;
            return false;
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            SaveData data = (SaveData)in.readObject();
            this.recordedPatterns.clear();
            this.recordedPatterns.addAll(data.patterns);
            this.frames.clear();
            this.frames.addAll(data.frames);
            this.currentPatternName = profileName;
            this.lastDebugMessage = "§aЗагружено " + this.frames.size();
            return true;
        } catch (Exception e2) {
            this.lastDebugMessage = "§cОшибка загрузки";
            return false;
        }
    }

    private void rebuildFramesFromPatterns() {
        for (NeuroPattern pattern : this.recordedPatterns) {
            Frame frame = new Frame();
            frame.deltaYaw = pattern.getDeltaYaw();
            frame.deltaPitch = pattern.getDeltaPitch();
            frame.angleYaw = pattern.getYaw();
            frame.anglePitch = pattern.getPitch();
            frame.distance = pattern.getDistance();
            frame.hasTarget = true;
            frame.smoothness = MathHelper.clamp((float)pattern.getSmoothness(), (float)0.18f, (float)0.9f);
            this.frames.add(frame);
        }
    }

    public boolean deletePatterns(String profileName) {
        File primaryFile = new File("data_patterns/" + profileName + PRIMARY_EXTENSION);
        File legacyFile = new File("neuro_patterns/" + profileName + LEGACY_EXTENSION);
        boolean deleted = false;
        if (primaryFile.exists()) {
            deleted = primaryFile.delete();
        }
        if (legacyFile.exists()) {
            boolean bl = deleted = legacyFile.delete() || deleted;
        }
        if (deleted) {
            if (profileName.equals(this.currentPatternName)) {
                this.currentPatternName = null;
            }
            this.lastDebugMessage = "§aУдалено";
            return true;
        }
        return false;
    }

    public int getPatternCount() {
        return this.recordedPatterns.size();
    }

    public int getFrameCount() {
        return this.frames.size();
    }

    public void startRecording() {
        this.recordedPatterns.clear();
        this.frames.clear();
        this.isRecording = true;
        this.isUsingNeuro = false;
        this.recordedThisSession = 0;
        this.lastRecordTime = 0L;
        this.currentPatternName = null;
        this.hasRecordedBefore = false;
        this.prevRecordYaw = 0.0f;
        this.prevRecordPitch = 0.0f;
        this.resetState();
        this.lastDebugMessage = "§aЗапись";
    }

    public void stopRecording() {
        this.isRecording = false;
        this.lastDebugMessage = "§eСтоп: " + this.frames.size();
    }

    public void clearPatterns() {
        this.recordedPatterns.clear();
        this.frames.clear();
        this.isRecording = false;
        this.isUsingNeuro = false;
        this.recordedThisSession = 0;
        this.currentPatternName = null;
        this.hasRecordedBefore = false;
        this.prevRecordYaw = 0.0f;
        this.prevRecordPitch = 0.0f;
        this.resetState();
        this.lastDebugMessage = "§eОчищено";
    }

    public void resetState() {
        this.playbackIndex = -1;
        this.ticksSinceSync = 0;
        this.smoothedYawDelta = 0.0f;
        this.smoothedPitchDelta = 0.0f;
        this.smoothedOutputYaw = Float.NaN;
        this.smoothedOutputPitch = Float.NaN;
        this.yawSpeedFactor = 1.0f;
        this.pitchSpeedFactor = 1.0f;
        this.speedProfileTicks = 0;
        this.currentAimPoint = null;
        this.targetRandomPoint = null;
        this.aimPointTicks = 0;
        this.lastAimTarget = null;
        this.lastWasIdle = true;
        this.attackCount = 0;
        this.rollNewRandomPoint();
    }

    public String getStatusString() {
        String status = "§8[§bData§8] §f" + this.frames.size();
        if (this.isRecording) {
            status = status + " §a[REC]";
        }
        if (this.isUsingNeuro) {
            status = status + " §b[ON " + this.playbackIndex + "]";
        }
        return status;
    }

    public List<String> getPatternNames() {
        ArrayList<String> names = new ArrayList<String>();
        this.collectPatternNames(names, new File(PATTERNS_DIRECTORY), PRIMARY_EXTENSION);
        this.collectPatternNames(names, new File(LEGACY_PATTERNS_DIRECTORY), LEGACY_EXTENSION);
        return names;
    }

    private void collectPatternNames(List<String> names, File directory, String extension) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles((dir, name) -> name.endsWith(extension));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name2 = file.getName().replace(extension, "");
            if (names.contains(name2)) continue;
            names.add(name2);
        }
    }

    private File resolveProfileFile(String profileName) {
        File primaryFile = new File("data_patterns/" + profileName + PRIMARY_EXTENSION);
        if (primaryFile.exists()) {
            return primaryFile;
        }
        return new File("neuro_patterns/" + profileName + LEGACY_EXTENSION);
    }
    public List<NeuroPattern> getRecordedPatterns() {
        return this.recordedPatterns;
    }
    public boolean isRecording() {
        return this.isRecording;
    }
    public void setRecording(boolean isRecording) {
        this.isRecording = isRecording;
    }
    public boolean isUsingNeuro() {
        return this.isUsingNeuro;
    }
    public void setUsingNeuro(boolean isUsingNeuro) {
        this.isUsingNeuro = isUsingNeuro;
    }
    public boolean isShowStats() {
        return this.showStats;
    }
    public void setShowStats(boolean showStats) {
        this.showStats = showStats;
    }
    public String getCurrentPatternName() {
        return this.currentPatternName;
    }
    public void setCurrentPatternName(String currentPatternName) {
        this.currentPatternName = currentPatternName;
    }
    public String getLastDebugMessage() {
        return this.lastDebugMessage;
    }
    public int getRecordedThisSession() {
        return this.recordedThisSession;
    }

    private static class Frame
    implements Serializable {
        private static final long serialVersionUID = 7L;
        float deltaYaw;
        float deltaPitch;
        float angleYaw;
        float anglePitch;
        double distance;
        boolean hasTarget;
        float smoothness;

        private Frame() {
        }
    }

    private static class AimData {
        Vec3d targetPoint;
        float perfectYaw;
        float perfectPitch;
        float angleYaw;
        float anglePitch;
        double distance;

        private AimData() {
        }
    }

    private static class SaveData
    implements Serializable {
        private static final long serialVersionUID = 7L;
        List<NeuroPattern> patterns;
        List<Frame> frames;

        private SaveData() {
        }
    }
}

