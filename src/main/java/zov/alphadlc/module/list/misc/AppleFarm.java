package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.event.list.EventPlayerUpdate;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.util.rotation.Rotation;
import zov.alphadlc.util.rotation.RotationComponent;

@ModuleInformation(
        moduleName = "Apple Farm",
        moduleDesc = "Стационарная автоматическая ферма дуба для яблок",
        moduleCategory = ModuleCategory.PLAYER
)
public class AppleFarm extends Module {

    private enum State {
        WAITING,
        PLANTING,
        GROWING,
        BREAKING_LEAVES,
        BREAKING_LOG
    }

    private State state = State.WAITING;

    /** Блок земли, на который игрок навёлся при включении. */
    private BlockPos groundPos;

    /** Блок, где должен находиться саженец. */
    private BlockPos saplingPos;

    private int previousSlot = -1;
    private int targetSlot = -1;

    private boolean attacking;
    private int growTicks;
    private int emptyTicks;

    @Override
    public void onEnable() {
        super.onEnable();

        state = State.WAITING;
        groundPos = null;
        saplingPos = null;
        previousSlot = -1;
        targetSlot = -1;
        attacking = false;
        growTicks = 0;
        emptyTicks = 0;

        if (mc.player == null || mc.world == null || mc.crosshairTarget == null) {
            disableWithMessage("Наведись на блок земли и включи Apple Farm.");
            return;
        }

        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) {
            disableWithMessage("Наведись на блок земли и включи Apple Farm.");
            return;
        }

        BlockPos hitPos = hit.getBlockPos();
        BlockState state = mc.world.getBlockState(hitPos);

        if (!isPlantableGround(state)) {
            disableWithMessage("Нужно навестись на землю/дёрн.");
            return;
        }

        groundPos = hitPos.toImmutable();
        saplingPos = groundPos.up();

