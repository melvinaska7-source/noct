package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.event.list.EventPlayerUpdate;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.util.packet.NetworkUtils;
import zov.alphadlc.util.player.other.InventoryUtil;

@ModuleInformation(moduleName = "RW Helper", moduleDesc = "Помощник для ReallyWorld сервера", moduleCategory = ModuleCategory.MISC)
public class RWHelper extends Module {

    public final BooleanSetting antipolet = new BooleanSetting("Анти-полет обход",false);

    boolean need;
    public boolean fireworkUse;

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof GameMessageS2CPacket p) {
            if (p.content().getString().contains("Анти Полет » Вы не можете взлететь!")) {
                need = true;
            }
        }
    }

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate e) {
        if (!antipolet.getValue()) return;

        if (need) {
            if (!mc.player.isOnGround() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                NetworkUtils.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                mc.player.startGliding();
                if (fireworkUse) {
                    InventoryUtil.swapAndUseHvH(Items.FIREWORK_ROCKET);
                    fireworkUse = false;
                }
            } else need = false;
        }
    }
} 