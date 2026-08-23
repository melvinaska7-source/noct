package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.util.render.math.Animation;
import zov.alphadlc.util.render.math.Easing;

@ModuleInformation(moduleName = "Apple Farm", moduleDesc = "Автоматически фармит яблоки — рубит деревья, сажает саженцы, поливает костной мукой", moduleCategory = ModuleCategory.PLAYER)
public class AppleFarm extends Module {

    private static final int RADIUS = 6;

    private enum State {
        BREAK_TREE,
        WAIT_DROP,
        PLACE_SAPLING,
        BONEMEAL,
    }

    private State state = State.BREAK_TREE;
    private int actionTimer = 0;
    private int waitTimer = 0;

    @Override
    public void onEnable() {
        state = State.BREAK_TREE;
        actionTimer = 0;
        waitTimer = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        state = State.BREAK_TREE;
        super.onDisable();
    }

    @Subscribe
    public void onTick(EventTick event) {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || mc.interactionManager == null) return;

        switch (state) {

            // 1. Рубим дерево
            case BREAK_TREE -> {
                if (actionTimer++ < 4) return; // ~80ms при 50 tps

                selectBestAxe(player);

                BlockPos log = findNearestBlock(player.getBlockPos(), RADIUS,
                        Blocks.OAK_LOG, Blocks.OAK_LEAVES,
                        Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES);

                if (log == null) {
                    // Всё срубили — ждём дроп
                    waitTimer = 0;
                    state = State.WAIT_DROP;
                    return;
                }

                lookAt(player, log);
                mc.interactionManager.attackBlock(log, Direction.UP);
                mc.interactionManager.updateBlockBreakingProgress(log, Direction.UP);
                actionTimer = 0;
            }

            // 2. Ждём дроп яблок
            case WAIT_DROP -> {
                if (waitTimer++ < 75) return; // ~1500ms

                if (hasSapling(player)) {
                    state = State.PLACE_SAPLING;
                } else {
                    state = State.BREAK_TREE; // нет саженца — ищем новое дерево
                }
                actionTimer = 0;
            }

            // 3. Ставим саженец
            case PLACE_SAPLING -> {
                if (actionTimer++ < 8) return; // ~150ms

                // Проверяем — может дерево уже выросло?
                BlockPos existingLog = findNearestBlock(player.getBlockPos(), RADIUS,
                        Blocks.OAK_LOG, Blocks.BIRCH_LOG);
                if (existingLog != null) {
                    state = State.BREAK_TREE;
                    actionTimer = 0;
                    return;
                }

                // Ищем саженец — может уже стоит?
                BlockPos existingSapling = findNearestBlock(player.getBlockPos(), RADIUS,
                        Blocks.OAK_SAPLING, Blocks.BIRCH_SAPLING);
                if (existingSapling != null) {
                    state = State.BONEMEAL;
                    actionTimer = 0;
                    return;
                }

                // Ищем место для посадки
                BlockPos ground = findPlantableSpot(player.getBlockPos());
                if (ground == null) return;

                selectItem(player, Items.OAK_SAPLING);
                lookAt(player, ground);

                if (!isLookingAt(player, ground, 8f)) return;

                BlockHitResult hit = new BlockHitResult(
                        Vec3d.ofCenter(ground).add(0, 0.5, 0),
                        Direction.UP,
                        ground,
                        false
                );
                ActionResult result = mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
                if (result.isAccepted()) {
                    state = State.BONEMEAL;
                }
                actionTimer = 0;
            }

            // 4. Поливаем костной мукой
            case BONEMEAL -> {
                if (actionTimer++ < 4) return; // ~80ms

                // Дерево выросло — рубим
                BlockPos log = findNearestBlock(player.getBlockPos(), RADIUS,
                        Blocks.OAK_LOG, Blocks.BIRCH_LOG);
                if (log != null) {
                    state = State.BREAK_TREE;
                    actionTimer = 0;
                    return;
                }

                // Ищем саженец
                BlockPos sapling = findNearestBlock(player.getBlockPos(), RADIUS,
                        Blocks.OAK_SAPLING, Blocks.BIRCH_SAPLING);

                // Саженец пропал — сажаем снова
                if (sapling == null) {
                    state = State.PLACE_SAPLING;
                    actionTimer = 0;
                    return;
                }

                // Нет костной муки — ждём
                if (!hasBoneMeal(player)) return;

                selectItem(player, Items.BONE_MEAL);
                lookAt(player, sapling);
                if (!isLookingAt(player, sapling, 8f)) return;

                BlockHitResult hit = new BlockHitResult(
                        Vec3d.ofCenter(sapling),
                        Direction.UP,
                        sapling,
                        false
                );
                mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
                actionTimer = 0;
            }
        }
    }

    // ---------------------------------------------------------------

    private BlockPos findPlantableSpot(BlockPos center) {
        for (int r = 0; r <= RADIUS; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    for (int y = -2; y <= 2; y++) {
                        BlockPos pos = center.add(x, y, z);
                        if (canPlantSapling(pos)) return pos;
                    }
                }
            }
        }
        return null;
    }

    private boolean canPlantSapling(BlockPos pos) {
        net.minecraft.block.Block below = mc.world.getBlockState(pos).getBlock();
        net.minecraft.block.Block above = mc.world.getBlockState(pos.up()).getBlock();
        boolean goodSoil = below == Blocks.GRASS_BLOCK
                || below == Blocks.DIRT
                || below == Blocks.COARSE_DIRT
                || below == Blocks.PODZOL
                || below == Blocks.ROOTED_DIRT
                || below == Blocks.MOSS_BLOCK;
        boolean airAbove = above instanceof AirBlock;
        return goodSoil && airAbove;
    }

    private BlockPos findNearestBlock(BlockPos center, int radius, net.minecraft.block.Block... blocks) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    net.minecraft.block.Block b = mc.world.getBlockState(pos).getBlock();
                    for (net.minecraft.block.Block t : blocks) {
                        if (b == t) {
                            double d = center.getSquaredDistance(pos);
                            if (d < nearestDist) {
                                nearestDist = d;
                                nearest = pos;
                            }
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private void lookAt(ClientPlayerEntity player, BlockPos pos) {
        Vec3d eyes = player.getEyePos();
        Vec3d target = Vec3d.ofCenter(pos);
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        player.setYaw((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        player.setPitch((float) -Math.toDegrees(Math.atan2(dy, dist)));
    }

    private boolean isLookingAt(ClientPlayerEntity player, BlockPos pos, float maxDeg) {
        Vec3d eyes = player.getEyePos();
        Vec3d target = Vec3d.ofCenter(pos);
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float wantYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return Math.abs(wrap(player.getYaw() - wantYaw)) < maxDeg
                && Math.abs(wrap(player.getPitch() - wantPitch)) < maxDeg;
    }

    private float wrap(float d) {
        d = d % 360f;
        if (d < -180f) d += 360f;
        if (d > 180f) d -= 360f;
        return d;
    }

    private boolean hasSapling(ClientPlayerEntity p) {
        for (int i = 0; i < p.getInventory().size(); i++)
            if (p.getInventory().getStack(i).getItem() == Items.OAK_SAPLING) return true;
        return false;
    }

    private boolean hasBoneMeal(ClientPlayerEntity p) {
        for (int i = 0; i < p.getInventory().size(); i++)
            if (p.getInventory().getStack(i).getItem() == Items.BONE_MEAL) return true;
        return false;
    }

    private void selectBestAxe(ClientPlayerEntity player) {
        net.minecraft.item.Item[] axes = {
                Items.NETHERITE_AXE, Items.DIAMOND_AXE,
                Items.IRON_AXE, Items.GOLDEN_AXE,
                Items.STONE_AXE, Items.WOODEN_AXE
        };
        for (net.minecraft.item.Item axe : axes)
            for (int i = 0; i < 9; i++)
                if (player.getInventory().getStack(i).getItem() == axe) {
                    player.getInventory().selectedSlot = i;
                    return;
                }
    }

    private void selectItem(ClientPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++)
            if (player.getInventory().getStack(i).getItem() == item) {
                player.getInventory().selectedSlot = i;
                return;
            }
    }
}
