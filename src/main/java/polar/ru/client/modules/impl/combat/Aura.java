package polar.ru.client.modules.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventAttackEntity;
import polar.ru.api.events.implement.EventGameUpdate;
import polar.ru.api.events.implement.EventMoveInput;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.events.implement.EventUpdatePost;
import polar.ru.api.storages.implement.FreeLookStorage;
import polar.ru.api.storages.implement.NeuroAuraStorage;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.combat.IdealHitUtils;
import polar.ru.api.utils.combat.RayTraceUtil;
import polar.ru.api.utils.math.MathUtils;
import polar.ru.api.utils.math.TimerUtils;
import polar.ru.api.utils.player.InventoryUtils;
import polar.ru.api.utils.rotate.MultipointUtils;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.combat.AntiBot;
import polar.ru.client.modules.impl.combat.ElytraTarget;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;
import polar.ru.client.modules.impl.combat.components.interpolation.BestPoint;
import polar.ru.client.modules.impl.combat.components.rotations.AresMineRotation;
import polar.ru.client.modules.impl.combat.components.rotations.CakeWorldRotation;
import polar.ru.client.modules.impl.combat.components.rotations.HolyWorldRots;
import polar.ru.client.modules.impl.combat.components.rotations.HvHRotation;
import polar.ru.client.modules.impl.combat.components.rotations.NeuroRotation;
import polar.ru.client.modules.impl.combat.components.rotations.SlothRotation;
import polar.ru.client.modules.impl.combat.components.rotations.SpaceTimesRotation;
import polar.ru.client.modules.impl.combat.components.rotations.SpookyTimeRotation;
import polar.ru.client.modules.impl.combat.components.rotations.SpookyTimeRotation2;
import polar.ru.client.modules.impl.combat.components.rotations.TestRotation;
import polar.ru.client.modules.impl.combat.components.rotations.VonTamRotation;
import polar.ru.client.modules.impl.combat.components.rotations.WellMineRotation;
import polar.ru.client.modules.impl.combat.components.rotations.WhiteRiseRotation;
import polar.ru.client.modules.impl.movement.AirStuck;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.mixin.ILivingEntity;
import polar.ru.polar;

