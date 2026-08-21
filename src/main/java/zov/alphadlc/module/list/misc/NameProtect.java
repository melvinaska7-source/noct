package zov.alphadlc.module.list.misc;

import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.util.friend.Friend;
import zov.alphadlc.util.friend.FriendRepository;

@ModuleInformation(moduleName = "Streamer Mode", moduleDesc = "Скрывает никнеймы игроков", moduleCategory = ModuleCategory.MISC)
public class NameProtect extends Module {

    public final BooleanSetting hideFriends = new BooleanSetting("Скрыть друзей", false);

    public String getCustomName() {
        return isEnabled() ? "alphadlc" : mc.player.getNameForScoreboard();
    }

    public String getCustomName(String originalName) {
        if (!isEnabled() || mc.player == null) {
            return originalName;
        }

        String me = mc.player.getNameForScoreboard();
        if (originalName.contains(me)) {
            return originalName.replace(me, "alphadlc");
        }

        if (hideFriends.getValue()) {
            var friends = FriendRepository.getFriends();
            for (Friend friend : friends) {
                if (originalName.contains(friend.name())) {
                    return originalName.replace(friend.name(), "alphadlc");
                }
            }
        }

        return originalName;
    }
}