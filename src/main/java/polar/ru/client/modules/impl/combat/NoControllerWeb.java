package polar.ru.client.modules.impl.combat;

import net.minecraft.block.Blocks;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventBlockCollide;
import polar.ru.client.modules.Module;

public class NoControllerWeb
extends Module {
    public static NoControllerWeb INSTANCE = new NoControllerWeb();

    public NoControllerWeb() {
        super("NoControllerWeb", "Позволяет ломать и бить сквозь паутину", Module.ModuleCategory.COMBAT);
    }

    @EventLink
        public void onBlockCollide(EventBlockCollide e2) {
        if (NoControllerWeb.mc.world == null || e2.getPos() == null) {
            return;
        }
        if (NoControllerWeb.mc.world.getBlockState(e2.getPos()).getBlock() == Blocks.COBWEB) {
            e2.setCancelled(true);
        }
    }
}

