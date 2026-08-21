package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.mixin.SlotAccessor;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeListSetting;
import zov.alphadlc.util.chat.ChatUtil;

@ModuleInformation(moduleName = "LockSlot", moduleDesc = "Блокировка выброса из слотов", moduleCategory = ModuleCategory.PLAYER)
public class LockSlot extends Module {
    private final ModeListSetting slots = new ModeListSetting("Слоты", 
        new BooleanSetting("1", false), 
        new BooleanSetting("2", false), 
        new BooleanSetting("3", false), 
        new BooleanSetting("4", false), 
        new BooleanSetting("5", false), 
        new BooleanSetting("6", false), 
        new BooleanSetting("7", false), 
        new BooleanSetting("8", false), 
        new BooleanSetting("9", false));

    @Subscribe
    private void onPacket(EventPacket e2) {
        int hotbarSlot;
        ClickSlotC2SPacket packet;
        if (this.mc.player == null || e2.getType() != EventPacket.Type.SEND) {
            return;
        }
        if (this.mc.currentScreen instanceof HandledScreen) {
            return;
        }
        Packet<?> packet2 = e2.getPacket();
        if (packet2 instanceof PlayerActionC2SPacket) {
            PlayerActionC2SPacket packet3 = (PlayerActionC2SPacket)packet2;
            if (packet3.getAction() != PlayerActionC2SPacket.Action.DROP_ITEM && packet3.getAction() != PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) {
                return;
            }
            if (this.isCurrentSlotLockedForDrop()) {
                e2.cancelEvent();
                this.sendLockedMessage(this.mc.player.getInventory().selectedSlot);
            }
            return;
        }
        packet2 = e2.getPacket();
        if (packet2 instanceof ClickSlotC2SPacket && (packet = (ClickSlotC2SPacket)packet2).getActionType() == SlotActionType.THROW && (hotbarSlot = this.getHotbarSlotFromClick(packet.getSlot())) >= 0 && this.isHotbarSlotLocked(hotbarSlot)) {
            e2.cancelEvent();
            this.sendLockedMessage(hotbarSlot);
        }
    }

    public boolean isCurrentSlotLockedForDrop() {
        if (!this.isEnabled() || this.mc.player == null || this.mc.player.getMainHandStack().isEmpty()) {
            return false;
        }
        if (this.mc.currentScreen instanceof HandledScreen) {
            return false;
        }
        return this.isHotbarSlotLocked(this.mc.player.getInventory().selectedSlot);
    }

    private boolean isHotbarSlotLocked(int slot) {
        if (slot < 0 || slot >= this.slots.getSettings().size()) {
            return false;
        }
        return this.slots.getSettings().get(slot).getValue();
    }

    private int getHotbarSlotFromClick(int slotId) {
        if (this.mc.player == null || slotId < 0 || slotId >= this.mc.player.currentScreenHandler.slots.size()) {
            return -1;
        }
        Slot slot = this.mc.player.currentScreenHandler.getSlot(slotId);
        SlotAccessor accessor = (SlotAccessor)slot;
        int inventoryIndex = accessor.getIndex();
        if (accessor.getInventory() == this.mc.player.getInventory() && inventoryIndex >= 0 && inventoryIndex <= 9) {
            return inventoryIndex;
        }
        return -1;
    }

    private void sendLockedMessage(int slot) {
        ChatUtil.send("Выброс предмета из слота " + (slot + 1) + " заблокирован");
    }
}
