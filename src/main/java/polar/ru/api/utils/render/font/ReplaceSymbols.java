package polar.ru.api.utils.render.font;

import java.util.HashMap;
import java.util.Map;
import polar.ru.api.utils.color.ColorUtils;

public class ReplaceSymbols {
    private static final Map<Integer, String> REPLACEMENTS = new HashMap<Integer, String>();
    private static final Map<Integer, Integer> RANK_COLORS = new HashMap<Integer, Integer>();
    private static final int[] RANKS = new int[]{42240, 42244, 42248, 42258, 42262, 42272, 42276, 42280, 42336, 42290, 42294, 42308, 42326, 42312, 42304, 42322, 42249, 42259, 42263, 42273, 42277, 42281, 42291, 42295, 42241, 42245, 42313, 4144, 4138, 4132, 4134, 4140, 4139, 4151, 4130, 4148, 4141, 4152, 4133, 4131, 4149, 4150, 4137, 4129, 4145, 4143, 4146, 4135, 4153, 4126};

    public static String replaceCodePoint(int codePoint) {
        return REPLACEMENTS.get(codePoint);
    }

    public static int getGradientColorForReplacement(int codePoint, int charIndex, int totalChars, float alpha, int currentColor) {
        if (ReplaceSymbols.isRank(codePoint)) {
            Integer baseColor = RANK_COLORS.get(codePoint);
            if (baseColor == null) {
                return ReplaceSymbols.withOpacity(currentColor, alpha);
            }
            int endColor = ColorUtils.darken(baseColor, 0.8f);
            float ratio = totalChars <= 1 ? 1.0f : (float)charIndex / (float)(totalChars - 1);
            int interpolatedColor = ColorUtils.interpolateColor(endColor, baseColor, ratio);
            return ReplaceSymbols.withOpacity(interpolatedColor, alpha);
        }
        return ReplaceSymbols.withOpacity(currentColor, alpha);
    }

    private static boolean isRank(int codePoint) {
        for (int rank : RANKS) {
            if (rank != codePoint) continue;
            return true;
        }
        return false;
    }

    private static int withOpacity(int color, float alpha) {
        int a2 = Math.max(0, Math.min(255, (int)(alpha * 255.0f)));
        return ColorUtils.setAlphaColor(color, a2);
    }

