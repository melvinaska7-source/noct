package polar.ru.client.modules.impl.movement;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class ElytraBoost
extends Module {
    private static final String[] RANGE_LABELS = new String[]{"0 - 5", "5 - 10", "10 - 15", "15 - 20", "20 - 25", "25 - 30", "30 - 35", "35 - 40", "40 - 45"};
    private static final int[] AI_YAW_VECTORS = new int[]{-45, 45, 135, -135};
    private static final int[] AI_PITCH_VECTORS = new int[]{-45, 45};
    public static ElytraBoost INSTANCE = new ElytraBoost();
    private final ModeSetting mode = new ModeSetting("Режим", "ReallyWorld", "ReallyWorld", "Bravo", "Кастомный");
    private final BooleanSetting autoDecrease = new BooleanSetting("Авто-уменьшение при флаге", false).visible(() -> this.mode.is("Кастомный"));
    private final FloatSetting[] xzSpeeds = new FloatSetting[9];
    private final FloatSetting[] yUpSpeeds = new FloatSetting[9];
    private final FloatSetting[] yDownSpeeds = new FloatSetting[9];
    private String lastAngleRangeXZ = "";
    private String lastAngleRangeY = "";
    private boolean lastYUp = true;
    private double lastX = 0.0;
    private double lastY = 0.0;
    private double lastZ = 0.0;

    public ElytraBoost() {
        super("ElytraBoost", "Ускоряет на элитрах", Module.ModuleCategory.MOVEMENT);
        String[] xzNames = new String[]{"XZ 0-5", "XZ 5-10", "XZ 10-15", "XZ 15-20", "XZ 20-25", "XZ 25-30", "XZ 30-35", "XZ 35-40", "XZ 40-45"};
        String[] yUpNames = new String[]{"Y Вверх 0-5", "Y Вверх 5-10", "Y Вверх 10-15", "Y Вверх 15-20", "Y Вверх 20-25", "Y Вверх 25-30", "Y Вверх 30-35", "Y Вверх 35-40", "Y Вверх 40-45"};
        String[] yDownNames = new String[]{"Y Вниз 0-5", "Y Вниз 5-10", "Y Вниз 10-15", "Y Вниз 15-20", "Y Вниз 20-25", "Y Вниз 25-30", "Y Вниз 30-35", "Y Вниз 35-40", "Y Вниз 40-45"};
        for (int i2 = 0; i2 < 9; ++i2) {
            this.xzSpeeds[i2] = new FloatSetting(xzNames[i2], 1.6f, 1.5f, 2.5f, 0.01f).visible(() -> this.mode.is("Кастомный"));
            this.yUpSpeeds[i2] = new FloatSetting(yUpNames[i2], 1.6f, 1.5f, 2.5f, 0.01f).visible(() -> this.mode.is("Кастомный"));
            this.yDownSpeeds[i2] = new FloatSetting(yDownNames[i2], 1.6f, 1.5f, 2.5f, 0.01f).visible(() -> this.mode.is("Кастомный"));
        }
        this.addSettings(this.mode, this.autoDecrease);
        this.addSettings(this.xzSpeeds);
        this.addSettings(this.yUpSpeeds);
        this.addSettings(this.yDownSpeeds);
        this.applyReallyWorldPreset();
    }

        public Vec2f computeBoost(float yaw, float pitch) {
        if (this.mode.is("Кастомный")) {
            float xz = this.getCustomSpeedXZ(yaw);
            float y2 = this.getCustomSpeedY(pitch);
            return new Vec2f(xz, y2);
        }
        if (this.mode.is("Bravo")) {
            float xz = this.getBravoSpeedXZ(pitch, yaw);
            float y3 = this.getBravoSpeedY(pitch);
            return new Vec2f(xz, y3);
        }
        float speed = this.getAiBoost(pitch, yaw, false, true);
        return new Vec2f(speed, speed);
    }

        public void handleFlag(double px, double py, double pz) {
        if (!this.autoDecrease.isState() || !this.mode.is("Кастомный")) {
            return;
        }
        double deltaX = Math.abs(px - this.lastX);
        double deltaY = Math.abs(py - this.lastY);
        double deltaZ = Math.abs(pz - this.lastZ);
        double deltaXZ = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (deltaXZ < 0.01 && deltaY < 0.01) {
            this.lastX = px;
            this.lastY = py;
            this.lastZ = pz;
            return;
        }
        if (deltaXZ > deltaY) {
            FloatSetting target = this.getSliderForRangeXZ(this.lastAngleRangeXZ);
            if (target != null) {
                float newVal = Math.max(1.5f, target.getValue().floatValue() - 0.01f);
                target.setValue(newVal);
                ChatUtils.sendMessage("Флагнут XZ: " + this.lastAngleRangeXZ + "° (авто-уменьшено на 0.01)");
            }
        } else {
            FloatSetting target = this.getSliderForRangeY(this.lastAngleRangeY, this.lastYUp);
            if (target != null) {
                float newVal = Math.max(1.5f, target.getValue().floatValue() - 0.01f);
                target.setValue(newVal);
                ChatUtils.sendMessage("Флагнут Y: " + this.lastAngleRangeY + "° (авто-уменьшено на 0.01)");
            }
        }
        this.lastX = px;
        this.lastY = py;
        this.lastZ = pz;
    }

    public void saveLastPos(double x2, double y2, double z2) {
        this.lastX = x2;
        this.lastY = y2;
        this.lastZ = z2;
    }

        private float getCustomSpeedXZ(float yaw) {
        float converted = this.convertAngleToRange(yaw);
        if (converted <= 5.0f) {
            this.lastAngleRangeXZ = "0-5";
            return this.xzSpeeds[0].getValue().floatValue();
        }
        if (converted <= 10.0f) {
            this.lastAngleRangeXZ = "5-10";
            return this.xzSpeeds[1].getValue().floatValue();
        }
        if (converted <= 15.0f) {
            this.lastAngleRangeXZ = "10-15";
            return this.xzSpeeds[2].getValue().floatValue();
        }
        if (converted <= 20.0f) {
            this.lastAngleRangeXZ = "15-20";
            return this.xzSpeeds[3].getValue().floatValue();
        }
        if (converted <= 25.0f) {
            this.lastAngleRangeXZ = "20-25";
            return this.xzSpeeds[4].getValue().floatValue();
        }
        if (converted <= 30.0f) {
            this.lastAngleRangeXZ = "25-30";
            return this.xzSpeeds[5].getValue().floatValue();
        }
        if (converted <= 35.0f) {
            this.lastAngleRangeXZ = "30-35";
            return this.xzSpeeds[6].getValue().floatValue();
        }
        if (converted <= 40.0f) {
            this.lastAngleRangeXZ = "35-40";
            return this.xzSpeeds[7].getValue().floatValue();
        }
        this.lastAngleRangeXZ = "40-45";
        return this.xzSpeeds[8].getValue().floatValue();
    }

        private float getCustomSpeedY(float pitch) {
        FloatSetting[] arr;
        boolean up;
        float converted = this.convertAngleToRange(pitch);
        this.lastYUp = up = pitch < 0.0f;
        FloatSetting[] floatSettingArray = arr = up ? this.yUpSpeeds : this.yDownSpeeds;
        if (converted <= 5.0f) {
            this.lastAngleRangeY = "0-5";
            return arr[0].getValue().floatValue();
        }
        if (converted <= 10.0f) {
            this.lastAngleRangeY = "5-10";
            return arr[1].getValue().floatValue();
        }
        if (converted <= 15.0f) {
            this.lastAngleRangeY = "10-15";
            return arr[2].getValue().floatValue();
        }
        if (converted <= 20.0f) {
            this.lastAngleRangeY = "15-20";
            return arr[3].getValue().floatValue();
        }
        if (converted <= 25.0f) {
            this.lastAngleRangeY = "20-25";
            return arr[4].getValue().floatValue();
        }
        if (converted <= 30.0f) {
            this.lastAngleRangeY = "25-30";
            return arr[5].getValue().floatValue();
        }
        if (converted <= 35.0f) {
            this.lastAngleRangeY = "30-35";
            return arr[6].getValue().floatValue();
        }
        if (converted <= 40.0f) {
            this.lastAngleRangeY = "35-40";
            return arr[7].getValue().floatValue();
        }
        this.lastAngleRangeY = "40-45";
        return arr[8].getValue().floatValue();
    }

    private float getAiBoost(float pitch, float yaw, boolean isBravo, boolean applyRwCap) {
        if (Math.abs(pitch) > 55.0f) {
            return 1.55f;
        }
        float boost = this.adjustBoostForYaw(yaw, applyRwCap);
        boost = this.adjustBoostForPitch(pitch, boost);
        boost = Math.max(isBravo ? 1.65f : 1.6f, boost);
        return Math.min(boost, isBravo ? 1.9f : 2.2f);
    }

    private float adjustBoostForYaw(float yaw, boolean applyRwCap) {
        int idx = ElytraBoost.findClosestVector(yaw, AI_YAW_VECTORS);
        if (idx == -1) {
            return 1.6f;
        }
        float dist = Math.abs(MathHelper.wrapDegrees((float)yaw) - (float)AI_YAW_VECTORS[idx]);
        float maxBoost = 2.2f;
        float minBoostVal = 1.6f;
        float maxDistance = 12.0f;
        float smartBoost = 0.0f;
        if (dist <= maxDistance) {
            float ratio = dist / maxDistance;
            smartBoost = maxBoost - (maxBoost - minBoostVal) * ratio;
        }
        float variableSpeed = ElytraBoost.getVariableSpeed(dist);
        float finalSpeed = Math.max(smartBoost, variableSpeed);
        return applyRwCap ? Math.min(finalSpeed, 1.8f) : finalSpeed;
    }

    private float adjustBoostForPitch(float pitch, float boost) {
        int idx = ElytraBoost.findClosestVector(pitch, AI_PITCH_VECTORS);
        if (idx == -1) {
            return boost;
        }
        float dist = Math.abs(Math.abs(pitch) - (float)Math.abs(AI_PITCH_VECTORS[idx]));
        if (dist < 30.0f) {
            boost += 0.4f * (1.0f - dist / 30.0f);
        }
        return boost;
    }

    private static float getVariableSpeed(float dist) {
        int level;
        float[] thresholds = new float[]{4.0f, 8.0f, 11.0f, 15.0f, 21.0f, 28.0f};
        float[] speeds = new float[]{2.2f, 2.1f, 2.0f, 1.9f, 1.8f, 1.7f, 1.6f};
        for (level = 0; level < thresholds.length && dist >= thresholds[level]; ++level) {
        }
        return speeds[level];
    }

    private static int findClosestVector(float angle, int[] vectors) {
        int minIdx = -1;
        float minDist = Float.MAX_VALUE;
        for (int i2 = 0; i2 < vectors.length; ++i2) {
            float d2 = Math.abs(MathHelper.wrapDegrees((float)angle) - (float)vectors[i2]);
            if (!(d2 < minDist)) continue;
            minDist = d2;
            minIdx = i2;
        }
        return minIdx;
    }

    private float getBravoSpeedXZ(float pitch, float yaw) {
        float absPitch = Math.abs(pitch);
        float absYaw = Math.abs(MathHelper.wrapDegrees((float)yaw) % 90.0f);
        float speed = absPitch >= 38.0f && absPitch <= 52.0f ? 2.0f : (absPitch >= 32.0f && absPitch <= 58.0f ? 1.96f : (absPitch >= 28.0f && absPitch <= 62.0f ? 1.95f : (absYaw >= 29.0f && absYaw <= 61.0f || absPitch >= 29.0f && absPitch <= 61.0f ? 1.963f : (absYaw >= 28.0f && absYaw <= 60.0f || absPitch >= 28.0f && absPitch <= 60.0f ? 1.954f : (absYaw >= 26.0f && absYaw <= 64.0f || absPitch >= 26.0f && absPitch <= 64.0f ? 1.874f : (absYaw >= 24.0f && absYaw <= 66.0f || absPitch >= 24.0f && absPitch <= 66.0f ? 1.72f : (absYaw >= 15.0f && absYaw <= 75.0f || absPitch >= 15.0f && absPitch <= 75.0f ? 1.72f : (absYaw >= 13.0f && absYaw <= 77.0f || absPitch >= 13.0f && absPitch <= 77.0f ? 1.72f : (absYaw >= 12.0f && absYaw <= 78.0f || absPitch >= 12.0f && absPitch <= 78.0f ? 1.72f : (absYaw >= 8.0f && absYaw <= 82.0f || absPitch >= 11.0f && absPitch <= 79.0f ? 1.72f : (absYaw >= 5.0f && absYaw <= 85.0f || absPitch >= 8.0f && absPitch <= 82.0f ? 1.67f : 1.71f)))))))))));
        return pitch > 15.0f ? speed - 0.068f : speed;
    }

    private float getBravoSpeedY(float pitch) {
        float absPitch = Math.abs(pitch);
        if (absPitch >= 37.0f && absPitch <= 38.0f) {
            return 2.03f;
        }
        if (absPitch >= 25.0f && absPitch <= 30.0f) {
            return 2.0f;
        }
        if (absPitch >= 35.0f && absPitch <= 45.0f) {
            return 1.99f;
        }
        if (absPitch >= 40.0f && absPitch <= 50.0f) {
            return 1.97f;
        }
        if (absPitch >= 50.0f && absPitch <= 60.0f) {
            return 1.96f;
        }
        if (absPitch >= 51.0f && absPitch <= 61.0f) {
            return 1.85f;
        }
        if (absPitch >= 52.0f && absPitch <= 65.0f) {
            return 1.8f;
        }
        return 1.59f;
    }

    private float convertAngleToRange(float angle) {
        float abs = Math.abs(angle);
        if (abs > 90.0f) {
            abs = 180.0f - abs;
        }
        if (abs > 45.0f) {
            abs = 90.0f - abs;
        }
        return abs;
    }

    private FloatSetting getSliderForRangeXZ(String range) {
        switch (range) {
            case "0-5": {
                return this.xzSpeeds[0];
            }
            case "5-10": {
                return this.xzSpeeds[1];
            }
            case "10-15": {
                return this.xzSpeeds[2];
            }
            case "15-20": {
                return this.xzSpeeds[3];
            }
            case "20-25": {
                return this.xzSpeeds[4];
            }
            case "25-30": {
                return this.xzSpeeds[5];
            }
            case "30-35": {
                return this.xzSpeeds[6];
            }
            case "35-40": {
                return this.xzSpeeds[7];
            }
            case "40-45": {
                return this.xzSpeeds[8];
            }
        }
        return null;
    }

    private FloatSetting getSliderForRangeY(String range, boolean up) {
        FloatSetting[] arr = up ? this.yUpSpeeds : this.yDownSpeeds;
        switch (range) {
            case "0-5": {
                return arr[0];
            }
            case "5-10": {
                return arr[1];
            }
            case "10-15": {
                return arr[2];
            }
            case "15-20": {
                return arr[3];
            }
            case "20-25": {
                return arr[4];
            }
            case "25-30": {
                return arr[5];
            }
            case "30-35": {
                return arr[6];
            }
            case "35-40": {
                return arr[7];
            }
            case "40-45": {
                return arr[8];
            }
        }
        return null;
    }

    private void applyReallyWorldPreset() {
        float[] xz = new float[]{1.61f, 1.61f, 1.64f, 1.68f, 1.74f, 1.8f, 1.8f, 1.8f, 1.79f};
        float[] yUp = new float[]{1.61f, 1.58f, 1.6f, 1.61f, 1.68f, 1.7f, 1.77f, 1.66f, 1.94f};
        float[] yDown = new float[]{1.87f, 2.06f, 2.09f, 2.12f, 2.2f, 2.2f, 2.23f, 2.06f, 2.08f};
        for (int i2 = 0; i2 < 9; ++i2) {
            this.xzSpeeds[i2].setValue(xz[i2]);
            this.yUpSpeeds[i2].setValue(yUp[i2]);
            this.yDownSpeeds[i2].setValue(yDown[i2]);
        }
    }

    private void applyGrimPreset() {
        float[] xz = new float[]{1.61f, 1.63f, 1.66f, 1.69f, 1.77f, 1.83f, 1.93f, 2.08f, 2.24f};
        float[] yUp = new float[]{1.61f, 1.63f, 1.66f, 1.69f, 1.77f, 1.83f, 1.93f, 2.03f, 2.24f};
        float[] yDown = new float[]{1.61f, 1.63f, 1.66f, 1.68f, 1.77f, 1.83f, 1.93f, 2.08f, 2.24f};
        for (int i2 = 0; i2 < 9; ++i2) {
            this.xzSpeeds[i2].setValue(xz[i2]);
            this.yUpSpeeds[i2].setValue(yUp[i2]);
            this.yDownSpeeds[i2].setValue(yDown[i2]);
        }
    }

    private void applyLonyGriefPreset() {
        float[] xz = new float[]{1.61f, 1.63f, 1.65f, 1.7f, 1.73f, 1.83f, 1.94f, 2.07f, 2.18f};
        float[] yUp = new float[]{1.63f, 1.61f, 1.61f, 1.63f, 1.66f, 1.7f, 1.78f, 2.03f, 2.03f};
        float[] yDown = new float[]{1.63f, 1.63f, 1.66f, 1.68f, 1.77f, 1.83f, 1.93f, 2.08f, 2.24f};
        for (int i2 = 0; i2 < 9; ++i2) {
            this.xzSpeeds[i2].setValue(xz[i2]);
            this.yUpSpeeds[i2].setValue(yUp[i2]);
            this.yDownSpeeds[i2].setValue(yDown[i2]);
        }
    }
    public ModeSetting getMode() {
        return this.mode;
    }
    public BooleanSetting getAutoDecrease() {
        return this.autoDecrease;
    }
    public FloatSetting[] getXzSpeeds() {
        return this.xzSpeeds;
    }
    public FloatSetting[] getYUpSpeeds() {
        return this.yUpSpeeds;
    }
    public FloatSetting[] getYDownSpeeds() {
        return this.yDownSpeeds;
    }
    public String getLastAngleRangeXZ() {
        return this.lastAngleRangeXZ;
    }
    public String getLastAngleRangeY() {
        return this.lastAngleRangeY;
    }
    public boolean isLastYUp() {
        return this.lastYUp;
    }
    public double getLastX() {
        return this.lastX;
    }
    public double getLastY() {
        return this.lastY;
    }
    public double getLastZ() {
        return this.lastZ;
    }
    public void setLastAngleRangeXZ(String lastAngleRangeXZ) {
        this.lastAngleRangeXZ = lastAngleRangeXZ;
    }
    public void setLastAngleRangeY(String lastAngleRangeY) {
        this.lastAngleRangeY = lastAngleRangeY;
    }
    public void setLastYUp(boolean lastYUp) {
        this.lastYUp = lastYUp;
    }
    public void setLastX(double lastX) {
        this.lastX = lastX;
    }
    public void setLastY(double lastY) {
        this.lastY = lastY;
    }
    public void setLastZ(double lastZ) {
        this.lastZ = lastZ;
    }
}

