package polar.ru.client.modules.impl.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class ChestStealer
extends Module {
    public static ChestStealer INSTANCE = new ChestStealer();
    private final FloatSetting stealDelay = new FloatSetting("Задержка", 100.0f, 0.0f, 1000.0f, 1.0f);
    private final BooleanSetting randomize = new BooleanSetting("Рандомизация", false);
    private long lastStealTime = 0L;

    public ChestStealer() {
        super("ChestStealer", "Автоматически открывает сундуки и забирает из них предметы", Module.ModuleCategory.PLAYER);
        this.addSettings(this.stealDelay, this.randomize);
    }

    @EventLink
    private void onUpdate(EventUpdate event) {
        long delay;
        if (ChestStealer.mc.player == null || ChestStealer.mc.interactionManager == null) {
            return;
        }
        ScreenHandler openContainer = ChestStealer.mc.player.currentScreenHandler;
        if (openContainer == null || openContainer == ChestStealer.mc.player.playerScreenHandler) {
            return;
        }
        if (!(openContainer instanceof GenericContainerScreenHandler) && !(openContainer instanceof HopperScreenHandler)) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastStealTime < (delay = (long)this.stealDelay.get())) {
            return;
        }
        DefaultedList slots = openContainer.slots;
        this.findValidItem((List<Slot>)slots, openContainer).ifPresent(slot -> {
            if (ChestStealer.mc.player.currentScreenHandler == openContainer) {
                ChestStealer.mc.interactionManager.clickSlot(openContainer.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, (PlayerEntity)ChestStealer.mc.player);
                this.lastStealTime = currentTime;
            }
        });
    }

    private Optional<Slot> findValidItem(List<Slot> slots, ScreenHandler handler) {
        int containerSlotCount = this.getContainerSlotCount(handler);
        if (containerSlotCount <= 0 || containerSlotCount > slots.size()) {
            return Optional.empty();
        }
        List<Slot> containerSlots = slots.subList(0, containerSlotCount);
        ArrayList<Slot> validSlots = new ArrayList<Slot>();
        for (Slot slot : containerSlots) {
            if (!slot.hasStack() || slot.getStack().isEmpty() || ChestStealer.mc.player.getItemCooldownManager().isCoolingDown(slot.getStack())) continue;
            validSlots.add(slot);
        }
        if (validSlots.isEmpty()) {
            return Optional.empty();
        }
        if (this.randomize.isState()) {
            int randomIndex = ThreadLocalRandom.current().nextInt(validSlots.size());
            return Optional.of((Slot)validSlots.get(randomIndex));
        }
        return Optional.of((Slot)validSlots.get(0));
    }

    private int getContainerSlotCount(ScreenHandler handler) {
        if (handler instanceof GenericContainerScreenHandler) {
            GenericContainerScreenHandler container = (GenericContainerScreenHandler)handler;
            Inventory inventory = container.getInventory();
            return inventory.size();
        }
        if (handler instanceof HopperScreenHandler) {
            return 5;
        }
        return 0;
    }

    @Override
    public void onDisable() {
        this.lastStealTime = 0L;
        super.onDisable();
    }
}

