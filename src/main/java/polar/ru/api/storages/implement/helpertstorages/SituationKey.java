package polar.ru.api.storages.implement.helpertstorages;


public final class SituationKey {
    private final String targetType;
    private final String distanceBucket;
    private final String movementState;
    private final String critState;
    private final String healthState;

    public String toString() {
        return this.targetType + "_" + this.distanceBucket + "_" + this.movementState + "_" + this.critState + "_" + this.healthState;
    }
    public SituationKey(String targetType, String distanceBucket, String movementState, String critState, String healthState) {
        this.targetType = targetType;
        this.distanceBucket = distanceBucket;
        this.movementState = movementState;
        this.critState = critState;
        this.healthState = healthState;
    }
    public String getTargetType() {
        return this.targetType;
    }
    public String getDistanceBucket() {
        return this.distanceBucket;
    }
    public String getMovementState() {
        return this.movementState;
    }
    public String getCritState() {
        return this.critState;
    }
    public String getHealthState() {
        return this.healthState;
    }
    public boolean equals(Object o2) {
        if (o2 == this) {
            return true;
        }
        if (!(o2 instanceof SituationKey)) {
            return false;
        }
        SituationKey other = (SituationKey)o2;
        String this$targetType = this.getTargetType();
        String other$targetType = other.getTargetType();
        if (this$targetType == null ? other$targetType != null : !this$targetType.equals(other$targetType)) {
            return false;
        }
        String this$distanceBucket = this.getDistanceBucket();
        String other$distanceBucket = other.getDistanceBucket();
        if (this$distanceBucket == null ? other$distanceBucket != null : !this$distanceBucket.equals(other$distanceBucket)) {
            return false;
        }
        String this$movementState = this.getMovementState();
        String other$movementState = other.getMovementState();
        if (this$movementState == null ? other$movementState != null : !this$movementState.equals(other$movementState)) {
            return false;
        }
        String this$critState = this.getCritState();
        String other$critState = other.getCritState();
        if (this$critState == null ? other$critState != null : !this$critState.equals(other$critState)) {
            return false;
        }
        String this$healthState = this.getHealthState();
        String other$healthState = other.getHealthState();
        return !(this$healthState == null ? other$healthState != null : !this$healthState.equals(other$healthState));
    }
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $targetType = this.getTargetType();
        result = result * 59 + ($targetType == null ? 43 : $targetType.hashCode());
        String $distanceBucket = this.getDistanceBucket();
        result = result * 59 + ($distanceBucket == null ? 43 : $distanceBucket.hashCode());
        String $movementState = this.getMovementState();
        result = result * 59 + ($movementState == null ? 43 : $movementState.hashCode());
        String $critState = this.getCritState();
        result = result * 59 + ($critState == null ? 43 : $critState.hashCode());
        String $healthState = this.getHealthState();
        result = result * 59 + ($healthState == null ? 43 : $healthState.hashCode());
        return result;
    }
}

