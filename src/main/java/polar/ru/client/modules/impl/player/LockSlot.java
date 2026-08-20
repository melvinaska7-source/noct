package polar.ru.client.modules.impl.player;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.mixin.SlotAccessor;

public class LockSlot
extends Module {
    public static LockSlot INSTANCE = new LockSlot();
    private final ListSetting slots = new ListSetting("Слоты", new BooleanSetting("1", false), new BooleanSetting("2", false), new BooleanSetting("3", false), new BooleanSetting("4", false), new BooleanSetting("5", false), new BooleanSetting("6", false), new BooleanSetting("7", false), new BooleanSetting("8", false), new BooleanSetting("9", false));

    public LockSlot() {
        super("LockSlot", "Блокирует выброс предметов из выбранных слотов", Module.ModuleCategory.PLAYER);
        this.addSettings(this.slots);
    }

    @EventLink
    public void onPacket(EventPacket event) {
        int hotbarSlot;
        ClickSlotC2SPacket packet;
        if (LockSlot.mc.player == null || event.getType() != EventPacket.Type.SEND) {
            return;
        }
        if (LockSlot.mc.currentScreen instanceof HandledScreen) {
            return;
        }
        Packet<?> var_2596_2 = event.getPacket();
        if (var_2596_2 instanceof PlayerActionC2SPacket) {
            PlayerActionC2SPacket packet2 = (PlayerActionC2SPacket)var_2596_2;
            if (packet2.getAction() != PlayerActionC2SPacket.Action.DROP_ITEM && packet2.getAction() != PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) {
                return;
            }
            if (this.isCurrentSlotLockedForDrop()) {
                event.cancel();
                this.sendLockedMessage(LockSlot.mc.player.getInventory().selectedSlot);
            }
            return;
        }
        var_2596_2 = event.getPacket();
        if (var_2596_2 instanceof ClickSlotC2SPacket && (packet = (ClickSlotC2SPacket)var_2596_2).getActionType() == SlotActionType.THROW && (hotbarSlot = this.getHotbarSlotFromClick(packet.getSlot())) >= 0 && this.isHotbarSlotLocked(hotbarSlot)) {
            event.cancel();
            this.sendLockedMessage(hotbarSlot);
        }
    }

    public boolean isCurrentSlotLockedForDrop() {
        if (!this.isEnable() || LockSlot.mc.player == null || LockSlot.mc.player.getMainHandStack().isEmpty()) {
            return false;
        }
        if (LockSlot.mc.currentScreen instanceof HandledScreen) {
            return false;
        }
        return this.isHotbarSlotLocked(LockSlot.mc.player.getInventory().selectedSlot);
    }

    private boolean isHotbarSlotLocked(int slot) {
        if (slot < 0 || slot >= this.slots.getSettings().size()) {
            return false;
        }
        return this.slots.getSettings().get(slot).isState();
    }

    private int getHotbarSlotFromClick(int slotId) {
        if (LockSlot.mc.player == null || slotId < 0 || slotId >= LockSlot.mc.player.currentScreenHandler.slots.size()) {
            return -1;
        }
        Slot slot = LockSlot.mc.player.currentScreenHandler.getSlot(slotId);
        SlotAccessor accessor = (SlotAccessor)slot;
        int inventoryIndex = accessor.polar$getIndex();
        if (accessor.polar$getInventory() == LockSlot.mc.player.getInventory() && inventoryIndex >= 0 && inventoryIndex <= 8) {
            return inventoryIndex;
        }
        return -1;
    }

    private void sendLockedMessage(int slot) {
        ChatUtils.sendMessage("Выброс предмета из слота " + (slot + 1) + " заблокирован");
    }
}

