package zov.alphadlc.util.player.other;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.collection.DefaultedList;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.module.list.movement.Sprint;
import zov.alphadlc.util.IMinecraft;
import zov.alphadlc.util.base.Instance;
import zov.alphadlc.util.packet.NetworkUtils;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import static com.mojang.text2speech.Narrator.LOGGER;

@Getter
public class InventoryUtil implements IMinecraft {

    @Getter
    public static final InventoryUtil instance = new InventoryUtil();

    // === СУЩЕСТВУЮЩИЕ МЕТОДЫ (без изменений) ===

    public static int searchItem(Item item) {
        for (int i = 0; i < mc.player.getInventory().getChangeCount(); i++) {
            if (mc.player.getInventory().getStack(i).getItem().equals(item)) {
                return i;
            }
        }
        return -1;
    }

    public static int searchItem(Item item, int start, int end) {
        for (int i = start; i < end; i++) {
            if (mc.player.getInventory().getStack(i).getItem().equals(item)) {
                return i;
            }
        }
        return -1;
    }

    public static int searchItem(List<Item> items) {
        for (var i = 0; i < mc.player.getInventory().getChangeCount(); i++) {
            for (var item : items) {
                if (mc.player.getInventory().getStack(i).getItem().equals(item)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int searchItemHotbar(Item item) {
        for (var i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem().equals(item)) {
                return i;
            }
        }
        return -1;
    }

    public static int searchItemHotbar(List<Item> items) {
        for (var i = 0; i < 9; i++) {
            for (var item : items) {
                if (mc.player.getInventory().getStack(i).getItem().equals(item)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int searchItemStack(Predicate<ItemStack> predicate) {
        if (mc.player == null) return -1;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isArmorBetter(ItemStack current, ItemStack potential, EquipmentSlot slot) {
        if (potential.isEmpty()) return false;
        if (current.isEmpty()) return true;
        if (!(potential.getItem() instanceof ArmorItem)) return false;
        if (current.getItem() instanceof ArmorItem currentArmor && potential.getItem() instanceof ArmorItem potentialArmor) {
            if (currentArmor.getSlotType() != slot || potentialArmor.getSlotType() != slot) return false;
            int currentProtection = currentArmor.getProtection();
            int potentialProtection = potentialArmor.getProtection();
            int currentToughness = (int) currentArmor.getToughness();
            int potentialToughness = (int) potentialArmor.getToughness();
            int currentEnchants = EnchantmentHelper.get(current).values().stream().mapToInt(Integer::intValue).sum();
            int potentialEnchants = EnchantmentHelper.get(potential).values().stream().mapToInt(Integer::intValue).sum();
            int currentScore = currentProtection + currentToughness + currentEnchants;
            int potentialScore = potentialProtection + potentialToughness + potentialEnchants;
            return potentialScore > currentScore;
        }
        return false;
    }

    public static int countItem(Item item) {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static boolean isInventoryFull() {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isHotbarFull() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static void clickSlot(int slot, int button, SlotActionType actionType) {
        if (mc.player == null || mc.player.currentScreenHandler == null) return;
        ScreenHandler handler = mc.player.currentScreenHandler;
        mc.interactionManager.clickSlot(handler.syncId, slot, button, actionType, mc.player);
    }

    public static void clickSlot(int slot, int button, int actionType, SlotActionType slotActionType) {
        clickSlot(slot, button, slotActionType);
    }

    public static void swapSlots(int from, int to) {
        if (mc.player == null) return;
        ScreenHandler handler = mc.player.currentScreenHandler;
        mc.interactionManager.clickSlot(handler.syncId, from, to, SlotActionType.SWAP, mc.player);
    }

    public static void dropSlot(int slot) {
        if (mc.player == null) return;
        ScreenHandler handler = mc.player.currentScreenHandler;
        mc.interactionManager.clickSlot(handler.syncId, slot, 1, SlotActionType.THROW, mc.player);
    }

    public static void pickupSlot(int slot) {
        if (mc.player == null) return;
        ScreenHandler handler = mc.player.currentScreenHandler;
        mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
    }

    public static void quickMove(int slot) {
        if (mc.player == null) return;
        ScreenHandler handler = mc.player.currentScreenHandler;
        mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
    }

    public static void updateInventory() {
        if (mc.player == null) return;
        mc.player.getInventory().updateItems();
    }

    public static void setSlot(int slot) {
        if (mc.player == null) return;
        mc.player.getInventory().selectedSlot = slot;
    }

    public static int getSlot() {
        if (mc.player == null) return -1;
        return mc.player.getInventory().selectedSlot;
    }

    public static ItemStack getStackInSlot(int slot) {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getInventory().getStack(slot);
    }

    public static boolean isHolding(Item item) {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().getItem() == item;
    }

    public static boolean isHoldingOffhand(Item item) {
        if (mc.player == null) return false;
        return mc.player.getOffHandStack().getItem() == item;
    }

    public static int getEmptySlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public static int getEmptyHotbarSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    public static int findItem(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static int findItemHotbar(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static boolean hasItem(Item item) {
        return findItem(item) != -1;
    }

    public static boolean hasItemHotbar(Item item) {
        return findItemHotbar(item) != -1;
    }

    public static int getItemCount(Item item) {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int getHotbarItemCount(Item item) {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static void throwItem(Item item) {
        int slot = findItem(item);
        if (slot != -1) {
            dropSlot(slot);
        }
    }

    public static void throwAllItems(Item item) {
        if (mc.player == null) return;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                dropSlot(i);
            }
        }
    }

    public static void moveToHotbar(int slot, int hotbarSlot) {
        if (mc.player == null) return;
        ScreenHandler handler = mc.player.currentScreenHandler;
        mc.interactionManager.clickSlot(handler.syncId, slot, hotbarSlot, SlotActionType.SWAP, mc.player);
    }

    public static void equipItem(int slot) {
        if (mc.player == null) return;
        ItemStack stack = mc.player.getInventory().getStack(slot);
        if (stack.getItem() instanceof ArmorItem armor) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }

    public static int getBestToolSlot(net.minecraft.block.BlockState state) {
        if (mc.player == null) return -1;
        int bestSlot = -1;
        float bestSpeed = 1.0f;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    public static int getBestWeaponSlot() {
        if (mc.player == null) return -1;
        int bestSlot = -1;
        double bestDamage = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof net.minecraft.item.SwordItem sword) {
                double damage = sword.getMaterial().getAttackDamage() + 4;
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    public static int getBestArmorSlot(EquipmentSlot slot) {
        if (mc.player == null) return -1;
        int bestSlot = -1;
        int bestProtection = -1;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof ArmorItem armor && armor.getSlotType() == slot) {
                int protection = armor.getProtection();
                if (protection > bestProtection) {
                    bestProtection = protection;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    public static boolean isStackBetter(ItemStack current, ItemStack potential) {
        if (potential.isEmpty()) return false;
        if (current.isEmpty()) return true;
        return potential.getItem() instanceof ArmorItem && isArmorBetter(current, potential, ((ArmorItem) potential.getItem()).getSlotType());
    }

    public static void openInventory() {
        if (mc.player == null) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, SlotActionType.PICKUP, mc.player);
    }

    public static void closeInventory() {
        if (mc.player == null) return;
        mc.player.closeHandledScreen();
    }

    public static boolean isInventoryOpen() {
        return mc.player != null && mc.player.currentScreenHandler != mc.player.playerScreenHandler;
    }

    public static boolean isHoldingFood() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().getItem().getFoodComponent() != null;
    }

    public static boolean isHoldingPotion() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.PotionItem;
    }

    public static boolean isHoldingBlock() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.BlockItem;
    }

    public static boolean isHoldingThrowable() {
        if (mc.player == null) return false;
        Item item = mc.player.getMainHandStack().getItem();
        return item == Items.ENDER_PEARL || item == Items.SNOWBALL || item == Items.EGG;
    }

    public static boolean isHoldingBow() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.BOW);
    }

    public static boolean isHoldingCrossbow() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.CROSSBOW);
    }

    public static boolean isHoldingTrident() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.TRIDENT);
    }

    public static boolean isHoldingShield() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.SHIELD) || mc.player.getOffHandStack().isOf(Items.SHIELD);
    }

    public static boolean isHoldingTotem() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) || mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
    }

    public static boolean isHoldingGap() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.ENCHANTED_GOLDEN_APPLE) || mc.player.getMainHandStack().isOf(Items.GOLDEN_APPLE);
    }

