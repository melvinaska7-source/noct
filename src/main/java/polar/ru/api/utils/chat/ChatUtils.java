package polar.ru.api.utils.chat;

import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import polar.ru.api.utils.color.ColorUtils;

public final class ChatUtils {
    public static void sendMessage(Object message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            System.out.println("[polar] " + String.valueOf(message));
            return;
        }
        MutableText text = Text.literal((String)"");
        String prefix = "lavanda";
        for (int i2 = 0; i2 < prefix.length(); ++i2) {
            text.append((Text)Text.literal((String)String.valueOf(prefix.charAt(i2))).setStyle(Style.EMPTY.withBold(Boolean.valueOf(true)).withColor(TextColor.fromRgb((int)ColorUtils.gradient(ColorUtils.getThemeColor(0), ColorUtils.getThemeColor(90), (float)i2 / (float)prefix.length())))));
        }
        text.append((Text)Text.literal((String)" - ").setStyle(Style.EMPTY.withBold(Boolean.valueOf(false)).withColor(TextColor.fromRgb((int)new Color(200, 200, 200).getRGB()))));
        text.append((Text)Text.literal((String)String.valueOf(message)).setStyle(Style.EMPTY.withBold(Boolean.valueOf(false)).withColor(TextColor.fromRgb((int)new Color(200, 200, 200).getRGB()))));
        mc.player.sendMessage((Text)text, false);
    }
    private ChatUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

