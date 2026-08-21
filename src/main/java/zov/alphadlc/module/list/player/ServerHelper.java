package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import zov.alphadlc.event.list.EventKeyInput;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.list.movement.Sprint;
import zov.alphadlc.module.settings.BindSetting;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.util.base.Instance;

@ModuleInformation(moduleName = "ServerHelper", moduleDesc = "Помощник для сервера", moduleCategory = ModuleCategory.PLAYER)
public class ServerHelper extends Module {
    private final ModeSetting server = new ModeSetting("Сервер", "ReallyWorld", "ReallyWorld");
    
    // ReallyWorld аспекты
    private final BindSetting antiFlyKey = new BindSetting("Анти Полет", 71).setVisible(() -> this.server.is("ReallyWorld"));
    private final BindSetting grinchPotionKey = new BindSetting("Зелье Гринча", -1).setVisible(() -> this.server.is("ReallyWorld"));
    private final BindSetting autoShiftKey = new BindSetting("AutoShift (Шифт для шара)", -1).setVisible(() -> this.server.is("ReallyWorld"));
    private final BindSetting newYearHorrorKey = new BindSetting("Новогодний ужас", -1).setVisible(() -> this.server.is("ReallyWorld"));
    private final BindSetting darkEssenceKey = new BindSetting("Эссенция кромешника", -1).setVisible(() -> this.server.is("ReallyWorld"));
    private final BindSetting snowballKey = new BindSetting("Снежок", -1).setVisible(() -> this.server.is("ReallyWorld"));
    private final BindSetting trapKey = new BindSetting("Трапка", -1).setVisible(() -> this.server.is("ReallyWorld"));
    private final BooleanSetting autoFixAll = new BooleanSetting("Auto /fix all", false).setVisible(() -> this.server.is("ReallyWorld"));
    
    private boolean useAntiFly;
    private boolean useGrinchPotion;
    private boolean useAutoShift;
    private boolean useNewYearHorror;
    private boolean useDarkEssence;
    private boolean useSnowball;
    private boolean useTrap;
    private boolean wasInPvp;
    private int nextFixTick;

    // Public getters for HUD
    public int getAntiFlyKey() {
        return antiFlyKey.getValue();
    }

    public int getGrinchPotionKey() {
        return grinchPotionKey.getValue();
    }

    public int getAutoShiftKey() {
        return autoShiftKey.getValue();
    }

    public int getNewYearHorrorKey() {
        return newYearHorrorKey.getValue();
    }

    public int getDarkEssenceKey() {
        return darkEssenceKey.getValue();
    }

    public int getSnowballKey() {
        return snowballKey.getValue();
    }

    public int getTrapKey() {
        return trapKey.getValue();
    }

    private boolean isGrinchPotion(ItemStack stack) {
        if (!stack.isOf(Items.SPLASH_POTION)) {
            return false;
        }
        String name = stack.getName().getString().toLowerCase();
        return name.contains("гринч");
    }

    private boolean isNewYearHorror(ItemStack stack) {
        if (!stack.isOf(Items.SPLASH_POTION)) {
            return false;
        }
        String name = stack.getName().getString().toLowerCase();
        return name.contains("новогодний ужас");
    }

    private boolean isDarkEssence(ItemStack stack) {
        if (!stack.isOf(Items.SPLASH_POTION)) {
            return false;
        }
        String name = stack.getName().getString().toLowerCase();
        return name.contains("эссенция кромешника");
    }

    private boolean isSnowball(ItemStack stack) {
        if (!stack.isOf(Items.SPLASH_POTION)) {
            return false;
        }
        String name = stack.getName().getString().toLowerCase();
        return name.contains("снежок");
    }

    private boolean isAntiFly(ItemStack stack) {
        if (!stack.isOf(Items.FIREWORK_STAR)) {
            return false;
        }
        return true;
    }

