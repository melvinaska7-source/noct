package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.Items;
import zov.alphadlc.event.list.EventPlayerUpdate;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;

@ModuleInformation(moduleName = "Fast Exp", moduleDesc = "Ускоренное использование опыта", moduleCategory = ModuleCategory.PLAYER)
public class FastExp extends Module {

    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate e) {
        if (!(mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE || mc.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE)) return;

        mc.itemUseCooldown = 0;
    }
}