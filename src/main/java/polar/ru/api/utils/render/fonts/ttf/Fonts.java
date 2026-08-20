package polar.ru.api.utils.render.fonts.ttf;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Identifier;
import polar.ru.api.utils.render.fonts.ttf.FontUtil;
import polar.ru.api.utils.render.fonts.ttf.GradientFontRenderer;
import polar.ru.api.utils.render.fonts.ttf.MCFontRenderer;

public class Fonts {
    private static final String MOD_ID = "polar";
    private static final Map<String, Map<Float, MCFontRenderer>> regularFonts = new HashMap<String, Map<Float, MCFontRenderer>>();
    private static final Map<String, Map<Float, GradientFontRenderer>> gradientFonts = new HashMap<String, Map<Float, GradientFontRenderer>>();
    public static MCFontRenderer comfortaa16;
    public static MCFontRenderer comfortaa18;
    public static MCFontRenderer comfortaa20;
    public static GradientFontRenderer comfortaaGradient18;
    public static MCFontRenderer roboto16;
    public static MCFontRenderer roboto18;
    public static MCFontRenderer roboto20;
    public static GradientFontRenderer robotoGradient18;
    public static MCFontRenderer montserrat16;
    public static MCFontRenderer montserrat18;
    public static MCFontRenderer montserrat20;
    public static GradientFontRenderer montserratGradient18;
    private static boolean initialized;

    public static void init() {
        if (initialized) {
            return;
        }
        comfortaa16 = Fonts.getFont("comfortaa.ttf", 16.0f);
        comfortaa18 = Fonts.getFont("comfortaa.ttf", 18.0f);
        comfortaa20 = Fonts.getFont("comfortaa.ttf", 20.0f);
        comfortaaGradient18 = Fonts.getGradientFont("comfortaa.ttf", 18.0f);
        roboto16 = Fonts.getFont("roboto.ttf", 16.0f);
        roboto18 = Fonts.getFont("roboto.ttf", 18.0f);
        roboto20 = Fonts.getFont("roboto.ttf", 20.0f);
        robotoGradient18 = Fonts.getGradientFont("roboto.ttf", 18.0f);
        montserrat16 = Fonts.getFont("montserrat.ttf", 16.0f);
        montserrat18 = Fonts.getFont("montserrat.ttf", 18.0f);
        montserrat20 = Fonts.getFont("montserrat.ttf", 20.0f);
        montserratGradient18 = Fonts.getGradientFont("montserrat.ttf", 18.0f);
        initialized = true;
    }

    public static MCFontRenderer getFont(String fontName, float size) {
        regularFonts.computeIfAbsent(fontName, k2 -> new HashMap());
        Map<Float, MCFontRenderer> fontSizes = regularFonts.get(fontName);
        if (fontSizes.containsKey(Float.valueOf(size))) {
            return fontSizes.get(Float.valueOf(size));
        }
        Font font = FontUtil.getFontFromTTF(Identifier.of((String)MOD_ID, (String)("fonts/ttf/" + fontName)), size, 0);
        if (font == null) {
            font = new Font("Arial", 0, (int)size);
        }
        MCFontRenderer renderer = new MCFontRenderer(font, true, true);
        fontSizes.put(Float.valueOf(size), renderer);
        return renderer;
    }

    public static GradientFontRenderer getGradientFont(String fontName, float size) {
        gradientFonts.computeIfAbsent(fontName, k2 -> new HashMap());
        Map<Float, GradientFontRenderer> fontSizes = gradientFonts.get(fontName);
        if (fontSizes.containsKey(Float.valueOf(size))) {
            return fontSizes.get(Float.valueOf(size));
        }
        Font font = FontUtil.getFontFromTTF(Identifier.of((String)MOD_ID, (String)("fonts/" + fontName)), size, 0);
        if (font == null) {
            font = new Font("Arial", 0, (int)size);
        }
        GradientFontRenderer renderer = new GradientFontRenderer(font, true, true);
        fontSizes.put(Float.valueOf(size), renderer);
        return renderer;
    }

    public static void drawStringWithFade(MCFontRenderer font, String text, float x2, float y2, float maxWidth, int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0f) {
            return;
        }
        float currentX = x2;
        float fadeZoneWidth = Math.min(22.0f, Math.max(8.0f, maxWidth * 0.35f));
        float fadeStartX = x2 + maxWidth - fadeZoneWidth;
        int originalAlpha = color >> 24 & 0xFF;
        for (int i2 = 0; i2 < text.length(); ++i2) {
            String ch = String.valueOf(text.charAt(i2));
            float charWidth = font.getStringWidth(ch);
            if (currentX > x2 + maxWidth && i2 > 0) break;
            int finalColor = color;
            if (currentX > fadeStartX) {
                float progress = (currentX - fadeStartX) / fadeZoneWidth;
                progress = Math.max(0.0f, Math.min(1.0f, progress));
                float fadeFactor = (float)Math.cos((double)progress * Math.PI / 2.0);
                int newAlpha = (int)((float)originalAlpha * fadeFactor);
                finalColor = color & 0xFFFFFF | newAlpha << 24;
            }
            if ((finalColor >> 24 & 0xFF) > 4) {
                font.drawString(ch, currentX, y2, finalColor);
            }
            currentX += charWidth;
        }
    }

    public static MCFontRenderer getSystemFont(String fontName, float size) {
        String key = "system_" + fontName;
        regularFonts.computeIfAbsent(key, k2 -> new HashMap());
        Map<Float, MCFontRenderer> fontSizes = regularFonts.get(key);
        if (fontSizes.containsKey(Float.valueOf(size))) {
            return fontSizes.get(Float.valueOf(size));
        }
        Font font = new Font(fontName, 0, (int)size);
        MCFontRenderer renderer = new MCFontRenderer(font, true, true);
        fontSizes.put(Float.valueOf(size), renderer);
        return renderer;
    }

    public static MCFontRenderer getSystemFont(String fontName, float size, int style) {
        String key = "system_" + fontName + "_" + style;
        regularFonts.computeIfAbsent(key, k2 -> new HashMap());
        Map<Float, MCFontRenderer> fontSizes = regularFonts.get(key);
        if (fontSizes.containsKey(Float.valueOf(size))) {
            return fontSizes.get(Float.valueOf(size));
        }
        Font font = new Font(fontName, style, (int)size);
        MCFontRenderer renderer = new MCFontRenderer(font, true, true);
        fontSizes.put(Float.valueOf(size), renderer);
        return renderer;
    }

    public static void clearCache() {
        regularFonts.clear();
        gradientFonts.clear();
        initialized = false;
    }

    public static void clearCache(String fontName) {
        regularFonts.remove(fontName);
        gradientFonts.remove(fontName);
    }
    public static boolean isInitialized() {
        return initialized;
    }

    static {
        initialized = false;
    }
}

