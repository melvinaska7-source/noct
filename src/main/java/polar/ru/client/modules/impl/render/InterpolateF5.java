package polar.ru.client.modules.impl.render;

import net.minecraft.util.math.MathHelper;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventRotation;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.storages.implement.FreeLookStorage;
import polar.ru.client.modules.Module;

public class InterpolateF5
extends Module {
    public static InterpolateF5 INSTANCE = new InterpolateF5();
    private static final float SWITCH_ANIM_SPEED = 0.26f;
    private static final float DISTANCE_SPEED = 0.13f;
    private static final float ROTATION_SMOOTH = 0.28f;
    private static final float CAMERA_DISTANCE = 4.1f;
    private static final float SNEAK_OFFSET = 0.5f;
    private static final float JUMP_MULTIPLIER = 2.0f;
    private static final float ANIM_SPEED = 0.13f;
    private float currentDistance;
    private float prevDistance;
    private float currentYaw;
    private float prevYaw;
    private float currentPitch;
    private float prevPitch;
    private float heightOffset;
    private float prevHeightOffset;
    private boolean switchAnimating;
    private boolean wasThirdPerson;
    private boolean needsInit = true;

    public InterpolateF5() {
        super("Cinematic Camera", "Плавная камера от ф5", Module.ModuleCategory.RENDER);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        boolean isThirdPerson;
        if (InterpolateF5.mc.player == null || InterpolateF5.mc.world == null) {
            return;
        }
        boolean bl = isThirdPerson = !InterpolateF5.mc.options.getPerspective().isFirstPerson();
        if (isThirdPerson && !this.wasThirdPerson) {
            this.initCamera(true);
        }
        if (!isThirdPerson && this.wasThirdPerson) {
            this.needsInit = true;
            this.switchAnimating = false;
        }
        this.wasThirdPerson = isThirdPerson;
        if (isThirdPerson) {
            this.updateCamera();
        }
    }

    @EventLink(priority=100)
    public void onRotation(EventRotation event) {
        if (InterpolateF5.mc.player == null || InterpolateF5.mc.world == null) {
            return;
        }
        if (InterpolateF5.mc.options.getPerspective().isFirstPerson()) {
            return;
        }
        event.setYaw(this.getInterpolatedYaw(event.getPartialTicks()));
        event.setPitch(this.getInterpolatedPitch(event.getPartialTicks()));
    }

    private void initCamera(boolean animateSwitch) {
        if (InterpolateF5.mc.player == null) {
            return;
        }
        this.currentYaw = this.prevYaw = this.getReferenceYaw();
        this.currentPitch = this.prevPitch = this.getReferencePitch();
        this.prevDistance = animateSwitch ? 0.0f : 4.1f;
        this.currentDistance = this.prevDistance;
        this.prevHeightOffset = 0.0f;
        this.heightOffset = 0.0f;
        this.switchAnimating = animateSwitch;
        this.needsInit = false;
    }

    private void updateCamera() {
        if (InterpolateF5.mc.player == null) {
            return;
        }
        if (this.needsInit) {
            this.initCamera(true);
            return;
        }
        this.prevYaw = this.currentYaw;
        this.prevPitch = this.currentPitch;
        this.prevDistance = this.currentDistance;
        this.prevHeightOffset = this.heightOffset;
        float rotationSpeed = 0.28f;
        this.currentYaw += MathHelper.wrapDegrees((float)(this.getReferenceYaw() - this.currentYaw)) * rotationSpeed;
        this.currentPitch = MathHelper.clamp((float)(this.currentPitch + (this.getReferencePitch() - this.currentPitch) * rotationSpeed), (float)-90.0f, (float)90.0f);
        float distanceSpeed = this.switchAnimating ? 0.26f : 0.13f;
        this.currentDistance += (4.1f - this.currentDistance) * distanceSpeed;
        if (this.switchAnimating && Math.abs(4.1f - this.currentDistance) <= 0.02f) {
            this.currentDistance = 4.1f;
            this.switchAnimating = false;
        }
        float targetOffset = 0.0f;
        if (InterpolateF5.mc.player.isSneaking()) {
            targetOffset = -0.5f;
        }
        if (!InterpolateF5.mc.player.isOnGround()) {
            targetOffset += (float)(-InterpolateF5.mc.player.getVelocity().y * 2.0);
        }
        this.heightOffset += (targetOffset - this.heightOffset) * 0.13f;
    }

    public float getInterpolatedYaw(float partialTicks) {
        if (InterpolateF5.mc.player == null) {
            return 0.0f;
        }
        return this.prevYaw + (this.currentYaw - this.prevYaw) * partialTicks;
    }

    public float getInterpolatedPitch(float partialTicks) {
        if (InterpolateF5.mc.player == null) {
            return 0.0f;
        }
        return MathHelper.clamp((float)(this.prevPitch + (this.currentPitch - this.prevPitch) * partialTicks), (float)-90.0f, (float)90.0f);
    }

    public float getInterpolatedDistance(float partialTicks) {
        return this.prevDistance + (this.currentDistance - this.prevDistance) * partialTicks;
    }

    public float getInterpolatedHeightOffset(float partialTicks) {
        return this.prevHeightOffset + (this.heightOffset - this.prevHeightOffset) * partialTicks;
    }

    private float getReferenceYaw() {
        if (FreeLookStorage.isActive()) {
            return FreeLookStorage.getFreeYaw();
        }
        return InterpolateF5.mc.player != null ? InterpolateF5.mc.player.getYaw() : 0.0f;
    }

    private float getReferencePitch() {
        if (FreeLookStorage.isActive()) {
            return FreeLookStorage.getFreePitch();
        }
        return InterpolateF5.mc.player != null ? InterpolateF5.mc.player.getPitch() : 0.0f;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.needsInit = true;
        this.wasThirdPerson = false;
        if (InterpolateF5.mc.player != null && !InterpolateF5.mc.options.getPerspective().isFirstPerson()) {
            this.initCamera(true);
            this.wasThirdPerson = true;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.needsInit = true;
        this.heightOffset = 0.0f;
        this.prevHeightOffset = 0.0f;
    }
}