        state = StateFor(saplingPos);
    }

    private State StateFor(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();

        if (block == Blocks.OAK_SAPLING) {
            return State.GROWING;
        }
        if (isOakLeaves(block)) {
            return State.BREAKING_LEAVES;
        }
        if (block == Blocks.OAK_LOG) {
            return State.BREAKING_LOG;
        }
        return State.PLANTING;
    }

    @Override
    public void onDisable() {
        releaseKeys();

        if (previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = previousSlot;
            mc.interactionManager.syncSelectedSlot();
        }

        previousSlot = -1;
        targetSlot = -1;
        groundPos = null;
        saplingPos = null;
        state = State.WAITING;

        try {
            RotationComponent.getInstance().stopRotation();
        } catch (Exception ignored) {
        }

        super.onDisable();
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate ignored) {
        if (mc.player == null || mc.world == null || groundPos == null || saplingPos == null) {
            return;
        }

        // Ферма стационарная: модуль сам не нажимает клавиши движения.
        // Baritone pathfinding здесь намеренно не запускается.

        switch (state) {
            case WAITING -> {
                state = StateFor(saplingPos);
            }

            case PLANTING -> plantSapling();

            case GROWING -> growTree();

            case BREAKING_LEAVES -> breakLeaves();

            case BREAKING_LOG -> breakLogs();
        }
    }

    private void plantSapling() {
        releaseAttack();

        int slot = findHotbarSlot(stack -> stack.isOf(Items.OAK_SAPLING));
        if (slot == -1) {
            disableWithMessage("В хотбаре нет дубового саженца.");
            return;
        }

        if (!isPlantableGround(mc.world.getBlockState(groundPos))) {
            disableWithMessage("Под саженцем больше нет подходящей земли.");
            return;
        }

        if (!mc.world.getBlockState(saplingPos).isAir()) {
            state = StateFor(saplingPos);
            return;
        }

        selectSlot(slot);
        aimAtBlock(groundPos);

        BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(groundPos).add(0.0, 0.5, 0.0),
                Direction.UP,
                groundPos,
                false
        );

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        state = State.GROWING;
        growTicks = 0;
    }

    private void growTree() {
        releaseAttack();

        Block block = mc.world.getBlockState(saplingPos).getBlock();

        if (block == Blocks.OAK_LOG) {
            state = State.BREAKING_LEAVES;
            emptyTicks = 0;
            return;
        }

        if (isOakLeaves(block)) {
            state = State.BREAKING_LEAVES;
            return;
        }

        if (block != Blocks.OAK_SAPLING) {
            state = State.PLANTING;
            return;
        }

        int boneMealSlot = findHotbarSlot(stack -> stack.isOf(Items.BONE_MEAL));
        if (boneMealSlot == -1) {
            disableWithMessage("В хотбаре нет костной муки.");
            return;
        }

        selectSlot(boneMealSlot);
        aimAtBlock(saplingPos);

        /*
         * Сбрасываем клиентский cooldown и используем костную муку
         * каждый тик. Сервер всё равно принимает/отклоняет действие
         * согласно своим правилам и cooldown.
         */
        mc.itemUseCooldown = 0;

        BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(saplingPos),
                Direction.UP,
                saplingPos,
                false
        );

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);

        growTicks++;

        // Если сервер не даёт вырастить дерево, не зацикливаемся бесконечно.
        if (growTicks > 20 * 30) {
            disableWithMessage("Дерево не выросло за 30 секунд.");
        }
    }

    private void breakLeaves() {
        BlockPos leaf = findNearestBlock(Blocks.OAK_LEAVES, 8);

        if (leaf == null) {
            emptyTicks++;
            if (emptyTicks >= 2) {
                state = State.BREAKING_LOG;
                emptyTicks = 0;
            }
            releaseAttack();
            return;
        }

        emptyTicks = 0;

        int hoeSlot = findHotbarSlot(stack -> stack.getItem() instanceof HoeItem);
        if (hoeSlot == -1) {
            disableWithMessage("В хотбаре нет мотыги.");
            return;
        }

        selectSlot(hoeSlot);
        aimAtBlock(leaf);

        /*
         * Не подбираем дроп и не ходим к нему.
         * Держим обычную кнопку атаки — Minecraft сам ведёт
         * прогресс разрушения блока.
         */
        mc.options.attackKey.setPressed(true);
        attacking = true;
    }

    private void breakLogs() {
        BlockPos log = findNearestBlock(Blocks.OAK_LOG, 8);

        if (log == null) {
            releaseAttack();
            state = State.PLANTING;
            growTicks = 0;
            emptyTicks = 0;
            return;
        }

        int axeSlot = findHotbarSlot(stack -> stack.getItem() instanceof AxeItem);
        if (axeSlot == -1) {
            disableWithMessage("В хотбаре нет топора.");
            return;
        }

        selectSlot(axeSlot);
        aimAtBlock(log);

        mc.options.attackKey.setPressed(true);
        attacking = true;
    }

    private BlockPos findNearestBlock(Block wanted, int range) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        int baseX = saplingPos.getX();
        int baseY = saplingPos.getY();
        int baseZ = saplingPos.getZ();

        for (int x = -range; x <= range; x++) {
            for (int y = -2; y <= 8; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = new BlockPos(baseX + x, baseY + y, baseZ + z);

                    if (mc.world.getBlockState(pos).getBlock() != wanted) {
                        continue;
                    }

                    double distance = mc.player.squaredDistanceTo(
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5
                    );

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }

        return best;
    }

    private void aimAtBlock(BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d eye = mc.player.getEyePos();

        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;

        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, horizontal));

        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);

        /*
         * Очень высокая скорость: голова фактически переходит
         * на следующий блок за один клиентский тик.
         */
        RotationComponent.update(
                new Rotation(yaw, pitch),
                360.0F,
                360.0F,
                360.0F,
                360.0F,
                2,
                100,
                true
        );
    }

    private void selectSlot(int slot) {
        if (slot < 0 || slot > 8 || mc.player == null) {
            return;
        }

        if (targetSlot == slot) {
            return;
        }

        if (previousSlot == -1) {
            previousSlot = mc.player.getInventory().selectedSlot;
        }

        mc.player.getInventory().selectedSlot = slot;
        mc.interactionManager.syncSelectedSlot();
        targetSlot = slot;
    }

    private int findHotbarSlot(java.util.function.Predicate<ItemStack> predicate) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPlantableGround(BlockState state) {
        Block block = state.getBlock();

        return block == Blocks.DIRT
                || block == Blocks.GRASS_BLOCK
                || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT;
    }

    private boolean isOakLeaves(Block block) {
        return block == Blocks.OAK_LEAVES;
    }

    private void releaseAttack() {
        if (mc.options != null) {
            mc.options.attackKey.setPressed(false);
        }
        attacking = false;
    }

    private void releaseKeys() {
        releaseAttack();

        if (mc.options != null) {
            mc.options.useKey.setPressed(false);
        }
    }

    private void disableWithMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(
                    net.minecraft.text.Text.literal("§c[Apple Farm] §f" + message),
                    false
            );
        }
        setEnabled(false);
    }
}
