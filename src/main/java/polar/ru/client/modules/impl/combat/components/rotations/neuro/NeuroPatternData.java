package polar.ru.client.modules.impl.combat.components.rotations.neuro;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NeuroPatternData
implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private List<RotationSnapshot> snapshots;
    private long recordStartTime;
    private long recordDuration;
    private transient float avgYawSpeed;
    private transient float avgPitchSpeed;
    private transient float maxYawSpeed;
    private transient float maxPitchSpeed;
    private transient int snapshotCount;

    public NeuroPatternData(String name) {
        this.name = name;
        this.snapshots = new ArrayList<RotationSnapshot>();
        this.recordStartTime = System.currentTimeMillis();
    }

    public void addSnapshot(float yaw, float pitch, float deltaYaw, float deltaPitch, boolean isAttacking, float distanceToTarget, String context) {
        RotationSnapshot snapshot = new RotationSnapshot(yaw, pitch, deltaYaw, deltaPitch, System.currentTimeMillis() - this.recordStartTime, isAttacking, distanceToTarget, context);
        this.snapshots.add(snapshot);
    }

    public void finishRecording() {
        this.recordDuration = System.currentTimeMillis() - this.recordStartTime;
        this.snapshotCount = this.snapshots.size();
        if (this.snapshots.isEmpty()) {
            return;
        }
        float totalYawSpeed = 0.0f;
        float totalPitchSpeed = 0.0f;
        this.maxYawSpeed = 0.0f;
        this.maxPitchSpeed = 0.0f;
        for (RotationSnapshot snapshot : this.snapshots) {
            float yawSpeed = Math.abs(snapshot.deltaYaw);
            float pitchSpeed = Math.abs(snapshot.deltaPitch);
            totalYawSpeed += yawSpeed;
            totalPitchSpeed += pitchSpeed;
            if (yawSpeed > this.maxYawSpeed) {
                this.maxYawSpeed = yawSpeed;
            }
            if (!(pitchSpeed > this.maxPitchSpeed)) continue;
            this.maxPitchSpeed = pitchSpeed;
        }
        this.avgYawSpeed = totalYawSpeed / (float)this.snapshotCount;
        this.avgPitchSpeed = totalPitchSpeed / (float)this.snapshotCount;
    }
    public String getName() {
        return this.name;
    }
    public List<RotationSnapshot> getSnapshots() {
        return this.snapshots;
    }
    public long getRecordStartTime() {
        return this.recordStartTime;
    }
    public long getRecordDuration() {
        return this.recordDuration;
    }
    public float getAvgYawSpeed() {
        return this.avgYawSpeed;
    }
    public float getAvgPitchSpeed() {
        return this.avgPitchSpeed;
    }
    public float getMaxYawSpeed() {
        return this.maxYawSpeed;
    }
    public float getMaxPitchSpeed() {
        return this.maxPitchSpeed;
    }
    public int getSnapshotCount() {
        return this.snapshotCount;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setSnapshots(List<RotationSnapshot> snapshots) {
        this.snapshots = snapshots;
    }
    public void setRecordStartTime(long recordStartTime) {
        this.recordStartTime = recordStartTime;
    }
    public void setRecordDuration(long recordDuration) {
        this.recordDuration = recordDuration;
    }
    public void setAvgYawSpeed(float avgYawSpeed) {
        this.avgYawSpeed = avgYawSpeed;
    }
    public void setAvgPitchSpeed(float avgPitchSpeed) {
        this.avgPitchSpeed = avgPitchSpeed;
    }
    public void setMaxYawSpeed(float maxYawSpeed) {
        this.maxYawSpeed = maxYawSpeed;
    }
    public void setMaxPitchSpeed(float maxPitchSpeed) {
        this.maxPitchSpeed = maxPitchSpeed;
    }
    public void setSnapshotCount(int snapshotCount) {
        this.snapshotCount = snapshotCount;
    }

    public static class RotationSnapshot
    implements Serializable {
        private static final long serialVersionUID = 1L;
        private float yaw;
        private float pitch;
        private float deltaYaw;
        private float deltaPitch;
        private long timestamp;
        private boolean isAttacking;
        private float distanceToTarget;
        private String context;

        public RotationSnapshot(float yaw, float pitch, float deltaYaw, float deltaPitch, long timestamp, boolean isAttacking, float distanceToTarget, String context) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.deltaYaw = deltaYaw;
            this.deltaPitch = deltaPitch;
            this.timestamp = timestamp;
            this.isAttacking = isAttacking;
            this.distanceToTarget = distanceToTarget;
            this.context = context;
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
        public long getTimestamp() {
            return this.timestamp;
        }
        public boolean isAttacking() {
            return this.isAttacking;
        }
        public float getDistanceToTarget() {
            return this.distanceToTarget;
        }
        public String getContext() {
            return this.context;
        }
        public void setYaw(float yaw) {
            this.yaw = yaw;
        }
        public void setPitch(float pitch) {
            this.pitch = pitch;
        }
        public void setDeltaYaw(float deltaYaw) {
            this.deltaYaw = deltaYaw;
        }
        public void setDeltaPitch(float deltaPitch) {
            this.deltaPitch = deltaPitch;
        }
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        public void setAttacking(boolean isAttacking) {
            this.isAttacking = isAttacking;
        }
        public void setDistanceToTarget(float distanceToTarget) {
            this.distanceToTarget = distanceToTarget;
        }
        public void setContext(String context) {
            this.context = context;
        }
    }
}

