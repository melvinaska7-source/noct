package polar.ru.client.modules.impl.misc;

import net.minecraft.item.ItemStack;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BindSetting;

public class SearchHelper
extends Module {
    public static SearchHelper INSTANCE = new SearchHelper();
    public final BindSetting bind = new BindSetting("Бинд", -1);

    public SearchHelper() {
        super("SearchHelper", "Ищет в АХ предмет из руки по бинду", Module.ModuleCategory.MISC);
        this.addSettings(this.bind);
    }

    public void onBindPressed() {
        if (SearchHelper.mc.player == null || mc.getNetworkHandler() == null || SearchHelper.mc.currentScreen != null) {
            return;
        }
        if (this.bind.getKey() == -1) {
            return;
        }
        ItemStack stack = SearchHelper.mc.player.getMainHandStack();
        if (stack.isEmpty()) {
            ChatUtils.sendMessage("§cВозьми предмет в руку!");
            return;
        }
        String itemName = stack.getName().getString();
        mc.getNetworkHandler().sendChatCommand("ah search " + itemName);
    }
}

