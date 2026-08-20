package polar.ru.api.utils.replace;

import net.minecraft.text.Text;

public class ReplaceUtils {
    public static Text replace(Text text, String target, String replacement) {
        if (text == null || target == null || target.isEmpty()) {
            return text;
        }
        String str = text.getString();
        if (!str.contains(target)) {
            return text;
        }
        return Text.literal(str.replace(target, replacement));
    }

    public static String replace(String text, String target, String replacement) {
        if (text == null || target == null || target.isEmpty()) {
            return text;
        }
        return text.replace(target, replacement);
    }
}
