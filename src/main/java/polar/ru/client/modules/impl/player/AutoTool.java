package polar.ru.client.modules.impl.player;

import net.minecraft.block.BlockState;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;

public class AutoTool
extends Module {
    public static AutoTool INSTANCE = new AutoTool();
    private final BooleanSetting packet = new BooleanSetting("Пакетный", false);
    private final BooleanSetting silent = new BooleanSetting("Видно только для других людей", false);
    private int previousSlot = -1;

    public AutoTool() {
        super("AutoTool", "При копании берет лучший предмет", Module.ModuleCategory.PLAYER);
        this.addSettings(this.packet, this.silent);
    }

    @EventLink
    public void onEvent(EventUpdate event) {
        if (AutoTool.mc.player == null || AutoTool.mc.world == null || AutoTool.mc.interactionManager == null || AutoTool.mc.player.isCreative()) {
            this.previousSlot = -1;
            return;
        }
        if (AutoTool.mc.interactionManager.isBreakingBlock()) {
            int toolSlot;
            if (this.previousSlot == -1) {
                this.previousSlot = AutoTool.mc.player.getInventory().selectedSlot;
            }
            if ((toolSlot = this.findOptimalTool()) != -1) {
                this.switchToSlot(toolSlot);
            }
        } else if (this.previousSlot != -1) {
            this.switchToSlot(this.previousSlot);
            this.previousSlot = -1;
        }
    }

    private void switchToSlot(int slot) {
        if (slot < 0 || slot > 8) {
            return;
        }
        if (AutoTool.mc.player.getInventory().selectedSlot == slot) {
            return;
        }
        if (this.silent.isState()) {
            mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(slot));
        } else if (this.packet.isState()) {
            AutoTool.mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(slot));
        } else {
            AutoTool.mc.player.getInventory().selectedSlot = slot;
        }
    }

    private int findOptimalTool() {
        HitResult hitResult = AutoTool.mc.crosshairTarget;
        if (!(hitResult instanceof BlockHitResult)) {
            return -1;
        }
        BlockHitResult blockHitResult = (BlockHitResult)hitResult;
        BlockState blockState = AutoTool.mc.world.getBlockState(blockHitResult.getBlockPos());
        return this.findBestToolSlot(blockState);
    }

    private int findBestToolSlot(BlockState blockState) {
        int bestSlot = -1;
        float bestSpeed = 1.0f;
        for (int i2 = 0; i2 < 9; ++i2) {
            float speed = AutoTool.mc.player.getInventory().getStack(i2).getMiningSpeedMultiplier(blockState);
            if (!(speed > bestSpeed)) continue;
            bestSpeed = speed;
            bestSlot = i2;
        }
        return bestSlot;
    }

    @Override
    public void onDisable() {
        this.previousSlot = -1;
        super.onDisable();
    }
}

