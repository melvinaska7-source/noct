package polar.ru.api.utils.player;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;
import polar.ru.api.QClient;
import polar.ru.api.utils.player.SlotSearchResult;

public final class HotbarUtil
implements QClient {
    private static int cachedSlot = -1;

    public static int getItemCount(Item item) {
        if (HotbarUtil.mc.player == null) {
            return 0;
        }
        int counter = 0;
        for (int i2 = 0; i2 < HotbarUtil.mc.player.getInventory().size(); ++i2) {
            ItemStack stack = HotbarUtil.mc.player.getInventory().getStack(i2);
            if (!stack.isOf(item)) continue;
            counter += stack.getCount();
        }
        return counter;
    }

    public static SlotSearchResult getAxe() {
        return HotbarUtil.findBest(itemStack -> itemStack.getItem() instanceof AxeItem, false);
    }

    public static SlotSearchResult getAxeHotBar() {
        return HotbarUtil.findBest(itemStack -> itemStack.getItem() instanceof AxeItem, true);
    }

    public static SlotSearchResult getPickAxe() {
        return HotbarUtil.findBest(itemStack -> itemStack.getItem() instanceof PickaxeItem, false);
    }

    public static SlotSearchResult getPickAxeHotbar() {
        return HotbarUtil.getPickAxeHotBar();
    }

    public static SlotSearchResult getPickAxeHotBar() {
        return HotbarUtil.findBest(itemStack -> itemStack.getItem() instanceof PickaxeItem, true);
    }

    public static SlotSearchResult getSword() {
        return HotbarUtil.findBest(itemStack -> itemStack.getItem() instanceof SwordItem, false);
    }

    public static SlotSearchResult getSwordHotBar() {
        return HotbarUtil.findBest(itemStack -> itemStack.getItem() instanceof SwordItem, true);
    }

    public static SlotSearchResult getSkull() {
        return HotbarUtil.findInHotBar(stack -> stack.isOf(Items.SKELETON_SKULL) || stack.isOf(Items.WITHER_SKELETON_SKULL) || stack.isOf(Items.CREEPER_HEAD) || stack.isOf(Items.PLAYER_HEAD) || stack.isOf(Items.ZOMBIE_HEAD));
    }

    public static int getElytra() {
        if (HotbarUtil.mc.player == null) {
            return -1;
        }
        for (ItemStack stack : HotbarUtil.mc.player.getInventory().armor) {
            if (!stack.isOf(Items.ELYTRA) || stack.getDamage() >= stack.getMaxDamage() - 1) continue;
            return -2;
        }
        for (int i2 = 0; i2 < 36; ++i2) {
            ItemStack stack;
            stack = HotbarUtil.mc.player.getInventory().getStack(i2);
            if (!stack.isOf(Items.ELYTRA) || stack.getDamage() >= stack.getMaxDamage() - 1) continue;
            return i2 < 9 ? i2 + 36 : i2;
        }
        return -1;
    }

    public static SlotSearchResult findInHotBar(Searcher searcher) {
        if (HotbarUtil.mc.player != null) {
            if (searcher.isValid(HotbarUtil.mc.player.getOffHandStack())) {
                return SlotSearchResult.inOffhand(HotbarUtil.mc.player.getOffHandStack());
            }
            for (int i2 = 0; i2 < 9; ++i2) {
                ItemStack stack = HotbarUtil.mc.player.getInventory().getStack(i2);
                if (!searcher.isValid(stack)) continue;
                return new SlotSearchResult(i2, true, stack);
            }
        }
        return SlotSearchResult.notFound();
    }

    public static SlotSearchResult findItemInHotBar(List<Item> items) {
        return HotbarUtil.findInHotBar(stack -> items.contains(stack.getItem()));
    }

    public static SlotSearchResult findItemInHotBar(Item ... items) {
        return HotbarUtil.findItemInHotBar(Arrays.asList(items));
    }

    public static SlotSearchResult findInInventory(Searcher searcher) {
        if (HotbarUtil.mc.player != null) {
            for (int i2 = 35; i2 >= 0; --i2) {
                ItemStack stack = HotbarUtil.mc.player.getInventory().getStack(i2);
                if (!searcher.isValid(stack)) continue;
                return new SlotSearchResult(i2, true, stack);
            }
        }
        return SlotSearchResult.notFound();
    }

    public static SlotSearchResult findItemInInventory(List<Item> items) {
        return HotbarUtil.findInInventory(stack -> items.contains(stack.getItem()));
    }

    public static SlotSearchResult findItemInInventory(Item ... items) {
        return HotbarUtil.findItemInInventory(Arrays.asList(items));
    }

    public static SlotSearchResult findBlockInHotBar(@NotNull List<Block> blocks) {
        return HotbarUtil.findItemInHotBar(blocks.stream().map(Block::asItem).toList());
    }

    public static SlotSearchResult findBlockInHotBar(Block ... blocks) {
        return HotbarUtil.findItemInHotBar(Arrays.stream(blocks).map(Block::asItem).toList());
    }

    public static SlotSearchResult findBlockInInventory(@NotNull List<Block> blocks) {
        return HotbarUtil.findItemInInventory(blocks.stream().map(Block::asItem).toList());
    }

    public static SlotSearchResult findBlockInInventory(Block ... blocks) {
        return HotbarUtil.findItemInInventory(Arrays.stream(blocks).map(Block::asItem).toList());
    }

    public static void saveSlot() {
        if (HotbarUtil.mc.player != null) {
            cachedSlot = HotbarUtil.mc.player.getInventory().selectedSlot;
        }
    }

    public static void returnSlot() {
        if (cachedSlot != -1) {
            HotbarUtil.switchTo(cachedSlot);
        }
        cachedSlot = -1;
    }

    public static void saveAndSwitchTo(int slot) {
        HotbarUtil.saveSlot();
        HotbarUtil.switchTo(slot);
    }

    public static void switchTo(int slot) {
        if (HotbarUtil.mc.player == null || mc.getNetworkHandler() == null || slot < 0 || slot > 8) {
            return;
        }
        if (HotbarUtil.mc.player.getInventory().selectedSlot == slot) {
            return;
        }
        HotbarUtil.mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(slot));
    }

    public static void switchToSilent(int slot) {
        if (HotbarUtil.mc.player == null || mc.getNetworkHandler() == null || slot < 0 || slot > 8) {
            return;
        }
        mc.getNetworkHandler().sendPacket((Packet)new UpdateSelectedSlotC2SPacket(slot));
    }

    public static SlotSearchResult getAntiWeaknessItem() {
        if (HotbarUtil.mc.player == null) {
            return SlotSearchResult.notFound();
        }
        Item mainHand = HotbarUtil.mc.player.getMainHandStack().getItem();
        if (mainHand instanceof SwordItem || mainHand instanceof PickaxeItem || mainHand instanceof AxeItem || mainHand instanceof ShovelItem) {
            return new SlotSearchResult(HotbarUtil.mc.player.getInventory().selectedSlot, true, HotbarUtil.mc.player.getMainHandStack());
        }
        return HotbarUtil.findInHotBar(stack -> stack.getItem() instanceof SwordItem || stack.getItem() instanceof PickaxeItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof ShovelItem);
    }

    public static float getHitDamage(@NotNull ItemStack weapon, PlayerEntity entity) {
        if (HotbarUtil.mc.player == null || HotbarUtil.mc.world == null) {
            return 0.0f;
        }
        float baseDamage = HotbarUtil.getBaseAttackDamage(weapon);
        if (HotbarUtil.mc.player.fallDistance > 0.0f) {
            baseDamage += baseDamage / 2.0f;
        }
        if (HotbarUtil.mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            int strength = Objects.requireNonNull(HotbarUtil.mc.player.getStatusEffect(StatusEffects.STRENGTH)).getAmplifier() + 1;
            baseDamage += 3.0f * (float)strength;
        }
        return DamageUtil.getDamageLeft((LivingEntity)entity, (float)baseDamage, (DamageSource)HotbarUtil.mc.world.getDamageSources().generic(), (float)entity.getArmor(), (float)((float)entity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS)));
    }

    public static SlotSearchResult findBedInHotBar() {
        return HotbarUtil.findInHotBar(stack -> stack.getItem() instanceof BedItem);
    }

    public static SlotSearchResult findBed() {
        return HotbarUtil.findInInventory(stack -> stack.getItem() instanceof BedItem);
    }

    public static Item getItem(String name) {
        if (name == null) {
            return Items.AIR;
        }
        String normalized = name.toLowerCase();
        for (Block block : Registries.BLOCK) {
            if (!block.getTranslationKey().replace("block.minecraft.", "").equals(normalized)) continue;
            return Item.fromBlock((Block)block);
        }
        for (Item item : Registries.ITEM) {
            if (!item.getTranslationKey().replace("item.minecraft.", "").equals(normalized)) continue;
            return item;
        }
        return Items.DIRT;
    }

    public static int getBedsCount() {
        if (HotbarUtil.mc.player == null) {
            return 0;
        }
        int counter = 0;
        for (int i2 = 0; i2 < HotbarUtil.mc.player.getInventory().size(); ++i2) {
            ItemStack stack = HotbarUtil.mc.player.getInventory().getStack(i2);
            if (!(stack.getItem() instanceof BedItem)) continue;
            counter += stack.getCount();
        }
        return counter;
    }

    private static SlotSearchResult findBest(Searcher searcher, boolean hotbarOnly) {
        if (HotbarUtil.mc.player == null) {
            return SlotSearchResult.notFound();
        }
        int bestSlot = -1;
        float bestDamage = 0.0f;
        int end = hotbarOnly ? 8 : 35;
        for (int i2 = 0; i2 <= end; ++i2) {
            float damage;
            ItemStack stack = HotbarUtil.mc.player.getInventory().getStack(i2);
            if (!searcher.isValid(stack) || !((damage = HotbarUtil.getBaseAttackDamage(stack)) > bestDamage)) continue;
            bestDamage = damage;
            bestSlot = i2;
        }
        return bestSlot == -1 ? SlotSearchResult.notFound() : new SlotSearchResult(bestSlot, true, HotbarUtil.mc.player.getInventory().getStack(bestSlot));
    }

    private static float getBaseAttackDamage(ItemStack stack) {
        AttributeModifiersComponent component = (AttributeModifiersComponent)stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, (Object)AttributeModifiersComponent.DEFAULT);
        double damage = 1.0;
        for (AttributeModifiersComponent.Entry entry : component.modifiers()) {
            if (!entry.attribute().equals((Object)EntityAttributes.ATTACK_DAMAGE)) continue;
            damage += entry.modifier().value();
        }
        return (float)damage;
    }

    public static boolean isHolding(Item item) {
        return HotbarUtil.mc.player != null && (HotbarUtil.mc.player.getMainHandStack().isOf(item) || HotbarUtil.mc.player.getOffHandStack().isOf(item));
    }

    public static Hand getHand(Item item) {
        if (HotbarUtil.mc.player == null) {
            return null;
        }
        if (HotbarUtil.mc.player.getOffHandStack().isOf(item)) {
            return Hand.OFF_HAND;
        }
        if (HotbarUtil.mc.player.getMainHandStack().isOf(item)) {
            return Hand.MAIN_HAND;
        }
        return null;
    }
    private HotbarUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static interface Searcher {
        public boolean isValid(ItemStack var1);
    }
}