public class Aura
extends Module {
    public static Aura INSTANCE = new Aura();
    public final ModeSetting rotationType = new ModeSetting("", "Smooth", "Smooth", "Sloth", "SpookyTime", "SpookyTime Fast", "NoRotate", "Space-Times", "FunTime", "Neuro", "AresMine", "CakeWorld", "HvH", "WellMine", "WhiteRise", "HolyWorld");
    private final ListSetting targets = new ListSetting("Таргеты", new BooleanSetting("Игроки", true), new BooleanSetting("Невидимки", true), new BooleanSetting("Мирные", false), new BooleanSetting("Мобы", true), new BooleanSetting("Голые", true));
    public final FloatSetting range = new FloatSetting("Дистанция атаки", 3.0f, 0.0f, 6.0f, 0.05f);
    private final FloatSetting aimRange = new FloatSetting("Дистанция наводки", 3.0f, 0.0f, 6.0f, 0.05f);
    private final FloatSetting elytraAimRange = new FloatSetting("Дистанция на элитрах", 50.0f, 10.0f, 100.0f, 0.05f);
    public final BooleanSetting smartCrit = new BooleanSetting("Умные криты", false);
    public final BooleanSetting sprintReset = new BooleanSetting("Сброс спринта", true);
    private final BooleanSetting throughWalls = new BooleanSetting("Бить через стены", true);
    private final BooleanSetting raycast = new BooleanSetting("Проверка на наведение", false);
    private final BooleanSetting unpressShield = new BooleanSetting("Отжимать щит", false);
    private final BooleanSetting breakShield = new BooleanSetting("Ломать щит", true);
    private final BooleanSetting attackOnEating = new BooleanSetting("Не бить когда ешь", true);
    public static BooleanSetting clientLook = new BooleanSetting("Наводка от первого лица", false);
    private final ModeSetting priority = new ModeSetting("Приоритет", "Дистанция", "Дистанция", "Здоровье", "Угол", "Никакой");
    public final BooleanSetting maxDamage = new BooleanSetting("Макс. урон", false);
    public final BooleanSetting maxDamageGrimBypass = new BooleanSetting("Обход Grim", true).visible(() -> this.maxDamage.isState());
    private LivingEntity target;
    private Vec2f currentRotations = new Vec2f(0.0f, 0.0f);
    private Vec2f targetRotations = new Vec2f(0.0f, 0.0f);
    private final NeuroAuraStorage dataSystem = new NeuroAuraStorage();
    private final TimerUtils attackTimer = new TimerUtils();
    private final BooleanSetting rwWallBypass = new BooleanSetting("Обход рв стен", false);
    private final BooleanSetting syncTps = new BooleanSetting("Синхронизировать с ТПСом", false);
    private final WellMineRotation wellMineRotation = new WellMineRotation();
    private final TestRotation testRotation = new TestRotation();
    private final SlothRotation slothRotation = new SlothRotation();
    private final SpaceTimesRotation spaceTimes = new SpaceTimesRotation(this);
    private final WhiteRiseRotation whiteRiseRotation = new WhiteRiseRotation(this);
    private final SpookyTimeRotation spookyTimeRotation = new SpookyTimeRotation();
    private final SpookyTimeRotation2 spookyTimeRotation2 = new SpookyTimeRotation2();
    private final NeuroRotation neuroRotation = new NeuroRotation();
    private final AresMineRotation aresMineRotation = new AresMineRotation();
    private final CakeWorldRotation cakeWorldRotation = new CakeWorldRotation();
    private final HvHRotation hvhRotation = new HvHRotation();
    private final HolyWorldRots holyWorldRots = new HolyWorldRots();
    private final VonTamRotation vonTamRotation = new VonTamRotation();
    private final TimerUtils backTimer = new TimerUtils();
    private long cps = 0L;
    private boolean needSprintReset = false;
    private boolean sprintResetDone = false;
    private int sprintResetTicks = 0;
    private int ticksToAttack = 0;
    private int bypassAttackAge = -1;
    private boolean bypassAttackQueued = false;
    private LivingEntity lastDataTarget = null;
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    public static float adjYaw;
    public static float adjPitch;
    public static float otvodkaYaw;
    public static float otvodkaPitch;
    public boolean isRotated;
    private Vec2f lastTargetRotation = null;
    private long targetLostTime = 0L;
    private static final long ROTATION_HOLD_TIME = 0L;
    private int lastSwappedMaxDamageSlot = -1;
    private int swapBackTick = -1;
    private ItemStack originalOffhandItem = ItemStack.EMPTY;
    private String maxDamageFilter = null;

    public Aura() {
        super("AttackAura", "Автоматически наводиться и бьёт таргета", Module.ModuleCategory.COMBAT);
        this.addSettings(this.rotationType, this.targets, this.range, this.aimRange, this.elytraAimRange, this.smartCrit, this.sprintReset, this.syncTps, this.attackOnEating, this.throughWalls, this.rwWallBypass, this.raycast, this.unpressShield, this.breakShield, this.maxDamage, this.maxDamageGrimBypass, clientLook, this.priority);
    }

    @EventLink
        public void onPlayerTick(EventUpdate e2) {
        if (Aura.mc.player == null || Aura.mc.world == null) {
            return;
        }
        this.lastYaw += 1.0f;
        this.updateTarget();
        if (this.dataSystem.isRecording()) {
            LivingEntity recordTarget = this.findTargetForRecording();
            this.dataSystem.recordTick(recordTarget, Aura.mc.player.getYaw(), Aura.mc.player.getPitch());
        }
    }

    @EventLink
        public void onAttackEntity(EventAttackEntity event) {
        if (Aura.mc.player == null || Aura.mc.world == null) {
            return;
        }
        if (event.getPlayer() != Aura.mc.player) {
            return;
        }
        Entity var_1297_2 = event.getTarget();
        if (!(var_1297_2 instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity)var_1297_2;
        if (!this.isValidTarget(living)) {
            return;
        }
        this.target = living;
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
    private void onGameUpdate(EventGameUpdate e2) {
        if (this.target == null) {
            return;
        }
        this.rotate();
    }

    @EventLink
        public void onTick(EventUpdate e2) {
        boolean packetCrits;
        if (Aura.mc.player == null || Aura.mc.world == null) {
            return;
        }
        if (this.swapBackTick != -1 && Aura.mc.player.age >= this.swapBackTick) {
            this.swapBackFromOffhand();
        }
        if (this.ticksToAttack > 0) {
            --this.ticksToAttack;
        }
        if (this.sprintResetDone) {
            ++this.sprintResetTicks;
        }
        boolean bl = packetCrits = ModuleClass.packetCriticals.isEnable() && Aura.mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
        if (!packetCrits) {
            this.processAttack();
        }
        if (this.dataSystem.isShowStats() && Aura.mc.player.age % 40 == 0 && (this.dataSystem.isRecording() || this.dataSystem.isUsingNeuro())) {
            Aura.mc.player.sendMessage((Text)Text.literal((String)this.dataSystem.getStatusString()), true);
        }
    }

    @EventLink
    public void onPost(EventUpdatePost e2) {
        boolean packetCrits;
        if (Aura.mc.player == null || Aura.mc.world == null) {
            return;
        }
        boolean bl = packetCrits = ModuleClass.packetCriticals.isEnable() && Aura.mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
        if (packetCrits && Aura.mc.player.fallDistance > 0.0f && Aura.mc.player.fallDistance < 1.0f) {
            this.processAttack();
        }
    }

        private LivingEntity findTargetForRecording() {
        LivingEntity bestTarget = null;
        double bestDistance = 100.0;
        Vec3d eyePos = Aura.mc.player.getEyePos();
        for (Entity entity : Aura.mc.world.getEntities()) {
            double distance;
            LivingEntity living;
            if (!(entity instanceof LivingEntity) || (living = (LivingEntity)entity) == Aura.mc.player || !living.isAlive() || living.getHealth() <= 0.0f || living instanceof ArmorStandEntity || (distance = eyePos.squaredDistanceTo(living.getBoundingBox().getCenter())) > bestDistance) continue;
            bestDistance = distance;
            bestTarget = living;
        }
        return bestTarget;
    }

        private void processAttack() {
        this.updateTarget();
        if (this.target != null) {
            this.lastTargetRotation = new Vec2f(Aura.mc.player.getYaw(), Aura.mc.player.getPitch());
            this.targetLostTime = 0L;
            long timeUntilAttack = this.cps - System.currentTimeMillis();
            if (timeUntilAttack > 0L && timeUntilAttack <= 250L) {
                this.vonTamRotation.prepareAttack();
            }
            if (this.shouldAttack() && this.cps <= System.currentTimeMillis()) {
                if (this.attackOnEating.isState() && Aura.mc.player.isUsingItem()) {
                    return;
                }
                if (this.sprintReset.isState() && Aura.mc.player.isSprinting() && !this.sprintResetDone) {
                    this.needSprintReset = true;
                    return;
                }
                if (this.sprintReset.isState() && this.sprintResetDone && this.sprintResetTicks < 1) {
                    return;
                }
                if (this.isBypassRotationActive() && !this.prepareBypassAttack()) {
                    return;
                }
                this.attack();
                this.resetBypassAttack();
                this.sprintResetDone = false;
                this.sprintResetTicks = 0;
            }
        } else {
            if (this.lastTargetRotation != null && this.targetLostTime == 0L) {
                this.targetLostTime = System.currentTimeMillis();
            }
            if (this.lastTargetRotation != null && System.currentTimeMillis() - this.targetLostTime < 0L) {
                RotationStorage.update(new Rotation(this.lastTargetRotation.x, this.lastTargetRotation.y), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, clientLook.isState());
                return;
            }
            this.lastTargetRotation = null;
            this.targetLostTime = 0L;
            this.cps = System.currentTimeMillis();
            this.backTimer.reset();
            adjPitch = 0.0f;
            adjYaw = 0.0f;
            this.wellMineRotation.reset();
            this.testRotation.reset();
            this.slothRotation.reset();
            this.whiteRiseRotation.reset();
            this.spookyTimeRotation.reset();
            this.vonTamRotation.reset();
            this.neuroRotation.reset();
            this.aresMineRotation.reset();
            this.cakeWorldRotation.reset();
            this.hvhRotation.reset();
            this.holyWorldRots.reset();
            this.dataSystem.resetState();
            this.sprintResetDone = false;
            this.sprintResetTicks = 0;
            this.ticksToAttack = 0;
            this.resetBypassAttack();
        }
    }

    public void Rotate() {
        this.rotate();
    }

        private void rotate() {
        RotationsSystem system;
        if (this.target == null) {
            return;
        }
        if (this.rotationType.is("Data") && this.target != this.lastDataTarget) {
            this.dataSystem.resetState();
            this.lastDataTarget = this.target;
        }
        if (this.isBypassRotationActive()) {
            this.updateBypassRotation(this.target);
            return;
        }
        if (this.rotationType.is("Smooth")) {
            system = new RotationsSystem(){

                @Override
                public void updateRotations(LivingEntity target) {
                    Vec2f rot;
                    Vec3d aimPoint = Aura.this.getPredictedRotationPoint(target, target.getBoundingBox().getCenter());
                    Aura.this.targetRotations = rot = RotationUtils.getRotations(aimPoint);
                    Aura.this.currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
                    RotationStorage.update(new Rotation(rot.x, rot.y), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, clientLook.isState());
                }
            };
        } else if (this.rotationType.is("WellMine")) {
            system = this.wellMineRotation;
        } else if (this.rotationType.is("Sloth")) {
            system = this.whiteRiseRotation;
        } else if (this.rotationType.is("SpookyTime")) {
            system = this.spookyTimeRotation;
        } else if (this.rotationType.is("SpookyTime Fast")) {
            system = this.spookyTimeRotation2;
        } else if (this.rotationType.is("WellMine")) {
            system = this.wellMineRotation;
        } else if (this.rotationType.is("WhiteRise")) {
            system = this.whiteRiseRotation;
        } else if (this.rotationType.is("FunTime")) {
            system = this.vonTamRotation;
        } else if (this.rotationType.is("Neuro")) {
            system = this.neuroRotation;
        } else if (this.rotationType.is("AresMine")) {
            system = this.aresMineRotation;
        } else if (this.rotationType.is("CakeWorld")) {
            system = this.cakeWorldRotation;
        } else if (this.rotationType.is("HvH")) {
            system = this.hvhRotation;
        } else if (this.rotationType.is("HolyWorld")) {
            system = this.holyWorldRots;
        } else if (this.rotationType.is("Space-Times")) {
            system = this.spaceTimes;
        } else if (this.rotationType.is("NoRotate")) {
            system = new RotationsSystem(){

                @Override
                public void updateRotations(LivingEntity target) {
                    RotationStorage.update(new Rotation(FreeLookStorage.getFreeYaw(), FreeLookStorage.getFreePitch()), MathUtils.random(100.0f, 170.0f), MathUtils.random(100.0f, 170.0f), MathUtils.random(100.0f, 170.0f), MathUtils.random(100.0f, 170.0f), 1, 6, false);
                }
            };
        } else if (this.rotationType.is("Data")) {
            system = new RotationsSystem(){

                @Override
                public void updateRotations(LivingEntity target) {
                    boolean focusRotation = Aura.this.shouldFocusDataRotation();
                    Rotation rotation = Aura.this.dataSystem.getNeuroRotation(target, mc.player.getYaw(), mc.player.getPitch(), focusRotation);
                    if (rotation == null) {
                        Vec3d point = MultipointUtils.getClosestPoint((Entity)target);
                        Vec2f rot = RotationUtils.getRotations(this.getPredictedPoint(target, point != null ? point : target.getBoundingBox().getCenter()));
                        rotation = new Rotation(rot.x, rot.y);
                    }
                    Aura.this.targetRotations = new Vec2f(rotation.getYaw(), rotation.getPitch());
                    Aura.this.currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
                    RotationStorage.update(rotation, focusRotation ? 24.0f : 11.5f, focusRotation ? 18.0f : 9.0f, focusRotation ? 18.0f : 9.0f, focusRotation ? 14.0f : 7.0f, 1, 1, clientLook.isState());
                }
            };
        } else {
            final Vec2f targetRot = RotationUtils.getRotations(this.getPredictedRotationPoint(this.target, this.target.getLeashPos(1.0f)));
            system = new RotationsSystem(){

                @Override
                public void updateRotations(LivingEntity target) {
                    Aura.this.currentRotations = new Vec2f(mc.player.getYaw(), mc.player.getPitch());
                    RotationStorage.update(new Rotation(targetRot.x, targetRot.y), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, clientLook.isState());
                }
            };
        }
        system.updateRotations(this.target);
    }

        private void updateBypassRotation(LivingEntity target) {
        Vec2f targetRot;
        Vec3d point = MultipointUtils.getClosestPoint((Entity)target);
        if (point == null) {
            point = target.getBoundingBox().getCenter();
        }
        Vec3d predicted = this.getPredictedRotationPoint(target, point);
        this.targetRotations = targetRot = RotationUtils.getRotations(predicted);
        this.currentRotations = new Vec2f(Aura.mc.player.getYaw(), Aura.mc.player.getPitch());
        boolean isBypassAttackTick = Aura.mc.player.age <= this.bypassAttackAge;
        float finalYaw = targetRot.x;
        float finalPitch = targetRot.y;
        if (!isBypassAttackTick) {
            finalYaw = FreeLookStorage.getFreeYaw();
            finalPitch = FreeLookStorage.getFreePitch();
        }
        RotationStorage.update(new Rotation(finalYaw, finalPitch), 360.0f, 360.0f, 360.0f, 360.0f, 0, 6, clientLook.isState());
    }

    private boolean isBypassRotationActive() {
        return this.isUsingRwWallBypass();
    }

    private boolean prepareBypassAttack() {
        if (!this.bypassAttackQueued) {
            this.bypassAttackQueued = true;
            this.bypassAttackAge = Aura.mc.player.age + 1;
            return false;
        }
        if (Aura.mc.player.age > this.bypassAttackAge) {
            this.resetBypassAttack();
            return false;
        }
        return this.isBypassAimReadyForAttack();
    }

        private boolean isBypassAimReadyForAttack() {
        if (this.target == null || Aura.mc.player == null) {
            return false;
        }
        float yawDiff = Math.abs(MathHelper.wrapDegrees((float)(this.targetRotations.x - Aura.mc.player.getYaw())));
        float pitchDiff = Math.abs(this.targetRotations.y - Aura.mc.player.getPitch());
        boolean onTarget = this.isUsingRwWallBypass() || this.isCurrentAimOnTarget();
        return yawDiff <= 3.0f && pitchDiff <= 2.5f && onTarget;
    }

    private boolean isUsingRwWallBypass() {
        return this.rwWallBypass.isState() && this.target != null && this.isTargetBehindWall(this.target);
    }

    private EntityHitResult getAttackRaycastResult() {
        Vec3d eyePos = Aura.mc.player.getCameraPosVec(1.0f);
        Vec3d lookVec = Aura.mc.player.getRotationVec(1.0f);
        float reach = this.getEffectiveRange() * 2.0f;
        Vec3d reachVec = eyePos.add(lookVec.multiply((double)reach));
        return ProjectileUtil.raycast((Entity)Aura.mc.player, (Vec3d)eyePos, (Vec3d)reachVec, (Box)Aura.mc.player.getBoundingBox().expand((double)reach), ex -> ex != Aura.mc.player && ex.isAlive(), (double)(reach * reach));
    }

    private boolean isTargetBehindWall(LivingEntity entity) {
        return entity != null && !Aura.mc.player.canSee((Entity)entity);
    }

        private Vec3d getPredictedRotationPoint(LivingEntity target, Vec3d point) {
        ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
        if (Aura.mc.player != null && target != null && elytraTarget != null && elytraTarget.isPredictionActive()) {
            return elytraTarget.getPredictedPoint(target, point);
        }
        return point;
    }

        private boolean isCurrentAimOnTarget() {
        if (this.target == null || Aura.mc.player == null) {
            return false;
        }
        if (Aura.mc.player.isGliding() && this.target.isGliding()) {
            return RayTraceUtil.rayTraceEntity(Aura.mc.player.getYaw(), Aura.mc.player.getPitch(), this.getMaxAimRange(), (Entity)this.target, false);
        }
        EntityHitResult result = this.getAttackRaycastResult();
        return result != null && result.getEntity() == this.target;
    }

        private LivingEntity findTarget() {
        ArrayList<LivingEntity> entities = new ArrayList<LivingEntity>();
        for (Entity entity2 : Aura.mc.world.getEntities()) {
            LivingEntity living;
            if (!(entity2 instanceof LivingEntity) || !this.isValidTarget(living = (LivingEntity)entity2)) continue;
            entities.add(living);
        }
        if (entities.isEmpty() || !this.isEnable()) {
            return null;
        }
        switch (this.priority.getCurrent()) {
            case "Дистанция": {
                entities.sort(Comparator.comparingDouble(entity -> entity.getBoundingBox().getCenter().squaredDistanceTo(Aura.mc.player.getEyePos())));
                break;
            }
            case "Здоровье": {
                entities.sort(Comparator.comparingDouble(LivingEntity::getHealth));
                break;
            }
            case "Угол": {
                entities.sort(Comparator.comparingDouble(entity -> {
                    Vec2f vec = RotationUtils.getRotations(entity.getBoundingBox().getCenter());
                    double dy = Math.abs(MathHelper.wrapDegrees((float)(vec.x - Aura.mc.player.getYaw())));
                    double dp = Math.abs(MathHelper.wrapDegrees((float)(vec.y - Aura.mc.player.getPitch())));
                    return dy + dp;
                }));
            }
        }
        return entities.isEmpty() ? null : (LivingEntity)entities.get(0);
    }

        private void updateTarget() {
        if (!this.isEnable()) {
            this.target = null;
            return;
        }
        if (this.target != null && this.isValidTarget(this.target)) {
            return;
        }
        this.target = this.findTarget();
    }

        private boolean shouldFocusDataRotation() {
        float focusThreshold;
        float cooldown = Aura.mc.player.getAttackCooldownProgress(1.5f);
        boolean readyByCooldown = cooldown >= (focusThreshold = Math.max(0.82f, IdealHitUtils.getAICooldown() - 0.08f));
        boolean fallingForCrit = !Aura.mc.player.isOnGround() && Aura.mc.player.getVelocity().y < 0.0 && Aura.mc.player.fallDistance > 0.0f;
        return readyByCooldown || fallingForCrit;
    }

        private void attack() {
        PlayerEntity player;
        int maxDamageSlot;
        if (this.unpressShield.isState() && Aura.mc.player.isBlocking()) {
            Aura.mc.interactionManager.stopUsingItem((PlayerEntity)Aura.mc.player);
        }
        this.tryBreakRwWallBlockPacket();
        if (this.maxDamage.isState() && this.canCrit() && (maxDamageSlot = this.findMaxDamageSlot()) != -1) {
            this.swapToOffhand(maxDamageSlot);
        }
        boolean attacked = false;
        LivingEntity var_1309_2 = this.target;
        if (var_1309_2 instanceof PlayerEntity && (player = (PlayerEntity)var_1309_2).isBlocking() && this.breakShield.isState()) {
            attacked = this.shieldBreak(player);
        }
        if (!attacked) {
            Aura.mc.interactionManager.attackEntity((PlayerEntity)Aura.mc.player, (Entity)this.target);
        }
        this.spookyTimeRotation.onAttack();
        this.vonTamRotation.onAttack();
        this.neuroRotation.onAttack();
        this.aresMineRotation.onAttack();
        this.cakeWorldRotation.onAttack();
        this.hvhRotation.onAttack();
        this.holyWorldRots.onAttack();
        Aura.mc.player.swingHand(Hand.MAIN_HAND);
        if (this.maxDamage.isState() && this.lastSwappedMaxDamageSlot != -1) {
            if (this.maxDamageGrimBypass.isState()) {
                this.swapBackTick = Aura.mc.player.age + 1;
            } else {
                this.swapBackFromOffhand();
            }
        }
        long cooldown = 467L;
        if (this.syncTps.isState()) {
            cooldown = (long)((float)this.getTpsAdjustedCooldown(cooldown) * 1.1f);
        }
        this.cps = System.currentTimeMillis() + cooldown;
        this.ticksToAttack = 10;
        this.attackTimer.reset();
    }

    private float getSyncTpsValue() {
        if (polar.INSTANCE == null || polar.INSTANCE.tpsCalc == null) {
            return 20.0f;
        }
        float tps = polar.INSTANCE.tpsCalc.getTPS();
        return MathHelper.clamp((float)tps, (float)0.1f, (float)20.0f);
    }

        private long getTpsAdjustedCooldown(long baseCooldown) {
        if (!this.syncTps.isState()) {
            return baseCooldown;
        }
        float tps = this.getSyncTpsValue();
        if (tps >= 20.0f) {
            return baseCooldown;
        }
        float multiplier = 20.0f / tps;
        float additionalFactor = 1.0f + (20.0f - tps) * 0.05f;
        long adjusted = (long)((float)baseCooldown * multiplier * additionalFactor);
        return Math.min(adjusted, 3000L);
    }

        private void tryBreakRwWallBlockPacket() {
        if (!this.rwWallBypass.isState() || this.target == null || Aura.mc.player == null || Aura.mc.world == null) {
            return;
        }
        if (Aura.mc.player.canSee((Entity)this.target)) {
            return;
        }
        if (Aura.mc.player.networkHandler == null) {
            return;
        }
        Vec3d startVec = Aura.mc.player.getEyePos();
        Vec3d targetPos = this.getPredictedRotationPoint(this.target, this.target.getEyePos());
        Vec3d direction = targetPos.subtract(startVec);
        double distance = direction.length();
        if (distance < 0.001) {
            return;
        }
        Vec3d normalizedDir = direction.normalize();
        for (double i2 = 0.0; i2 < distance; i2 += 0.5) {
            Vec3d point = startVec.add(normalizedDir.multiply(i2));
            BlockPos pos = BlockPos.ofFloored((Position)point);
            if (Aura.mc.world.getBlockState(pos).isAir() || Aura.mc.world.getBlockState(pos).getHardness((BlockView)Aura.mc.world, pos) < 0.0f) continue;
            Aura.mc.player.networkHandler.sendPacket((Packet)new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
            Aura.mc.player.networkHandler.sendPacket((Packet)new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean shieldBreak(PlayerEntity entity) {
        if (Aura.mc.player == null || Aura.mc.interactionManager == null || entity == null) {
            return false;
        }
        int axeHotbarSlot = this.findAxeHotbarSlot();
        if (axeHotbarSlot != -1) {
            this.attackWithHotbarSlot(entity, axeHotbarSlot);
            return true;
        }
        int axeInventorySlot = this.findAxeInventorySlot();
        if (axeInventorySlot == -1) {
            return false;
        }
        int selectedSlot = Aura.mc.player.getInventory().selectedSlot;
        int containerSlot = InventoryUtils.toContainerSlot(axeInventorySlot);
        Aura.mc.interactionManager.clickSlot(0, containerSlot, selectedSlot, SlotActionType.SWAP, (PlayerEntity)Aura.mc.player);
        Aura.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
        try {
            Aura.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(selectedSlot));
            Aura.mc.interactionManager.attackEntity((PlayerEntity)Aura.mc.player, (Entity)entity);
            boolean bl = true;
            return bl;
        }
        finally {
            Aura.mc.interactionManager.clickSlot(0, containerSlot, selectedSlot, SlotActionType.SWAP, (PlayerEntity)Aura.mc.player);
            Aura.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
            Aura.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(selectedSlot));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void attackWithHotbarSlot(PlayerEntity entity, int slot) {
        int previousSlot = Aura.mc.player.getInventory().selectedSlot;
        if (slot != previousSlot) {
            Aura.mc.player.getInventory().selectedSlot = slot;
            Aura.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(slot));
        }
        try {
            Aura.mc.interactionManager.attackEntity((PlayerEntity)Aura.mc.player, (Entity)entity);
        }
        finally {
            if (slot != previousSlot) {
                Aura.mc.player.getInventory().selectedSlot = previousSlot;
                Aura.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(previousSlot));
            }
        }
    }

    private boolean canCrit() {
        if (Aura.mc.player == null) {
            return false;
        }
        return !Aura.mc.player.isOnGround() && Aura.mc.player.getVelocity().y <= 0.0 && !Aura.mc.player.isClimbing() && !Aura.mc.player.isTouchingWater() && Aura.mc.player.getVehicle() == null;
    }

    private int findMaxDamageSlot() {
        double damage;
        ItemStack stack;
        int slot;
        if (Aura.mc.player == null) {
            return -1;
        }
        int bestSlot = -1;
        double maxDamage = 0.0;
        for (slot = 0; slot < 9; ++slot) {
            stack = Aura.mc.player.getInventory().getStack(slot);
            if (stack.isEmpty() || !((damage = this.extractDamageFromItem(stack)) > maxDamage)) continue;
            maxDamage = damage;
            bestSlot = slot;
        }
        if (bestSlot == -1 || maxDamage == 0.0) {
            for (slot = 9; slot < 36; ++slot) {
                stack = Aura.mc.player.getInventory().getStack(slot);
                if (stack.isEmpty() || !((damage = this.extractDamageFromItem(stack)) > maxDamage)) continue;
                maxDamage = damage;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private double extractDamageFromItem(ItemStack stack) {
        AttributeModifiersComponent modifiers;
        LoreComponent lore;
        if (stack == null || stack.isEmpty()) {
            return 0.0;
        }
        Item item = stack.getItem();
        if (item instanceof SwordItem) {
            return 0.0;
        }
        String displayName = stack.getName().getString().toLowerCase(Locale.ROOT);
        if (this.maxDamageFilter != null && !this.maxDamageFilter.isEmpty()) {
            String filterLower = this.maxDamageFilter.toLowerCase(Locale.ROOT);
            if (!displayName.contains(filterLower)) {
                boolean foundInLore = false;
                LoreComponent lore2 = (LoreComponent)stack.get(DataComponentTypes.LORE);
                if (lore2 != null) {
                    for (Text line : lore2.lines()) {
                        String loreText = line.getString().toLowerCase(Locale.ROOT);
                        if (!loreText.contains(filterLower)) continue;
                        foundInLore = true;
                        break;
                    }
                }
                if (!foundInLore) {
                    return 0.0;
                }
            }
        } else if (displayName.contains("талисман") || displayName.contains("сфер")) {
            return 0.0;
        }
        if ((lore = (LoreComponent)stack.get(DataComponentTypes.LORE)) != null) {
            for (Text line : lore.lines()) {
                String loreText = line.getString();
                double damage = this.extractDamageFromString(loreText);
                if (!(damage > 0.0)) continue;
                return damage;
            }
        }
        if ((modifiers = (AttributeModifiersComponent)stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS)) != null) {
            for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                if (!entry.attribute().equals((Object)EntityAttributes.ATTACK_DAMAGE)) continue;
                return entry.modifier().value();
            }
        }
        if (item instanceof AxeItem) {
            return 9.0;
        }
        return 0.0;
    }

    private double extractDamageFromString(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        Pattern pattern = Pattern.compile("(?:урон|damage)[:\\s]*([+-]?\\d+\\.?\\d*)", 2);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            }
            catch (NumberFormatException e2) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private void swapToOffhand(int slot) {
        if (Aura.mc.player == null || Aura.mc.interactionManager == null) {
            return;
        }
        if (slot < 0 || slot > 35) {
            return;
        }
        this.originalOffhandItem = Aura.mc.player.getOffHandStack().copy();
        int syncId = Aura.mc.player.currentScreenHandler.syncId;
        int containerSlot = slot < 9 ? 36 + slot : slot;
        if (this.maxDamageGrimBypass.isState()) {
            try {
                Thread.sleep(5L);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        }
        Aura.mc.interactionManager.clickSlot(syncId, containerSlot, 40, SlotActionType.SWAP, (PlayerEntity)Aura.mc.player);
        if (!this.maxDamageGrimBypass.isState()) {
            Aura.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(syncId));
        } else {
            this.swapBackTick = Aura.mc.player.age + 3;
        }
        this.lastSwappedMaxDamageSlot = slot;
    }

    private void swapBackFromOffhand() {
        if (Aura.mc.player == null || Aura.mc.interactionManager == null) {
            return;
        }
        if (this.lastSwappedMaxDamageSlot == -1) {
            return;
        }
        int syncId = Aura.mc.player.currentScreenHandler.syncId;
        int containerSlot = this.lastSwappedMaxDamageSlot < 9 ? 36 + this.lastSwappedMaxDamageSlot : this.lastSwappedMaxDamageSlot;
        if (this.maxDamageGrimBypass.isState()) {
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        }
        Aura.mc.interactionManager.clickSlot(syncId, containerSlot, 40, SlotActionType.SWAP, (PlayerEntity)Aura.mc.player);
        Aura.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(syncId));
        this.lastSwappedMaxDamageSlot = -1;
        this.swapBackTick = -1;
        this.originalOffhandItem = ItemStack.EMPTY;
    }

    public void setMaxDamageFilter(String filter) {
        this.maxDamageFilter = filter;
    }

    public void clearMaxDamageFilter() {
        this.maxDamageFilter = null;
    }

    public String getMaxDamageFilter() {
        return this.maxDamageFilter;
    }

    private int findAxeHotbarSlot() {
        for (int i2 = 0; i2 < 9; ++i2) {
            if (!(Aura.mc.player.getInventory().getStack(i2).getItem() instanceof AxeItem)) continue;
            return i2;
        }
        return -1;
    }

    private int findAxeInventorySlot() {
        for (int i2 = 9; i2 < 36; ++i2) {
            if (!(Aura.mc.player.getInventory().getStack(i2).getItem() instanceof AxeItem)) continue;
            return i2;
        }
        return -1;
    }

    private boolean isWeapon() {
        Item item = Aura.mc.player.getMainHandStack().getItem();
        return item != Items.AIR && (item instanceof SwordItem || item instanceof PickaxeItem || item instanceof AxeItem || item instanceof HoeItem || item instanceof ShovelItem || item instanceof MaceItem || item == Items.MACE);
    }

    private boolean isValidTarget(LivingEntity entity) {
        Vec3d nearestPoint;
        if (entity == null || entity == Aura.mc.player || !entity.isAlive() || entity.getHealth() <= 0.0f || entity instanceof ArmorStandEntity) {
            return false;
        }
        if (AntiBot.checkBot(entity)) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if (!this.targets.is("Игроки")) {
                return false;
            }
            if (!this.targets.is("Голые") && player.getArmor() == 0) {
                return false;
            }
            if (player.hasStatusEffect(StatusEffects.INVISIBILITY) && !this.targets.is("Невидимки")) {
                return false;
            }
            if (polar.INSTANCE.friendStorage.isFriend(entity.getName().getString())) {
                return false;
            }
        } else if (entity instanceof HostileEntity ? !this.targets.is("Мобы") : !this.targets.is("Мирные")) {
            return false;
        }
        if ((nearestPoint = BestPoint.getNearestPoint((Entity)entity)) == null) {
            nearestPoint = MultipointUtils.getClosestPoint((Entity)entity);
        }
        if (Aura.mc.player.getEyePos().distanceTo(nearestPoint) > (double)this.getMaxAimRange()) {
            return false;
        }
        return this.throughWalls.isState() || this.rwWallBypass.isState() || Aura.mc.player.canSee((Entity)entity);
    }

    private boolean shouldAttack() {
        Vec3d checkPoint;
        if (Aura.mc.player.getAttackCooldownProgress(1.5f) < IdealHitUtils.getAICooldown()) {
            return false;
        }
        EntityHitResult result = this.getAttackRaycastResult();
        boolean aimOnTarget = this.isCurrentAimOnTarget();
        if (this.raycast.isState() && !this.isUsingRwWallBypass() && !aimOnTarget) {
            return false;
        }
        if (this.rotationType.is("Data") && !this.isUsingRwWallBypass() && !this.isDataAimReady(result, aimOnTarget)) {
            return false;
        }
        if (Aura.mc.player.isGliding() && this.target.isGliding()) {
            Vec3d aimPoint;
            ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
            double currentDistance = elytraTarget != null && elytraTarget.isPredictionActive() ? (elytraTarget.hasChasePosition() ? elytraTarget.getPredictedDistance() : ((aimPoint = elytraTarget.getAimPoint(this.target)) != null ? Aura.mc.player.getEyePos().distanceTo(aimPoint) : Aura.mc.player.getEyePos().distanceTo(this.target.getEyePos()))) : Aura.mc.player.getEyePos().distanceTo(this.target.getBoundingBox().getCenter());
            return currentDistance <= (double)this.getEffectiveRange();
        }
        double distanceCheck = Aura.mc.player.getEyePos().distanceTo(this.target.getBoundingBox().getCenter());
        Vec3d VanillaChestLootTableGenerator = checkPoint = distanceCheck > 3.0 ? BestPoint.getNearestPoint((Entity)this.target) : this.target.getBoundingBox().getCenter();
        if (checkPoint == null) {
            checkPoint = MultipointUtils.getClosestPoint((Entity)this.target);
        }
        if (Aura.mc.player.getEyePos().distanceTo(checkPoint) > (double)this.getEffectiveRange()) {
            return false;
        }
        return IdealHitUtils.canCritical(this.target);
    }

    public int getWhiteRiseTicksToAttack() {
        return this.ticksToAttack;
    }

    private boolean isDataAimReady(EntityHitResult result, boolean aimOnTarget) {
        float yawDiff = Math.abs(MathHelper.wrapDegrees((float)(this.targetRotations.x - Aura.mc.player.getYaw())));
        float pitchDiff = Math.abs(this.targetRotations.y - Aura.mc.player.getPitch());
        boolean onTarget = Aura.mc.player.isGliding() && this.target != null && this.target.isGliding() ? aimOnTarget : result != null && result.getEntity() == this.target;
        return yawDiff <= 1.15f && pitchDiff <= 0.9f && onTarget;
    }

    public boolean isAboveWater() {
        BlockPos pos = BlockPos.ofFloored((Position)Aura.mc.player.getPos().add(0.0, -0.4, 0.0));
        return !Aura.mc.player.isSubmergedInWater() && Aura.mc.world.getBlockState(pos).isOf(Blocks.WATER);
    }

    public float getAttackCooldown() {
        return MathHelper.clamp((float)((float)((ILivingEntity)Aura.mc.player).getLastAttackedTicks() / this.getAttackCooldownProgressPerTick()), (float)0.0f, (float)1.0f);
    }

    public float getAttackCooldownProgressPerTick() {
        return (float)(1.0 / Aura.mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED) * 20.0);
    }

    private float getMaxAimRange() {
        return Aura.mc.player.isGliding() ? this.elytraAimRange.getValue().floatValue() : this.getEffectiveRange() + this.aimRange.getValue().floatValue();
    }

    private float getEffectiveRange() {
        float base = this.range.getValue().floatValue();
        if (AirStuck.INSTANCE.isEnable() && AirStuck.INSTANCE.extraRangeEnabled.isState()) {
            base += AirStuck.INSTANCE.extraRange.getValue().floatValue();
        }
        return base;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (this.target != null) {
            this.backTimer.reset();
        }
        this.target = null;
        this.wellMineRotation.reset();
        this.testRotation.reset();
        this.slothRotation.reset();
        this.whiteRiseRotation.reset();
        this.spookyTimeRotation.reset();
        this.vonTamRotation.reset();
        this.dataSystem.resetState();
        this.lastDataTarget = null;
        this.holyWorldRots.reset();
        this.needSprintReset = false;
        this.sprintResetDone = false;
        this.sprintResetTicks = 0;
        this.ticksToAttack = 0;
        this.resetBypassAttack();
        this.lastTargetRotation = null;
        this.targetLostTime = 0L;
        if (this.lastSwappedMaxDamageSlot != -1) {
            this.swapBackFromOffhand();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.wellMineRotation.reset();
        this.testRotation.reset();
        this.slothRotation.reset();
        this.whiteRiseRotation.reset();
        this.spookyTimeRotation.reset();
        this.vonTamRotation.reset();
        this.dataSystem.resetState();
        this.lastDataTarget = null;
        this.holyWorldRots.reset();
        this.needSprintReset = false;
        this.sprintResetDone = false;
        this.sprintResetTicks = 0;
        this.ticksToAttack = 0;
        this.resetBypassAttack();
        this.lastTargetRotation = null;
        this.targetLostTime = 0L;
        this.lastSwappedMaxDamageSlot = -1;
        this.swapBackTick = -1;
        this.originalOffhandItem = ItemStack.EMPTY;
        if (Aura.mc.player != null) {
            this.currentRotations = new Vec2f(Aura.mc.player.getYaw(), Aura.mc.player.getPitch());
        }
    }

    private void resetBypassAttack() {
        this.bypassAttackAge = -1;
        this.bypassAttackQueued = false;
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
    public NeuroAuraStorage getDataSystem() {
        return this.dataSystem;
    }
    public TimerUtils getAttackTimer() {
        return this.attackTimer;
    }
    public boolean isNeedSprintReset() {
        return this.needSprintReset;
    }
}

