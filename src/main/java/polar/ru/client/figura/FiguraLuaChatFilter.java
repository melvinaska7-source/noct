package polar.ru.client.figura;

import net.minecraft.text.Text;

public final class FiguraLuaChatFilter {
    private FiguraLuaChatFilter() {
    }

    public static boolean shouldSuppress(Text message) {
        if (message == null) {
            return false;
        }
        String text = message.getString();
        if (text.isEmpty()) {
            return false;
        }
        return text.contains("[lua]") || text.contains("[error]") || text.contains("[ERROR]") || text.contains("Figura Networking API") || text.contains("ERR_NOT_ALLOWED");
    }
}

