package polar.ru.client.modules.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventAttackEntity;
import polar.ru.client.modules.Module;
import polar.ru.polar;

public class NoFriendDamage
extends Module {
    public static NoFriendDamage INSTANCE = new NoFriendDamage();

    public NoFriendDamage() {
        super("NoFriendDamage", "Не позволяет бить друзей", Module.ModuleCategory.PLAYER);
    }

    @EventLink
    public void onAttack(EventAttackEntity event) {
        if (NoFriendDamage.mc.player == null || NoFriendDamage.mc.world == null) {
            return;
        }
        Entity var_1297_2 = event.getTarget();
        if (var_1297_2 instanceof PlayerEntity) {
            PlayerEntity targetPlayer = (PlayerEntity)var_1297_2;
            if (polar.INSTANCE != null && polar.INSTANCE.friendStorage != null && polar.INSTANCE.friendStorage.isFriend(targetPlayer.getName().getString())) {
                event.cancel();
            }
        }
    }
}

