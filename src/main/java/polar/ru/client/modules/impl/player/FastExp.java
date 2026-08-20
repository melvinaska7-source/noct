package polar.ru.client.modules.impl.player;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;
import polar.ru.mixin.IMinecraftClientAccessor;

public class FastExp
extends Module {
    public static FastExp INSTANCE = new FastExp();

    public FastExp() {
        super("FastExp", "Позволяет бросать пузырьки опыта без задержки", Module.ModuleCategory.PLAYER);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (FastExp.mc.player == null) {
            return;
        }
        ItemStack stack = FastExp.mc.player.getMainHandStack();
        if (stack.isOf(Items.EXPERIENCE_BOTTLE)) {
            ((IMinecraftClientAccessor)mc).setItemUseCooldown(0);
        }
    }
}

