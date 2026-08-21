package zov.alphadlc.module.list.render;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import zov.alphadlc.AlphaDLC;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeListSetting;
import zov.alphadlc.util.friend.FriendRepository;

@ModuleInformation(moduleName = "Cape", moduleDesc = "Свой плащ для игрока", moduleCategory = ModuleCategory.RENDER)
public class Cape extends Module {
    private final ModeListSetting targets = new ModeListSetting("На кого работать",
            new BooleanSetting("Себя", true),
            new BooleanSetting("Друзья", true));

    public static Cape getInstance() {
        return AlphaDLC.getInstance().getModuleStorage().get(Cape.class);
    }

    public boolean shouldRenderCape(AbstractClientPlayerEntity player) {
        if (!this.isEnabled() || player == null) {
            return false;
        }
        boolean isSelf = player == this.mc.player;
        boolean isFriend = FriendRepository.isFriend(player.getNameForScoreboard());
        boolean selfEnabled = this.targets.isEnabled("Себя");
        boolean friendsEnabled = this.targets.isEnabled("Друзья");
        
        if (isSelf) {
            return selfEnabled;
        }
        if (isFriend) {
            return friendsEnabled;
        }
        // Не работает на других игроков
        return false;
    }
}
