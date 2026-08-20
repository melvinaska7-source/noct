package polar.ru.client.modules.impl.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;

public class AntiBot
extends Module {
    public static AntiBot INSTANCE = new AntiBot();
    public static final List<Entity> isBot = new ArrayList<Entity>();

    public AntiBot() {
        super("AntiBot", "Определяет ботов на сервере", Module.ModuleCategory.COMBAT);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        this.newMatrix();
    }

    public void newMatrix() {
        if (AntiBot.mc.world == null) {
            return;
        }
        for (PlayerEntity player : AntiBot.mc.world.getPlayers()) {
            if (AntiBot.mc.player != player && ((ItemStack)player.getInventory().armor.get(0)).getItem() != Items.AIR && ((ItemStack)player.getInventory().armor.get(1)).getItem() != Items.AIR && ((ItemStack)player.getInventory().armor.get(2)).getItem() != Items.AIR && ((ItemStack)player.getInventory().armor.get(3)).getItem() != Items.AIR && ((ItemStack)player.getInventory().armor.get(0)).isEnchantable() && ((ItemStack)player.getInventory().armor.get(1)).isEnchantable() && ((ItemStack)player.getInventory().armor.get(2)).isEnchantable() && ((ItemStack)player.getInventory().armor.get(3)).isEnchantable() && player.getOffHandStack().getItem() == Items.AIR && (((ItemStack)player.getInventory().armor.get(0)).getItem() == Items.LEATHER_BOOTS || ((ItemStack)player.getInventory().armor.get(1)).getItem() == Items.LEATHER_LEGGINGS || ((ItemStack)player.getInventory().armor.get(2)).getItem() == Items.LEATHER_CHESTPLATE || ((ItemStack)player.getInventory().armor.get(3)).getItem() == Items.LEATHER_HELMET || ((ItemStack)player.getInventory().armor.get(0)).getItem() == Items.IRON_BOOTS || ((ItemStack)player.getInventory().armor.get(1)).getItem() == Items.IRON_LEGGINGS || ((ItemStack)player.getInventory().armor.get(2)).getItem() == Items.IRON_CHESTPLATE || ((ItemStack)player.getInventory().armor.get(3)).getItem() == Items.IRON_HELMET) && player.getMainHandStack().getItem() != Items.AIR && !((ItemStack)player.getInventory().armor.get(0)).isDamaged() && !((ItemStack)player.getInventory().armor.get(1)).isDamaged() && !((ItemStack)player.getInventory().armor.get(2)).isDamaged() && !((ItemStack)player.getInventory().armor.get(3)).isDamaged() && player.getHungerManager().getFoodLevel() == 20) {
                if (!isBot.contains(player)) {
                    isBot.add((Entity)player);
                }
                return;
            }
            isBot.remove(player);
        }
    }

    public static boolean checkBot(LivingEntity entity) {
        return entity instanceof PlayerEntity && isBot.contains(entity);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        isBot.clear();
    }
}

