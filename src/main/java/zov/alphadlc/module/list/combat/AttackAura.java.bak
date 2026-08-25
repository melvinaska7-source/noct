package zov.alphadlc.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.util.friend.FriendRepository;
import zov.alphadlc.event.list.MoveInputEvent;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.chat.ChatUtil;
import zov.alphadlc.util.player.combat.AuraUtil;
import zov.alphadlc.util.player.combat.MaceUtil;
import zov.alphadlc.util.player.move.MoveUtil;
import zov.alphadlc.util.player.other.InventoryUtil;
import zov.alphadlc.util.rotation.Rotation;
import zov.alphadlc.util.rotation.RotationComponent;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * AttackAura — агрессивная KillAura для обхода античитов.
 * Быстрые ротации, минимальная задержка, авто-щитобой.
 */
@ModuleInformation(
    moduleName = "AttackAura",
    moduleDesc = "Агрессивная KillAura для обхода античитов",
    moduleCategory = ModuleCategory.COMBAT,
    moduleKeybind = -1
)
public class AttackAura extends Module {

    // === Settings ===
    private final ModeSetting rotationMode = new ModeSetting("Ротация", "Instant", "Instant", "Smooth", "Silent");
    private final ModeSetting targetMode = new ModeSetting("Цели", "Все", "Все", "Игроки", "Мобы");
    private final SliderSetting attackRange = new SliderSetting("Дистанция", 3.5f, 1.0f, 6.0f, 0.1f);
    private final SliderSetting fov = new SliderSetting("FOV", 180.0f, 30.0f, 180.0f, 5.0f);
    private final BooleanSetting onlyCrits = new BooleanSetting("Только криты", true);
    private final BooleanSetting shieldBreaker = new BooleanSetting("Пробитие щита", true);
    private final BooleanSetting smartSprint = new BooleanSetting("Умный спринт", true);
    private final BooleanSetting wallCheck = new BooleanSetting("Проверка стен", false);
    private final BooleanSetting autoMace = new BooleanSetting("Авто-булава", true);
    private final BooleanSetting hitThroughWalls = new BooleanSetting("Сквозь стены", false);

    // === State ===
    private LivingEntity target;
    private int attackCooldown = 0;
    private int blockHitDelay = 0;
    private int axeSlot = -1;
    private int prevSlot = -1;
    private boolean wasSprinting = false;

    @Override
    public void onEnable() {
        target = null;
        attackCooldown = 0;
        blockHitDelay = 0;
        axeSlot = -1;
        prevSlot = -1;
        wasSprinting = false;
    }

    @Override
    public void onDisable() {
        target = null;
        restoreSlot();
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (!isValidTarget(target)) {
            target = findTarget().orElse(null);
        }

        if (target == null) {
            restoreSlot();
            return;
        }

        rotateToTarget();

        if (canAttack()) {
            performAttack();
        }

        attackCooldown++;
        blockHitDelay = Math.max(0, blockHitDelay - 1);
    }

    @Subscribe
    public void onMoveInput(MoveInputEvent event) {
        if (target != null && smartSprint.getValue()) {
            float yawToTarget = (float) MathHelper.wrapDegrees(
                Math.toDegrees(Math.atan2(target.getZ() - mc.player.getZ(),
                    target.getX() - mc.player.getX())) - 90.0);
            MoveUtil.fixMovement(event, yawToTarget);
        }
    }

    // === Target Finding ===

    private Optional<LivingEntity> findTarget() {
        if (mc.world == null || mc.player == null) return Optional.empty();

        double reach = attackRange.getValue();
        float fovVal = (float) fov.getValue();

        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
            .filter(LivingEntity.class::isInstance)
            .map(LivingEntity.class::cast)
            .filter(e -> e != mc.player && e.isAlive())
            .filter(this::isValidTargetType)
            .filter(e -> AuraUtil.isInReach(e, reach))
            .filter(e -> isInFov(e, fovVal))
            .filter(e -> hitThroughWalls.getValue() || AuraUtil.isVisible(mc.player.getEyePos(), e, reach))
            .min(Comparator.comparingDouble(AuraUtil::distanceSqToEntity));
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        return AuraUtil.isInReach(entity, attackRange.getValue())
            && isValidTargetType(entity)
            && (hitThroughWalls.getValue() || AuraUtil.isVisible(mc.player.getEyePos(), entity, attackRange.getValue()));
    }

    private boolean isValidTargetType(LivingEntity entity) {
        String mode = targetMode.getValue();
        if (mode.equals("Игроки") && !(entity instanceof PlayerEntity)) return false;
        if (mode.equals("Мобы") && (entity instanceof PlayerEntity)) return false;

        if (entity instanceof PlayerEntity player) {
            if (FriendRepository.isFriend(player.getNameForScoreboard()))
                return false;
            return true;
        }
        return (entity instanceof HostileEntity)
            || (entity instanceof SlimeEntity)
            || (entity instanceof FlyingEntity)
            || (entity instanceof EnderDragonEntity)
            || (entity instanceof PassiveEntity)
            || (entity instanceof GolemEntity)
            || (entity instanceof AllayEntity)
            || (entity instanceof AmbientEntity);
    }

