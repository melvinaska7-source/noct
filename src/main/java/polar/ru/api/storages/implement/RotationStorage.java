package polar.ru.api.storages.implement;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.QClient;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventKeyboardInput;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.storages.implement.FreeLookStorage;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;

public class RotationStorage
implements QClient {
    public static RotationStorage instance;
    private RotationTask currentTask = RotationTask.IDLE;
    private float currentYawSpeed;
    private float currentPitchSpeed;
    private float currentYawReturnSpeed;
    private float currentPitchReturnSpeed;
    private int currentPriority;
    private int currentTimeout;
    private int idleTicks;
    private Rotation targetRotation;

    public RotationStorage() {
        instance = this;
        EventInvoker.register(this);
    }

    public static double direction(float rotationYaw, float moveForward, float moveStrafing) {
        if (moveForward < 0.0f) {
            rotationYaw += 180.0f;
        }
        float forward = 1.0f;
        if (moveForward < 0.0f) {
            forward = -0.5f;
        }
        if (moveForward > 0.0f) {
            forward = 0.5f;
        }
        if (moveStrafing > 0.0f) {
            rotationYaw -= 90.0f * forward;
        }
        if (moveStrafing < 0.0f) {
            rotationYaw += 90.0f * forward;
        }
        return Math.toRadians(rotationYaw);
    }

    public static void fixMovement(EventKeyboardInput event, float yaw) {
        float forward = event.getMovementForward();
        float strafe = event.getMovementSideways();
        if (forward == 0.0f && strafe == 0.0f) {
            return;
        }
        double targetAngle = MathHelper.wrapDegrees((double)Math.toDegrees(RotationStorage.direction(yaw, forward, strafe)));
        float bestForward = 0.0f;
        float bestStrafe = 0.0f;
        float smallestDifference = Float.MAX_VALUE;
        for (float testForward = -1.0f; testForward <= 1.0f; testForward += 1.0f) {
            for (float testStrafe = -1.0f; testStrafe <= 1.0f; testStrafe += 1.0f) {
                double testAngle;
                float difference;
                if (testForward == 0.0f && testStrafe == 0.0f || !((difference = Math.abs(MathHelper.wrapDegrees((float)((float)(targetAngle - (testAngle = MathHelper.wrapDegrees((double)Math.toDegrees(RotationStorage.direction(yaw, testForward, testStrafe))))))))) < smallestDifference)) continue;
                smallestDifference = difference;
                bestForward = testForward;
                bestStrafe = testStrafe;
            }
        }
        event.setMovementForward(bestForward);
        event.setMovementSideways(bestStrafe);
    }

    @EventLink
    public void onInput(EventKeyboardInput event) {
        if (this.isRotating()) {
            RotationStorage.fixMovement(event, MathHelper.wrapDegrees((float)RotationStorage.mc.gameRenderer.getCamera().getYaw()));
        }
    }

    private void resetRotation() {
        Rotation targetRotation = new Rotation(FreeLookStorage.getFreeYaw(), FreeLookStorage.getFreePitch());
        if (this.updateRotation(targetRotation, this.currentYawReturnSpeed(), this.currentPitchReturnSpeed())) {
            this.stopRotation();
        }
    }

    @EventLink
    public void onEventTick(EventUpdate event) {
        if (this.currentTask().equals((Object)RotationTask.AIM) && this.idleTicks() > this.currentTimeout()) {
            this.currentTask(RotationTask.RESET);
        }
        if (this.currentTask().equals((Object)RotationTask.RESET)) {
            this.resetRotation();
        }
        ++this.idleTicks;
    }

    public static void update(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
        RotationStorage instance = RotationStorage.instance;
        if (RotationStorage.mc.player == null) {
            return;
        }
        if (instance.currentPriority() > priority) {
            return;
        }
        if (instance.currentTask().equals((Object)RotationTask.IDLE) && !clientRotation) {
            FreeLookStorage.setActive(true);
        }
        instance.currentYawSpeed(yawSpeed);
        instance.currentPitchSpeed(pitchSpeed);
        instance.currentYawReturnSpeed(yawReturnSpeed);
        instance.currentPitchReturnSpeed(pitchReturnSpeed);
        instance.currentTimeout(timeout);
        instance.currentPriority(priority);
        instance.currentTask(RotationTask.AIM);
        instance.targetRotation(target);
        instance.updateRotation(target, yawSpeed, pitchSpeed);
    }

    public static void update(Rotation targetRotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
        RotationStorage.update(targetRotation, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    public static void update(Rotation targetRotation, float yawSpeed, float pitchSpeed, float returnSpeed, int timeout, int priority) {
        RotationStorage.update(targetRotation, yawSpeed, pitchSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    private boolean updateRotation(Rotation targetRotation, float yawSpeed, float pitchSpeed) {
        if (RotationStorage.mc.player == null) {
            return false;
        }
        Rotation currentRotation = new Rotation((Entity)RotationStorage.mc.player);
        float yawDelta = MathHelper.wrapDegrees((float)(targetRotation.getYaw() - currentRotation.getYaw()));
        float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();
        float clampedYaw = Math.min(Math.abs(yawDelta), yawSpeed);
        float clampedPitch = Math.min(Math.abs(pitchDelta), pitchSpeed);
        float yaw = RotationStorage.mc.player.getYaw();
        RotationStorage.mc.player.setYaw(yaw += GCDUtil.getFixedRotation(MathHelper.clamp((float)yawDelta, (float)(-clampedYaw), (float)clampedYaw)));
        RotationStorage.mc.player.setPitch(MathHelper.clamp((float)(RotationStorage.mc.player.getPitch() + GCDUtil.getFixedRotation(MathHelper.clamp((float)pitchDelta, (float)(-clampedPitch), (float)clampedPitch))), (float)-90.0f, (float)90.0f));
        this.idleTicks(0);
        return new Rotation((Entity)RotationStorage.mc.player).getDelta(targetRotation) < 1.0f;
    }

    public void stopRotation() {
        this.currentTask(RotationTask.IDLE);
        this.currentPriority(0);
        FreeLookStorage.setActive(false);
    }

    public boolean isRotating() {
        return !this.currentTask.equals((Object)RotationTask.IDLE);
    }
    public RotationTask currentTask() {
        return this.currentTask;
    }
    public float currentYawSpeed() {
        return this.currentYawSpeed;
    }
    public float currentPitchSpeed() {
        return this.currentPitchSpeed;
    }
    public float currentYawReturnSpeed() {
        return this.currentYawReturnSpeed;
    }
    public float currentPitchReturnSpeed() {
        return this.currentPitchReturnSpeed;
    }
    public int currentPriority() {
        return this.currentPriority;
    }
    public int currentTimeout() {
        return this.currentTimeout;
    }
    public int idleTicks() {
        return this.idleTicks;
    }
    public Rotation targetRotation() {
        return this.targetRotation;
    }
    public RotationStorage currentTask(RotationTask currentTask) {
        this.currentTask = currentTask;
        return this;
    }
    public RotationStorage currentYawSpeed(float currentYawSpeed) {
        this.currentYawSpeed = currentYawSpeed;
        return this;
    }
    public RotationStorage currentPitchSpeed(float currentPitchSpeed) {
        this.currentPitchSpeed = currentPitchSpeed;
        return this;
    }
    public RotationStorage currentYawReturnSpeed(float currentYawReturnSpeed) {
        this.currentYawReturnSpeed = currentYawReturnSpeed;
        return this;
    }
    public RotationStorage currentPitchReturnSpeed(float currentPitchReturnSpeed) {
        this.currentPitchReturnSpeed = currentPitchReturnSpeed;
        return this;
    }
    public RotationStorage currentPriority(int currentPriority) {
        this.currentPriority = currentPriority;
        return this;
    }
    public RotationStorage currentTimeout(int currentTimeout) {
        this.currentTimeout = currentTimeout;
        return this;
    }
    public RotationStorage idleTicks(int idleTicks) {
        this.idleTicks = idleTicks;
        return this;
    }
    public RotationStorage targetRotation(Rotation targetRotation) {
        this.targetRotation = targetRotation;
        return this;
    }

    public static enum RotationTask {
        AIM,
        RESET,
        IDLE;

    }
}

