package polar.ru.api.utils.render.fonts.msdf;

import java.util.HashMap;
import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.utils.render.fonts.msdf.Font;
import polar.ru.api.utils.render.fonts.msdf.MsdfFont;

public class Fonts {
    private static final HashMap<String, MsdfFont> loadedFonts = new HashMap();
    private static final HashMap<String, Font[]> fontCache = new HashMap();
    private static boolean initialized = false;

    public static polar.ru.api.utils.render.fonts.ttf.MCFontRenderer getTtfFont(String name, float size) {
        return polar.ru.api.utils.render.fonts.ttf.Fonts.getFont(name, size);
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        Fonts.loadFont("altmanager");
        Fonts.loadFont("clickgui");
        Fonts.loadFont("desc");
        Fonts.loadFont("energy");
        Fonts.loadFont("icon");
        Fonts.loadFont("icon1");
        Fonts.loadFont("iconnew");
        Fonts.loadFont("kantumruy");
        Fonts.loadFont("logo");
        Fonts.loadFont("mainmenu");
        Fonts.loadFont("menu");
        Fonts.loadFont("moe1");
        Fonts.loadFont("moe2");
        Fonts.loadFont("moe3");
        Fonts.loadFont("semibold");
        Fonts.loadFont("sf_regular");
        Fonts.loadFont("suisse");
        Fonts.loadFont("theme");
        Fonts.loadFont("vector");
        Fonts.loadFont("wave");
        Fonts.loadFont("wonderful");
    }

    private static void loadFont(String name) {
        try {
            MsdfFont msdfFont = MsdfFont.builder().atlas(name).data(name).build();
            loadedFonts.put(name, msdfFont);
            Font[] fonts = new Font[100];
            for (int i2 = 8; i2 < 100; ++i2) {
                fonts[i2] = new Font(msdfFont, (float)i2);
            }
            fontCache.put(name, fonts);
        }
        catch (Exception e2) {
            System.err.println("[Fonts] Failed to load " + name + ": " + e2.getMessage());
        }
    }

    public static Font getFont(String name, int size) {
        Font[] fonts;
        if (!initialized) {
            Fonts.init();
        }
        String cleanName = name.replace(".ttf", "");
        if (cleanName.equals("sf_semibold") || cleanName.equals("sfsemibold")) {
            cleanName = "semibold";
        }
        if (cleanName.equals("sfregular") || cleanName.equals("sf-regular") || cleanName.equals("sf_medium") || cleanName.equals("sf_bold")) {
            cleanName = "sf_regular";
        }
        if (cleanName.equals("icona") || cleanName.equals("icon_a") || cleanName.equals("icons")) {
            cleanName = "icon";
        }
        if (cleanName.equals("comfortaa") || cleanName.equals("montserrat") || cleanName.equals("roboto") || cleanName.equals("inter")) {
            cleanName = "sf_regular";
        }
        if (cleanName.equals("main_menu")) {
            cleanName = "mainmenu";
        }
        if (size < 8) {
            size = 8;
        }
        if (size >= 100) {
            size = 99;
        }
        if ((fonts = fontCache.get(cleanName)) != null && fonts[size] != null) {
            return fonts[size];
        }
        if (!loadedFonts.containsKey(cleanName)) {
            Fonts.loadFont(cleanName);
        }
        if ((fonts = fontCache.get(cleanName)) != null && fonts[size] != null) {
            return fonts[size];
        }
        Font[] fallbackFonts = fontCache.get("sf_regular");
        if (fallbackFonts != null && fallbackFonts[size] != null) {
            return fallbackFonts[size];
        }
        return null;
    }

    public static void drawStringWithFade(Font font, String text, float x2, float y2, float maxWidth, int color) {
        if (font == null) {
            return;
        }
        MatrixStack stack = new MatrixStack();
        font.drawStringWithFade(stack, text, x2, y2, maxWidth, color);
    }
}

