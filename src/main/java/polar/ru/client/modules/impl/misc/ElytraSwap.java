package polar.ru.client.modules.impl.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventBinding;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.api.utils.player.InventoryUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.movement.InventoryWalk;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;

public class ElytraSwap
extends Module {
    public static ElytraSwap INSTANCE = new ElytraSwap();
    private static final long SWAP_COOLDOWN_MS = 600L;
    private static final long FIREWORK_COOLDOWN_MS = 50L;
    private static final long PENDING_FIREWORK_TIMEOUT_MS = 500L;
    private final BindSetting elytraBind = new BindSetting("Бинд элитры", -1);
    private final BindSetting fireworkBind = new BindSetting("Бинд фейерверка", -1);
    private final BooleanSetting autoFly = new BooleanSetting("Авто-взлёт", true);
    private final BooleanSetting syncGuiMove = new BooleanSetting("Синхр. с GuiMove", true);
    private ItemStack currentChest = ItemStack.EMPTY;
    private long swapLastMs = 0L;
    private long fireworkLastMs = 0L;
    private long pendingFireworkUntil = 0L;
    private boolean swapQueued = false;
    private boolean fireworkQueued = false;

    public ElytraSwap() {
        super("Elytra Util", "Автоматический свап элитр", Module.ModuleCategory.MISC);
        this.addSettings(this.elytraBind, this.fireworkBind, this.autoFly, this.syncGuiMove);
    }

    @EventLink
    public void onUpdate(EventUpdate ignored) {
        if (ElytraSwap.mc.player == null || ElytraSwap.mc.world == null) {
            return;
        }
        this.currentChest = ElytraSwap.mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (this.swapQueued) {
            this.swapQueued = false;
            this.trySwap();
        }
        if (this.fireworkQueued) {
            this.fireworkQueued = false;
            this.requestFirework();
        }
        if (this.autoFly.isState() && this.currentChest.isOf(Items.ELYTRA)) {
            this.tryTakeoff(this.currentChest);
        }
        this.tryLaunchPendingFirework();
    }

    @EventLink
    public void onBinding(EventBinding event) {
        if (event.getKey() == this.elytraBind.getKey()) {
            this.swapQueued = true;
        }
        if (event.getKey() == this.fireworkBind.getKey()) {
            this.fireworkQueued = true;
        }
    }

    private void trySwap() {
        if (System.currentTimeMillis() - this.swapLastMs < 600L) {
            return;
        }
        this.doChangeChest(this.currentChest);
        this.swapLastMs = System.currentTimeMillis();
    }

    private void doChangeChest(ItemStack chest) {
        if (chest.isOf(Items.ELYTRA)) {
            int armorSlot = this.findChestplateSlot();
            if (armorSlot < 0) {
                ChatUtils.sendMessage(String.valueOf(Formatting.RED) + String.valueOf(Formatting.BOLD) + "Нет нагрудника!");
                return;
            }
            this.moveToChestSlot(armorSlot);
        } else {
            int elytraSlot = this.findItemSlot(Items.ELYTRA);
            if (elytraSlot < 0) {
                ChatUtils.sendMessage(String.valueOf(Formatting.RED) + String.valueOf(Formatting.BOLD) + "Нет элитры!");
                return;
            }
            this.moveToChestSlot(elytraSlot);
        }
    }

    private void moveToChestSlot(int slot) {
        boolean guiActive;
        InventoryWalk guiMove = InventoryWalk.INSTANCE;
        boolean bl = guiActive = guiMove != null && guiMove.isEnable() && this.syncGuiMove.isState();
        if (guiActive) {
            guiMove.swapBypass = true;
        }
        if (slot >= 0 && slot < 9) {
            ElytraSwap.mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, (PlayerEntity)ElytraSwap.mc.player);
        } else {
            ElytraSwap.mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, (PlayerEntity)ElytraSwap.mc.player);
            ElytraSwap.mc.interactionManager.clickSlot(0, 6, 0, SlotActionType.SWAP, (PlayerEntity)ElytraSwap.mc.player);
            ElytraSwap.mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, (PlayerEntity)ElytraSwap.mc.player);
        }
        ElytraSwap.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
        if (guiActive) {
            guiMove.swapBypass = false;
        }
    }

    private void requestFirework() {
        if (System.currentTimeMillis() - this.fireworkLastMs < 50L) {
            return;
        }
        if (!this.hasFirework()) {
            return;
        }
        if (this.launchFirework()) {
            this.fireworkLastMs = System.currentTimeMillis();
            this.pendingFireworkUntil = 0L;
            return;
        }
        if (this.canWaitForFlight()) {
            this.pendingFireworkUntil = System.currentTimeMillis() + 500L;
        }
    }

    private void tryLaunchPendingFirework() {
        if (this.pendingFireworkUntil == 0L) {
            return;
        }
        if (System.currentTimeMillis() > this.pendingFireworkUntil || !this.hasFirework()) {
            this.pendingFireworkUntil = 0L;
            return;
        }
        if (this.launchFirework()) {
            this.fireworkLastMs = System.currentTimeMillis();
            this.pendingFireworkUntil = 0L;
        }
    }

    private boolean launchFirework() {
        boolean guiActive;
        if (!ElytraSwap.mc.player.isGliding()) {
            return false;
        }
        if (!this.hasFirework()) {
            return false;
        }
        InventoryWalk guiMove = InventoryWalk.INSTANCE;
        boolean bl = guiActive = guiMove != null && guiMove.isEnable() && this.syncGuiMove.isState();
        if (guiActive) {
            guiMove.swapBypass = true;
        }
        InventoryUtils.swapAndUseHvH(Items.FIREWORK_ROCKET);
        if (guiActive) {
            guiMove.swapBypass = false;
        }
        return true;
    }

    private boolean canWaitForFlight() {
        return this.currentChest.isOf(Items.ELYTRA) || ElytraSwap.mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private boolean hasFirework() {
        return this.findItemSlot(Items.FIREWORK_ROCKET) >= 0;
    }

    private void tryTakeoff(ItemStack chest) {
        if (ElytraSwap.mc.player.isTouchingWater() || ElytraSwap.mc.player.isInLava()) {
            return;
        }
        if (ElytraSwap.mc.player.isOnGround()) {
            ElytraSwap.mc.player.jump();
        } else if (this.isElytraUsable(chest) && !ElytraSwap.mc.player.isGliding() && !ElytraSwap.mc.player.getAbilities().flying) {
            ElytraSwap.mc.player.startGliding();
            ElytraSwap.mc.player.networkHandler.sendPacket((Packet)new ClientCommandC2SPacket((Entity)ElytraSwap.mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
    }

    private boolean isElytraUsable(ItemStack stack) {
        return stack.getDamage() < stack.getMaxDamage() - 1;
    }

    private int findItemSlot(Item item) {
        for (int i2 = 0; i2 < 36; ++i2) {
            ItemStack stack = ElytraSwap.mc.player.getInventory().getStack(i2);
            if (!stack.isOf(item)) continue;
            return i2 < 9 ? i2 + 36 : i2;
        }
        return -1;
    }

    private int findChestplateSlot() {
        for (int i2 = 0; i2 < 36; ++i2) {
            ItemStack stack = ElytraSwap.mc.player.getInventory().getStack(i2);
            Item item = stack.getItem();
            if (item != Items.LEATHER_CHESTPLATE && item != Items.CHAINMAIL_CHESTPLATE && item != Items.IRON_CHESTPLATE && item != Items.GOLDEN_CHESTPLATE && item != Items.DIAMOND_CHESTPLATE && item != Items.NETHERITE_CHESTPLATE) continue;
            return i2 < 9 ? i2 + 36 : i2;
        }
        return -1;
    }

    @Override
    public void onDisable() {
        this.pendingFireworkUntil = 0L;
        this.swapQueued = false;
        this.fireworkQueued = false;
        super.onDisable();
    }
}