    private boolean isInFov(Entity entity, float maxFov) {
        if (maxFov >= 180.0f) return true;
        Vec3d eye = mc.player.getEyePos();
        Vec3d toEntity = entity.getBoundingBox().getCenter().subtract(eye).normalize();
        Vec3d look = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw());
        double angle = Math.toDegrees(Math.acos(MathHelper.clamp(look.dotProduct(toEntity), -1.0, 1.0)));
        return angle <= maxFov / 2.0f;
    }

    // === Attack Logic ===

    private void performAttack() {
        if (mc.interactionManager == null) return;

        if (shieldBreaker.getValue() && target.isBlocking()) {
            handleShieldBreak();
            return;
        }

        if (autoMace.getValue() && MaceUtil.hasMace() && shouldUseMace()) {
            useMaceAttack();
            return;
        }

        if (shouldAttackNormal()) {
            doAttack();
        }
    }

    private void handleShieldBreak() {
        int axeHotbar = findAxe(0, 9);
        if (axeHotbar != -1) {
            if (mc.player.getInventory().selectedSlot != axeHotbar) {
                if (prevSlot == -1) prevSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = axeHotbar;
            }
            doAttack();
            return;
        }

        int axeInv = findAxe(9, 36);
        if (axeInv != -1 && blockHitDelay <= 0) {
            int emptySlot = findEmptyHotbarSlot();
            if (emptySlot != -1) {
                InventoryUtil.swapSlots(axeInv, emptySlot);
                blockHitDelay = 5;
                return;
            }
        }

        doAttack();
    }

    private void useMaceAttack() {
        int maceSlot = MaceUtil.findMaceSlot();
        if (maceSlot == -1) return;

        if (mc.player.getInventory().selectedSlot != maceSlot) {
            if (prevSlot == -1) prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = maceSlot;
        }

        if (mc.player.fallDistance > 1.5f || MaceUtil.willLandSoon()) {
            doAttack();
        }
    }

    private void doAttack() {
        if (onlyCrits.getValue() && !smartSprint.getValue() && mc.player.isSprinting()
            && !mc.player.isTouchingWater() && !mc.player.isInLava()) {
            mc.player.setSprinting(false);
            mc.player.networkHandler.sendPacket(
                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            wasSprinting = true;
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        attackCooldown = 0;

        if (wasSprinting && smartSprint.getValue()) {
            mc.player.setSprinting(true);
            mc.player.networkHandler.sendPacket(
                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            wasSprinting = false;
        }
    }

    private boolean canAttack() {
        if (target == null || mc.player == null) return false;

        if (!AuraUtil.canAttack(mc.player.getYaw(), mc.player.getPitch(),
            attackRange.getValue(), target, hitThroughWalls.getValue())) {
            return false;
        }

        if (onlyCrits.getValue() && !AuraUtil.isCritPossible()) {
            return false;
        }

        return mc.player.getAttackCooldownProgress(0.5f) >= 0.9f && attackCooldown >= 10;
    }

    private boolean shouldAttackNormal() {
        if (autoMace.getValue() && MaceUtil.isHoldingMace()) {
            return mc.player.fallDistance > 1.5f || MaceUtil.willLandSoon();
        }
        return true;
    }

    private boolean shouldUseMace() {
        return mc.player.fallDistance > 1.0f || MaceUtil.willLandSoon() || mc.player.getVelocity().y < -0.5;
    }

    private int findAxe(int start, int end) {
        for (int i = start; i < end; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void restoreSlot() {
        if (prevSlot != -1) {
            mc.player.getInventory().selectedSlot = prevSlot;
            prevSlot = -1;
        }
    }

    private void rotateToTarget() {
        if (target == null) return;

        Vec3d eye = mc.player.getEyePos();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);

        double dx = targetPos.x - eye.x;
        double dy = targetPos.y - eye.y;
        double dz = targetPos.z - eye.z;

        double distanceXZ = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, distanceXZ));

        String mode = rotationMode.getValue();
        if (mode.equals("Instant")) {
            mc.player.setYaw(targetYaw);
            mc.player.setPitch(MathHelper.clamp(targetPitch, -90f, 90f));
        } else if (mode.equals("Smooth")) {
            float[] smoothed = AuraUtil.smoothRotate(mc.player.getYaw(), mc.player.getPitch(), targetYaw, targetPitch, 30f);
            mc.player.setYaw(smoothed[0]);
            mc.player.setPitch(MathHelper.clamp(smoothed[1], -90f, 90f));
        } else if (mode.equals("Silent")) {
            RotationComponent.update(new Rotation(targetYaw, MathHelper.clamp(targetPitch, -90f, 90f)), 45f, 45f, 30, 1);
        }
    }
}
