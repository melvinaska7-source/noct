package zov.alphadlc.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.event.list.EventPlayerSyncEnd;
import zov.alphadlc.event.list.EventPlayerUpdate;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.friend.FriendRepository;
import zov.alphadlc.util.math.RotationUtil;
import zov.alphadlc.util.math.StopWatch;
import zov.alphadlc.util.player.other.InventoryUtil;
import zov.alphadlc.util.rotation.Rotation;
import zov.alphadlc.util.rotation.RotationComponent;

@ModuleInformation(moduleName = "AutoTrap", moduleDesc = "Ставит двойную паутину в ноги и рост цели", moduleCategory = ModuleCategory.COMBAT)
public class AutoTrap extends Module {

    private final SliderSetting range = new SliderSetting("Радиус", 3.2f, 2.0f, 4.5f, 0.1f);
    private final SliderSetting delay = new SliderSetting("Задержка (тики)", 7, 4, 12, 1);
    private final BooleanSetting pauseInGui = new BooleanSetting("Пауза в GUI", true);
    private final BooleanSetting predict = new BooleanSetting("Предикт", true);
    private final SliderSetting predictTicks = new SliderSetting("Предикт тиков", 3, 1, 10, 1).setVisible(predict::getValue);

    private final StopWatch timer = new StopWatch();

    @Subscribe
    private void onUpdate(EventPlayerUpdate ignored) {
        if (mc.player == null || mc.world == null) return;
        if (pauseInGui.getValue() && mc.currentScreen != null) return;
        if (!timer.isReached((long) (delay.getValue() * 50))) return;

        PlayerEntity target = findTarget();
        if (target == null) return;

        BlockPos placePos = getTrapPosition(target);
        if (placePos == null) return;

        BlockHitResult hit = buildHit(placePos);
        if (hit == null) return;

        Vec2f rot = RotationUtil.calculate(mc.player.getEyePos(), hit.getPos());
        RotationComponent.update(new Rotation(rot), 65f, 65f, 180f, 2, 6);
    }

    @Subscribe
    private void onSyncEnd(EventPlayerSyncEnd ignored) {
        if (mc.player == null || mc.world == null) return;
        if (pauseInGui.getValue() && mc.currentScreen != null) return;
        if (!timer.isReached((long) (delay.getValue() * 50))) return;

        PlayerEntity target = findTarget();
        if (target == null) return;

        int slot = InventoryUtil.searchItemHotbar(Items.COBWEB);
        if (slot == -1) return;

        BlockPos trapPos = getTrapPosition(target);
        if (trapPos == null) return;
        
        // Пытаемся поставить паутины в ноги и на рост (полный блок)
        BlockPos feetPos = trapPos;
        BlockPos headPos = trapPos.up();
        
        boolean placedAny = false;
        
        // Ставим в ноги если можем
        if (mc.world.getBlockState(feetPos).isAir()) {
            BlockHitResult hit = buildHit(feetPos);
            if (hit != null) {
                if (placeWeb(hit, slot)) {
                    placedAny = true;
                }
            }
        }
        
        // Ставим на рост если можем
        if (mc.world.getBlockState(headPos).isAir()) {
            BlockHitResult hit = buildHit(headPos);
            if (hit != null) {
                if (placeWeb(hit, slot)) {
                    placedAny = true;
                }
            }
        }

        // Сбрасываем задержку только если хоть одна паутина поставлена
        if (placedAny) {
            timer.reset();
        }
    }
    
    private boolean placeWeb(BlockHitResult hit, int slot) {
        int previous = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        mc.interactionManager.syncSelectedSlot();

        ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (result instanceof ActionResult.Success) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        mc.player.getInventory().selectedSlot = previous;
        mc.interactionManager.syncSelectedSlot();
        
        return result instanceof ActionResult.Success;
    }

    private PlayerEntity findTarget() {
        PlayerEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            if (FriendRepository.isFriend(player.getNameForScoreboard())) continue;
            double distance = mc.player.distanceTo(player);
            if (distance > range.getValue()) continue;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    // Получение позиции для трапа с учётом предикта
    private BlockPos getTrapPosition(PlayerEntity target) {
        if (!predict.getValue()) {
            return target.getBlockPos();
        }
        
        // Предикт движения цели
        Vec3d velocity = new Vec3d(
            target.getX() - target.prevX,
            target.getY() - target.prevY,
            target.getZ() - target.prevZ
        );
        
        // Если цель не двигается, используем текущую позицию
        if (velocity.lengthSquared() < 0.001) {
            return target.getBlockPos();
        }
        
        // Предиктим позицию на N тиков вперёд
        Vec3d predictedPos = target.getPos().add(velocity.multiply(predictTicks.getValue()));
        return BlockPos.ofFloored(predictedPos);
    }

    private BlockHitResult buildHit(BlockPos placePos) {
        if (!mc.world.getBlockState(placePos).isAir()) return null;
        for (Direction dir : Direction.values()) {
            BlockPos support = placePos.offset(dir);
            BlockState state = mc.world.getBlockState(support);
            if (state.isAir() || state.isOf(Blocks.COBWEB)) continue;

            Direction face = dir.getOpposite();
            Vec3d center = new Vec3d(
                    support.getX() + 0.5 + face.getOffsetX() * 0.5,
                    support.getY() + 0.5 + face.getOffsetY() * 0.5,
                    support.getZ() + 0.5 + face.getOffsetZ() * 0.5
            );
            return new BlockHitResult(center, face, support, false);
        }
        return null;
    }
}
