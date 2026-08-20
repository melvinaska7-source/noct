package polar.ru.client.modules.impl.render;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.AllayEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.AllayEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventAttackEntity;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.polar;

public class Satellite
extends Module {
    private static final Identifier ALLAY_TEXTURE = Identifier.ofVanilla((String)"textures/entity/allay/allay.png");
    private static final long ATTACK_FOLLOW_TIMEOUT_MS = 3600L;
    private static final long ATTACK_LAUNCH_DURATION_MS = 560L;
    private static final long ATTACK_RETURN_DURATION_MS = 920L;
    public static Satellite INSTANCE = new Satellite();
    public final ModeSetting shoulder = new ModeSetting("Плечо", "Правое", "Правое", "Левое");
    public final FloatSetting scale = new FloatSetting("Размер", 0.38f, 0.15f, 1.25f, 0.01f);
    public final FloatSetting offsetX = new FloatSetting("Смещение X", 0.0f, -1.0f, 1.0f, 0.01f);
    public final FloatSetting offsetY = new FloatSetting("Смещение Y", 0.18f, -1.0f, 1.0f, 0.01f);
    public final FloatSetting offsetZ = new FloatSetting("Смещение Z", 0.0f, -1.0f, 1.0f, 0.01f);
    public final FloatSetting rotateX = new FloatSetting("Поворот X", 0.0f, -180.0f, 180.0f, 1.0f);
    public final FloatSetting rotateY = new FloatSetting("Поворот Y", 0.0f, -180.0f, 180.0f, 1.0f);
    public final FloatSetting rotateZ = new FloatSetting("Поворот Z", 0.0f, -180.0f, 180.0f, 1.0f);
    public final BooleanSetting showSelf = new BooleanSetting("Показывать на себе", true);
    public final BooleanSetting showOthers = new BooleanSetting("Показывать на других", true);
    public final BooleanSetting showFriends = new BooleanSetting("Показывать на друзьях", true);
    public final BooleanSetting attackEnemies = new BooleanSetting("Атаковать врагов", true);
    public final BooleanSetting idleAnimation = new BooleanSetting("Idle-анимация", true);
    public final FloatSetting idleSpeed = new FloatSetting("Скорость idle", 1.0f, 0.1f, 3.0f, 0.05f).visible(() -> this.idleAnimation.isState());
    public final FloatSetting idleStrength = new FloatSetting("Сила idle", 0.35f, 0.0f, 1.5f, 0.05f).visible(() -> this.idleAnimation.isState());
    private final AllayEntityRenderState attackState = new AllayEntityRenderState();
    private AllayEntityModel attackModel;
    private int attackTargetId = Integer.MIN_VALUE;
    private long attackStartedAt;
    private long lastAttackAt;
    private long attackReturnStartedAt;
    private Vec3d attackReturnStartPos = new Vec3d(0.0, 0.0, 0.0);
    private float attackOrbitSeed;
    private float attackCurveSide;
    private float attackCurveLift;
    private float attackCurveDepth;
    private float attackRadiusJitter;
    private float attackHeightJitter;
    private float attackBobSeed;
    private float attackOrbitSpeed;
    private float attackOrbitDirection;
    private float attackLookYaw;
    private float attackLookPitch;
    private boolean attackLookInitialized;

    public Satellite() {
        super("Satellite", "Питомец-аллей на плече", Module.ModuleCategory.RENDER);
        this.addSettings(this.shoulder, this.scale, this.offsetX, this.offsetY, this.offsetZ, this.rotateX, this.rotateY, this.rotateZ, this.showSelf, this.showOthers, this.showFriends, this.attackEnemies, this.idleAnimation, this.idleSpeed, this.idleStrength);
    }

    @Override
    public void onDisable() {
        this.clearAttackTarget();
        super.onDisable();
    }

    @EventLink
    public void onAttack(EventAttackEntity event) {
        if (!this.attackEnemies.isState() || event == null || event.getPlayer() == null || event.getTarget() == null || Satellite.mc.player == null) {
            return;
        }
        if (event.getPlayer().getId() != Satellite.mc.player.getId() || event.getTarget() == Satellite.mc.player) {
            return;
        }
        long now = System.currentTimeMillis();
        if (this.attackTargetId != event.getTarget().getId()) {
            this.attackStartedAt = now;
            this.randomizeAttackPath(now);
        }
        this.attackTargetId = event.getTarget().getId();
        this.lastAttackAt = now;
        this.attackReturnStartedAt = 0L;
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (!this.attackEnemies.isState()) {
            this.clearAttackTarget();
            return;
        }
        this.updateAttackLifecycle();
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        if (Satellite.mc.player == null || Satellite.mc.world == null || event == null) {
            return;
        }
        float tickDelta = event.getTickDelta();
        long now = System.currentTimeMillis();
        Entity target = this.updateAttackLifecycle();
        if (target == null) {
            return;
        }
        this.ensureAttackModel();
        if (this.attackModel == null) {
            return;
        }
        this.renderAttackSatellite(event, target, this.getAttackRenderPosition(target, tickDelta, now), tickDelta, now);
    }

    private void renderAttackSatellite(Event3DRender event, Entity target, Vec3d renderPos, float tickDelta, long now) {
        EntityPose var_4050_2;
        Vec3d cameraPos = event.getCamera().getPos();
        Vec3d targetPos = this.getInterpolatedEntityPos(target, tickDelta);
        float elapsed = (float)(now - this.attackStartedAt) / 1000.0f;
        Vec3d focusPos = targetPos.add(0.0, (double)target.getHeight() * 0.56, 0.0);
        float desiredYaw = this.getLookYaw(renderPos, focusPos);
        float desiredPitch = this.getLookPitch(renderPos, focusPos);
        if (!this.attackLookInitialized) {
            this.attackLookYaw = desiredYaw;
            this.attackLookPitch = desiredPitch;
            this.attackLookInitialized = true;
        } else {
            this.attackLookYaw = MathHelper.lerpAngleDegrees((float)0.32f, (float)this.attackLookYaw, (float)desiredYaw);
            this.attackLookPitch = MathHelper.lerp((float)0.24f, (float)this.attackLookPitch, (float)desiredPitch);
        }
        float headYaw = MathHelper.clamp((float)MathHelper.wrapDegrees((float)(desiredYaw - this.attackLookYaw)), (float)-85.0f, (float)85.0f);
        MatrixStack matrices = event.getMatrices();
        matrices.push();
        matrices.translate(renderPos.x - cameraPos.x, renderPos.y - cameraPos.y, renderPos.z - cameraPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - this.attackLookYaw));
        matrices.scale(this.scale.get(), this.scale.get(), this.scale.get());
        matrices.scale(-1.0f, -1.0f, 1.0f);
        matrices.translate(0.0f, -1.501f, 0.0f);
        this.attackState.age = (float)Satellite.mc.player.age + tickDelta + elapsed * 20.0f;
        this.attackState.limbFrequency = elapsed * 6.4f;
        this.attackState.limbAmplitudeMultiplier = 0.72f + MathHelper.sin((float)(elapsed * 7.0f + this.attackBobSeed)) * 0.12f;
        this.attackState.yawDegrees = headYaw;
        this.attackState.pitch = this.attackLookPitch;
        this.attackState.invisible = false;
        this.attackState.invisibleToPlayer = false;
        this.attackState.hasOutline = false;
        this.attackState.shaking = false;
        this.attackState.baby = false;
        this.attackState.touchingWater = target.isTouchingWater();
        this.attackState.bodyYaw = this.attackLookYaw;
        this.attackState.baseScale = 1.0f;
        this.attackState.ageScale = 1.0f;
        if (target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)target;
            var_4050_2 = living.getPose();
        } else {
            var_4050_2 = EntityPose.STANDING;
        }
        this.attackState.pose = var_4050_2;
        this.attackState.deathTime = 0.0f;
        this.attackState.hurt = false;
        this.attackState.dancing = false;
        this.attackState.spinning = false;
        this.attackState.spinningAnimationTicks = 0.0f;
        this.attackState.itemHoldAnimationTicks = 0.65f;
        this.attackModel.setAngles(this.attackState);
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vertexConsumer = immediate.getBuffer(this.attackModel.getLayer(ALLAY_TEXTURE));
        this.attackModel.render(matrices, vertexConsumer, 0xF000F0, OverlayTexture.DEFAULT_UV);
        immediate.draw();
        matrices.pop();
    }

    public boolean shouldRender(PlayerEntityRenderState playerState) {
        boolean self;
        if (!this.isEnable() || Satellite.mc.player == null || Satellite.mc.world == null || playerState == null || playerState.spectator) {
            return false;
        }
        boolean bl = self = playerState.id == Satellite.mc.player.getId();
        if (self) {
            if (this.hasActiveAttackTarget()) {
                return false;
            }
            return this.shouldRenderOwnShoulderPet();
        }
        Entity entity = Satellite.mc.world.getEntityById(playerState.id);
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if (polar.INSTANCE != null && polar.INSTANCE.friendStorage != null && polar.INSTANCE.friendStorage.isFriend(player.getName().getString())) {
                return this.showFriends.isState();
            }
        }
        return this.showOthers.isState();
    }

    public boolean isLeftShoulder() {
        return this.shoulder.is("Левое");
    }

    public boolean hasActiveAttackTarget() {
        return this.updateAttackLifecycle() != null;
    }

    private boolean shouldRenderOwnShoulderPet() {
        return this.showSelf.isState() && !Satellite.mc.options.getPerspective().isFirstPerson();
    }

    private Entity updateAttackLifecycle() {
        LivingEntity living;
        if (!this.attackEnemies.isState() || Satellite.mc.world == null || Satellite.mc.player == null || this.attackTargetId == Integer.MIN_VALUE) {
            return null;
        }
        Entity target = Satellite.mc.world.getEntityById(this.attackTargetId);
        if (target == null || target.isRemoved() || target == Satellite.mc.player) {
            this.clearAttackTarget();
            return null;
        }
        if (target instanceof LivingEntity && !(living = (LivingEntity)target).isAlive()) {
            this.clearAttackTarget();
            return null;
        }
        if (Satellite.mc.player.squaredDistanceTo(target) > 4096.0) {
            this.clearAttackTarget();
            return null;
        }
        long now = System.currentTimeMillis();
        if (this.attackReturnStartedAt == 0L && now - this.lastAttackAt > 3600L) {
            float elapsed = (float)(now - this.attackStartedAt) / 1000.0f;
            this.attackReturnStartPos = this.getOrbitPosition(target, this.getInterpolatedEntityPos(target, 1.0f), elapsed);
            this.attackReturnStartedAt = now;
        }
        if (this.attackReturnStartedAt != 0L && now - this.attackReturnStartedAt > 920L) {
            this.clearAttackTarget();
            return null;
        }
        return target;
    }

    private Vec3d getAttackRenderPosition(Entity target, float tickDelta, long now) {
        Vec3d shoulderPos = this.getShoulderWorldPosition(tickDelta);
        Vec3d targetPos = this.getInterpolatedEntityPos(target, tickDelta);
        float elapsed = (float)(now - this.attackStartedAt) / 1000.0f;
        Vec3d orbitPos = this.getOrbitPosition(target, targetPos, elapsed);
        if (this.attackReturnStartedAt == 0L) {
            float launchProgress = MathHelper.clamp((float)((float)(now - this.attackStartedAt) / 560.0f), (float)0.0f, (float)1.0f);
            if (launchProgress < 1.0f) {
                return this.buildLaunchCurve(shoulderPos, orbitPos, launchProgress);
            }
            return orbitPos;
        }
        float returnProgress = MathHelper.clamp((float)((float)(now - this.attackReturnStartedAt) / 920.0f), (float)0.0f, (float)1.0f);
        return this.buildReturnCurve(this.attackReturnStartPos, shoulderPos, returnProgress);
    }

    private Vec3d getOrbitPosition(Entity target, Vec3d targetPos, float elapsed) {
        double baseRadius = Math.max(0.86, (double)target.getWidth() * 1.05 + 0.46) * (double)this.attackRadiusJitter;
        double angle = this.attackOrbitSeed * ((float)Math.PI / 180) + elapsed * this.attackOrbitSpeed * this.attackOrbitDirection;
        double radiusPulse = Math.sin(elapsed * 1.25f + this.attackBobSeed * 0.45f) * 0.07;
        double orbitRadius = baseRadius + radiusPulse;
        double orbitX = Math.cos(angle) * orbitRadius;
        double orbitZ = Math.sin(angle) * orbitRadius;
        double orbitY = targetPos.y + (double)target.getHeight() * (0.78 + (double)this.attackHeightJitter) + Math.sin(elapsed * 2.9f + this.attackBobSeed) * 0.2 + Math.cos(elapsed * 1.8f + this.attackBobSeed * 0.8f) * 0.08;
        return new Vec3d(targetPos.x + orbitX, orbitY, targetPos.z + orbitZ);
    }

    private Vec3d buildLaunchCurve(Vec3d start, Vec3d end, float progress) {
        float eased = this.easeInOut(progress);
        Vec3d direction = end.subtract(start);
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
        horizontal = horizontal.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : horizontal.normalize();
        Vec3d sideways = new Vec3d(horizontal.z, 0.0, -horizontal.x).normalize();
        Vec3d lift = new Vec3d(0.0, (double)this.attackCurveLift, 0.0);
        Vec3d control1 = start.add(sideways.multiply((double)this.attackCurveSide * 0.52)).add(lift.multiply(0.82));
        Vec3d control2 = end.add(sideways.multiply((double)(-this.attackCurveSide) * 0.28)).add(horizontal.multiply((double)this.attackCurveDepth * 0.18)).add(lift.multiply(0.58));
        return this.cubicBezier(start, control1, control2, end, eased);
    }

    private Vec3d buildReturnCurve(Vec3d start, Vec3d end, float progress) {
        float eased = this.easeInOut(progress);
        Vec3d direction = end.subtract(start);
        Vec3d horizontal = new Vec3d(direction.x, 0.0, direction.z);
        horizontal = horizontal.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : horizontal.normalize();
        Vec3d sideways = new Vec3d(horizontal.z, 0.0, -horizontal.x).normalize();
        Vec3d lift = new Vec3d(0.0, (double)this.attackCurveLift * 0.72, 0.0);
        Vec3d control1 = start.add(sideways.multiply((double)(-this.attackCurveSide) * 0.24)).add(lift.multiply(0.62));
        Vec3d control2 = end.add(sideways.multiply((double)this.attackCurveSide * 0.3)).add(horizontal.multiply((double)(-this.attackCurveDepth) * 0.1)).add(lift.multiply(0.22));
        Vec3d bezier = this.cubicBezier(start, control1, control2, end, eased);
        return eased > 0.985f ? end : bezier;
    }

    private Vec3d getShoulderWorldPosition(float tickDelta) {
        Vec3d playerPos = this.getInterpolatedEntityPos((Entity)Satellite.mc.player, tickDelta);
        float bodyYaw = MathHelper.lerpAngleDegrees((float)tickDelta, (float)Satellite.mc.player.prevBodyYaw, (float)Satellite.mc.player.bodyYaw);
        float yawRad = bodyYaw * ((float)Math.PI / 180);
        Vec3d forward = new Vec3d((double)(-MathHelper.sin((float)yawRad)), 0.0, (double)MathHelper.cos((float)yawRad));
        Vec3d right = new Vec3d(forward.z, 0.0, -forward.x);
        double side = (this.isLeftShoulder() ? 1.0 : -1.0) * (double)Satellite.mc.player.getWidth() * 0.42;
        double height = (double)Satellite.mc.player.getHeight() - (Satellite.mc.player.isSneaking() ? 0.38 : 0.24);
        double back = 0.0;
        Vec3d shoulderPos = playerPos.add(0.0, height, 0.0).add(right.multiply(side)).add(forward.multiply(back)).add(right.multiply((double)this.offsetX.get() * 0.65)).add(0.0, (double)this.offsetY.get() * 0.45, 0.0).add(forward.multiply((double)this.offsetZ.get() * 0.35));
        if (this.idleAnimation.isState()) {
            float time = ((float)Satellite.mc.player.age + tickDelta) * (0.7f + this.idleSpeed.get() * 0.65f);
            float bob = MathHelper.sin((float)(time * 0.42f)) * 0.03f * this.idleStrength.get();
            shoulderPos = shoulderPos.add(0.0, (double)bob, 0.0);
        }
        return shoulderPos;
    }

    private Vec3d getInterpolatedEntityPos(Entity entity, float tickDelta) {
        return new Vec3d(MathHelper.lerp((double)tickDelta, (double)entity.prevX, (double)entity.getX()), MathHelper.lerp((double)tickDelta, (double)entity.prevY, (double)entity.getY()), MathHelper.lerp((double)tickDelta, (double)entity.prevZ, (double)entity.getZ()));
    }

    private Vec3d cubicBezier(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t2) {
        float inv = 1.0f - t2;
        double w0 = inv * inv * inv;
        double w1 = 3.0 * (double)inv * (double)inv * (double)t2;
        double w2 = 3.0 * (double)inv * (double)t2 * (double)t2;
        double w3 = t2 * t2 * t2;
        return new Vec3d(p0.x * w0 + p1.x * w1 + p2.x * w2 + p3.x * w3, p0.y * w0 + p1.y * w1 + p2.y * w2 + p3.y * w3, p0.z * w0 + p1.z * w1 + p2.z * w2 + p3.z * w3);
    }

    private float easeInOut(float value) {
        float clamped = MathHelper.clamp((float)value, (float)0.0f, (float)1.0f);
        return clamped * clamped * clamped * (clamped * (clamped * 6.0f - 15.0f) + 10.0f);
    }

    private void ensureAttackModel() {
        if (this.attackModel != null || mc == null) {
            return;
        }
        this.attackModel = new AllayEntityModel(mc.getLoadedEntityModels().getModelPart(EntityModelLayers.ALLAY));
    }

    private void randomizeAttackPath(long now) {
        this.attackOrbitSeed = this.randomRange(0.0f, 360.0f);
        this.attackCurveSide = this.randomRange(-1.1f, 1.1f);
        this.attackCurveLift = this.randomRange(0.48f, 0.96f);
        this.attackCurveDepth = this.randomRange(-0.42f, 0.42f);
        this.attackRadiusJitter = this.randomRange(0.92f, 1.24f);
        this.attackHeightJitter = this.randomRange(-0.06f, 0.14f);
        this.attackBobSeed = this.randomRange(0.0f, (float)Math.PI * 2);
        this.attackOrbitSpeed = this.randomRange(1.7f, 2.45f);
        this.attackOrbitDirection = Math.random() > 0.5 ? 1.0f : -1.0f;
    }

    private float randomRange(float min, float max) {
        return min + (float)Math.random() * (max - min);
    }

    private float getLookYaw(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
    }

    private float getLookPitch(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        return MathHelper.clamp((float)((float)(-Math.toDegrees(Math.atan2(dy, horizontalDistance)))), (float)-35.0f, (float)35.0f);
    }

    private void clearAttackTarget() {
        this.attackTargetId = Integer.MIN_VALUE;
        this.attackStartedAt = 0L;
        this.lastAttackAt = 0L;
        this.attackReturnStartedAt = 0L;
        this.attackReturnStartPos = new Vec3d(0.0, 0.0, 0.0);
        this.attackLookYaw = 0.0f;
        this.attackLookPitch = 0.0f;
        this.attackLookInitialized = false;
    }
}

