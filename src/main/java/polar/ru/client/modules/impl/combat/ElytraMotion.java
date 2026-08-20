package polar.ru.client.modules.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventMove;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.math.TimerUtils;
import polar.ru.api.utils.player.InventoryUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.combat.ElytraTarget;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class ElytraMotion
extends Module {
    public static ElytraMotion INSTANCE = new ElytraMotion();
    private final ModeSetting mode = new ModeSetting("Mode", "New", "New", "Rw");
    public final BooleanSetting moment = new BooleanSetting("Moment", true);
    public final BooleanSetting speedCheck = new BooleanSetting("Speed Check", true);
    public final FloatSetting attackDistance = new FloatSetting("Distance", 3.0f, 1.0f, 6.0f, 0.1f);
    private final BooleanSetting autoFirework = new BooleanSetting("Auto Firework", false);
    private final TimerUtils timer = new TimerUtils();
    public boolean freeze = false;
    private boolean wasLocked = false;
    private Vec3d frozenPosition = null;
    private Vec3d beforeFreezeVelocity = null;

    public ElytraMotion() {
        super("Elytra Motion", "Останавливает движение при бою на элитрах", Module.ModuleCategory.MOVEMENT);
        this.addSettings(this.mode, this.moment, this.speedCheck, this.attackDistance, this.autoFirework);
    }

    @EventLink
    public void eventUpdate(EventUpdate event) {
        Aura killAura;
        if (ElytraMotion.mc.player == null || ElytraMotion.mc.world == null) {
            this.freeze = false;
            this.unlockKeys();
            return;
        }
        if (this.mode.is("New")) {
            this.updateNew();
        } else {
            this.updateOld();
        }
        if (this.autoFirework.isState() && (killAura = ModuleClass.aura) != null && killAura.getTarget() != null && this.timer.finished(500L)) {
            InventoryUtils.swapAndUseHvH(Items.FIREWORK_ROCKET);
            this.timer.reset();
        }
    }

    private void updateNew() {
        boolean isSpeed;
        if (!ElytraMotion.mc.player.isGliding() || this.shouldSuspendForElytraTarget()) {
            this.unfreezeNew();
            return;
        }
        Aura aura = ModuleClass.aura;
        LivingEntity target = aura != null ? aura.getTarget() : null;
        boolean bl = isSpeed = this.speedCheck.isState() && target != null && this.getEntityBPS(target) >= 20.0;
        if (!(target == null || !(this.getHeadDistance(target) <= this.attackDistance.getValue().doubleValue()) || isSpeed || this.moment.isState() && target.isGliding())) {
            if (!this.freeze) {
                this.frozenPosition = ElytraMotion.mc.player.getPos();
                this.beforeFreezeVelocity = ElytraMotion.mc.player.getVelocity();
            }
            this.freeze = true;
            this.lockKeys();
        } else {
            if (this.beforeFreezeVelocity != null) {
                ElytraMotion.mc.player.setVelocity(this.beforeFreezeVelocity.x, 0.0, this.beforeFreezeVelocity.z);
            }
            this.unfreezeNew();
        }
    }

    private void updateOld() {
        if (!ElytraMotion.mc.player.isGliding()) {
            this.freeze = false;
            this.unlockKeys();
            return;
        }
        Aura aura = ModuleClass.aura;
        ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
        this.freeze = this.checkOld(aura, elytraTarget);
        this.unlockKeys();
    }

    @EventLink
    public void eventMotion(EventMove event) {
        if (ElytraMotion.mc.player == null || !this.freeze) {
            return;
        }
        if (this.mode.is("New") && ElytraMotion.mc.player.isGliding() && this.frozenPosition != null) {
            event.setMovePos(Vec3d.ZERO);
            ElytraMotion.mc.player.setVelocity(0.0, 0.0, 0.0);
            ElytraMotion.mc.player.updatePosition(this.frozenPosition.x, this.frozenPosition.y, this.frozenPosition.z);
        }
        if (this.mode.is("Rw") && ElytraMotion.mc.player.isGliding()) {
            event.setMovePos(Vec3d.ZERO);
        }
    }

    private boolean checkOld(Aura aura, ElytraTarget elytraTarget) {
        if (aura == null || elytraTarget == null) {
            return false;
        }
        LivingEntity target = aura.getTarget();
        if (target != null && ElytraMotion.mc.player.isGliding()) {
            boolean canTarget = elytraTarget.shouldSyncTargetFlight(target);
            return !canTarget && ElytraMotion.mc.player.distanceTo((Entity)target) < this.attackDistance.getValue().floatValue();
        }
        return false;
    }

    private boolean shouldSuspendForElytraTarget() {
        boolean auraFlightActive;
        ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
        if (elytraTarget == null || !elytraTarget.isPredictionActive()) {
            return false;
        }
        Aura aura = ModuleClass.aura;
        LivingEntity target = aura != null && aura.isEnable() ? aura.getTarget() : null;
        boolean bl = auraFlightActive = ElytraMotion.mc.player.isGliding() && target != null && target.isGliding();
        if (auraFlightActive) {
            this.freeze = false;
            this.frozenPosition = null;
            this.beforeFreezeVelocity = null;
        }
        return auraFlightActive;
    }

    private double getHeadDistance(LivingEntity target) {
        Vec3d playerEye = ElytraMotion.mc.player.getEyePos();
        Vec3d targetHead = target.getEyePos().add(0.0, (target.getY() - target.prevY) * 2.0, 0.0);
        return playerEye.distanceTo(targetHead);
    }

    private double getEntityBPS(LivingEntity entity) {
        double dx = entity.getX() - entity.prevX;
        double dz = entity.getZ() - entity.prevZ;
        return Math.sqrt(dx * dx + dz * dz) * 20.0;
    }

    private void unfreezeNew() {
        this.freeze = false;
        this.frozenPosition = null;
        this.beforeFreezeVelocity = null;
        this.unlockKeys();
    }

    private void lockKeys() {
        if (!this.wasLocked) {
            ElytraMotion.mc.options.forwardKey.setPressed(false);
            ElytraMotion.mc.options.backKey.setPressed(false);
            ElytraMotion.mc.options.leftKey.setPressed(false);
            ElytraMotion.mc.options.rightKey.setPressed(false);
            ElytraMotion.mc.options.jumpKey.setPressed(false);
            ElytraMotion.mc.options.sneakKey.setPressed(false);
            this.wasLocked = true;
        }
    }

    private void unlockKeys() {
        if (this.wasLocked) {
            this.wasLocked = false;
        }
    }

    @Override
    public void onDisable() {
        this.freeze = false;
        this.frozenPosition = null;
        this.beforeFreezeVelocity = null;
        this.unlockKeys();
        super.onDisable();
    }
}

