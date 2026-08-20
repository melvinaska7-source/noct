package polar.ru.api.utils.color;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import polar.ru.api.utils.math.MathUtils;
import polar.ru.polar;

public class ColorUtils {
    public static final Color green = new Color(36, 218, 118);
    public static final Color yellow = new Color(255, 196, 67);
    public static final Color orange = new Color(255, 134, 0);
    public static final Color red = new Color(239, 72, 54);
    public static final Color Blues = new Color(125, 217, 250);

    public static int red(int c2) {
        return c2 >> 16 & 0xFF;
    }

    public static int green(int c2) {
        return c2 >> 8 & 0xFF;
    }

    public static float redf(int c2) {
        return (float)ColorUtils.red(c2) / 255.0f;
    }

    public static float greenf(int c2) {
        return (float)ColorUtils.green(c2) / 255.0f;
    }

    public static float bluef(int c2) {
        return (float)ColorUtils.blue(c2) / 255.0f;
    }

    public static float alphaf(int c2) {
        return (float)ColorUtils.alpha(c2) / 255.0f;
    }

    public static int getColor(int brightness, int alpha) {
        return ColorUtils.getColor(brightness, brightness, brightness, alpha);
    }

    public static int gradient(int color1, int color2, float amount) {
        amount = MathHelper.clamp((float)amount, (float)0.0f, (float)1.0f);
        int r2 = MathHelper.lerp((float)amount, (int)ColorUtils.red(color1), (int)ColorUtils.red(color2));
        int g2 = MathHelper.lerp((float)amount, (int)ColorUtils.green(color1), (int)ColorUtils.green(color2));
        int b2 = MathHelper.lerp((float)amount, (int)ColorUtils.blue(color1), (int)ColorUtils.blue(color2));
        int a2 = MathHelper.lerp((float)amount, (int)ColorUtils.alpha(color1), (int)ColorUtils.alpha(color2));
        return ColorUtils.rgba(r2, g2, b2, a2);
    }

    public static int toColor(String hexColor) {
        if (hexColor == null || hexColor.length() != 7 || !hexColor.startsWith("#")) {
            return -16777216;
        }
        try {
            int rgb = Integer.parseInt(hexColor.substring(1), 16);
            return 0xFF000000 | rgb;
        }
        catch (NumberFormatException e2) {
            return -16777216;
        }
    }

    public static int applyAlpha(int color, float alphaMul) {
        int a2 = color >> 24 & 0xFF;
        int na = (int)((float)a2 * Math.max(0.0f, Math.min(1.0f, alphaMul)));
        return color & 0xFFFFFF | na << 24;
    }

    public static int r(int color) {
        return color >> 16 & 0xFF;
    }

    public static int g(int color) {
        return color >> 8 & 0xFF;
    }

    public static int b(int color) {
        return color & 0xFF;
    }

    public static int a(int color) {
        return color >> 24 & 0xFF;
    }

