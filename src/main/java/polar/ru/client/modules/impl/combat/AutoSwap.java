package polar.ru.client.modules.impl.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventBinding;
import polar.ru.api.events.implement.EventMoveInput;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.movement.Sprint;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class AutoSwap
extends Module {
    public static AutoSwap INSTANCE = new AutoSwap();
    private final ModeSetting firstItem = new ModeSetting("Первый предмет", "Руна", "Руна", "Тотем", "Шар", "Гепл", "Щит");
    private final ModeSetting secondItem = new ModeSetting("Второй предмет", "Тотем", "Руна", "Тотем", "Шар", "Гепл", "Щит");
    private final BindSetting swapKey = new BindSetting("Кнопка свапа", -98);
    private final BooleanSetting ignoreRegularTotems = new BooleanSetting("Игнорировать обычные тотемы", false);
    private final BooleanSetting bypassgrim = new BooleanSetting("Обходить Grim", true);
    private int bypassTicks;
    private boolean sprintPaused;
    private int swapCooldown;
    private int targetSlot = -1;
    private boolean needSwap = false;

    public AutoSwap() {
        super("AutoSwap", "Быстрая смена предметов в офф-хенде", Module.ModuleCategory.COMBAT);
        this.addSettings(this.firstItem, this.secondItem, this.swapKey, this.ignoreRegularTotems, this.bypassgrim);
    }

    @Override
    public void onEnable() {
        this.needSwap = false;
        this.targetSlot = -1;
        this.bypassTicks = 0;
        this.swapCooldown = 0;
        super.onEnable();
    }

    @EventLink
    public void onBinding(EventBinding event) {
        if (AutoSwap.mc.currentScreen != null) {
            return;
        }
        if (AutoSwap.mc.player == null || AutoSwap.mc.world == null) {
            return;
        }
        if (event.getKey() == this.swapKey.getKey() && this.swapCooldown == 0) {
            this.needSwap = true;
        }
    }

    @EventLink
    public void onInput(EventMoveInput e2) {
        if (this.bypassgrim.isState() && this.bypassTicks > 0) {
            if (AutoSwap.mc.player == null) {
                return;
            }
            AutoSwap.mc.player.setSprinting(false);
            e2.setForward(0.0f);
            e2.setStrafe(0.0f);
            e2.setJump(false);
            e2.setSneak(false);
        }
    }

    @EventLink
        public void onUpdate(EventUpdate e2) {
        if (AutoSwap.mc.player == null || AutoSwap.mc.world == null) {
            return;
        }
        if (this.swapCooldown > 0) {
            --this.swapCooldown;
        }
        if (this.bypassgrim.isState() && this.bypassTicks > 0) {
            AutoSwap.mc.player.setSprinting(false);
            --this.bypassTicks;
            if (this.bypassTicks == 1) {
                this.performSwap();
            }
            if (this.bypassTicks == 0) {
                this.restoreSprint();
            }
            return;
        }
        if (this.needSwap && this.targetSlot == -1) {
            this.needSwap = false;
            Item offhand = AutoSwap.mc.player.getOffHandStack().getItem();
            Item first = this.getItem(this.firstItem.getCurrent());
            Item second = this.getItem(this.secondItem.getCurrent());
            int firstSlot = this.findItemSlot(first);
            int secondSlot = this.findItemSlot(second);
            if (firstSlot == -1 && secondSlot == -1) {
                return;
            }
            int slot = offhand == first && secondSlot != -1 ? secondSlot : (firstSlot != -1 ? firstSlot : secondSlot);
            if (slot == -1) {
                return;
            }
            this.targetSlot = slot;
            if (this.bypassgrim.isState()) {
                this.disableSprint();
                this.bypassTicks = 2;
                this.swapCooldown = 2;
            } else {
                this.performSwap();
                this.swapCooldown = 2;
            }
        }
    }

    private void performSwap() {
        if (this.targetSlot == -1) {
            return;
        }
        this.doSwap(this.targetSlot);
        AutoSwap.mc.player.networkHandler.sendPacket((Packet)new CloseHandledScreenC2SPacket(0));
        this.targetSlot = -1;
    }

    private void doSwap(int slot) {
        if (slot >= 36 && slot <= 44) {
            int hotbarSlot = slot - 36;
            AutoSwap.mc.interactionManager.clickSlot(0, 45, hotbarSlot, SlotActionType.SWAP, (PlayerEntity)AutoSwap.mc.player);
        } else {
            AutoSwap.mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, (PlayerEntity)AutoSwap.mc.player);
            AutoSwap.mc.interactionManager.clickSlot(0, 45, 0, SlotActionType.SWAP, (PlayerEntity)AutoSwap.mc.player);
            AutoSwap.mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, (PlayerEntity)AutoSwap.mc.player);
        }
    }

    private int findItemSlot(Item item) {
        boolean shouldIgnoreRegular = this.ignoreRegularTotems.isState() && item == Items.TOTEM_OF_UNDYING && this.firstItem.getCurrent().equals("Тотем") && this.secondItem.getCurrent().equals("Тотем");
        for (int i2 = 9; i2 < 45; ++i2) {
            ItemStack stack = AutoSwap.mc.player.playerScreenHandler.getSlot(i2).getStack();
            if (stack.getItem() != item || shouldIgnoreRegular && !stack.hasEnchantments()) continue;
            return i2;
        }
        return -1;
    }

    private Item getItem(String name) {
        return switch (name) {
            case "Руна" -> Items.FIREWORK_STAR;
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Шар" -> Items.PLAYER_HEAD;
            case "Гепл" -> Items.GOLDEN_APPLE;
            case "Щит" -> Items.SHIELD;
            default -> Items.AIR;
        };
    }

    private void disableSprint() {
        if (this.sprintPaused) {
            return;
        }
        Sprint.pushPause(1000L);
        this.sprintPaused = true;
    }

    private void restoreSprint() {
        if (!this.sprintPaused) {
            return;
        }
        this.sprintPaused = false;
        Sprint.popPause();
    }

    @Override
    public void onDisable() {
        this.bypassTicks = 0;
        this.swapCooldown = 0;
        this.needSwap = false;
        this.targetSlot = -1;
        this.restoreSprint();
        super.onDisable();
    }
}