    private boolean isTrapItem(ItemStack stack) {
        return stack.isOf(Items.HEART_OF_THE_SEA);
    }

    private boolean isTrapPotion(ItemStack stack) {
        if (!stack.isOf(Items.SPLASH_POTION)) {
            return false;
        }
        String name = stack.getName().getString().toLowerCase();
        return name.contains("ловуш") || name.contains("ловушк");
    }

    private int findSlot(Predicate<ItemStack> predicate, int from, int to) {
        for (int i2 = from; i2 < to; ++i2) {
            if (!predicate.test(this.mc.player.getInventory().getStack(i2))) continue;
            return i2;
        }
        return -1;
    }
    
    private int findTrapInHotbar() {
        return this.findSlot(this::isTrapItem, 0, 9);
    }

    private int findTrapInInventory() {
        return this.findSlot(this::isTrapItem, 9, 45);
    }

    @Subscribe
    private void onKey(EventKeyInput e2) {
        if (this.mc.currentScreen != null) {
            return;
        }
        if (e2.getAction() != 1) {
            return;
        }
        if (e2.getKey() == this.antiFlyKey.getValue().intValue()) {
            this.useAntiFly = true;
        }
        if (e2.getKey() == this.grinchPotionKey.getValue().intValue()) {
            this.useGrinchPotion = true;
        }
        if (e2.getKey() == this.autoShiftKey.getValue().intValue()) {
            this.useAutoShift = true;
        }
        if (e2.getKey() == this.newYearHorrorKey.getValue().intValue()) {
            this.useNewYearHorror = true;
        }
        if (e2.getKey() == this.darkEssenceKey.getValue().intValue()) {
            this.useDarkEssence = true;
        }
        if (e2.getKey() == this.snowballKey.getValue().intValue()) {
            this.useSnowball = true;
        }
        if (e2.getKey() == this.trapKey.getValue().intValue()) {
            this.useTrap = true;
        }
    }

    @Subscribe
    private void onTick(EventTick e2) {
        if (this.useAntiFly) {
            this.useAntiFly = false;
            this.useItem(this::isAntiFly);
        }
        if (this.useGrinchPotion) {
            this.useGrinchPotion = false;
            this.useItem(this::isGrinchPotion);
        }
        if (this.useAutoShift) {
            this.useAutoShift = false;
            this.pulseShift();
        }
        if (this.useNewYearHorror) {
            this.useNewYearHorror = false;
            this.useItem(this::isNewYearHorror);
        }
        if (this.useDarkEssence) {
            this.useDarkEssence = false;
            this.useItem(this::isDarkEssence);
        }
        if (this.useSnowball) {
            this.useSnowball = false;
            this.useItem(this::isSnowball);
        }
        if (this.useTrap) {
            this.useTrap = false;
            this.useTrapItem();
        }
        
        // Auto /fix all при выходе из pvp
        handleAutoFixAll();
    }
    
    private void handleAutoFixAll() {
        if (!this.server.is("ReallyWorld") || !autoFixAll.getValue()) {
            wasInPvp = isPvp();
            return;
        }
        boolean inPvp = isPvp();
        if (wasInPvp && !inPvp && mc.player.age >= nextFixTick) {
            mc.player.networkHandler.sendChatCommand("fix all");
            nextFixTick = mc.player.age + 40;
        }
        wasInPvp = inPvp;
    }
    
    private boolean isPvp() {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        // Простая проверка - есть ли игроки поблизости
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .anyMatch(p -> p.squaredDistanceTo(mc.player) < 100);
    }
    
