package zov.alphadlc.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.util.base.Instance;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ModuleInformation(moduleName = "Anti Bot", moduleDesc = "Удаляет всех ботов из мира", moduleCategory = ModuleCategory.COMBAT)
public class AntiBot extends Module {

    private final ModeSetting mode = new ModeSetting("Обход", "ReallyWorld", "ReallyWorld", "Matrix", "LonyGrief");

    public static final List<Entity> bot = new ArrayList<>();

    @Subscribe
    public void onUpdate(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player) || player.equals(mc.player)) continue;
            boolean isBot = false;

            if (mode.is("ReallyWorld")) {
                boolean hasValidArmor = player.getInventory().armor.stream().allMatch(armorItem ->
                        armorItem.getItem() != Items.AIR && armorItem.isEnchantable() && !armorItem.isDamaged());

                boolean hasValidEquipment = player.getOffHandStack().getItem() == Items.AIR &&
                        (player.getInventory().armor.stream().anyMatch(armorItem ->
                                armorItem.getItem() == Items.LEATHER_BOOTS ||
                                armorItem.getItem() == Items.LEATHER_LEGGINGS ||
                                armorItem.getItem() == Items.LEATHER_CHESTPLATE ||
                                armorItem.getItem() == Items.LEATHER_HELMET ||
                                armorItem.getItem() == Items.IRON_BOOTS ||
                                armorItem.getItem() == Items.IRON_LEGGINGS ||
                                armorItem.getItem() == Items.IRON_CHESTPLATE ||
                                armorItem.getItem() == Items.IRON_HELMET));

                boolean hasFullFood = player.getHungerManager().getFoodLevel() == 20;

                isBot = hasValidArmor && hasValidEquipment && hasFullFood;

            } else if (mode.is("Matrix")) {
                UUID offlineUuid = UUID.nameUUIDFromBytes(
                        ("OfflinePlayer:" + player.getName().getString()).getBytes(StandardCharsets.UTF_8));
                isBot = player.isAlive() && !bot.contains(player) &&
                        !player.getUuid().equals(offlineUuid);

            } else if (mode.is("LonyGrief")) {
                String playerName = player.getDisplayName().getString().trim();
                String[] nameParts = playerName.split("\\s+");
                boolean isBotCandidate = (nameParts.length == 1) && !playerName.contains("?");

                isBot = isBotCandidate;
            }

            if (isBot) {
                if (!bot.contains(player)) {
                    bot.add(player);
                }
            } else {
                bot.remove(player);
            }
        }
    }

    @Override
    public void onDisable() {
        bot.clear();
        super.onDisable();
    }

    public static boolean isBot(Entity entity) {
        AntiBot instance = Instance.get(AntiBot.class);
        return instance != null && instance.isEnabled() && bot.contains(entity);
    }
}