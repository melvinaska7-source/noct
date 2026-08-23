package zov.alphadlc.util.chat;

import net.minecraft.text.Text;
import zov.alphadlc.util.IMinecraft;

import java.util.Arrays;

public class ChatUtil implements IMinecraft {

    // Твой градиент: розовый -> фиолетовый -> голубой
    private static final String PREFIX = "§x§8§5§1§9§5§A§l[§x§8§0§2§3§6§5§lN§x§7§A§2§D§6§F§lo§x§7§5§3§7§7§A§lc§x§6§F§4§1§8§4§lt§x§6§A§4§B§8§F§lu§x§6§4§5§5§9§A§lr§x§5§F§5§F§A§4§ln§x§5§9§6§9§A§F§le§x§5§4§7§3§B§9§lD§x§4§E§7§D§C§4§lL§x§4§9§8§7§C§E§lC§x§4§3§9§1§D§9§l]§r §8» §r";

    public static void addMessage(Object message) {
        if (mc.player == null) return;
        mc.player.sendMessage(Text.of(PREFIX + message.toString()), false);
    }

    public static void addMessage(Object... messages) {
        if (mc.player == null) return;
        mc.player.sendMessage(Text.of(PREFIX + String.join(",", Arrays.toString(messages))), false);
    }
}
