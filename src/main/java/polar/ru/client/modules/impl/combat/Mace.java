package polar.ru.client.modules.impl.combat;

import java.util.Comparator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.math.TimerUtils;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;
import polar.ru.client.modules.impl.combat.components.rotations.AresMineRotation;
import polar.ru.client.modules.impl.combat.components.rotations.CakeWorldRotation;
import polar.ru.client.modules.impl.combat.components.rotations.HvHRotation;
import polar.ru.client.modules.impl.combat.components.rotations.NeuroRotation;
import polar.ru.client.modules.impl.combat.components.rotations.SlothRotation;
import polar.ru.client.modules.impl.combat.components.rotations.SpookyTimeRotation;
import polar.ru.client.modules.impl.combat.components.rotations.SpookyTimeRotation2;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class Mace
extends Module {
    public static Mace INSTANCE = new Mace();
    public final ModeSetting rotationType = new ModeSetting("Ротация", "Smooth", "Smooth", "Sloth", "SpookyTime", "SpookyTime Fast", "NoRotate", "FunTime", "Neuro", "AresMine", "CakeWorld", "HvH");
    public final FloatSetting range = new FloatSetting("Дистанция атаки", 3.0f, 0.0f, 6.0f, 0.05f);
    private final FloatSetting aimRange = new FloatSetting("Дистанция наводки", 3.0f, 0.0f, 6.0f, 0.05f);
    private final BooleanSetting windChargeTrigger = new BooleanSetting("Wind Charge триггер", true);
    private final BooleanSetting fallDamageTrigger = new BooleanSetting("Падение триггер", true);
    private final FloatSetting fallHeight = new FloatSetting("Высота падения", 4.0f, 1.0f, 20.0f, 0.5f).visible(() -> this.fallDamageTrigger.isState());
    private final BooleanSetting autoSwitch = new BooleanSetting("Авто смена", true);
    private final BooleanSetting silentSwitch = new BooleanSetting("Тихая смена", true).visible(() -> this.autoSwitch.isState());
    public static BooleanSetting clientLook = new BooleanSetting("Наводка от первого лица", false);
    private LivingEntity target;
    private Vec2f currentRotations = new Vec2f(0.0f, 0.0f);
    private Vec2f targetRotations = new Vec2f(0.0f, 0.0f);
    private final TimerUtils attackTimer = new TimerUtils();
    private final SlothRotation slothRotation = new SlothRotation();
    private final SpookyTimeRotation spookyTimeRotation = new SpookyTimeRotation();
    private final SpookyTimeRotation2 spookyTimeRotation2 = new SpookyTimeRotation2();
    private final NeuroRotation neuroRotation = new NeuroRotation();
    private final AresMineRotation aresMineRotation = new AresMineRotation();
    private final CakeWorldRotation cakeWorldRotation = new CakeWorldRotation();
    private final HvHRotation hvhRotation = new HvHRotation();
    private boolean lastOnGround = true;
    private float fallStartY = 0.0f;
    private boolean windChargeUsed = false;
    private int windChargeCooldown = 0;

    public Mace() {
        super("Mace", "Автоудар булавой", Module.ModuleCategory.COMBAT);
        this.addSettings(this.rotationType, this.range, this.aimRange, this.windChargeTrigger, this.fallDamageTrigger, this.fallHeight, this.autoSwitch, this.silentSwitch, clientLook);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (Mace.mc.player == null || Mace.mc.world == null) {
            return;
        }
        if (!Mace.mc.player.isOnGround() && this.lastOnGround) {
            double d2 = Mace.mc.player.getY();
        }
        if (Mace.mc.player.isOnGround() && !this.lastOnGround) {
            float fallDistance = this.fallStartY - (float)Mace.mc.player.getY();
            if (this.fallDamageTrigger.isState() && fallDistance >= this.fallHeight.getValue().floatValue()) {
                this.performMaceAttack();
            }
        }
        this.lastOnGround = Mace.mc.player.isOnGround();
        if (this.windChargeCooldown > 0) {
            --this.windChargeCooldown;
        }
        if (Mace.mc.player.getMainHandStack().getItem() == Items.WIND_CHARGE && Mace.mc.player.getItemUseTime() > 0 && this.windChargeCooldown == 0) {
            this.windChargeUsed = true;
            this.windChargeCooldown = 20;
        }
        if (this.windChargeUsed && this.windChargeTrigger.isState()) {
            this.performMaceAttack();
            this.windChargeUsed = false;
        }
    }

    private void performMaceAttack() {
        if (Mace.mc.player == null || Mace.mc.world == null) {
            return;
        }
        this.target = this.findTarget();
        if (this.target == null) {
            return;
        }
        if (this.autoSwitch.isState()) {
            this.switchToMace();
        }
        this.rotateToTarget();
        this.attackEntity(this.target);
    }

    private LivingEntity findTarget() {
        float rangeValue = this.range.getValue().floatValue();
        double rangeSquared = (double)rangeValue * (double)rangeValue;
        return Mace.mc.world.getPlayers().stream().filter(player -> player != Mace.mc.player).filter(player -> Mace.mc.player.squaredDistanceTo((Entity)player) <= rangeSquared).filter(player -> player.isAlive()).min(Comparator.comparingDouble(player -> Mace.mc.player.squaredDistanceTo((Entity)player))).orElse(null);
    }

    private void switchToMace() {
        int maceSlot = this.findMaceSlot();
        if (maceSlot == -1) {
            return;
        }
        if (this.silentSwitch.isState()) {
            Mace.mc.player.getInventory().selectedSlot = maceSlot;
        } else {
            Mace.mc.player.getInventory().selectedSlot = maceSlot;
            mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(maceSlot));
        }
    }

    private int findMaceSlot() {
        for (int i2 = 0; i2 < 9; ++i2) {
            if (Mace.mc.player.getInventory().getStack(i2).getItem() != Items.MACE) continue;
            return i2;
        }
        return -1;
    }

    private void rotateToTarget() {
        Vec2f rotations;
        if (this.target == null) {
            return;
        }
        Vec3d predictedPos = this.getPredictedRotationPoint(this.target, this.target.getBoundingBox().getCenter());
        this.targetRotations = rotations = RotationUtils.getRotations(predictedPos);
        this.currentRotations = new Vec2f(Mace.mc.player.getYaw(), Mace.mc.player.getPitch());
        RotationsSystem system = this.getRotationSystem();
        if (system != null) {
            system.updateRotations(this.target);
        } else {
            double yaw = rotations.x;
            double pitch = rotations.y;
            RotationStorage.update(new Rotation((float)yaw, (float)pitch), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, clientLook.isState());
        }
    }

    private RotationsSystem getRotationSystem() {
        if (this.rotationType.is("Smooth")) {
            return new RotationsSystem(){

                @Override
                public void updateRotations(LivingEntity target) {
                    Vec2f rot;
                    Vec3d aimPoint = Mace.this.getPredictedRotationPoint(target, target.getBoundingBox().getCenter());
                    Mace.this.targetRotations = rot = RotationUtils.getRotations(aimPoint);
                    Mace.this.currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
                    double yaw = rot.x;
                    double pitch = rot.y;
                    RotationStorage.update(new Rotation((float)yaw, (float)pitch), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, clientLook.isState());
                }
            };
        }
        if (this.rotationType.is("Sloth")) {
            return this.slothRotation;
        }
        if (this.rotationType.is("SpookyTime")) {
            return this.spookyTimeRotation;
        }
        if (this.rotationType.is("SpookyTime Fast")) {
            return this.spookyTimeRotation2;
        }
        if (this.rotationType.is("Neuro")) {
            return this.neuroRotation;
        }
        if (this.rotationType.is("AresMine")) {
            return this.aresMineRotation;
        }
        if (this.rotationType.is("CakeWorld")) {
            return this.cakeWorldRotation;
        }
        if (this.rotationType.is("HvH")) {
            return this.hvhRotation;
        }
        return null;
    }

    private void attackEntity(LivingEntity entity) {
        if (Mace.mc.player == null || entity == null) {
            return;
        }
        if (Mace.mc.player.getMainHandStack().getItem() != Items.MACE) {
            return;
        }
        Mace.mc.interactionManager.attackEntity((PlayerEntity)Mace.mc.player, (Entity)entity);
        Mace.mc.player.swingHand(Hand.MAIN_HAND);
        this.resetRotations();
    }

    private void resetRotations() {
        this.slothRotation.reset();
        this.spookyTimeRotation.reset();
        this.spookyTimeRotation2.reset();
        this.neuroRotation.reset();
        this.aresMineRotation.reset();
        this.cakeWorldRotation.reset();
        this.hvhRotation.reset();
    }

    private Vec3d getPredictedRotationPoint(LivingEntity target, Vec3d defaultPoint) {
        double distance = Mace.mc.player.squaredDistanceTo((Entity)target);
        double ticksToReach = distance / 8.0;
        Vec3d velocity = target.getVelocity();
        return defaultPoint.add(velocity.multiply(ticksToReach));
    }

    @Override
    public void onDisable() {
        this.target = null;
        this.resetRotations();
    }
    public LivingEntity getTarget() {
        return this.target;
    }
    public Vec2f getCurrentRotations() {
        return this.currentRotations;
    }
    public Vec2f getTargetRotations() {
        return this.targetRotations;
    }
    public TimerUtils getAttackTimer() {
        return this.attackTimer;
    }
}

