package zov.alphadlc.module.list.combat;

import com.google.common.eventbus.Subscribe;
import zov.alphadlc.event.list.EventAttack;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.util.friend.Friend;
import zov.alphadlc.util.friend.FriendRepository;

@ModuleInformation(moduleName = "No Friend Damage", moduleDesc = "Не атакует друзей из списка", moduleCategory = ModuleCategory.COMBAT)
public class NoFriendDamage extends Module {

    @Subscribe
    private void onAttack(EventAttack e) {
        for (Friend friend : FriendRepository.getFriends()) {
            if (e.getEntity() == mc.player) continue;
            if (!e.getEntity().getNameForScoreboard().equals(friend.name())) continue;
            e.cancelEvent();
        }
    }
}