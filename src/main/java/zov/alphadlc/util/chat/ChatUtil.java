package zov.alphadlc.util.chat;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import zov.alphadlc.util.IMinecraft;

import java.util.Arrays;

public class ChatUtil implements IMinecraft {
    public static void send(Object message) {
        if (mc.player == null) return;

        mc.player.sendMessage(Text.of("alphadlc Dlc " + Formatting.DARK_GRAY + "-> " + Formatting.RESET + message.toString()), false);
    }

    public static void send(Object... messages) {
        if (mc.player == null) return;

        mc.player.sendMessage(Text.of("alphadlc Dlc " + Formatting.DARK_GRAY + "-> " + Formatting.RESET + String.join(",", Arrays.toString(messages))), false);
    }
}