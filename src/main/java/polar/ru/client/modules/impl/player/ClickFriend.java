package polar.ru.client.modules.impl.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventBinding;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BindSetting;
public class ClickFriend
extends Module {
    public static ClickFriend INSTANCE = new ClickFriend();
    private final BindSetting keyToClickFriend = new BindSetting("Кнопка", -1);
    private GameProfile profile;

    public ClickFriend() {
        super("ClickFriend", "Добавляет игрока в друзья", Module.ModuleCategory.PLAYER);
        this.addSettings(this.keyToClickFriend);
    }

    @EventLink
    public void onBinding(EventBinding event) {
        PlayerEntity targetPlayer;
        if (ClickFriend.mc.currentScreen != null || ClickFriend.mc.player == null || ClickFriend.mc.world == null) {
            return;
        }
        if (event.getKey() == this.keyToClickFriend.getKey() && (targetPlayer = this.getPlayerUnderCrosshair()) != null) {
            String playerName = targetPlayer.getName().getString();
            if (polar.ru.polar.INSTANCE.friendStorage.isFriend(playerName)) {
                polar.ru.polar.INSTANCE.friendStorage.remove(playerName);
                ChatUtils.sendMessage("§cУдалён из друзей: §f" + playerName);
            } else {
                polar.ru.polar.INSTANCE.friendStorage.add(playerName);
                ChatUtils.sendMessage("§aДобавлен в друзья: §f" + playerName);
            }
        }
    }

    private PlayerEntity getPlayerUnderCrosshair() {
        Entity var_1297_2;
        float rangeValue;
        Vec3d lookVec;
        Vec3d reachVec;
        Vec3d eyePos = ClickFriend.mc.player.getCameraPosVec(1.0f);
        EntityHitResult result = ProjectileUtil.raycast((Entity)ClickFriend.mc.player, (Vec3d)eyePos, (Vec3d)(reachVec = eyePos.add((lookVec = ClickFriend.mc.player.getRotationVec(1.0f)).multiply((double)(rangeValue = 10.0f)))), (Box)ClickFriend.mc.player.getBoundingBox().expand((double)rangeValue), entity -> entity != ClickFriend.mc.player && entity.isAlive() && entity instanceof PlayerEntity, (double)(rangeValue * rangeValue));
        if (result != null && (var_1297_2 = result.getEntity()) instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)var_1297_2;
            return player;
        }
        return null;
    }
}

