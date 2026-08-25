package zov.alphadlc.module.list.misc;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import zov.alphadlc.core.Category;
import zov.alphadlc.core.Module;
import zov.alphadlc.core.ModuleRegister;
import zov.alphadlc.event.EventTarget;
import zov.alphadlc.event.list.EventPlayerUpdate;
import zov.alphadlc.setting.BooleanSetting;
import zov.alphadlc.setting.ModeSetting;
import zov.alphadlc.setting.SliderSetting;
import zov.alphadlc.util.chat.ChatUtil;
import zov.alphadlc.util.render.math.MathUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleRegister(name = "AppleFarm", description = "Автоматическая ферма яблок/дерева", category = Category.Misc)
public class AppleFarm extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final ModeSetting breakMode = new ModeSetting("Режим рубки", "На месте", "На месте", "Baritone");
    private final BooleanSetting autoStop = new BooleanSetting("Авто-стоп", true);
    private final SliderSetting actionDelay = new SliderSetting("Задержка действий (тики)", 5.0f, 0.0f, 40.0f, 1.0f);
    private final SliderSetting scanRadius = new SliderSetting("Радиус сканирования", 6.0f, 1.0f, 10.0f, 1.0f);
    private final SliderSetting scanHeight = new SliderSetting("Высота сканирования", 20.0f, 1.0f, 40.0f, 1.0f);
    private final SliderSetting maxFarmDistance = new SliderSetting("Макс. дистанция", 5.0f, 1.0f, 12.0f, 0.5f);
    private final SliderSetting reachDistance = new SliderSetting("Дистанция ломания", 5.5f, 1.0f, 6.0f, 0.1f);

    private BlockPos farmLocation = null;
    private BlockPos currentTargetBlock = null;
    private Direction targetBlockSide = null;
    private boolean isBaritoneMining = false;
    private boolean hasBaritone = false;
    private int actionTimer = 0;
    private int commandTimer = 0;
    private int notifyTimer = 0;
    private int breakTimer = 0;

    public AppleFarm() {
        addSettings(breakMode, autoStop, actionDelay, scanRadius, scanHeight,
                    maxFarmDistance, reachDistance);
    }

    @Override
    public void onEnable() {
        isBaritoneMining = false;
        currentTargetBlock = null;
        targetBlockSide = null;
        actionTimer = 0;
        commandTimer = 0;
        notifyTimer = 0;
        breakTimer = 0;

        hasBaritone = detectBaritone();

        if (breakMode.getValue().equals("Baritone") && !hasBaritone) {
            ChatUtil.error("Baritone не найден! Используйте режим 'На месте'.");
            setEnabled(false);
            return;
        }

        farmLocation = getLookTargetBlock();
        if (farmLocation == null) {
            ChatUtil.error("Посмотрите на блок земли и включите модуль снова.");
            setEnabled(false);
            return;
        }

        ChatUtil.success("Ферма запущена на " + farmLocation.toShortString());
    }

    @Override
    public void onDisable() {
        if (breakMode.getValue().equals("Baritone") && hasBaritone && isBaritoneMining && autoStop.getValue()) {
            sendBaritoneCommand("#stop");
        }
        isBaritoneMining = false;
        currentTargetBlock = null;
        targetBlockSide = null;
        farmLocation = null;
    }

    @EventTarget
    public void onUpdate(EventPlayerUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (farmLocation == null) {
            farmLocation = getLookTargetBlock();
            if (farmLocation == null) return;
        }

        if (mc.player.squaredDistanceTo(farmLocation.toCenterPos()) > Math.pow(maxFarmDistance.getValue(), 2)) {
            if (notifyTimer++ > 50) {
                ChatUtil.error("Слишком далеко от точки фермы!");
                notifyTimer = 0;
            }
            if (breakMode.getValue().equals("Baritone") && isBaritoneMining && autoStop.getValue()) {
                sendBaritoneCommand("#stop");
                isBaritoneMining = false;
            }
            return;
        }

        if (isTreeGrown()) {
            if (breakMode.getValue().equals("Baritone")) {
                updateBaritoneMining();
            }
            performManualChop();
            return;
        }

        currentTargetBlock = null;
        targetBlockSide = null;

        if (breakMode.getValue().equals("Baritone") && isBaritoneMining && autoStop.getValue()) {
            sendBaritoneCommand("#stop");
            isBaritoneMining = false;
        }

        if (actionTimer-- > 0) return;
        actionTimer = actionDelay.getValue().intValue();

        if (!isSaplingPlanted()) {
            if (!tryPlantSapling()) {
                if (notifyTimer++ > 50) {
                    Item sapling = findSaplingInInventory();
                    if (sapling == Items.AIR) {
                        ChatUtil.error("В инвентаре нет саженцев!");
                    } else {
                        ChatUtil.info("Не удалось посадить саженец");
                    }
                    notifyTimer = 0;
                }
            }
            return;
        }

        if (!tryApplyBoneMeal()) {
            if (notifyTimer++ > 100) {
                ChatUtil.info("Нет костной муки, ждем естественного роста...");
                notifyTimer = 0;
            }
        }
    }

    private void updateBaritoneMining() {
        if (commandTimer-- > 0) return;

        String targets = anyLeavesNearby() ? "oak_leaves spruce_leaves birch_leaves" : "oak_log spruce_log birch_log";
        sendBaritoneCommand("#mine 1 " + targets);
        isBaritoneMining = true;
        commandTimer = 30;
    }

    private void performManualChop() {
        if (breakTimer-- > 0) return;

        if (currentTargetBlock == null || !isValidChopTarget(currentTargetBlock)) {
            BlockInfo best = findBestBlockToChop();
            if (best == null) {
                currentTargetBlock = null;
                targetBlockSide = null;
                return;
            }
            currentTargetBlock = best.pos;
            targetBlockSide = best.side;
        }

        if (currentTargetBlock == null || targetBlockSide == null) return;

        if (!aimAtBlock(currentTargetBlock, targetBlockSide)) return;

        mc.interactionManager.attackBlock(currentTargetBlock, targetBlockSide);
        mc.player.swingHand(Hand.MAIN_HAND);
        breakTimer = 3;
    }

    private boolean tryPlantSapling() {
        Item sapling = findSaplingInInventory();
        if (sapling == Items.AIR) return false;

        if (mc.world.getBlockState(farmLocation).isOf(Blocks.GRASS_BLOCK) ||
            mc.world.getBlockState(farmLocation).isOf(Blocks.DIRT) ||
            mc.world.getBlockState(farmLocation).isOf(Blocks.PODZOL)) {
        } else {
            return false;
        }

        BlockPos plantPos = farmLocation.up();
        if (!mc.world.isAir(plantPos)) return false;

        BlockHitResult hit = new BlockHitResult(
            plantPos.toCenterPos(), Direction.UP, plantPos, false
        );

        return interactWithItem(sapling, hit);
    }

    private boolean tryApplyBoneMeal() {
        int mealSlot = findHotbarSlot(Items.BONE_MEAL);
        if (mealSlot == -1) {
            int invSlot = findInventorySlot(Items.BONE_MEAL);
            if (invSlot == -1) return false;

            int swapTo = findEmptyHotbarSlot();
            if (swapTo == -1) return false;

            mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                invSlot < 9 ? invSlot + 36 : invSlot,
                swapTo,
                SlotActionType.SWAP,
                mc.player
            );
            mealSlot = swapTo;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = mealSlot;

        BlockHitResult hit = new BlockHitResult(
            farmLocation.toCenterPos(), Direction.UP, farmLocation, false
        );

        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (oldSlot != mealSlot) {
            mc.player.getInventory().selectedSlot = oldSlot;
        }

        return result.isAccepted();
    }

    private boolean interactWithItem(Item item, BlockHitResult hit) {
        int slot = findHotbarSlot(item);
        if (slot == -1) return false;

        int oldSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (oldSlot != slot) {
            mc.player.getInventory().selectedSlot = oldSlot;
        }

        return result.isAccepted();
    }

    private boolean isTreeGrown() {
        int r = scanRadius.getValue().intValue();
        int h = scanHeight.getValue().intValue();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = 0; y <= h; y++) {
                    BlockPos pos = farmLocation.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.isIn(net.minecraft.registry.tag.BlockTags.LOGS) ||
                        state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSaplingPlanted() {
        BlockState state = mc.world.getBlockState(farmLocation.up());
        return state.isIn(net.minecraft.registry.tag.BlockTags.SAPLINGS);
    }

    private BlockPos getLookTargetBlock() {
        if (mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            if (canPlantAt(pos)) return pos;
            if (canPlantAt(pos.down())) return pos.down();
        }
        return null;
    }

    private boolean canPlantAt(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT) || state.isOf(Blocks.PODZOL);
    }

    private Item findSaplingInInventory() {
        Item[] saplings = {
            Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING,
            Items.JUNGLE_SAPLING, Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING,
            Items.CHERRY_SAPLING, Items.MANGROVE_PROPAGULE
        };
        for (int i = 0; i < 36; i++) {
            Item it = mc.player.getInventory().getStack(i).getItem();
            for (Item sapling : saplings) {
                if (it == sapling) return it;
            }
        }
        return Items.AIR;
    }

    private boolean isValidChopTarget(BlockPos pos) {
        if (pos == null) return false;
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isIn(net.minecraft.registry.tag.BlockTags.LOGS) &&
            !state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) return false;

        Direction side = getVisibleSide(pos);
        if (side == null) return false;

        targetBlockSide = side;
        return mc.player.squaredDistanceTo(pos.toCenterPos()) <= Math.pow(reachDistance.getValue(), 2);
    }

    private BlockInfo findBestBlockToChop() {
        List<BlockInfo> logs = new ArrayList<>();
        List<BlockInfo> leaves = new ArrayList<>();

        int r = scanRadius.getValue().intValue();
        int h = scanHeight.getValue().intValue();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = 0; y <= h; y++) {
                    BlockPos pos = farmLocation.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    double distSq = mc.player.squaredDistanceTo(pos.toCenterPos());

                    if (distSq > Math.pow(reachDistance.getValue(), 2)) continue;

                    Direction side = getVisibleSide(pos);
                    if (side == null) continue;

                    BlockInfo info = new BlockInfo(pos, side, distSq);
                    if (state.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) logs.add(info);
                    else if (state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) leaves.add(info);
                }
            }
        }

        Comparator<BlockInfo> byDist = Comparator.comparingDouble(i -> i.distSq);
        if (!leaves.isEmpty()) return leaves.stream().min(byDist).orElse(null);
        return logs.stream().min(byDist).orElse(null);
    }

    private boolean anyLeavesNearby() {
        int r = scanRadius.getValue().intValue();
        int h = scanHeight.getValue().intValue();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = 0; y <= h; y++) {
                    if (mc.world.getBlockState(farmLocation.add(x, y, z))
                        .isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) return true;
                }
            }
        }
        return false;
    }

    private Direction getVisibleSide(BlockPos pos) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d center = pos.toCenterPos();
        for (Direction side : Direction.values()) {
            Vec3d point = center.add(
                side.getOffsetX() * 0.49,
                side.getOffsetY() * 0.49,
                side.getOffsetZ() * 0.49
            );
            HitResult hit = mc.world.raycast(new RaycastContext(
                eyes, point,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
            ));
            if (hit instanceof BlockHitResult bhit &&
                bhit.getType() == HitResult.Type.BLOCK &&
                bhit.getBlockPos().equals(pos)) {
                return bhit.getSide();
            }
        }
        return null;
    }

    private boolean aimAtBlock(BlockPos pos, Direction side) {
        Vec3d hitVec = pos.toCenterPos().add(
            side.getOffsetX() * 0.5,
            side.getOffsetY() * 0.5,
            side.getOffsetZ() * 0.5
        );
        return rotateTo(hitVec);
    }

    private boolean rotateTo(Vec3d target) {
        Vec3d eyes = mc.player.getEyePos();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        float yawDiff = MathHelper.wrapDegrees(yaw - mc.player.getYaw());
        float pitchDiff = MathHelper.wrapDegrees(pitch - mc.player.getPitch());

        float step = 25f;
        float nextYaw = mc.player.getYaw() + Math.max(-step, Math.min(step, yawDiff));
        float nextPitch = mc.player.getPitch() + Math.max(-step, Math.min(step, pitchDiff));

        mc.player.setYaw(nextYaw);
        mc.player.setPitch(MathHelper.clamp(nextPitch, -90f, 90f));

        return Math.abs(yawDiff) < 5f && Math.abs(pitchDiff) < 5f;
    }

    private int findHotbarSlot(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    private int findInventorySlot(Item item) {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    private boolean detectBaritone() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void sendBaritoneCommand(String command) {
        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand(command.replace("#", ""));
        }
    }

    private static class BlockInfo {
        final BlockPos pos;
        final Direction side;
        final double distSq;

        BlockInfo(BlockPos pos, Direction side, double distSq) {
            this.pos = pos;
            this.side = side;
            this.distSq = distSq;
        }
    }
}
