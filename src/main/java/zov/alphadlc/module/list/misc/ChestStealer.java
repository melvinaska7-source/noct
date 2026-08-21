package zov.alphadlc.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec2f;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.math.RotationUtil;
import zov.alphadlc.util.rotation.Rotation;
import zov.alphadlc.util.rotation.RotationComponent;

@ModuleInformation(moduleName = "ChestStealer", moduleDesc = "Забирает предметы из контейнеров", moduleCategory = ModuleCategory.MISC)
public class ChestStealer extends Module {

    private final SliderSetting delay = new SliderSetting("Задержка", 100, 0, 1000, 10);
    private final BooleanSetting autoOpen = new BooleanSetting("Авто-открытие", false);

    private long lastClick;

    @Subscribe
    private void onTick(EventTick ignored) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler instanceof GenericContainerScreenHandler || handler instanceof ShulkerBoxScreenHandler) {
            stealFrom(handler);
            return;
        }

        if (autoOpen.getValue() && mc.currentScreen == null) {
            tryOpenChest();
        }
    }

    private void stealFrom(ScreenHandler handler) {
        if (System.currentTimeMillis() - lastClick < delay.getValue()) return;

        for (Slot slot : handler.slots) {
            if (!slot.hasStack()) continue;
            if (slot.inventory == mc.player.getInventory()) continue;

            // Забираем всё без исключений
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
            lastClick = System.currentTimeMillis();
            return;
        }
    }

    private void tryOpenChest() {
        BlockPos chest = findNearestChest(5);
        if (chest == null) return;

        Vec3d center = Vec3d.ofCenter(chest);
        Vec2f angle = RotationUtil.calculate(center);
        RotationComponent.update(new Rotation(angle.x, angle.y), 60f, 60f, 2, 2);

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        HitResult hit = mc.player.raycast(5.0, tickDelta, false);
        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = mc.world.getBlockState(blockHit.getBlockPos());
            if (state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.ENDER_CHEST)
                    || state.getBlock() instanceof net.minecraft.block.ShulkerBoxBlock) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHit);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private BlockPos findNearestChest(int range) {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterate(playerPos.add(-range, -range, -range), playerPos.add(range, range, range))) {
            BlockState state = mc.world.getBlockState(pos);
            boolean container = state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST)
                    || state.isOf(Blocks.ENDER_CHEST) || state.getBlock() instanceof net.minecraft.block.ShulkerBoxBlock;
            if (!container) continue;

            double dist = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
            if (dist < bestDist && dist <= 25.0) {
                bestDist = dist;
                best = pos.toImmutable();
            }
        }
        return best;
    }
}
