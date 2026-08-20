package polar.ru.client.modules.impl.movement;

import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventMoveInput;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class TargetStrafe
extends Module {
    public static TargetStrafe INSTANCE = new TargetStrafe();
    private final ModeSetting mode = new ModeSetting("Режим", "Legit Smooth", "Legit Smooth", "HVH Sharp");
    private final FloatSetting speed = new FloatSetting("Скорость", 0.3f, 0.1f, 1.0f, 0.05f);
    private final FloatSetting distance = new FloatSetting("Дистанция", 1.5f, 0.5f, 3.0f, 0.1f);
    private final BooleanSetting onlyInCombat = new BooleanSetting("Только в бою", true);
    private final FloatSetting smoothness = new FloatSetting("Плавность", 0.5f, 0.1f, 1.0f, 0.05f).visible(() -> this.mode.is("Legit Smooth"));
    private final FloatSetting sharpness = new FloatSetting("Резкость", 1.0f, 0.1f, 1.0f, 0.05f).visible(() -> this.mode.is("HVH Sharp"));
    private final FloatSetting directionChangeInterval = new FloatSetting("Интервал смены (мс)", 500.0f, 100.0f, 2000.0f, 50.0f).visible(() -> this.mode.is("HVH Sharp"));
    private final FloatSetting jitterStrength = new FloatSetting("Сила рывков", 60.0f, 0.0f, 120.0f, 5.0f).visible(() -> this.mode.is("HVH Sharp"));
    private final BooleanSetting instantTurn = new BooleanSetting("Мгновенный поворот", true).visible(() -> this.mode.is("HVH Sharp"));
    private float strafeAngle = 0.0f;
    private float targetStrafeAngle = 0.0f;
    private int direction = 1;
    private long lastDirectionChange = 0L;
    private final Random random = new Random();
    private boolean hasTarget = false;

    public TargetStrafe() {
        super("TargetStrafe", "Стрейф вокруг таргета", Module.ModuleCategory.MOVEMENT);
        this.addSettings(this.mode, this.speed, this.distance, this.onlyInCombat, this.smoothness, this.sharpness, this.directionChangeInterval, this.jitterStrength, this.instantTurn);
    }

    @Override
    public void onEnable() {
        this.strafeAngle = 0.0f;
        this.targetStrafeAngle = 0.0f;
        this.direction = 1;
        this.lastDirectionChange = System.currentTimeMillis();
        this.hasTarget = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.strafeAngle = 0.0f;
        this.targetStrafeAngle = 0.0f;
        super.onDisable();
    }

    @EventLink
        public void onUpdate(EventUpdate event) {
        long interval;
        if (TargetStrafe.mc.player == null || TargetStrafe.mc.world == null) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null) {
            this.hasTarget = false;
            if (this.onlyInCombat.isState()) {
                return;
            }
            this.strafeAngle += this.speed.getValue().floatValue() * 0.1f;
            return;
        }
        this.hasTarget = true;
        Vec3d playerPos = TargetStrafe.mc.player.getPos();
        Vec3d targetPos = target.getPos();
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        long l2 = interval = this.mode.is("HVH Sharp") ? (long)this.directionChangeInterval.getValue().floatValue() : (long)(2000.0 + this.random.nextDouble() * 2000.0);
        if (System.currentTimeMillis() - this.lastDirectionChange > interval) {
            this.direction *= -1;
            this.lastDirectionChange = System.currentTimeMillis();
            if (this.mode.is("HVH Sharp") && this.random.nextFloat() < 0.4f) {
                this.strafeAngle += (float)this.direction * this.jitterStrength.getValue().floatValue() * 0.5f;
            }
        }
        float angleToTarget = (float)Math.toDegrees(Math.atan2(dx, dz));
        if (this.mode.is("Legit Smooth")) {
            float smoothSpeed = this.speed.getValue().floatValue() * this.smoothness.getValue().floatValue();
            this.targetStrafeAngle = angleToTarget + (float)this.direction * 90.0f;
            float angleDiff = MathHelper.wrapDegrees((float)(this.targetStrafeAngle - this.strafeAngle));
            this.strafeAngle += angleDiff * (smoothSpeed * 0.5f);
            this.strafeAngle += (float)(this.random.nextDouble() - 0.5) * 5.0f * (1.0f - this.smoothness.getValue().floatValue());
        } else {
            float sharpSpeed = this.speed.getValue().floatValue() * this.sharpness.getValue().floatValue();
            float baseAngle = angleToTarget + (float)this.direction * 90.0f;
            float randomOffset = (float)(this.random.nextDouble() - 0.5) * this.jitterStrength.getValue().floatValue();
            this.targetStrafeAngle = baseAngle + randomOffset;
            if (this.instantTurn.isState()) {
                this.strafeAngle = this.targetStrafeAngle;
            } else {
                float angleDiff = MathHelper.wrapDegrees((float)(this.targetStrafeAngle - this.strafeAngle));
                this.strafeAngle += angleDiff * Math.min(sharpSpeed * 1.5f, 1.0f);
            }
            if (this.random.nextFloat() < 0.08f) {
                this.strafeAngle += (this.random.nextFloat() - 0.5f) * this.jitterStrength.getValue().floatValue() * 0.8f;
            }
            if (this.random.nextFloat() < 0.03f) {
                this.strafeAngle += (float)this.direction * (this.jitterStrength.getValue().floatValue() * 0.3f);
            }
        }
        float distThreshold = this.distance.getValue().floatValue();
        if (dist > (double)(distThreshold + 0.5f)) {
            this.strafeAngle = angleToTarget;
        } else if (dist < (double)(distThreshold - 0.5f)) {
            this.strafeAngle = angleToTarget + 180.0f;
        }
        this.strafeAngle = MathHelper.wrapDegrees((float)this.strafeAngle);
    }

    @EventLink
        public void onMoveInput(EventMoveInput event) {
        if (TargetStrafe.mc.player == null || TargetStrafe.mc.world == null) {
            return;
        }
        if (!this.isEnable()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null && this.onlyInCombat.isState()) {
            return;
        }
        float desiredYaw = this.strafeAngle;
        float currentYaw = TargetStrafe.mc.player.getYaw();
        float relativeAngle = MathHelper.wrapDegrees((float)(desiredYaw - currentYaw));
        float forward = MathHelper.cos((float)(relativeAngle * ((float)Math.PI / 180)));
        float strafe = MathHelper.sin((float)(relativeAngle * ((float)Math.PI / 180)));
        event.setForward(forward);
        event.setStrafe(strafe);
    }

    private LivingEntity getTarget() {
        if (ModuleClass.aura != null && ModuleClass.aura.isEnable()) {
            return ModuleClass.aura.getTarget();
        }
        return null;
    }

    public float getStrafeAngle() {
        return this.strafeAngle;
    }

    public String getMode() {
        return this.mode.getCurrent();
    }
}