    private void pulseShift() {
        if (mc.options == null) {
            return;
        }
        mc.options.sneakKey.setPressed(true);
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                if (mc.options != null) {
                    mc.options.sneakKey.setPressed(false);
                }
            }
        }, 150L);
    }
    
    private void useTrapItem() {
        if (this.mc.player == null) {
            return;
        }
        if (this.isTrapItem(this.mc.player.getMainHandStack())) {
            this.mc.interactionManager.interactItem((PlayerEntity)this.mc.player, Hand.MAIN_HAND);
            return;
        }
        if (this.isTrapItem(this.mc.player.getOffHandStack())) {
            this.mc.interactionManager.interactItem((PlayerEntity)this.mc.player, Hand.OFF_HAND);
            return;
        }
        int slot = this.findTrapInHotbar();
        if (slot != -1) {
            int prev = this.mc.player.getInventory().selectedSlot;
            this.mc.player.getInventory().selectedSlot = slot;
            this.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(slot));
            this.mc.interactionManager.interactItem((PlayerEntity)this.mc.player, Hand.MAIN_HAND);
            this.mc.player.getInventory().selectedSlot = prev;
            this.mc.player.networkHandler.sendPacket((Packet)new UpdateSelectedSlotC2SPacket(prev));
            return;
        }
        slot = this.findTrapInInventory();
        if (slot != -1) {
            int prev = this.mc.player.getInventory().selectedSlot;
            int syncId = this.mc.player.currentScreenHandler.syncId;
            this.mc.interactionManager.clickSlot(syncId, slot, prev, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
            this.mc.interactionManager.interactItem((PlayerEntity)this.mc.player, Hand.MAIN_HAND);
            this.mc.interactionManager.clickSlot(syncId, slot, prev, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
        }
    }
    
    private int findTrapPotionInHotbar() {
        return this.findSlot(this::isTrapPotion, 0, 9);
    }

    private int findTrapPotionInInventory() {
        return this.findSlot(this::isTrapPotion, 9, 45);
    }

    private void useItem(Predicate<ItemStack> predicate) {
        if (this.mc.player == null || this.mc.getNetworkHandler() == null) {
            return;
        }
        if (predicate.test(this.mc.player.getOffHandStack())) {
            this.shiftClick();
            return;
        }
        int hotbarSlot = this.findSlot(predicate, 0, 9);
        if (hotbarSlot != -1) {
            boolean wasSprinting = this.stopSprintingIfNeeded();
            this.swapToOffhand(this.mc.player.currentScreenHandler.syncId, 45, hotbarSlot);
            this.shiftClick();
            this.swapToOffhand(this.mc.player.currentScreenHandler.syncId, 45, hotbarSlot);
            if (wasSprinting) {
                this.resumeSprinting();
            }
            return;
        }
        int invSlot = this.findSlot(predicate, 9, 45);
        if (invSlot != -1) {
            boolean wasSprinting = this.stopSprintingIfNeeded();
            this.swapToOffhand(0, invSlot, 40);
            this.shiftClick();
            this.swapToOffhand(0, invSlot, 40);
            if (wasSprinting) {
                this.resumeSprinting();
            }
        }
    }

    private void shiftClick() {
        this.mc.getNetworkHandler().sendPacket((Packet)new ClientCommandC2SPacket((Entity)this.mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        this.mc.getNetworkHandler().sendPacket((Packet)new ClientCommandC2SPacket((Entity)this.mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
    }

    private void swapToOffhand(int syncId, int slot, int button) {
        this.mc.interactionManager.clickSlot(syncId, slot, button, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
        this.mc.getNetworkHandler().sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
    }

    private boolean stopSprintingIfNeeded() {
        if (!this.mc.player.isSprinting()) {
            return false;
        }
        this.mc.getNetworkHandler().sendPacket((Packet)new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
        this.mc.player.setSprinting(false);
        this.mc.getNetworkHandler().sendPacket((Packet)new ClientCommandC2SPacket((Entity)this.mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        if (!Instance.get(Sprint.class).isEnabled()) {
            this.mc.options.sprintKey.setPressed(false);
        }
        return true;
    }

    private void resumeSprinting() {
        this.mc.getNetworkHandler().sendPacket((Packet)new PlayerInputC2SPacket(this.mc.player.input.playerInput));
    }
}
