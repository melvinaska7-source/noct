package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import zov.alphadlc.event.list.EventKeyInput;
import zov.alphadlc.event.list.EventPlayerUpdate;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BindSetting;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.util.chat.ChatUtil;
import zov.alphadlc.util.packet.NetworkUtils;
import zov.alphadlc.util.player.other.InventoryUtil;

@ModuleInformation(moduleName = "Elytra Helper", moduleDesc = "Помощник для элитр", moduleCategory = ModuleCategory.PLAYER)
public class ElytraHelper extends Module {
    private final ModeSetting mode = new ModeSetting("Мод", "Vanilla", "Vanilla", "Grim", "Polar");
    private final BindSetting swapKey = new BindSetting("Кнопка свапа", -1);
    private final BindSetting fireworkKey = new BindSetting("Кнопка феерверка", -1);
    private final ModeSetting throwFireworkMode = new ModeSetting("Мод пуска феера", "Обычный", "Обычный", "Легитный");
    private final BooleanSetting autoTakeoff = new BooleanSetting("Автовзлёт", true);
    private boolean fireworkUsed;
    private boolean swapped;
    private int lastElytraSlot = -1;

    @Subscribe
    private void onKey(EventKeyInput e2) {
        if (e2.getAction() == 0) {
            return;
        }
        if (e2.getKey() == this.swapKey.getValue().intValue()) {
            this.swapped = true;
        }
        if (e2.getKey() == this.fireworkKey.getValue().intValue() && this.mc.player.isGliding()) {
            this.fireworkUsed = true;
        }
    }

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate e2) {
        if (!this.swapped) {
            return;
        }
        this.swapped = false;
        this.swap(this.mode, this.mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA);
    }

    @Subscribe
    private void onTick(EventTick e2) {
        if (this.mc.player == null) {
            return;
        }
        if (this.autoTakeoff.getValue()) {
            ItemStack chest = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (!(chest.getItem() != Items.ELYTRA || this.mc.player.isInLava() || this.mc.player.isTouchingWater() || !this.mc.player.isOnGround() || this.mc.player.hasVehicle() || this.mc.player.isGliding() || this.mc.player.isSpectator() || this.mc.options.jumpKey.isPressed())) {
                this.mc.player.jump();
            }
            if (!(chest.getItem() != Items.ELYTRA || this.mc.player.isInLava() || this.mc.player.isTouchingWater() || this.mc.player.isOnGround() || this.mc.player.hasVehicle() || this.mc.player.isGliding() || this.mc.player.isSpectator())) {
                NetworkUtils.sendSilentPacket(new ClientCommandC2SPacket((Entity)this.mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                this.mc.player.startGliding();
            }
        }
        if (!this.fireworkUsed) {
            return;
        }
        switch (this.throwFireworkMode.getValue()) {
            case "Обычный": {
                InventoryUtil.swapAndUseHvH(Items.FIREWORK_ROCKET);
                break;
            }
            case "Легитный": {
                InventoryUtil.swapAndUseLegit(Items.FIREWORK_ROCKET);
            }
        }
        this.fireworkUsed = false;
    }

    public void swap(ModeSetting mode, boolean chestplate) {
        switch (mode.getValue()) {
            case "Vanilla": {
                this.vanillaSwap(chestplate);
                break;
            }
            case "Grim": {
                this.grimSwap(chestplate);
                break;
            }
            case "Polar": {
                this.polarSwap(chestplate);
            }
        }
    }

    private void vanillaSwap(boolean chestplate) {
        if (chestplate) {
            this.dequipElytraVanilla();
        } else {
            this.equipElytraVanilla();
        }
    }

    private void equipElytraVanilla() {
        int slot = InventoryUtil.findBestElytraSlot();
        if (slot == -1) {
            ChatUtil.send("Элитра" + String.valueOf(Formatting.GRAY) + " не найдена в инвентаре");
            return;
        }
        if (slot >= 0 && slot <= 8) {
            this.lastElytraSlot = slot;
            this.mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
        } else if (slot >= 9 && slot <= 35) {
            this.lastElytraSlot = 8;
            this.mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
            this.mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
            this.mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
        }
    }

    private void dequipElytraVanilla() {
        int chestplateSlot = InventoryUtil.findBestChestplateSlot();
        if (chestplateSlot == -1) {
            ChatUtil.send("Нагрудник" + String.valueOf(Formatting.GRAY) + " не найден в инвентаре");
            return;
        }
        if (this.lastElytraSlot >= 0 && this.lastElytraSlot <= 8) {
            if (chestplateSlot == this.lastElytraSlot) {
                this.mc.interactionManager.clickSlot(0, 6, this.lastElytraSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
            } else {
                this.mc.interactionManager.clickSlot(0, chestplateSlot, this.lastElytraSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                this.mc.interactionManager.clickSlot(0, 6, this.lastElytraSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
            }
        } else if (chestplateSlot >= 0 && chestplateSlot <= 8) {
            this.mc.interactionManager.clickSlot(0, 6, chestplateSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
        } else if (chestplateSlot >= 9 && chestplateSlot <= 35) {
            this.mc.interactionManager.clickSlot(0, chestplateSlot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
            this.mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
            this.mc.interactionManager.clickSlot(0, chestplateSlot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
        }
    }

    private void grimSwap(boolean chestplate) {
        if (chestplate) {
            this.dequipElytraGrim();
        } else {
            this.equipElytraGrim();
        }
    }

    private void equipElytraGrim() {
        int slot = InventoryUtil.findBestElytraSlot();
        if (slot == -1) {
            ChatUtil.send("Элитра" + String.valueOf(Formatting.GRAY) + " не найдена в инвентаре");
            return;
        }
        if (slot >= 0 && slot <= 8) {
            this.lastElytraSlot = slot;
            InventoryUtil.swapWithBypassGrim(() -> this.mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, (PlayerEntity)this.mc.player));
        } else if (slot >= 9 && slot <= 35) {
            this.lastElytraSlot = 8;
            InventoryUtil.swapWithBypassGrim(() -> {
                this.mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                this.mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                this.mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
            });
        }
    }

    private void dequipElytraGrim() {
        int chestplateSlot = InventoryUtil.findBestChestplateSlot();
        if (chestplateSlot == -1) {
            ChatUtil.send("Нагрудник" + String.valueOf(Formatting.GRAY) + " не найден в инвентаре");
            return;
        }
        if (this.lastElytraSlot >= 0 && this.lastElytraSlot <= 8) {
            if (chestplateSlot == this.lastElytraSlot) {
                InventoryUtil.swapWithBypassGrim(() -> this.mc.interactionManager.clickSlot(0, 6, this.lastElytraSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player));
            } else {
                InventoryUtil.swapWithBypassGrim(() -> {
                    this.mc.interactionManager.clickSlot(0, chestplateSlot, this.lastElytraSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                    this.mc.interactionManager.clickSlot(0, 6, this.lastElytraSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                });
            }
        } else if (chestplateSlot >= 0 && chestplateSlot <= 8) {
            InventoryUtil.swapWithBypassGrim(() -> this.mc.interactionManager.clickSlot(0, 6, chestplateSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player));
        } else if (chestplateSlot >= 9 && chestplateSlot <= 35) {
            InventoryUtil.swapWithBypassGrim(() -> {
                this.mc.interactionManager.clickSlot(0, chestplateSlot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                this.mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                this.mc.interactionManager.clickSlot(0, chestplateSlot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
            });
        }
    }

    private void polarSwap(boolean chestplate) {
        if (chestplate) {
            this.dequipElytraPolar();
        } else {
            this.equipElytraPolar();
        }
    }

    private void equipElytraPolar() {
        int slot = InventoryUtil.findBestElytraSlot();
        if (slot == -1) {
            ChatUtil.send("Элитра" + String.valueOf(Formatting.GRAY) + " не найдена в инвентаре");
            return;
        }
        if (slot >= 0 && slot <= 8) {
            this.lastElytraSlot = slot;
            InventoryUtil.swapWithBypassPolar(() -> this.mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, (PlayerEntity)this.mc.player));
        } else if (slot >= 9 && slot <= 35) {
            this.lastElytraSlot = 8;
            InventoryUtil.swapWithBypassPolar(() -> {
                this.mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                this.mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                this.mc.interactionManager.clickSlot(0, slot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
            });
        }
    }

    private void dequipElytraPolar() {
        int chestplateSlot = InventoryUtil.findBestChestplateSlot();
        if (chestplateSlot == -1) {
            ChatUtil.send("Нагрудник" + String.valueOf(Formatting.GRAY) + " не найден в инвентаре");
            return;
        }
        if (this.lastElytraSlot >= 0 && this.lastElytraSlot <= 8) {
            if (chestplateSlot == this.lastElytraSlot) {
                InventoryUtil.swapWithBypassPolar(() -> this.mc.interactionManager.clickSlot(0, 6, this.lastElytraSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player));
            } else {
                InventoryUtil.swapWithBypassPolar(() -> {
                    this.mc.interactionManager.clickSlot(0, chestplateSlot, this.lastElytraSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                    this.mc.interactionManager.clickSlot(0, 6, this.lastElytraSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                });
            }
        } else if (chestplateSlot >= 0 && chestplateSlot <= 8) {
            InventoryUtil.swapWithBypassPolar(() -> this.mc.interactionManager.clickSlot(0, 6, chestplateSlot, SlotActionType.SWAP, (PlayerEntity)this.mc.player));
        } else if (chestplateSlot >= 9 && chestplateSlot <= 35) {
            InventoryUtil.swapWithBypassPolar(() -> {
                this.mc.interactionManager.clickSlot(0, chestplateSlot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                this.mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
                this.mc.interactionManager.clickSlot(0, chestplateSlot, 8, SlotActionType.SWAP, (PlayerEntity)this.mc.player);
            });
        }
    }
}
