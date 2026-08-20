package polar.ru.client.modules.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventMoveInput;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.combat.IdealHitUtils;
import polar.ru.api.utils.math.TimerUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.movement.Sprint;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.polar;

public class TriggerBot
extends Module {
    public static TriggerBot INSTANCE = new TriggerBot();
    private final FloatSetting range = new FloatSetting("Дистанция атаки", 3.0f, 0.0f, 6.0f, 0.05f);
    private final ListSetting options = new ListSetting("Опции", new BooleanSetting("Только криты", false), new BooleanSetting("Умные криты", true), new BooleanSetting("Сброс спринта", true), new BooleanSetting("Бить через стены", false), new BooleanSetting("Проверка на наведение", true), new BooleanSetting("Отжимать щит", false), new BooleanSetting("Ломать щит", true));
    private final ListSetting targets = new ListSetting("Таргеты", new BooleanSetting("Игроки", true), new BooleanSetting("Невидимки", true), new BooleanSetting("Мирные", false), new BooleanSetting("Мобы", true));
    private LivingEntity target;
    private final TimerUtils attackTimer = new TimerUtils();
    private boolean needSprintReset = false;
    private boolean sprintResetDone = false;
    private int sprintResetTicks = 0;

    public TriggerBot() {
        super("TriggerBot", "Автоматически атакует при наведении на цель", Module.ModuleCategory.COMBAT);
        this.addSettings(this.range, this.options, this.targets);
    }

    @EventLink
    public void onMoveInput(EventMoveInput event) {
        if (this.needSprintReset) {
            event.setForward(0.0f);
            event.setStrafe(0.0f);
            this.needSprintReset = false;
            this.sprintResetDone = true;
            this.sprintResetTicks = 0;
        }
    }

    @EventLink
    public void onUpdate(EventUpdate e2) {
        if (TriggerBot.mc.player == null || TriggerBot.mc.world == null) {
            return;
        }
        if (this.sprintResetDone) {
            ++this.sprintResetTicks;
        }
        this.target = this.getTargetUnderCrosshair();
        if (this.target != null) {
            this.processAttack();
        } else {
            this.resetSprintState();
        }
    }

    private void processAttack() {
        if (!this.shouldAttack()) {
            return;
        }
        if (this.options.is("Сброс спринта") && TriggerBot.mc.player.isSprinting() && !this.sprintResetDone && !this.shouldSkipSprintResetInWater()) {
            this.needSprintReset = true;
            return;
        }
        if (this.options.is("Сброс спринта") && this.sprintResetDone && this.sprintResetTicks < 1) {
            return;
        }
        this.attack();
        this.sprintResetDone = false;
        this.sprintResetTicks = 0;
    }

    private LivingEntity getTargetUnderCrosshair() {
        LivingEntity living;
        Entity var_1297_2;
        float rangeValue;
        Vec3d lookVec;
        Vec3d reachVec;
        Vec3d eyePos = TriggerBot.mc.player.getCameraPosVec(1.0f);
        EntityHitResult result = ProjectileUtil.raycast((Entity)TriggerBot.mc.player, (Vec3d)eyePos, (Vec3d)(reachVec = eyePos.add((lookVec = TriggerBot.mc.player.getRotationVec(1.0f)).multiply((double)(rangeValue = this.range.getValue().floatValue())))), (Box)TriggerBot.mc.player.getBoundingBox().expand((double)rangeValue), entity -> entity != TriggerBot.mc.player && entity.isAlive() && entity instanceof LivingEntity, (double)(rangeValue * rangeValue));
        if (result != null && (var_1297_2 = result.getEntity()) instanceof LivingEntity && this.isValidTarget(living = (LivingEntity)var_1297_2)) {
            return living;
        }
        return null;
    }

    private void attack() {
        PlayerEntity player;
        LivingEntity var_1309_2;
        if (this.options.is("Отжимать щит") && TriggerBot.mc.player.isBlocking()) {
            TriggerBot.mc.interactionManager.stopUsingItem((PlayerEntity)TriggerBot.mc.player);
        }
        if ((var_1309_2 = this.target) instanceof PlayerEntity && (player = (PlayerEntity)var_1309_2).isBlocking() && this.options.is("Ломать щит")) {
            this.shieldBreak(player);
        } else {
            TriggerBot.mc.interactionManager.attackEntity((PlayerEntity)TriggerBot.mc.player, (Entity)this.target);
        }
        TriggerBot.mc.player.swingHand(Hand.MAIN_HAND);
        this.attackTimer.reset();
    }

    private void shieldBreak(PlayerEntity entity) {
        int axeSlot = this.findAxeSlot();
        if (axeSlot != -1) {
            int prevSlot = TriggerBot.mc.player.getInventory().selectedSlot;
            TriggerBot.mc.player.getInventory().selectedSlot = axeSlot;
            TriggerBot.mc.interactionManager.attackEntity((PlayerEntity)TriggerBot.mc.player, (Entity)entity);
            TriggerBot.mc.player.swingHand(Hand.MAIN_HAND);
            TriggerBot.mc.player.getInventory().selectedSlot = prevSlot;
        } else {
            TriggerBot.mc.interactionManager.attackEntity((PlayerEntity)TriggerBot.mc.player, (Entity)entity);
        }
    }

    private int findAxeSlot() {
        for (int i2 = 0; i2 < 9; ++i2) {
            if (!(TriggerBot.mc.player.getInventory().getStack(i2).getItem() instanceof AxeItem)) continue;
            return i2;
        }
        return -1;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || entity == TriggerBot.mc.player) {
            return false;
        }
        if (!entity.isAlive() || entity.getHealth() <= 0.0f) {
            return false;
        }
        if (entity instanceof ArmorStandEntity) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if (!this.targets.is("Игроки")) {
                return false;
            }
            if (player.hasStatusEffect(StatusEffects.INVISIBILITY) && !this.targets.is("Невидимки")) {
                return false;
            }
            if (polar.INSTANCE.friendStorage.isFriend(entity.getName().getString())) {
                return false;
            }
        } else if (entity instanceof PassiveEntity || entity instanceof CodEntity ? !this.targets.is("Мирные") : entity instanceof HostileEntity && !this.targets.is("Мобы")) {
            return false;
        }
        if (TriggerBot.mc.player.getEyePos().distanceTo(entity.getBoundingBox().getCenter()) > (double)this.range.getValue().floatValue()) {
            return false;
        }
        return this.options.is("Бить через стены") || TriggerBot.mc.player.canSee((Entity)entity);
    }

    private boolean shouldAttack() {
        float rangeValue;
        Vec3d lookVec;
        Vec3d reachVec;
        Vec3d eyePos;
        EntityHitResult result;
        if (TriggerBot.mc.player.getAttackCooldownProgress(1.5f) < IdealHitUtils.getAICooldown()) {
            return false;
        }
        if (this.options.is("Проверка на наведение") && ((result = ProjectileUtil.raycast((Entity)TriggerBot.mc.player, (Vec3d)(eyePos = TriggerBot.mc.player.getCameraPosVec(1.0f)), (Vec3d)(reachVec = eyePos.add((lookVec = TriggerBot.mc.player.getRotationVec(1.0f)).multiply((double)(rangeValue = this.range.getValue().floatValue())))), (Box)TriggerBot.mc.player.getBoundingBox().expand((double)rangeValue), ex -> ex != TriggerBot.mc.player && ex.isAlive(), (double)(rangeValue * rangeValue))) == null || result.getEntity() != this.target)) {
            return false;
        }
        if (this.options.is("Только криты") && !IdealHitUtils.canCritical(this.target)) {
            return false;
        }
        if (!this.options.is("Умные криты") || !IdealHitUtils.canCritical(this.target)) {
            // empty if block
        }
        return true;
    }

    private void resetSprintState() {
        this.sprintResetDone = false;
        this.sprintResetTicks = 0;
    }

    private boolean shouldSkipSprintResetInWater() {
        return TriggerBot.mc.player != null && (TriggerBot.mc.player.isTouchingWater() || TriggerBot.mc.player.isSubmergedInWater()) && Sprint.INSTANCE != null && Sprint.INSTANCE.shouldKeepSprintInWater();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.target = null;
        this.needSprintReset = false;
        this.sprintResetDone = false;
        this.sprintResetTicks = 0;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.needSprintReset = false;
        this.sprintResetDone = false;
        this.sprintResetTicks = 0;
    }
    public LivingEntity getTarget() {
        return this.target;
    }
}

