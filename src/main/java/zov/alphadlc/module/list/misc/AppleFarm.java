package zov.alphadlc.module.list.misc;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.chat.ChatUtil;
import zov.alphadlc.util.player.other.InventoryUtil;

import com.google.common.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * AppleFarm — автоматическая ферма яблок/дерева.
 * Сажает саженцы, удобряет костной мукой, рубит дерево.
 */
@ModuleInformation(
    moduleName = "AppleFarm",
    moduleDesc = "Автоматическая ферма яблок/дерева",
    moduleCategory = ModuleCategory.MISC,
    moduleKeybind = -1
)
public class AppleFarm extends Module {

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
            ChatUtil.addMessage("§cBaritone не найден! Используйте режим 'На месте'.");
            setEnabled(false);
            return;
        }

        farmLocation = getLookTargetBlock();
        if (farmLocation == null) {
            ChatUtil.addMessage("§cПосмотрите на блок земли и включите модуль снова.");
            setEnabled(false);
            return;
        }

        ChatUtil.addMessage("§aФерма запущена на " + farmLocation.toShortString());
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

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (farmLocation == null) {
            farmLocation = getLookTargetBlock();
            if (farmLocation == null) return;
        }

        if (mc.player.squaredDistanceTo(farmLocation.toCenterPos()) > Math.pow(maxFarmDistance.getValue(), 2)) {
            if (notifyTimer++ > 50) {
                ChatUtil.addMessage("§eСлишком далеко от точки фермы!");
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
        actionTimer = (int) actionDelay.getValue();

        if (!isSaplingPlanted()) {
            if (!tryPlantSapling()) {
                if (notifyTimer++ > 50) {
                    Item sapling = findSaplingInInventory();
                    if (sapling == Items.AIR) {
                        ChatUtil.addMessage("§cВ инвентаре нет саженцев!");
                    } else {
                        ChatUtil.addMessage("§eНе удалось посадить саженец");
                    }
                    notifyTimer = 0;
                }
            }
            return;
        }

        if (!tryApplyBoneMeal()) {
            if (notifyTimer++ > 100) {
                ChatUtil.addMessage("§7Нет костной муки, ждем естественного роста...");
                notifyTimer = 0;
            }
        }
    }

    private void updateBaritoneMining() {
        if (commandTimer-- > 0) return;

        String targets = anyLeavesNearby()
            ? "oak_leaves spruce_leaves birch_leaves jungle_leaves acacia_leaves dark_oak_leaves cherry_leaves mangrove_leaves"
            : "oak_log spruce_log birch_log jungle_log acacia_log dark_oak_log cherry_log mangrove_log";
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

        int axeSlot = InventoryUtil.getBestToolSlot(mc.world.getBlockState(currentTargetBlock));
        if (axeSlot != -1 && mc.player.getInventory().selectedSlot != axeSlot) {
            mc.player.getInventory().selectedSlot = axeSlot;
        }

        mc.interactionManager.attackBlock(currentTargetBlock, targetBlockSide);
        mc.player.swingHand(Hand.MAIN_HAND);
        breakTimer = 3;
    }

    private boolean tryPlantSapling() {
        Item sapling = findSaplingInInventory();
        if (sapling == Items.AIR) return false;

        BlockState groundState = mc.world.getBlockState(farmLocation);
        if (!groundState.isOf(Blocks.GRASS_BLOCK) && !groundState.isOf(Blocks.DIRT)
            && !groundState.isOf(Blocks.PODZOL) && !groundState.isOf(Blocks.COARSE_DIRT)) {
            return false;
        }

        BlockPos plantPos = farmLocation.up();
        if (!mc.world.isAir(plantPos)) return false;

        Vec3d lookTarget = plantPos.toCenterPos().add(0, -0.5, 0);
        lookAt(lookTarget);

        int slot = findHotbarSlot(sapling);
        if (slot == -1) return false;

        int oldSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        BlockHitResult hit = new BlockHitResult(
            plantPos.toCenterPos(), Direction.UP, farmLocation, false
        );

        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (oldSlot != slot) {
            mc.player.getInventory().selectedSlot = oldSlot;
        }

        return result.isAccepted();
    }

    private boolean tryApplyBoneMeal() {
        int mealSlot = findHotbarSlot(Items.BONE_MEAL);
        if (mealSlot == -1) {
            int invSlot = findInventorySlot(Items.BONE_MEAL);
            if (invSlot == -1) return false;

            int swapTo = InventoryUtil.getEmptyHotbarSlot();
            if (swapTo == -1) return false;

            int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;
            InventoryUtil.swapSlots(containerSlot, swapTo);
            mealSlot = swapTo;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = mealSlot;

        lookAt(farmLocation.up().toCenterPos());

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

    private boolean isTreeGrown() {
        int r = (int) scanRadius.getValue();
        int h = (int) scanHeight.getValue();
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
        return state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT)
            || state.isOf(Blocks.PODZOL) || state.isOf(Blocks.COARSE_DIRT);
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

        int r = (int) scanRadius.getValue();
        int h = (int) scanHeight.getValue();

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
        int r = (int) scanRadius.getValue();
        int h = (int) scanHeight.getValue();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = 0; y <= h; y++) {
                    if (mc.world.getBlockState(farmLocation.add(x, y, z)).isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) {
                        return true;
                    }
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
            var hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
                eyes, point,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
            ));
            if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
                return hit.getSide();
            }
        }
        return null;
    }

    private boolean aimAtBlock(BlockPos pos, Direction side) {
        Vec3d target = pos.toCenterPos().add(
            side.getOffsetX() * 0.5,
            side.getOffsetY() * 0.5,
            side.getOffsetZ() * 0.5
        );
        return lookAt(target);
    }

    private boolean lookAt(Vec3d target) {
        Vec3d eyePos = mc.player.getEyePos();
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;

        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));
        pitch = Math.max(-90, Math.min(90, pitch));

        float yawDiff = Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
        float pitchDiff = Math.abs(pitch - mc.player.getPitch());

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);

        return yawDiff < 10 && pitchDiff < 10;
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