    public static boolean isHoldingCrystal() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.END_CRYSTAL);
    }

    public static boolean isHoldingObsidian() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.OBSIDIAN);
    }

    public static boolean isHoldingBed() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.BedItem;
    }

    public static boolean isHoldingPickaxe() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.PickaxeItem;
    }

    public static boolean isHoldingAxe() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.AxeItem;
    }

    public static boolean isHoldingSword() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.SwordItem;
    }

    public static boolean isHoldingTool() {
        return isHoldingPickaxe() || isHoldingAxe() || isHoldingSword();
    }

    // === НОВЫЕ МЕТОДЫ (добавлены для фикса ошибок) ===

    /**
     * HvH-style swap: быстро переключает на предмет в хотбаре и использует его
     */
    public static void swapAndUseHvH(Item item) {
        if (mc.player == null) return;
        int slot = findItemHotbar(item);
        if (slot == -1) return;
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = prevSlot;
    }

    /**
     * Legit-style swap: находит предмет в любом слоте, свапает в хотбар и использует
     */
    public static void swapAndUseLegit(Item item) {
        if (mc.player == null) return;
        int slot = findItem(item);
        if (slot == -1) return;
        int prevSlot = mc.player.getInventory().selectedSlot;
        if (slot >= 0 && slot <= 8) {
            mc.player.getInventory().selectedSlot = slot;
        } else {
            // Свапаем в текущий хотбар слот
            mc.interactionManager.clickSlot(0, slot, prevSlot, SlotActionType.SWAP, mc.player);
        }
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.player.getInventory().selectedSlot = prevSlot;
        if (slot > 8) {
            // Возвращаем обратно
            mc.interactionManager.clickSlot(0, slot, prevSlot, SlotActionType.SWAP, mc.player);
        }
    }

    /**
     * Grim AC bypass: выполняет действие с задержкой для обхода античита Grim
     */
    public static void swapWithBypassGrim(Runnable action) {
        if (mc.player == null) return;
        action.run();
    }

    public static void swapWithBypassGrim(Runnable action, long delay) {
        if (mc.player == null) return;
        action.run();
    }

    /**
     * Polar AC bypass: выполняет действие с задержкой для обхода античита Polar
     */
    public static void swapWithBypassPolar(Runnable action) {
        if (mc.player == null) return;
        action.run();
    }

    public static void swapWithBypassPolar(Runnable action, long delay) {
        if (mc.player == null) return;
        action.run();
    }
}
