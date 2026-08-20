package polar.ru.client.modules.impl.misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.chunk.WorldChunk;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventGameUpdate;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.api.utils.math.TimerUtils;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.TextSetting;

public final class AutoWarden
extends Module {
    public static final AutoWarden INSTANCE = new AutoWarden();
    private static final double OPEN_RANGE = 4.5;
    private static final double OPEN_RANGE_SQ = 20.25;
    private static final int OPEN_CONFIRM_TICKS = 20;
    private static final int MAX_OPEN_ATTEMPTS = 3;
    private static final int MIN_OPEN_ROTATION_TICKS = 3;
    private static final int VERTICAL_RANGE = 32;
    private static final int MAX_CHUNKS_PER_PASS = 2;
    private static final long APPROACH_STUCK_MS = 3500L;
    private static final double APPROACH_REACH_SQ = 2.25;
    private static final long UNSTUCK_COOLDOWN_MS = 1200L;
    private static final int MAX_UNSTUCK_ATTEMPTS = 4;
    private static final double RUBBERBAND_DISTANCE_SQ = 9.0;
    private static final int UNSTUCK_SEARCH_RADIUS = 3;
    private final FloatSetting scanRadius = new FloatSetting("Радиус поиска", 48.0f, 8.0f, 128.0f, 4.0f);
    private final FloatSetting stealDelay = new FloatSetting("Задержка лута", 150.0f, 0.0f, 1000.0f, 10.0f);
    private final FloatSetting warpDelay = new FloatSetting("Задержка варпа", 7000.0f, 1000.0f, 10000.0f, 100.0f);
    private final FloatSetting spawnDelay = new FloatSetting("Задержка спавна", 31000.0f, 1000.0f, 60000.0f, 100.0f);
    private final BooleanSetting loop = new BooleanSetting("Повторять цикл", true);
    private final BooleanSetting autoDeposit = new BooleanSetting("Авто-сброс", false);
    private final TextSetting warpName = new TextSetting("Название варпа", "warden", 32);
    private final TextSetting farmAnka = new TextSetting("Анка для фарма", "1", 16);
    private final TextSetting depositAnka = new TextSetting("Анка для сброса", "2", 16);
    private final FloatSetting depositDelay = new FloatSetting("Задержка сброса", 3000.0f, 1000.0f, 10000.0f, 100.0f);
    private final TimerUtils phaseTimer = new TimerUtils();
    private final Set<BlockPos> skippedChests = new HashSet<BlockPos>();
    private final Set<BlockPos> discoveredChests = new HashSet<BlockPos>();
    private final Set<ChunkPos> scannedChunks = new HashSet<ChunkPos>();
    private final Map<BlockPos, Set<BlockPos>> failedApproaches = new HashMap<BlockPos, Set<BlockPos>>();
    private long lastStealTime;
    private ChunkPos scanAnchorChunk;
    private boolean chestScanComplete;
    private State state = State.WARPING;
    private BlockPos targetChest;
    private BlockPos currentApproachPos;
    private BlockPos lastGotoPos;
    private BlockPos pendingOpenChest;
    private int pendingOpenTicks;
    private BlockHitResult currentChestHit;
    private boolean openingChest;
    private int rotationTicks;
    private float currentYaw;
    private float currentPitch;
    private boolean spawnSent;
    private Vec3d approachAnchor;
    private long approachProgressAtMs;
    private int openAttemptsForChest;
    private Vec3d lastPlayerPos;
    private long lastUnstuckAtMs;
    private int unstuckAttempts;
    private int depositPhase = 0;

    public AutoWarden() {
        super("AutoWarden", "Варп на warden, лут сундуков через баритон", Module.ModuleCategory.MISC);
        this.addSettings(this.scanRadius, this.stealDelay, this.warpDelay, this.spawnDelay, this.loop, this.autoDeposit, this.warpName, this.farmAnka, this.depositAnka, this.depositDelay);
    }

    @Override
    public void onEnable() {
        this.resetSession();
        this.applyBaritonePathSettings();
        this.sendWarp();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.stopBaritone();
        this.restoreBaritoneSettings();
        this.resetSession();
        super.onDisable();
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (AutoWarden.mc.player == null || AutoWarden.mc.world == null) {
            return;
        }
        if (AutoWarden.mc.player.getHealth() <= 0.0f && AutoWarden.mc.player.deathTime > 0) {
            this.setEnabled(false);
            return;
        }
        if (mc.getNetworkHandler() == null) {
            return;
        }
        try {
            switch (this.state.ordinal()) {
                case 0: {
                    this.tickWarping();
                    break;
                }
                case 1: {
                    this.tickLooting();
                    break;
                }
                case 2: {
                    this.tickReturning();
                    break;
                }
                case 4: {
                    this.tickTeleportingToDeposit();
                    break;
                }
                case 3: {
                    this.tickDepositing();
                    break;
                }
                case 5: {
                    this.tickTeleportingBack();
                }
            }
        }
        catch (Exception ignored) {
            this.stopBaritone();
        }
    }

    @EventLink
    public void onGameUpdate(EventGameUpdate event) {
        if (!this.openingChest || this.targetChest == null || AutoWarden.mc.player == null || AutoWarden.mc.world == null) {
            return;
        }
        if (!this.isChestBlock(AutoWarden.mc.world.getBlockState(this.targetChest))) {
            this.cancelOpening();
            return;
        }
        Vec3d target = this.currentChestHit != null ? this.currentChestHit.getPos() : Vec3d.ofCenter((Vec3i)this.targetChest);
        float[] rotations = this.calculateRotation(target);
        float deltaYaw = MathHelper.wrapDegrees((float)(rotations[0] - this.currentYaw));
        float deltaPitch = rotations[1] - this.currentPitch;
        this.currentYaw += deltaYaw * 0.8f;
        this.currentPitch = MathHelper.clamp((float)(this.currentPitch + deltaPitch * 0.8f), (float)-90.0f, (float)90.0f);
        RotationStorage.update(new Rotation(this.currentYaw, this.currentPitch), 360.0f, 360.0f, 360.0f, 360.0f, 1, 1, false);
        ++this.rotationTicks;
    }

    private void tickWarping() {
        if (!this.phaseTimer.finished((long)this.warpDelay.getValue().floatValue())) {
            return;
        }
        this.state = State.LOOTING;
        this.skippedChests.clear();
        this.failedApproaches.clear();
        this.resetChestScan();
        this.targetChest = null;
        this.lastGotoPos = null;
        this.phaseTimer.reset();
        ChatUtils.sendMessage("AutoWarden: поиск сундуков");
    }

    private void tickLooting() {
        boolean canOpen;
        BlockPos nearestChest;
        if (!this.isAreaReady()) {
            return;
        }
        if (this.isInventoryFull()) {
            this.stopBaritone();
            if (this.autoDeposit.isState()) {
                this.state = State.TELEPORTING_TO_DEPOSIT;
                this.phaseTimer.reset();
                ChatUtils.sendMessage("AutoWarden: инвентарь полон, телепорт на спавн для сброса");
            } else {
                this.state = State.RETURNING;
                this.spawnSent = false;
                this.phaseTimer.reset();
                ChatUtils.sendMessage("AutoWarden: инвентарь полон, /spawn через 31 сек");
            }
            return;
        }
        if (this.handleFlagAndBadBlockUnderPlayer()) {
            return;
        }
        if (AutoWarden.mc.currentScreen instanceof HandledScreen) {
            if (!this.isValidChestScreen()) {
                AutoWarden.mc.player.closeHandledScreen();
                this.failCurrentApproach();
                return;
            }
            this.pendingOpenChest = null;
            this.pendingOpenTicks = 0;
            this.resetOpenAttempts();
            this.tickContainerLoot();
            return;
        }
        if (this.pendingOpenChest != null) {
            this.tickPendingOpen();
            return;
        }
        if (this.openingChest) {
            this.tickChestOpening();
            return;
        }
        if (this.targetChest != null && (!this.isChestBlock(AutoWarden.mc.world.getBlockState(this.targetChest)) || this.skippedChests.contains(this.targetChest))) {
            this.targetChest = null;
            this.currentApproachPos = null;
            this.lastGotoPos = null;
        }
        if ((nearestChest = this.findNearestChest()) == null) {
            this.targetChest = null;
            this.currentApproachPos = null;
            this.lastGotoPos = null;
            return;
        }
        if (this.shouldRetargetChest(nearestChest)) {
            this.targetChest = nearestChest.toImmutable();
            this.currentApproachPos = null;
            this.lastGotoPos = null;
            this.openAttemptsForChest = 0;
            this.resetApproachTracking();
            this.stopBaritone();
        }
        this.currentApproachPos = this.findBestApproachPos(this.targetChest);
        if (this.currentApproachPos == null) {
            if (!this.hasUntriedApproach(this.targetChest)) {
                this.markChestCompleted(this.targetChest);
            }
            this.targetChest = null;
            this.lastGotoPos = null;
            this.resetApproachTracking();
            return;
        }
        if (AutoWarden.mc.interactionManager == null) {
            return;
        }
        this.updateApproachTracking();
        BlockHitResult chestHit = this.raycastChest(AutoWarden.mc.player.getEyePos(), this.targetChest);
        boolean bl = canOpen = this.isValidChestHit(chestHit, this.targetChest) && AutoWarden.mc.player.squaredDistanceTo(Vec3d.ofCenter((Vec3i)this.targetChest)) <= 20.25;
        if (canOpen && (this.isNearApproach(this.currentApproachPos) || this.isApproachStuck())) {
            this.stopBaritone();
            this.resetApproachTracking();
            this.startOpening(this.targetChest, chestHit);
            return;
        }
        if (this.isApproachStuck()) {
            this.stopBaritone();
            if (canOpen) {
                this.resetApproachTracking();
                this.startOpening(this.targetChest, chestHit);
                return;
            }
            this.failCurrentApproach();
            this.resetApproachTracking();
            this.lastGotoPos = null;
            if (this.targetChest != null && !this.hasUntriedApproach(this.targetChest)) {
                this.markChestCompleted(this.targetChest);
                this.targetChest = null;
            }
            return;
        }
        this.pathToChest(this.currentApproachPos);
    }

    private boolean handleFlagAndBadBlockUnderPlayer() {
        boolean shouldUnstuck;
        if (AutoWarden.mc.player == null || AutoWarden.mc.world == null || mc.getNetworkHandler() == null) {
            return false;
        }
        Vec3d currentPos = AutoWarden.mc.player.getPos();
        boolean rubberbandDetected = false;
        if (this.lastPlayerPos != null) {
            rubberbandDetected = currentPos.squaredDistanceTo(this.lastPlayerPos) >= 9.0;
        }
        this.lastPlayerPos = currentPos;
        boolean badSupport = this.isStandingOnProblemBlock();
        boolean bl = shouldUnstuck = rubberbandDetected || this.isApproachStuck() && badSupport;
        if (!shouldUnstuck) {
            if (!this.isApproachStuck()) {
                this.unstuckAttempts = 0;
            }
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastUnstuckAtMs < 1200L) {
            return false;
        }
        this.lastUnstuckAtMs = now;
        ++this.unstuckAttempts;
        this.stopBaritone();
        this.cancelOpening();
        this.applyBaritoneUnstuckSettings();
        BlockPos rescuePos = this.findNearestSafeStandPos(AutoWarden.mc.player.getBlockPos());
        if (rescuePos != null) {
            mc.getNetworkHandler().sendChatMessage("#goto " + rescuePos.getX() + " " + rescuePos.getY() + " " + rescuePos.getZ());
            this.lastGotoPos = rescuePos.toImmutable();
            this.resetApproachTracking();
            ChatUtils.sendMessage("AutoWarden: анти-стак, выхожу на твердый блок");
        } else {
            this.failCurrentApproach();
            ChatUtils.sendMessage("AutoWarden: анти-стак, не нашел безопасную точку");
        }
        if (this.unstuckAttempts >= 4) {
            if (this.targetChest != null) {
                this.markChestCompleted(this.targetChest);
                this.targetChest = null;
            }
            this.unstuckAttempts = 0;
            ChatUtils.sendMessage("AutoWarden: много флагов, пропускаю проблемный сундук");
        }
        return true;
    }

    private BlockPos findNearestSafeStandPos(BlockPos from) {
        if (AutoWarden.mc.player == null || AutoWarden.mc.world == null || from == null) {
            return null;
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -3; dx <= 3; ++dx) {
            for (int dz = -3; dz <= 3; ++dz) {
                for (int dy = -1; dy <= 2; ++dy) {
                    double dist;
                    BlockState ground;
                    BlockPos candidate = from.add(dx, dy, dz);
                    if (!this.canStandAt(candidate) || this.isProblemBlock(ground = AutoWarden.mc.world.getBlockState(candidate.down())) || !((dist = AutoWarden.mc.player.squaredDistanceTo(Vec3d.ofCenter((Vec3i)candidate))) < bestDist)) continue;
                    bestDist = dist;
                    best = candidate.toImmutable();
                }
            }
        }
        return best;
    }

    private boolean isStandingOnProblemBlock() {
        if (AutoWarden.mc.player == null || AutoWarden.mc.world == null) {
            return false;
        }
        BlockPos supportPos = BlockPos.ofFloored((double)AutoWarden.mc.player.getX(), (double)(AutoWarden.mc.player.getY() - 0.2), (double)AutoWarden.mc.player.getZ());
        BlockState support = AutoWarden.mc.world.getBlockState(supportPos);
        if (support.isAir()) {
            return false;
        }
        if (this.isProblemBlock(support)) {
            return true;
        }
        return !support.isSideSolidFullSquare((BlockView)AutoWarden.mc.world, supportPos, Direction.UP);
    }

    private boolean isProblemBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        Block block = state.getBlock();
        String path = Registries.BLOCK.getId(block).getPath();
        return path.contains("candle") || path.contains("torch") || path.contains("lantern") || path.contains("carpet") || block == Blocks.SCAFFOLDING;
    }

    private void applyBaritoneUnstuckSettings() {
        if (mc.getNetworkHandler() == null) {
            return;
        }
        mc.getNetworkHandler().sendChatMessage("#set allowBreak true");
        mc.getNetworkHandler().sendChatMessage("#set allowPlace false");
        mc.getNetworkHandler().sendChatMessage("#set allowParkour true");
        mc.getNetworkHandler().sendChatMessage("#set allowParkourPlace false");
    }

    private boolean shouldRetargetChest(BlockPos nearestChest) {
        if (nearestChest == null) {
            return false;
        }
        if (this.targetChest == null || this.skippedChests.contains(this.targetChest) || !this.isChestBlock(AutoWarden.mc.world.getBlockState(this.targetChest))) {
            return true;
        }
        if (nearestChest.equals((Object)this.targetChest)) {
            return false;
        }
        return this.findBestApproachPos(this.targetChest) == null && !this.hasUntriedApproach(this.targetChest);
    }

    private void resetApproachTracking() {
        this.approachAnchor = null;
        this.approachProgressAtMs = 0L;
    }

    private void updateApproachTracking() {
        if (AutoWarden.mc.player == null) {
            return;
        }
        Vec3d pos = AutoWarden.mc.player.getPos();
        long now = System.currentTimeMillis();
        if (this.approachAnchor == null || pos.squaredDistanceTo(this.approachAnchor) >= 0.64) {
            this.approachAnchor = pos;
            this.approachProgressAtMs = now;
        }
    }

    private boolean isApproachStuck() {
        return this.approachProgressAtMs > 0L && System.currentTimeMillis() - this.approachProgressAtMs >= 3500L;
    }

    private boolean isNearApproach(BlockPos approachPos) {
        if (approachPos == null || AutoWarden.mc.player == null) {
            return false;
        }
        double dx = AutoWarden.mc.player.getX() - ((double)approachPos.getX() + 0.5);
        double dz = AutoWarden.mc.player.getZ() - ((double)approachPos.getZ() + 0.5);
        double dy = AutoWarden.mc.player.getY() - (double)approachPos.getY();
        return dx * dx + dz * dz <= 2.25 && Math.abs(dy) <= 2.0;
    }

    private void failCurrentApproach() {
        if (this.targetChest != null && this.currentApproachPos != null) {
            this.failedApproaches.computeIfAbsent(this.targetChest, ignored -> new HashSet()).add(this.currentApproachPos.toImmutable());
        }
        this.currentApproachPos = null;
        this.lastGotoPos = null;
        this.currentChestHit = null;
        this.resetOpenAttempts();
        this.resetApproachTracking();
    }

    private boolean hasUntriedApproach(BlockPos chest) {
        Set failed = this.failedApproaches.getOrDefault(chest, Set.of());
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos standPos = chest.offset(direction);
            if (!this.canStandAt(standPos) || failed.contains(standPos)) continue;
            return true;
        }
        return false;
    }

    private void tickPendingOpen() {
        ++this.pendingOpenTicks;
        if (AutoWarden.mc.currentScreen instanceof HandledScreen) {
            this.pendingOpenChest = null;
            this.pendingOpenTicks = 0;
            this.resetOpenAttempts();
            return;
        }
        if (this.pendingOpenTicks < 20) {
            return;
        }
        this.pendingOpenChest = null;
        this.pendingOpenTicks = 0;
        if (this.targetChest != null && this.currentApproachPos != null && this.openAttemptsForChest < 3) {
            ++this.openAttemptsForChest;
            BlockHitResult retryHit = this.resolveChestHit(this.targetChest);
            if (this.isValidChestHit(retryHit, this.targetChest)) {
                this.startOpening(this.targetChest, retryHit);
                return;
            }
        }
        this.resetOpenAttempts();
        if (this.targetChest != null && this.currentApproachPos != null) {
            this.failCurrentApproach();
        }
        if (this.targetChest != null && !this.hasUntriedApproach(this.targetChest)) {
            this.markChestCompleted(this.targetChest);
            this.targetChest = null;
        }
    }

    private void markChestCompleted(BlockPos chest) {
        this.skippedChests.add(chest);
        this.discoveredChests.remove(chest);
        this.failedApproaches.remove(chest);
    }

    private void tickContainerLoot() {
        ScreenHandler handler = AutoWarden.mc.player.currentScreenHandler;
        if (handler == null || handler == AutoWarden.mc.player.playerScreenHandler) {
            return;
        }
        if (!(handler instanceof GenericContainerScreenHandler) && !(handler instanceof HopperScreenHandler)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastStealTime < (long)this.stealDelay.getValue().floatValue()) {
            return;
        }
        DefaultedList slots = handler.slots;
        Optional<Slot> slot = this.findLootSlot((List<Slot>)slots, handler);
        if (slot.isPresent()) {
            AutoWarden.mc.interactionManager.clickSlot(handler.syncId, slot.get().id, 0, SlotActionType.QUICK_MOVE, (PlayerEntity)AutoWarden.mc.player);
            this.lastStealTime = now;
            return;
        }
        if (this.targetChest != null) {
            this.markChestCompleted(this.targetChest);
        }
        AutoWarden.mc.player.closeHandledScreen();
        this.targetChest = null;
        this.currentApproachPos = null;
        this.lastGotoPos = null;
        this.resetChestScan();
    }

    private void tickChestOpening() {
        if (this.rotationTicks > 20) {
            this.failCurrentApproach();
            if (this.targetChest != null && !this.hasUntriedApproach(this.targetChest)) {
                this.markChestCompleted(this.targetChest);
                this.targetChest = null;
            }
            this.cancelOpening();
            return;
        }
        if (this.rotationTicks < 3) {
            return;
        }
        BlockHitResult hit = this.resolveChestHit(this.targetChest);
        if (!this.isValidChestHit(hit, this.targetChest)) {
            this.failCurrentApproach();
            if (this.targetChest != null && !this.hasUntriedApproach(this.targetChest)) {
                this.markChestCompleted(this.targetChest);
                this.targetChest = null;
            }
            this.cancelOpening();
            return;
        }
        AutoWarden.mc.interactionManager.interactBlock(AutoWarden.mc.player, Hand.MAIN_HAND, hit);
        AutoWarden.mc.player.swingHand(Hand.MAIN_HAND);
        this.pendingOpenChest = this.targetChest.toImmutable();
        this.pendingOpenTicks = 0;
        this.cancelOpening();
    }

    private BlockHitResult resolveChestHit(BlockPos chest) {
        if (this.isValidChestHit(this.currentChestHit, chest)) {
            return this.currentChestHit;
        }
        BlockHitResult freshHit = this.raycastChest(AutoWarden.mc.player.getEyePos(), chest);
        if (this.isValidChestHit(freshHit, chest)) {
            this.currentChestHit = freshHit;
        }
        return freshHit;
    }

    private void tickReturning() {
        if (!this.spawnSent) {
            if (!this.phaseTimer.finished((long)this.spawnDelay.getValue().floatValue())) {
                return;
            }
            this.stopBaritone();
            mc.getNetworkHandler().sendChatCommand("spawn");
            this.spawnSent = true;
            this.phaseTimer.reset();
            ChatUtils.sendMessage("AutoWarden: выполнен /spawn");
            return;
        }
        if (!this.phaseTimer.finished((long)this.warpDelay.getValue().floatValue())) {
            return;
        }
        if (this.loop.isState()) {
            this.state = State.WARPING;
            this.skippedChests.clear();
            this.failedApproaches.clear();
            this.resetChestScan();
            this.targetChest = null;
            this.lastGotoPos = null;
            this.sendWarp();
            this.phaseTimer.reset();
            ChatUtils.sendMessage("AutoWarden: повторный варп");
            return;
        }
        this.toggle();
    }

    private void tickTeleportingToDeposit() {
        if (!this.phaseTimer.finished(2000L)) {
            return;
        }
        this.stopBaritone();
        mc.getNetworkHandler().sendChatCommand("spawn");
        this.phaseTimer.reset();
        this.state = State.DEPOSITING;
        this.depositPhase = 0;
        ChatUtils.sendMessage("AutoWarden: телепорт на спавн для сброса");
    }

    private void tickDepositing() {
        if (this.depositPhase == 0) {
            if (!this.phaseTimer.finished((long)this.depositDelay.getValue().floatValue())) {
                return;
            }
            String ankaNumber = this.depositAnka.get();
            mc.getNetworkHandler().sendChatMessage("/an" + ankaNumber);
            this.phaseTimer.reset();
            this.depositPhase = 1;
            ChatUtils.sendMessage("AutoWarden: телепорт на анку сброса " + ankaNumber);
            return;
        }
        if (this.depositPhase == 1) {
            BlockHitResult hit;
            if (!this.phaseTimer.finished(4000L)) {
                return;
            }
            if (AutoWarden.mc.currentScreen instanceof HandledScreen && this.isValidChestScreen()) {
                this.depositInventory();
                AutoWarden.mc.player.closeHandledScreen();
                this.phaseTimer.reset();
                this.depositPhase = 0;
                this.state = State.TELEPORTING_BACK;
                ChatUtils.sendMessage("AutoWarden: предметы сброшены, возвращаюсь на фарм");
                return;
            }
            BlockPos depositChest = this.findNearestChestForDeposit();
            if (depositChest != null && this.isValidChestHit(hit = this.raycastChest(AutoWarden.mc.player.getEyePos(), depositChest), depositChest)) {
                AutoWarden.mc.interactionManager.interactBlock(AutoWarden.mc.player, Hand.MAIN_HAND, hit);
                AutoWarden.mc.player.swingHand(Hand.MAIN_HAND);
                this.phaseTimer.reset();
                this.depositPhase = 2;
                return;
            }
            this.phaseTimer.reset();
            this.depositPhase = 0;
            this.state = State.TELEPORTING_BACK;
            ChatUtils.sendMessage("AutoWarden: сундук не найден, возвращаюсь");
            return;
        }
        if (this.depositPhase == 2) {
            if (!this.phaseTimer.finished(1500L)) {
                return;
            }
            if (AutoWarden.mc.currentScreen instanceof HandledScreen && this.isValidChestScreen()) {
                this.depositInventory();
                AutoWarden.mc.player.closeHandledScreen();
            }
            this.phaseTimer.reset();
            this.depositPhase = 0;
            this.state = State.TELEPORTING_BACK;
            ChatUtils.sendMessage("AutoWarden: сброс завершён, возвращаюсь на фарм");
        }
    }

    private void tickTeleportingBack() {
        if (!this.phaseTimer.finished((long)this.warpDelay.getValue().floatValue())) {
            return;
        }
        this.stopBaritone();
        String ankaNumber = this.farmAnka.get();
        mc.getNetworkHandler().sendChatMessage("/an" + ankaNumber);
        this.phaseTimer.reset();
        this.state = State.LOOTING;
        ChatUtils.sendMessage("AutoWarden: возврат на анку " + ankaNumber + ", продолжаю лут");
    }

    private void depositInventory() {
        ScreenHandler handler = AutoWarden.mc.player.currentScreenHandler;
        if (handler == null || handler == AutoWarden.mc.player.playerScreenHandler) {
            return;
        }
        int containerSlots = this.getContainerSlotCount(handler);
        DefaultedList slots = handler.slots;
        for (int i2 = containerSlots; i2 < slots.size(); ++i2) {
            Slot slot = (Slot)slots.get(i2);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || this.isJunkStack(stack)) continue;
            AutoWarden.mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, (PlayerEntity)AutoWarden.mc.player);
        }
        ChatUtils.sendMessage("AutoWarden: инвентарь сброшен в сундук");
    }

    private void sendWarp() {
        String warp = this.warpName.get();
        String ankaNumber = this.farmAnka.get();
        mc.getNetworkHandler().sendChatCommand("warp " + warp);
        this.phaseTimer.reset();
        ChatUtils.sendMessage("AutoWarden: выполнен /warp " + warp + ", анка " + ankaNumber + " через 3 сек");
        new Thread(() -> {
            try {
                Thread.sleep(3000L);
                if (mc.getNetworkHandler() != null) {
                    mc.execute(() -> {
                        mc.getNetworkHandler().sendChatMessage("/an" + ankaNumber);
                        ChatUtils.sendMessage("AutoWarden: телепорт на анку " + ankaNumber);
                    });
                }
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        }, "AutoWarden-warp").start();
    }

    private void pathToChest(BlockPos approachPos) {
        boolean sameTarget;
        if (approachPos == null) {
            return;
        }
        boolean bl = sameTarget = this.lastGotoPos != null && this.lastGotoPos.equals((Object)approachPos);
        if (sameTarget && !this.isApproachStuck()) {
            return;
        }
        if (sameTarget) {
            this.lastGotoPos = null;
        }
        this.applyBaritonePathSettings();
        mc.getNetworkHandler().sendChatMessage("#goto " + approachPos.getX() + " " + approachPos.getY() + " " + approachPos.getZ());
        this.lastGotoPos = approachPos.toImmutable();
        this.resetApproachTracking();
    }

    private BlockPos findBestApproachPos(BlockPos chest) {
        if (chest == null || AutoWarden.mc.player == null) {
            return null;
        }
        Set failed = this.failedApproaches.getOrDefault(chest, Set.of());
        BlockPos bestVisible = null;
        double bestVisibleDist = Double.MAX_VALUE;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            double dist;
            BlockHitResult hit;
            BlockPos standPos = chest.offset(direction);
            if (!this.canStandAt(standPos) || failed.contains(standPos) || this.isDecorationBlockingChest(chest, standPos) || !this.isValidChestHit(hit = this.raycastChest(this.getStandEyePos(standPos), chest), chest) || !((dist = AutoWarden.mc.player.squaredDistanceTo(Vec3d.ofCenter((Vec3i)standPos))) < bestVisibleDist)) continue;
            bestVisibleDist = dist;
            bestVisible = standPos;
        }
        return bestVisible;
    }

    private boolean isDecorationBlockingChest(BlockPos chest, BlockPos standPos) {
        BlockPos hitPos;
        if (AutoWarden.mc.world == null) {
            return true;
        }
        BlockState standBlock = AutoWarden.mc.world.getBlockState(standPos);
        BlockState headBlock = AutoWarden.mc.world.getBlockState(standPos.up());
        if (this.isDecorationBlock(standBlock) || this.isDecorationBlock(headBlock)) {
            return true;
        }
        Vec3d eye = this.getStandEyePos(standPos);
        Vec3d chestCenter = Vec3d.ofCenter((Vec3i)chest);
        for (Direction direction : Direction.values()) {
            BlockPos decorPos = chest.offset(direction);
            BlockState decorState = AutoWarden.mc.world.getBlockState(decorPos);
            if (!this.isDecorationBlock(decorState)) continue;
            for (int i2 = 0; i2 < 5; ++i2) {
                Vec3d target = chestCenter.add((double)direction.getOffsetX() * 0.1 * (double)i2, (double)direction.getOffsetY() * 0.1 * (double)i2, (double)direction.getOffsetZ() * 0.1 * (double)i2);
                BlockHitResult hit = this.raycastBlock(eye, target);
                if (hit == null || hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals((Object)decorPos)) continue;
                return true;
            }
        }
        BlockHitResult directHit = this.raycastBlock(eye, chestCenter);
        return directHit != null && directHit.getType() == HitResult.Type.BLOCK && !(hitPos = directHit.getBlockPos()).equals((Object)chest) && this.isDecorationBlock(AutoWarden.mc.world.getBlockState(hitPos));
    }

    private boolean isDecorationBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        Block block = state.getBlock();
        String path = Registries.BLOCK.getId(block).getPath();
        return block == Blocks.TORCH || block == Blocks.SOUL_TORCH || block == Blocks.WALL_TORCH || block == Blocks.SOUL_WALL_TORCH || block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WALL_TORCH || block == Blocks.LANTERN || block == Blocks.SOUL_LANTERN || path.contains("candle") || path.contains("carpet");
    }

    private Vec3d getStandEyePos(BlockPos standPos) {
        return new Vec3d((double)standPos.getX() + 0.5, (double)((float)standPos.getY() + AutoWarden.mc.player.getStandingEyeHeight()), (double)standPos.getZ() + 0.5);
    }

    private BlockHitResult raycastChest(Vec3d eyePos, BlockPos chest) {
        if (AutoWarden.mc.world == null || chest == null || eyePos == null) {
            return null;
        }
        Vec3d center = Vec3d.ofCenter((Vec3i)chest);
        BlockHitResult best = null;
        double bestDist = Double.MAX_VALUE;
        Vec3d[] targets = new Vec3d[Direction.values().length + 1];
        targets[0] = center;
        for (int i2 = 0; i2 < Direction.values().length; ++i2) {
            Direction dir = Direction.values()[i2];
            targets[i2 + 1] = center.add((double)dir.getOffsetX() * 0.45, (double)dir.getOffsetY() * 0.45, (double)dir.getOffsetZ() * 0.45);
        }
        for (Vec3d target : targets) {
            BlockHitResult hit = this.raycastBlock(eyePos, target);
            if (!this.isValidChestHit(hit, chest)) continue;
            double dist = eyePos.squaredDistanceTo(hit.getPos());
            boolean directHit = hit.getBlockPos().equals((Object)chest);
            if (best != null && (!directHit || best.getBlockPos().equals((Object)chest)) && (directHit != best.getBlockPos().equals((Object)chest) || !(dist < bestDist))) continue;
            bestDist = dist;
            best = hit;
        }
        return best;
    }

    private boolean isValidChestHit(BlockHitResult hit, BlockPos chest) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK || chest == null || AutoWarden.mc.world == null) {
            return false;
        }
        BlockPos hitPos = hit.getBlockPos();
        BlockState hitState = AutoWarden.mc.world.getBlockState(hitPos);
        if (!this.isChestBlock(hitState)) {
            return false;
        }
        return this.isSameChest(hitPos, chest);
    }

    private boolean isValidChestScreen() {
        ScreenHandler handler = AutoWarden.mc.player.currentScreenHandler;
        if (!(handler instanceof GenericContainerScreenHandler)) {
            return handler instanceof HopperScreenHandler;
        }
        GenericContainerScreenHandler container = (GenericContainerScreenHandler)handler;
        return container.getRows() * 9 >= 9;
    }

    private BlockHitResult raycastBlock(Vec3d start, Vec3d end) {
        return AutoWarden.mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)AutoWarden.mc.player));
    }

    private boolean isSameChest(BlockPos hitPos, BlockPos chestPos) {
        if (hitPos == null || chestPos == null || AutoWarden.mc.world == null) {
            return false;
        }
        if (hitPos.equals((Object)chestPos)) {
            return this.isChestBlock(AutoWarden.mc.world.getBlockState(hitPos));
        }
        BlockState hitState = AutoWarden.mc.world.getBlockState(hitPos);
        BlockState chestState = AutoWarden.mc.world.getBlockState(chestPos);
        if (!this.isChestBlock(hitState) || !this.isChestBlock(chestState)) {
            return false;
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (!hitPos.equals((Object)chestPos.offset(direction)) && !chestPos.equals((Object)hitPos.offset(direction))) continue;
            ChestType hitType = (ChestType)hitState.get((Property)ChestBlock.CHEST_TYPE);
            ChestType chestType = (ChestType)chestState.get((Property)ChestBlock.CHEST_TYPE);
            return hitType != ChestType.SINGLE || chestType != ChestType.SINGLE;
        }
        return false;
    }

    private boolean canStandAt(BlockPos pos) {
        if (AutoWarden.mc.world == null) {
            return false;
        }
        BlockState feet = AutoWarden.mc.world.getBlockState(pos);
        BlockState head = AutoWarden.mc.world.getBlockState(pos.up());
        BlockState ground = AutoWarden.mc.world.getBlockState(pos.down());
        if (!feet.getCollisionShape((BlockView)AutoWarden.mc.world, pos).isEmpty()) {
            return false;
        }
        if (!head.getCollisionShape((BlockView)AutoWarden.mc.world, pos.up()).isEmpty()) {
            return false;
        }
        return !ground.isAir() && !ground.getCollisionShape((BlockView)AutoWarden.mc.world, pos.down()).isEmpty();
    }

    private void applyBaritonePathSettings() {
        if (mc.getNetworkHandler() == null) {
            return;
        }
        mc.getNetworkHandler().sendChatMessage("#set allowBreak false");
        mc.getNetworkHandler().sendChatMessage("#set allowPlace false");
        mc.getNetworkHandler().sendChatMessage("#set allowParkour true");
        mc.getNetworkHandler().sendChatMessage("#set allowParkourPlace false");
    }

    private void restoreBaritoneSettings() {
        if (mc.getNetworkHandler() == null) {
            return;
        }
        mc.getNetworkHandler().sendChatMessage("#set allowBreak true");
        mc.getNetworkHandler().sendChatMessage("#set allowPlace true");
        mc.getNetworkHandler().sendChatMessage("#set allowParkourPlace true");
    }

    private void stopBaritone() {
        if (mc.getNetworkHandler() == null) {
            return;
        }
        mc.getNetworkHandler().sendChatMessage("#stop");
    }

    private BlockPos findNearestChest() {
        if (!this.isAreaReady()) {
            return null;
        }
        this.ensureChestScan();
        this.scanChestChunks(2);
        this.cleanupDiscoveredChests();
        BlockPos nearest = this.getNearestDiscoveredChest();
        if (nearest != null) {
            return nearest;
        }
        if (this.chestScanComplete) {
            this.resetChestScan();
            this.ensureChestScan();
            this.scanChestChunks(2);
            this.cleanupDiscoveredChests();
            return this.getNearestDiscoveredChest();
        }
        return null;
    }

    private BlockPos getNearestDiscoveredChest() {
        return this.discoveredChests.stream().filter(pos -> !this.skippedChests.contains(pos)).filter(pos -> this.isChestBlock(AutoWarden.mc.world.getBlockState(pos))).filter(pos -> this.findBestApproachPos((BlockPos)pos) != null).min(Comparator.comparingDouble(pos -> AutoWarden.mc.player.squaredDistanceTo(Vec3d.ofCenter((Vec3i)pos)))).orElse(null);
    }

    private void cleanupDiscoveredChests() {
        if (AutoWarden.mc.world == null) {
            this.discoveredChests.clear();
            return;
        }
        this.discoveredChests.removeIf(pos -> this.skippedChests.contains(pos) || !this.isChestBlock(AutoWarden.mc.world.getBlockState(pos)));
    }

    private void ensureChestScan() {
        ChunkPos playerChunk = AutoWarden.mc.player.getChunkPos();
        if (this.scanAnchorChunk == null || !this.scanAnchorChunk.equals((Object)playerChunk)) {
            this.resetChestScan();
            this.scanAnchorChunk = playerChunk;
        }
    }

    private void resetChestScan() {
        this.scannedChunks.clear();
        this.discoveredChests.clear();
        this.chestScanComplete = false;
        this.scanAnchorChunk = null;
    }

    private void scanChestChunks(int maxChunks) {
        if (this.chestScanComplete || AutoWarden.mc.player == null || AutoWarden.mc.world == null) {
            return;
        }
        BlockPos playerPos = AutoWarden.mc.player.getBlockPos();
        int radius = this.scanRadius.getValue().intValue();
        int radiusSq = radius * radius;
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        int chunkRange = (radius >> 4) + 1;
        int minY = Math.max(AutoWarden.mc.world.getBottomY(), playerPos.getY() - 32);
        int maxY = Math.min(AutoWarden.mc.world.getTopYInclusive(), playerPos.getY() + 32);
        int totalLoadedChunks = 0;
        for (int cx = -chunkRange; cx <= chunkRange; ++cx) {
            for (int cz = -chunkRange; cz <= chunkRange; ++cz) {
                ChunkPos cp = new ChunkPos(playerChunkX + cx, playerChunkZ + cz);
                if (!AutoWarden.mc.world.isChunkLoaded(cp.x, cp.z)) continue;
                ++totalLoadedChunks;
            }
        }
        if (totalLoadedChunks == 0) {
            this.chestScanComplete = false;
            return;
        }
        ArrayList<ChunkPos> chunksToScan = new ArrayList<ChunkPos>();
        for (int cx = -chunkRange; cx <= chunkRange; ++cx) {
            for (int cz = -chunkRange; cz <= chunkRange; ++cz) {
                ChunkPos cp = new ChunkPos(playerChunkX + cx, playerChunkZ + cz);
                if (!AutoWarden.mc.world.isChunkLoaded(cp.x, cp.z) || this.scannedChunks.contains(cp)) continue;
                chunksToScan.add(cp);
            }
        }
        chunksToScan.sort(Comparator.comparingLong(pos -> this.chunkDistanceSq((ChunkPos)pos, playerChunkX, playerChunkZ)));
        int scanned = 0;
        for (ChunkPos cp : chunksToScan) {
            if (scanned >= maxChunks) break;
            WorldChunk chunk = AutoWarden.mc.world.getChunk(cp.x, cp.z);
            if (chunk == null) continue;
            this.scanChunkForChests(chunk, playerPos, radiusSq, minY, maxY);
            this.scannedChunks.add(cp);
            ++scanned;
        }
        boolean bl = this.chestScanComplete = this.scannedChunks.size() >= totalLoadedChunks;
        if (this.chestScanComplete && this.discoveredChests.isEmpty()) {
            this.scannedChunks.clear();
            this.chestScanComplete = false;
        }
    }

    private long chunkDistanceSq(ChunkPos cp, int pcx, int pcz) {
        long dx = cp.x - pcx;
        long dz = cp.z - pcz;
        return dx * dx + dz * dz;
    }

    private void scanChunkForChests(WorldChunk chunk, BlockPos playerPos, int radiusSq, int minY, int maxY) {
        int minX = chunk.getPos().getStartX();
        int minZ = chunk.getPos().getStartZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int x2 = minX; x2 <= maxX; ++x2) {
            for (int z2 = minZ; z2 <= maxZ; ++z2) {
                for (int y2 = minY; y2 <= maxY; ++y2) {
                    BlockState state;
                    mutable.set(x2, y2, z2);
                    if (mutable.getSquaredDistance((Vec3i)playerPos) > (double)radiusSq || !this.isChestBlock(state = chunk.getBlockState((BlockPos)mutable)) || this.isDoubleChestPart((BlockPos)mutable, state) || this.skippedChests.contains(mutable)) continue;
                    this.discoveredChests.add(mutable.toImmutable());
                }
            }
        }
    }

    private boolean isAreaReady() {
        if (AutoWarden.mc.player == null || AutoWarden.mc.world == null) {
            return false;
        }
        ChunkPos cp = AutoWarden.mc.player.getChunkPos();
        return AutoWarden.mc.world.isChunkLoaded(cp.x, cp.z);
    }

    private boolean isDoubleChestPart(BlockPos pos, BlockState state) {
        if (!state.isOf(Blocks.CHEST)) {
            return false;
        }
        ChestType type = (ChestType)state.get((Property)ChestBlock.CHEST_TYPE);
        return type == ChestType.LEFT;
    }

    private boolean isChestBlock(BlockState state) {
        return state.isOf(Blocks.CHEST);
    }

    private Optional<Slot> findLootSlot(List<Slot> slots, ScreenHandler handler) {
        int containerSlotCount = this.getContainerSlotCount(handler);
        if (containerSlotCount <= 0 || containerSlotCount > slots.size()) {
            return Optional.empty();
        }
        for (int i2 = 0; i2 < containerSlotCount; ++i2) {
            Slot slot = slots.get(i2);
            ItemStack stack = slot.getStack();
            if (!slot.hasStack() || stack.isEmpty() || this.isJunkStack(stack) || AutoWarden.mc.player.getItemCooldownManager().isCoolingDown(stack)) continue;
            return Optional.of(slot);
        }
        return Optional.empty();
    }

    private boolean isJunkStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        Item item = stack.getItem();
        if (item == Items.EXPERIENCE_BOTTLE) {
            return true;
        }
        if (item == Items.IRON_INGOT || item == Items.GOLD_INGOT || item == Items.RAW_IRON || item == Items.RAW_GOLD || item == Items.IRON_ORE || item == Items.GOLD_ORE || item == Items.DEEPSLATE_IRON_ORE || item == Items.DEEPSLATE_GOLD_ORE) {
            return true;
        }
        if (item == Items.EMERALD || item == Items.EMERALD_ORE || item == Items.DEEPSLATE_EMERALD_ORE) {
            return true;
        }
        String itemPath = Registries.ITEM.getId(item).getPath();
        if (itemPath.endsWith("_spawn_egg") && !itemPath.equals("villager_spawn_egg") && !itemPath.equals("zombie_villager_spawn_egg")) {
            return true;
        }
        if (item == Items.ENDER_PEARL || item == Items.ENDER_EYE) {
            return true;
        }
        if (item == Items.NETHER_STAR) {
            return true;
        }
        if (item == Items.LEATHER) {
            return true;
        }
        if (item == Items.COOKED_MUTTON) {
            return true;
        }
        if (item == Items.NAME_TAG) {
            return true;
        }
        if (item == Items.GUNPOWDER) {
            return true;
        }
        if (item == Items.PUFFERFISH) {
            return true;
        }
        if (item == Items.SHULKER_SHELL) {
            return true;
        }
        if (item == Items.DIAMOND || item == Items.DIAMOND_ORE || item == Items.DEEPSLATE_DIAMOND_ORE || item == Items.BOOKSHELF || item == Items.GHAST_TEAR || item == Items.BOW || item == Items.SCULK_CATALYST || item == Items.ENCHANTING_TABLE || item == Items.TNT || item == Items.REINFORCED_DEEPSLATE) {
            return true;
        }
        if (item instanceof BlockItem || item instanceof AxeItem || item instanceof ShovelItem || item == Items.ENCHANTED_BOOK || item == Items.NAUTILUS_SHELL || item == Items.TRIDENT || item == Items.FERMENTED_SPIDER_EYE) {
            return true;
        }
        if (item == Items.TORCH || item == Items.SOUL_TORCH || item == Items.REDSTONE_TORCH || item == Items.LANTERN || item == Items.SOUL_LANTERN) {
            return true;
        }
        String path = Registries.ITEM.getId(item).getPath();
        return path.startsWith("music_disc") || path.contains("disc_fragment") || path.contains("banner_pattern") || path.contains("torch") || path.contains("lantern") || path.contains("armor_trim") || path.contains("smithing_template") || path.contains("trim") || path.contains("shard");
    }

    private int getContainerSlotCount(ScreenHandler handler) {
        if (!(handler instanceof GenericContainerScreenHandler) && !(handler instanceof HopperScreenHandler)) {
            return 0;
        }
        int count = 0;
        for (Slot slot : handler.slots) {
            if (slot.inventory == AutoWarden.mc.player.getInventory()) continue;
            ++count;
        }
        return count;
    }

    private boolean isInventoryFull() {
        for (int i2 = 0; i2 < 36; ++i2) {
            if (!AutoWarden.mc.player.getInventory().getStack(i2).isEmpty()) continue;
            return false;
        }
        return true;
    }

    private void startOpening(BlockPos pos, BlockHitResult hit) {
        if (pos == null || !this.isValidChestHit(hit, pos)) {
            return;
        }
        this.targetChest = pos.toImmutable();
        this.currentChestHit = hit;
        this.openingChest = true;
        this.rotationTicks = 0;
        this.currentYaw = AutoWarden.mc.player.getYaw();
        this.currentPitch = AutoWarden.mc.player.getPitch();
    }

    private void cancelOpening() {
        this.openingChest = false;
        this.rotationTicks = 0;
    }

    private float[] calculateRotation(Vec3d target) {
        Vec3d eye = AutoWarden.mc.player.getEyePos();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
        return new float[]{yaw, MathHelper.clamp((float)pitch, (float)-90.0f, (float)90.0f)};
    }

    private void resetSession() {
        this.state = State.WARPING;
        this.targetChest = null;
        this.currentApproachPos = null;
        this.lastGotoPos = null;
        this.pendingOpenChest = null;
        this.pendingOpenTicks = 0;
        this.currentChestHit = null;
        this.skippedChests.clear();
        this.failedApproaches.clear();
        this.resetChestScan();
        this.openingChest = false;
        this.rotationTicks = 0;
        this.spawnSent = false;
        this.lastStealTime = 0L;
        this.openAttemptsForChest = 0;
        this.unstuckAttempts = 0;
        this.lastUnstuckAtMs = 0L;
        this.lastPlayerPos = null;
        this.depositPhase = 0;
        this.resetApproachTracking();
        this.phaseTimer.reset();
    }

    private BlockPos findNearestChestForDeposit() {
        if (AutoWarden.mc.player == null || AutoWarden.mc.world == null) {
            return null;
        }
        BlockPos playerPos = AutoWarden.mc.player.getBlockPos();
        int radius = 8;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                for (int dy = -4; dy <= 4; ++dy) {
                    BlockHitResult hit;
                    double dist;
                    BlockPos candidate = playerPos.add(dx, dy, dz);
                    if (!this.isChestBlock(AutoWarden.mc.world.getBlockState(candidate)) || (dist = AutoWarden.mc.player.squaredDistanceTo(Vec3d.ofCenter((Vec3i)candidate))) > 20.25 || !this.isValidChestHit(hit = this.raycastChest(AutoWarden.mc.player.getEyePos(), candidate), candidate) || !(dist < bestDist)) continue;
                    bestDist = dist;
                    best = candidate.toImmutable();
                }
            }
        }
        return best;
    }

    private void resetOpenAttempts() {
        this.openAttemptsForChest = 0;
    }

    private static enum State {
        WARPING,
        LOOTING,
        RETURNING,
        DEPOSITING,
        TELEPORTING_TO_DEPOSIT,
        TELEPORTING_BACK;

    }
}

