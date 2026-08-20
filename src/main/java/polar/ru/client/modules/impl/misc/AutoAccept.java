package polar.ru.client.modules.impl.misc;

import java.util.Locale;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.polar;

public class AutoAccept
extends Module {
    public static AutoAccept INSTANCE = new AutoAccept();
    private final BooleanSetting onlyFriend = new BooleanSetting("Только друзья", false);

    public AutoAccept() {
        super("AutoAccept", "Автоматически принимает телепорт", Module.ModuleCategory.MISC);
        this.addSettings(this.onlyFriend);
    }

    @EventLink
    public void onEvent(EventPacket event) {
        GameMessageS2CPacket messagePacket;
        String raw;
        if (AutoAccept.mc.player == null || AutoAccept.mc.world == null) {
            return;
        }
        if (event.getType() != EventPacket.Type.RECEIVE) {
            return;
        }
        Packet<?> packet = event.getPacket();
        if (packet instanceof GameMessageS2CPacket && ((raw = (messagePacket = (GameMessageS2CPacket)packet).content().getString().toLowerCase(Locale.ROOT)).contains("телепортироваться") || raw.contains("has requested teleport") || raw.contains("просит к вам телепортироваться"))) {
            if (this.onlyFriend.isState()) {
                boolean isFriend = false;
                if (polar.INSTANCE.friendStorage != null) {
                    for (String friend : polar.INSTANCE.friendStorage.getFriends()) {
                        if (!raw.contains(friend.toLowerCase(Locale.ROOT))) continue;
                        isFriend = true;
                        break;
                    }
                }
                if (!isFriend) {
                    return;
                }
            }
            AutoAccept.mc.player.networkHandler.sendChatCommand("tpaccept");
        }
    }
}