    static {
        REPLACEMENTS.put(9889, "");
        REPLACEMENTS.put(9733, "");
        REPLACEMENTS.put(42240, "PLAYER");
        REPLACEMENTS.put(42244, "HERO");
        REPLACEMENTS.put(42248, "TITAN");
        REPLACEMENTS.put(42258, "AVENGER");
        REPLACEMENTS.put(42262, "OVERLORD");
        REPLACEMENTS.put(42272, "MAGISTER");
        REPLACEMENTS.put(42276, "IMPERATOR");
        REPLACEMENTS.put(42280, "DRAGON");
        REPLACEMENTS.put(42336, "D.HELPER");
        REPLACEMENTS.put(42313, "PEGAS");
        REPLACEMENTS.put(42290, "BULL");
        REPLACEMENTS.put(42294, "TIGER");
        REPLACEMENTS.put(42308, "VAMPIRE");
        REPLACEMENTS.put(42326, "BUNNY");
        REPLACEMENTS.put(42312, "COBRA");
        REPLACEMENTS.put(42304, "HYDRA");
        REPLACEMENTS.put(42322, "RABBIT");
        REPLACEMENTS.put(42249, "HELPER");
        REPLACEMENTS.put(42259, "ML.MODER");
        REPLACEMENTS.put(42263, "MODER");
        REPLACEMENTS.put(42273, "MODER+");
        REPLACEMENTS.put(42277, "ST.MODER");
        REPLACEMENTS.put(42281, "GL.MODER");
        REPLACEMENTS.put(42291, "ML.ADMIN");
        REPLACEMENTS.put(42295, "ADMIN");
        REPLACEMENTS.put(42241, "MEDIA");
        REPLACEMENTS.put(42245, "YT");
        REPLACEMENTS.put(42305, "GOD");
        REPLACEMENTS.put(4144, "HERO");
        REPLACEMENTS.put(4138, "TITAN");
        REPLACEMENTS.put(4132, "PRINCE");
        REPLACEMENTS.put(4134, "PHOENIX");
        REPLACEMENTS.put(4140, "OVERLORD");
        REPLACEMENTS.put(4139, "GUARDIAN");
        REPLACEMENTS.put(4151, "KRATOS");
        REPLACEMENTS.put(4130, "PHANTOM");
        REPLACEMENTS.put(4148, "CUSTOM");
        REPLACEMENTS.put(4141, "WINTER");
        REPLACEMENTS.put(4152, "SAKURA");
        REPLACEMENTS.put(4133, "SUMMER");
        REPLACEMENTS.put(4131, "HALLOWEEN");
        REPLACEMENTS.put(4149, "TIKTOK");
        REPLACEMENTS.put(4150, "TIKTOK+");
        REPLACEMENTS.put(4137, "MEDIA");
        REPLACEMENTS.put(4129, "YOUTUBE");
        REPLACEMENTS.put(4145, "HELPER");
        REPLACEMENTS.put(4143, "ML.ADMIN");
        REPLACEMENTS.put(4146, "MODER");
        REPLACEMENTS.put(4135, "CURATOR");
        REPLACEMENTS.put(4153, "SPECTATOR");
        REPLACEMENTS.put(4126, "DEVELOPER");
        REPLACEMENTS.put(7424, "A");
        REPLACEMENTS.put(665, "B");
        REPLACEMENTS.put(7428, "C");
        REPLACEMENTS.put(7429, "D");
        REPLACEMENTS.put(7431, "E");
        REPLACEMENTS.put(42800, "F");
        REPLACEMENTS.put(610, "G");
        REPLACEMENTS.put(668, "H");
        REPLACEMENTS.put(618, "I");
        REPLACEMENTS.put(7434, "J");
        REPLACEMENTS.put(7435, "K");
        REPLACEMENTS.put(671, "L");
        REPLACEMENTS.put(7437, "M");
        REPLACEMENTS.put(628, "N");
        REPLACEMENTS.put(7439, "O");
        REPLACEMENTS.put(7448, "P");
        REPLACEMENTS.put(491, "Q");
        REPLACEMENTS.put(640, "R");
        REPLACEMENTS.put(7451, "T");
        REPLACEMENTS.put(7452, "U");
        REPLACEMENTS.put(42801, "S");
        REPLACEMENTS.put(7456, "V");
        REPLACEMENTS.put(7457, "W");
        REPLACEMENTS.put(7521, "X");
        REPLACEMENTS.put(655, "Y");
        REPLACEMENTS.put(7458, "Z");
        RANK_COLORS.put(42240, ColorUtils.rgb(141, 143, 141));
        RANK_COLORS.put(42244, ColorUtils.rgb(100, 113, 251));
        RANK_COLORS.put(42248, ColorUtils.rgb(245, 220, 29));
        RANK_COLORS.put(42258, ColorUtils.rgb(79, 201, 83));
        RANK_COLORS.put(42262, ColorUtils.rgb(85, 255, 255));
        RANK_COLORS.put(42272, ColorUtils.rgb(224, 138, 52));
        RANK_COLORS.put(42276, ColorUtils.rgb(202, 60, 60));
        RANK_COLORS.put(42280, ColorUtils.rgb(245, 51, 238));
        RANK_COLORS.put(42336, ColorUtils.rgb(214, 200, 42));
        RANK_COLORS.put(42290, ColorUtils.rgb(121, 81, 202));
        RANK_COLORS.put(42294, ColorUtils.rgb(202, 130, 60));
        RANK_COLORS.put(42308, ColorUtils.rgb(202, 60, 60));
        RANK_COLORS.put(42326, ColorUtils.rgb(68, 65, 66));
        RANK_COLORS.put(42312, ColorUtils.rgb(127, 214, 86));
        RANK_COLORS.put(42304, ColorUtils.rgb(92, 120, 7));
        RANK_COLORS.put(42322, ColorUtils.rgb(230, 232, 230));
        RANK_COLORS.put(42249, ColorUtils.rgb(214, 200, 42));
        RANK_COLORS.put(42259, ColorUtils.rgb(100, 113, 251));
        RANK_COLORS.put(42263, ColorUtils.rgb(100, 113, 251));
        RANK_COLORS.put(42273, ColorUtils.rgb(121, 81, 202));
        RANK_COLORS.put(42277, ColorUtils.rgb(100, 113, 251));
        RANK_COLORS.put(42281, ColorUtils.rgb(121, 81, 202));
        RANK_COLORS.put(42291, ColorUtils.rgb(64, 151, 214));
        RANK_COLORS.put(42295, ColorUtils.rgb(202, 60, 60));
        RANK_COLORS.put(42241, ColorUtils.rgb(121, 81, 202));
        RANK_COLORS.put(42245, ColorUtils.rgb(255, 255, 255));
        RANK_COLORS.put(42305, ColorUtils.rgb(245, 198, 29));
        RANK_COLORS.put(42313, ColorUtils.rgb(202, 130, 60));
        RANK_COLORS.put(4144, ColorUtils.rgb(13, 176, 209));
        RANK_COLORS.put(4138, ColorUtils.rgb(21, 232, 24));
        RANK_COLORS.put(4132, ColorUtils.rgb(232, 169, 21));
        RANK_COLORS.put(4134, ColorUtils.rgb(237, 215, 19));
        RANK_COLORS.put(4140, ColorUtils.rgb(64, 163, 152));
        RANK_COLORS.put(4139, ColorUtils.rgb(86, 196, 99));
        RANK_COLORS.put(4151, ColorUtils.rgb(147, 46, 230));
        RANK_COLORS.put(4130, ColorUtils.rgb(230, 46, 46));
        RANK_COLORS.put(4148, ColorUtils.rgb(16, 35, 179));
        RANK_COLORS.put(4141, ColorUtils.rgb(55, 154, 184));
        RANK_COLORS.put(4152, ColorUtils.rgb(184, 39, 159));
        RANK_COLORS.put(4133, ColorUtils.rgb(255, 182, 56));
        RANK_COLORS.put(4131, ColorUtils.rgb(232, 60, 30));
        RANK_COLORS.put(4149, ColorUtils.rgb(0, 0, 0));
        RANK_COLORS.put(4150, ColorUtils.rgb(0, 0, 0));
        RANK_COLORS.put(4137, ColorUtils.rgb(37, 232, 30));
        RANK_COLORS.put(4129, ColorUtils.rgb(232, 30, 30));
        RANK_COLORS.put(4145, ColorUtils.rgb(30, 134, 232));
        RANK_COLORS.put(4143, ColorUtils.rgb(89, 167, 227));
        RANK_COLORS.put(4146, ColorUtils.rgb(62, 137, 194));
        RANK_COLORS.put(4135, ColorUtils.rgb(56, 235, 74));
        RANK_COLORS.put(4153, ColorUtils.rgb(173, 184, 174));
        RANK_COLORS.put(4126, ColorUtils.rgb(255, 0, 25));
    }
}

