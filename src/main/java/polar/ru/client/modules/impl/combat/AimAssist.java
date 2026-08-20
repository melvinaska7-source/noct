package polar.ru.client.modules.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventGameUpdate;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.polar;

public class AimAssist
extends Module {
    public static AimAssist INSTANCE = new AimAssist();
    private final ListSetting targetTypes = new ListSetting("Типы целей", new BooleanSetting("Игроки", true), new BooleanSetting("Невидимки", true), new BooleanSetting("Мирные", false), new BooleanSetting("Мобы", true));
    private final FloatSetting range = new FloatSetting("Дистанция", 4.5f, 1.0f, 10.0f, 0.1f);
    private final FloatSetting speed = new FloatSetting("Скорость наводки", 5.0f, 1.0f, 20.0f, 0.5f);
    private final FloatSetting fov = new FloatSetting("FOV", 90.0f, 30.0f, 180.0f, 5.0f);
    private final BooleanSetting silent = new BooleanSetting("Тихие повороты", true);
    private final BooleanSetting clickAim = new BooleanSetting("Наводка по клику", false);
    private final BooleanSetting ignoreTeammates = new BooleanSetting("Игнорировать тиммейтов", false);
    private LivingEntity target;
    private boolean aiming;

    public AimAssist() {
        super("AimAssist", "Помогает наводиться на цели", Module.ModuleCategory.COMBAT);
        this.addSettings(this.targetTypes, this.range, this.speed, this.fov, this.silent, this.clickAim, this.ignoreTeammates);
    }

        private boolean isValidTarget(LivingEntity entity) {
        if (entity == AimAssist.mc.player) {
            return false;
        }
        if (!entity.isAlive() || entity.getHealth() <= 0.0f) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if (!this.targetTypes.is("Игроки")) {
                return false;
            }
            if (!this.targetTypes.is("Невидимки") && player.isInvisible()) {
                return false;
            }
            if (polar.INSTANCE.friendStorage.isFriend(entity.getName().getString())) {
                return false;
            }
            if (this.ignoreTeammates.isState() && this.isTeammate(player)) {
                return false;
            }
        } else if (entity instanceof HostileEntity ? !this.targetTypes.is("Мобы") : !this.targetTypes.is("Мирные")) {
            return false;
        }
        return true;
    }

    private boolean isTeammate(PlayerEntity player) {
        if (AimAssist.mc.player == null) {
            return false;
        }
        return AimAssist.mc.player.getScoreboardTeam() != null && player.getScoreboardTeam() != null && AimAssist.mc.player.getScoreboardTeam().equals(player.getScoreboardTeam());
    }

        private LivingEntity findBestTarget() {
        ArrayList<LivingEntity> targets = new ArrayList<LivingEntity>();
        float rangeValue = this.range.getValue().floatValue();
        for (Entity entity2 : AimAssist.mc.world.getEntities()) {
            Vec3d targetPos;
            Vec3d eyePos;
            double angle;
            double dist;
            LivingEntity living;
            if (!(entity2 instanceof LivingEntity) || !this.isValidTarget(living = (LivingEntity)entity2) || (dist = (double)AimAssist.mc.player.distanceTo((Entity)living)) > (double)rangeValue || (angle = Math.toDegrees(Math.acos((eyePos = AimAssist.mc.player.getEyePos()).subtract(targetPos = living.getBoundingBox().getCenter()).normalize().dotProduct(AimAssist.mc.player.getRotationVec(1.0f).normalize())))) > (double)(this.fov.getValue().floatValue() / 2.0f)) continue;
            targets.add(living);
        }
        if (targets.isEmpty()) {
            return null;
        }
        targets.sort(Comparator.comparingDouble(entity -> {
            double dist = AimAssist.mc.player.distanceTo((Entity)entity);
            Vec3d eyePos = AimAssist.mc.player.getEyePos();
            Vec3d targetPos = entity.getBoundingBox().getCenter();
            double angle = Math.toDegrees(Math.acos(eyePos.subtract(targetPos).normalize().dotProduct(AimAssist.mc.player.getRotationVec(1.0f).normalize())));
            return dist * 0.7 + angle * 0.3;
        }));
        return (LivingEntity)targets.get(0);
    }

    @EventLink
    public void onGameUpdate(EventGameUpdate e2) {
        if (AimAssist.mc.player == null || AimAssist.mc.world == null) {
            return;
        }
        if (this.clickAim.isState() && !AimAssist.mc.options.attackKey.isPressed()) {
            this.target = null;
            this.aiming = false;
            return;
        }
        LivingEntity newTarget = this.findBestTarget();
        if (newTarget != null) {
            this.target = newTarget;
            this.aiming = true;
            this.aimAtTarget();
        } else {
            this.target = null;
            this.aiming = false;
        }
    }

        private void aimAtTarget() {
        if (this.target == null) {
            return;
        }
        Vec3d targetPos = this.target.getBoundingBox().getCenter();
        Vec2f targetRot = RotationUtils.getRotations(targetPos);
        float currentYaw = AimAssist.mc.player.getYaw();
        float currentPitch = AimAssist.mc.player.getPitch();
        float targetYaw = targetRot.x;
        float targetPitch = targetRot.y;
        float yawDiff = MathHelper.wrapDegrees((float)(targetYaw - currentYaw));
        float pitchDiff = targetPitch - currentPitch;
        float speedValue = this.speed.getValue().floatValue();
        float maxChange = speedValue * 0.5f;
        float newYaw = currentYaw + MathHelper.clamp((float)yawDiff, (float)(-maxChange), (float)maxChange);
        float newPitch = currentPitch + MathHelper.clamp((float)pitchDiff, (float)(-maxChange), (float)maxChange);
        if (this.silent.isState()) {
            float gcd = GCDUtil.getGCD();
            newYaw -= (newYaw - AimAssist.mc.player.getYaw()) % gcd;
            newPitch -= (newPitch - AimAssist.mc.player.getPitch()) % gcd;
            RotationStorage.update(new Rotation(newYaw, newPitch), 180.0f, 180.0f, 45.0f, 45.0f, 0, 2, false);
        } else {
            AimAssist.mc.player.setYaw(newYaw);
            AimAssist.mc.player.setPitch(newPitch);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.target = null;
        this.aiming = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.target = null;
        this.aiming = false;
    }
    public LivingEntity getTarget() {
        return this.target;
    }
}