    public static int hexToRgb(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Недопустимый формат HEX: " + hex);
        }
        int r2 = Integer.parseInt(hex.substring(0, 2), 16);
        int g2 = Integer.parseInt(hex.substring(2, 4), 16);
        int b2 = Integer.parseInt(hex.substring(4, 6), 16);
        return ColorUtils.rgb(r2, g2, b2);
    }

    public static int getThemeColor() {
        if (!polar.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        }
        return ColorUtils.getThemeColor(0);
    }

    public static int getThemeColor(int index) {
        return polar.INSTANCE.themeStorage.getThemes().getTheme().getColor(index);
    }

    public static int getThemeStaticColor() {
        return polar.INSTANCE.themeStorage.getThemes().getTheme().color[0];
    }

    public static int rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
        float hue = (float)angle / 360.0f;
        int color = Color.HSBtoRGB(hue, saturation, brightness);
        return ColorUtils.getColor(ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), Math.max(0, Math.min(255, (int)(opacity * 255.0f))));
    }

    public static int interpolate(int color1, int color2, double amount) {
        amount = (float)MathUtils.clamp(0.0, 1.0, amount);
        return ColorUtils.getColor(MathUtils.ler1p(ColorUtils.red(color1), ColorUtils.red(color2), amount), MathUtils.ler1p(ColorUtils.green(color1), ColorUtils.green(color2), amount), MathUtils.ler1p(ColorUtils.blue(color1), ColorUtils.blue(color2), amount), MathUtils.ler1p(ColorUtils.alpha(color1), ColorUtils.alpha(color2), amount));
    }

    public static int[] genGradientForText(int color1, int color2, int length) {
        int[] gradient = new int[length];
        for (int i2 = 0; i2 < length; ++i2) {
            double pc = (double)i2 / (double)(length - 1);
            gradient[i2] = ColorUtils.interpolate(color1, color2, pc);
        }
        return gradient;
    }

    public static int blue(int c2) {
        return c2 & 0xFF;
    }

    public static int overCol(int c1, int c2, float pc01) {
        return ColorUtils.getColor((float)ColorUtils.red(c1) * (1.0f - pc01) + (float)ColorUtils.red(c2) * pc01, (float)ColorUtils.green(c1) * (1.0f - pc01) + (float)ColorUtils.green(c2) * pc01, (float)ColorUtils.blue(c1) * (1.0f - pc01) + (float)ColorUtils.blue(c2) * pc01, (float)ColorUtils.alpha(c1) * (1.0f - pc01) + (float)ColorUtils.alpha(c2) * pc01);
    }

    public static int darken(int color, float factor) {
        float[] rgb = ColorUtils.getColorT(color);
        float[] hsb = Color.RGBtoHSB((int)(rgb[0] * 255.0f), (int)(rgb[1] * 255.0f), (int)(rgb[2] * 255.0f), null);
        hsb[2] = hsb[2] * factor;
        hsb[2] = Math.max(0.0f, Math.min(1.0f, hsb[2]));
        int darkenedRGB = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return ColorUtils.applyOpacity(darkenedRGB, (int)(rgb[3] * 255.0f));
    }

    public static int brighten(int color, float factor) {
        float[] rgb = ColorUtils.getColorT(color);
        float[] hsb = Color.RGBtoHSB((int)(rgb[0] * 255.0f), (int)(rgb[1] * 255.0f), (int)(rgb[2] * 255.0f), null);
        hsb[2] = hsb[2] + (1.0f - hsb[2]) * factor;
        hsb[2] = Math.max(0.0f, Math.min(1.0f, hsb[2]));
        int brightenedRGB = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        return ColorUtils.applyOpacity(brightenedRGB, (int)(rgb[3] * 255.0f));
    }

    public static int multDark(int c2, float brpc) {
        return ColorUtils.getColor((float)ColorUtils.red(c2) * brpc, (float)ColorUtils.green(c2) * brpc, (float)ColorUtils.blue(c2) * brpc, (float)ColorUtils.alpha(c2));
    }

    public static int overCol(int c1, int c2) {
        return ColorUtils.overCol(c1, c2, 0.5f);
    }

    public static int alpha(int c2) {
        return c2 >> 24 & 0xFF;
    }

    public static int multAlpha(int c2, float apc) {
        return ColorUtils.getColor((float)ColorUtils.red(c2), (float)ColorUtils.green(c2), (float)ColorUtils.blue(c2), (float)ColorUtils.alpha(c2) * apc);
    }

    public static int replAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return alpha << 24 | color & 0xFFFFFF;
    }

    public static Color random() {
        return new Color(Color.HSBtoRGB((float)Math.random(), (float)(0.75 + Math.random() / 4.0), (float)(0.75 + Math.random() / 4.0)));
    }

    public static int getColor(float r2, float g2, float b2, float a2) {
        return new Color((int)r2, (int)g2, (int)b2, (int)a2).getRGB();
    }

    public static float[] getRGBAf(int c2) {
        return new float[]{ColorUtils.redf(c2), ColorUtils.greenf(c2), ColorUtils.bluef(c2), ColorUtils.alphaf(c2)};
    }

    public static float[] getRGBAf1(int c2) {
        return new float[]{(float)ColorUtils.red(c2) / 255.0f, (float)ColorUtils.green(c2) / 255.0f, (float)ColorUtils.blue(c2) / 255.0f, (float)ColorUtils.alpha(c2) / 255.0f};
    }

    public static Color interpolateTwoColors(int speed, int index, Color start, Color end, boolean trueColor) {
        int angle = 0;
        angle = speed == 0 ? index % 360 : (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        boolean tur = trueColor;
        return tur ? ColorUtils.interpolateColorHue(start, end, (float)angle / 360.0f) : ColorUtils.interpolateColorC(start, end, (float)angle / 360.0f);
    }

    public static Color interpolateTwoColors(int speed, int index, Color start, Color end) {
        return ColorUtils.interpolateTwoColors(speed, index, start, end, false);
    }

    public static Color astolfo(float yDist, float yTotal, float saturation, float speedt) {
        float hue;
        float speed = 1800.0f;
        for (hue = (float)(System.currentTimeMillis() % (long)((int)speed)) + (yTotal - yDist) * speedt; hue > speed; hue -= speed) {
        }
        if ((hue /= speed) > 1.0f) {
            hue = 1.0f - (hue - 1.0f);
        }
        return Color.getHSBColor(hue += 1.0f, saturation, 1.0f);
    }

    private static int calculateHueDegrees(int divisor, int offset) {
        long currentTime = System.currentTimeMillis();
        long calculatedValue = (currentTime / (long)divisor + (long)offset) % 360L;
        return (int)calculatedValue;
    }

    public static void setColor(Color color, float alpha) {
        float red = (float)color.getRed() / 255.0f;
        float green = (float)color.getGreen() / 255.0f;
        float blue = (float)color.getBlue() / 255.0f;
        RenderSystem.setShaderColor((float)red, (float)green, (float)blue, (float)alpha);
    }

    public static int rgb(int r2, int g2, int b2) {
        return 0xFF000000 | r2 << 16 | g2 << 8 | b2;
    }

    public static int rgba(int r2, int g2, int b2, int a2) {
        return a2 << 24 | r2 << 16 | g2 << 8 | b2;
    }

    public static float[] rgba(int color) {
        return new float[]{(float)(color >> 16 & 0xFF) / 255.0f, (float)(color >> 8 & 0xFF) / 255.0f, (float)(color & 0xFF) / 255.0f, (float)(color >> 24 & 0xFF) / 255.0f};
    }

    public static int rgba(double r2, double g2, double b2, double a2) {
        return ColorUtils.rgba((int)r2, (int)g2, (int)b2, (int)a2);
    }

    public static int getRed(int hex) {
        return hex >> 16 & 0xFF;
    }

    public static int getGreen(int hex) {
        return hex >> 8 & 0xFF;
    }

    public static int interpolate(int start, int end, float value) {
        float[] startColor = ColorUtils.rgba(start);
        float[] endColor = ColorUtils.rgba(end);
        return ColorUtils.rgba((int)MathUtils.interpolate(startColor[0] * 255.0f, endColor[0] * 255.0f, value), (int)MathUtils.interpolate(startColor[1] * 255.0f, endColor[1] * 255.0f, value), (int)MathUtils.interpolate(startColor[2] * 255.0f, endColor[2] * 255.0f, value), (int)MathUtils.interpolate(startColor[3] * 255.0f, endColor[3] * 255.0f, value));
    }

    public static int interpolateColor(int color1, int color2, float amount) {
        amount = Math.min(1.0f, Math.max(0.0f, amount));
        int red1 = ColorUtils.getRed(color1);
        int green1 = ColorUtils.getGreen(color1);
        int blue1 = ColorUtils.getBlue(color1);
        int alpha1 = ColorUtils.getAlpha(color1);
        int red2 = ColorUtils.getRed(color2);
        int green2 = ColorUtils.getGreen(color2);
        int blue2 = ColorUtils.getBlue(color2);
        int alpha2 = ColorUtils.getAlpha(color2);
        int interpolatedRed = ColorUtils.interpolateInt(red1, red2, amount);
        int interpolatedGreen = ColorUtils.interpolateInt(green1, green2, amount);
        int interpolatedBlue = ColorUtils.interpolateInt(blue1, blue2, amount);
        int interpolatedAlpha = ColorUtils.interpolateInt(alpha1, alpha2, amount);
        return interpolatedAlpha << 24 | interpolatedRed << 16 | interpolatedGreen << 8 | interpolatedBlue;
    }

    public static MutableText gradient(String message, int first, int end) {
        MutableText text = Text.empty();
        for (int i2 = 0; i2 < message.length(); ++i2) {
            int color = ColorUtils.interpolateColor(first, end, (float)i2 / (float)message.length());
            MutableText charText = Text.literal((String)String.valueOf(message.charAt(i2))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)color)));
            text.append((Text)charText);
        }
        return text;
    }

    public static Text replace(Text original, String find, String replaceWith) {
        if (original == null || find == null || replaceWith == null) {
            return original;
        }
        String originalText = original.getString();
        String replacedText = originalText.replace(find, replaceWith);
        return Text.literal((String)replacedText);
    }

    public static int gradient(int speed, int index, int ... colors) {
        int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int colorIndex = (int)((float)angle / 360.0f * (float)colors.length);
        if (colorIndex == colors.length) {
            --colorIndex;
        }
        int color1 = colors[colorIndex];
        int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
        return ColorUtils.interpolateColor(color1, color2, (float)angle / 360.0f * (float)colors.length - (float)colorIndex);
    }

    public static int themeGradient(int speed, int index, float darkenFactor) {
        int theme = ColorUtils.getThemeColor();
        return ColorUtils.gradient(speed, index, theme, ColorUtils.darken(theme, darkenFactor));
    }

    public static int getBlue(int hex) {
        return hex & 0xFF;
    }

    public static int getAlpha(int hex) {
        return hex >> 24 & 0xFF;
    }

    public static int getColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= alpha << 24;
        color |= red << 16;
        return (color |= green << 8) | blue;
    }

    public static int getColor(int bright) {
        return ColorUtils.getColor(bright, bright, bright, 255);
    }

    public static float[] getColorA(int color) {
        return new float[]{(float)ColorUtils.red(color) / 255.0f, (float)ColorUtils.green(color) / 255.0f, (float)ColorUtils.blue(color) / 255.0f, ColorUtils.alphaf(color)};
    }

    public static float[] getColorT(int color) {
        return new float[]{(float)ColorUtils.red(color) / 255.0f, (float)ColorUtils.green(color) / 255.0f, (float)ColorUtils.blue(color) / 255.0f, ColorUtils.alphaf(color)};
    }

    public static void setColor(double red, double green, double blue, double alpha) {
        GL11.glColor4d((double)red, (double)green, (double)blue, (double)alpha);
    }

    public static int setAlphaColor(int color, int alpha) {
        return color & 0xFFFFFF | alpha << 24;
    }

    public static float lerp(float a2, float b2, float f2) {
        return a2 + f2 * (b2 - a2);
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1.0f, Math.max(0.0f, amount));
        return new Color(ColorUtils.interpolateInt(color1.getRed(), color2.getRed(), amount), ColorUtils.interpolateInt(color1.getGreen(), color2.getGreen(), amount), ColorUtils.interpolateInt(color1.getBlue(), color2.getBlue(), amount), ColorUtils.interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static Double interpolate(double oldValue, double newValue, double interpolationValue) {
        return oldValue + (newValue - oldValue) * interpolationValue;
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return ColorUtils.interpolate(oldValue, newValue, (double)((float)interpolationValue)).floatValue();
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return ColorUtils.interpolate(oldValue, newValue, (float)interpolationValue);
    }

    public static Color interpolateColorHue(Color color1, Color color2, float amount) {
        amount = Math.min(1.0f, Math.max(0.0f, amount));
        float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);
        Color resultColor = Color.getHSBColor(ColorUtils.interpolateFloat(color1HSB[0], color2HSB[0], amount), ColorUtils.interpolateFloat(color1HSB[1], color2HSB[1], amount), ColorUtils.interpolateFloat(color1HSB[2], color2HSB[2], amount));
        return new Color(resultColor.getRed(), resultColor.getGreen(), resultColor.getBlue(), ColorUtils.interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static void setColor(Color color) {
        if (color == null) {
            color = Color.white;
        }
        ColorUtils.setColor((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, (float)color.getAlpha() / 255.0f);
    }

    public static void setColor(int color) {
        ColorUtils.setColor(color, (float)(color >> 24 & 0xFF) / 255.0f);
    }

    public static void setColor(int color, float alpha) {
        float r2 = (float)(color >> 16 & 0xFF) / 255.0f;
        float g2 = (float)(color >> 8 & 0xFF) / 255.0f;
        float b2 = (float)(color & 0xFF) / 255.0f;
        RenderSystem.setShaderColor((float)r2, (float)g2, (float)b2, (float)alpha);
    }

    public static int applyOpacity(int color, float alpha) {
        return ColorUtils.rgba((double)ColorUtils.getRed(color), (double)ColorUtils.getGreen(color), (double)ColorUtils.getBlue(color), (float)ColorUtils.getAlpha(color) * alpha / 255.0f);
    }

    public static int reFactorColor(int color, float factor) {
        return ColorUtils.rgba(ColorUtils.extractRedf(color) * factor, ColorUtils.extractGreenf(color) * factor, ColorUtils.extractBluef(color) * factor, ColorUtils.extractAlphaf(color));
    }

    public static float extractRedf(int color) {
        return (float)(color >> 16 & 0xFF) / 255.0f;
    }

    public static int extractRed(int color) {
        return color >> 16 & 0xFF;
    }

    public static float extractBluef(int color) {
        return (float)(color & 0xFF) / 255.0f;
    }

    public static int extractBlue(int color) {
        return color & 0xFF;
    }

    public static float extractGreenf(int color) {
        return (float)(color >> 8 & 0xFF) / 255.0f;
    }

    public static int extractGreen(int color) {
        return color >> 8 & 0xFF;
    }

    public static float extractAlphaf(int color) {
        return (float)(color >> 24 & 0xFF) / 255.0f;
    }

    public static int extractAlpha(int color) {
        return color >> 24 & 0xFF;
    }
}

