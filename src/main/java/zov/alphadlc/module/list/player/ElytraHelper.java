package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.chat.ChatUtil;
import zov.alphadlc.util.player.other.InventoryUtil;

@ModuleInformation(
    moduleName = "ElytraHelper",
    moduleDesc = "Авто-элитра и автоматическая экипировка",
    moduleCategory = ModuleCategory.PLAYER,
    moduleKeybind = -1
)
public class ElytraHelper extends Module {

    private final BooleanSetting autoEquip = new BooleanSetting("Авто-экипировка", true);
    private final BooleanSetting autoFirework = new BooleanSetting("Авто-фейерверк", true);
    private final SliderSetting minHeight = new SliderSetting("Мин. высота", 50, 10, 200, 5);
    private final SliderSetting fireworkDelay = new SliderSetting("Задержка фейерверка", 20, 5, 100, 1);

    private int tickCounter = 0;
    private boolean wasFlying = false;

    @Override
    public void onEnable() {
        tickCounter = 0;
        wasFlying = false;
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        // 1.21.4: isFallFlying() → isGliding()
        if (autoEquip.getValue() && !mc.player.isGliding() && shouldEquipElytra()) {
            equipElytra();
        }

        if (autoFirework.getValue() && mc.player.isGliding()) {
            tickCounter++;
            if (tickCounter >= fireworkDelay.getValue()) {
                useFirework();
                tickCounter = 0;
            }
        }

        wasFlying = mc.player.isGliding();
    }

    private boolean shouldEquipElytra() {
        return mc.player.getY() > minHeight.getValue()
            && !mc.player.isOnGround()
            && mc.player.getVelocity().y < -0.5;
    }

    private void equipElytra() {
        int elytraSlot = InventoryUtil.findItem(Items.ELYTRA);
        if (elytraSlot != -1) {
            InventoryUtil.equipItem(elytraSlot);
        } else if (autoEquip.getValue()) {
            ChatUtil.send("Элитра" + String.valueOf(Formatting.GRAY) + " не найдена в инвентаре");
        }
    }

    private void useFirework() {
        int fireworkSlot = InventoryUtil.findItemHotbar(Items.FIREWORK_ROCKET);
        if (fireworkSlot != -1) {
            int prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = fireworkSlot;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.getInventory().selectedSlot = prevSlot;
        } else if (autoFirework.getValue()) {
            ChatUtil.send("Фейерверк" + String.valueOf(Formatting.GRAY) + " не найден в хотбаре");
        }
    }

    private void equipChestplate() {
        int chestplateSlot = findChestplate();
        if (chestplateSlot != -1) {
            InventoryUtil.equipItem(chestplateSlot);
        } else {
            ChatUtil.send("Нагрудник" + String.valueOf(Formatting.GRAY) + " не найден в инвентаре");
        }
    }

    private int findChestplate() {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof ArmorItem armor) {
                // 1.21.4: getSlotType() → getEquipmentSlot()
                if (armor.getEquipmentSlot() == EquipmentSlot.CHEST) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Метод для AirStuck — свапает элитру/нагрудник
     * @param mode режим (не используется в текущей реализации)
     * @param equipElytra true — надеть элитру, false — нагрудник
     */
    public void swap(ModeSetting mode, boolean equipElytra) {
        if (equipElytra) {
            equipElytra();
        } else {
            equipChestplate();
        }
    }
}
