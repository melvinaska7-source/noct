package polar.ru.api.utils.color.fontscolor;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.Objects;
import net.minecraft.util.math.MathHelper;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.math.MathUtils;

public class ColorRGBA {
    public static final ColorRGBA WHITE = new ColorRGBA(255, 255, 255);
    public static final ColorRGBA BLACK = new ColorRGBA(0, 0, 0);
    public static final ColorRGBA GREEN = new ColorRGBA(0, 255, 0);
    public static final ColorRGBA RED = new ColorRGBA(255, 0, 0);
    public static final ColorRGBA BLUE = new ColorRGBA(0, 0, 255);
    public static final ColorRGBA YELLOW = new ColorRGBA(255, 255, 0);
    public static final ColorRGBA GRAY = new ColorRGBA(88, 87, 93);
    public static final ColorRGBA TRANSPARENT = new ColorRGBA(0, 0, 0, 0);
    private transient float[] hsbValues;
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;
    private static final ByteBuffer PIXEL_BUFFER = ByteBuffer.allocateDirect(4);

    public ColorRGBA(int color) {
        this(ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color), ColorUtils.alpha(color));
    }

    public ColorRGBA(Color color) {
        this(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public ColorRGBA(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    public ColorRGBA(int red, int green, int blue, int alpha) {
        red = MathHelper.clamp((int)red, (int)0, (int)255);
        green = MathHelper.clamp((int)green, (int)0, (int)255);
        blue = MathHelper.clamp((int)blue, (int)0, (int)255);
        alpha = MathHelper.clamp((int)alpha, (int)0, (int)255);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public ColorRGBA(int red, int green, int blue, float alpha) {
        red = MathHelper.clamp((int)red, (int)0, (int)255);
        green = MathHelper.clamp((int)green, (int)0, (int)255);
        blue = MathHelper.clamp((int)blue, (int)0, (int)255);
        alpha = MathHelper.clamp((float)alpha, (float)0.0f, (float)255.0f);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = (int)alpha;
    }

    public int getRGB() {
        int a2 = Math.round(this.clamp(this.alpha));
        int r2 = Math.round(this.clamp(this.red));
        int g2 = Math.round(this.clamp(this.green));
        int b2 = Math.round(this.clamp(this.blue));
        return (a2 & 0xFF) << 24 | (r2 & 0xFF) << 16 | (g2 & 0xFF) << 8 | b2 & 0xFF;
    }

    private int clamp(float value) {
        return (int)Math.max(0.0f, Math.min(255.0f, value));
    }

    public static ColorRGBA fromHex(String hex) {
        String sanitized;
        String string = sanitized = hex.startsWith("#") ? hex.substring(1) : hex;
        if (sanitized.length() != 6 && sanitized.length() != 8) {
            throw new IllegalArgumentException("Hex color must be in the format #RRGGBB or #RRGGBBAA");
        }
        int red = Integer.parseInt(sanitized.substring(0, 2), 16);
        int green = Integer.parseInt(sanitized.substring(2, 4), 16);
        int blue = Integer.parseInt(sanitized.substring(4, 6), 16);
        int alpha = sanitized.length() == 8 ? Integer.parseInt(sanitized.substring(6, 8), 16) : 255;
        return new ColorRGBA(red, green, blue, alpha);
    }

    public static ColorRGBA lerp(ColorRGBA startColor, ColorRGBA endColor, float delta) {
        float clampedDelta = Math.max(0.0f, Math.min(1.0f, delta));
        int r2 = (int)((float)startColor.getRed() + (float)(endColor.getRed() - startColor.getRed()) * clampedDelta);
        int g2 = (int)((float)startColor.getGreen() + (float)(endColor.getGreen() - startColor.getGreen()) * clampedDelta);
        int b2 = (int)((float)startColor.getBlue() + (float)(endColor.getBlue() - startColor.getBlue()) * clampedDelta);
        int a2 = (int)((float)startColor.getAlpha() + (float)(endColor.getAlpha() - startColor.getAlpha()) * clampedDelta);
        return new ColorRGBA(r2, g2, b2, a2);
    }

    public static ColorRGBA fromInt(int colorInt) {
        int alpha = colorInt >> 24 & 0xFF;
        int red = colorInt >> 16 & 0xFF;
        int green = colorInt >> 8 & 0xFF;
        int blue = colorInt & 0xFF;
        return new ColorRGBA(red, green, blue, alpha);
    }

    public ColorRGBA withAlpha(float newAlpha) {
        return new ColorRGBA(this.red, this.green, this.blue, (int)newAlpha);
    }

    public ColorRGBA withAlpha(int newAlpha) {
        return new ColorRGBA(this.red, this.green, this.blue, newAlpha);
    }

    public ColorRGBA mulAlpha(float percent) {
        return this.withAlpha((int)((float)this.alpha * percent));
    }

    public ColorRGBA mix(ColorRGBA color2, float amount) {
        amount = Math.min(1.0f, Math.max(0.0f, amount));
        return new ColorRGBA((int)MathUtils.interpolate((double)this.getRed(), (double)color2.getRed(), (double)amount), (int)MathUtils.interpolate((double)this.getGreen(), (double)color2.getGreen(), (double)amount), (int)MathUtils.interpolate((double)this.getBlue(), (double)color2.getBlue(), (double)amount), (int)MathUtils.interpolate((double)this.getAlpha(), (double)color2.getAlpha(), (double)amount));
    }

    public ColorRGBA darker(float amount) {
        amount = MathHelper.clamp((float)amount, (float)0.0f, (float)1.0f);
        return new ColorRGBA((int)((float)this.red * (1.0f - amount)), (int)((float)this.green * (1.0f - amount)), (int)((float)this.blue * (1.0f - amount)), this.alpha);
    }

    public static ColorRGBA fromHSB(float hue, float saturation, float brightness) {
        if (saturation == 0.0f) {
            int grayValue = (int)(brightness * 255.0f + 0.5f);
            return new ColorRGBA(grayValue, grayValue, grayValue);
        }
        float h2 = (hue - (float)Math.floor(hue)) * 6.0f;
        float f2 = h2 - (float)Math.floor(h2);
        float p2 = brightness * (1.0f - saturation);
        float q2 = brightness * (1.0f - saturation * f2);
        float t2 = brightness * (1.0f - saturation * (1.0f - f2));
        float r2 = 0.0f;
        float g2 = 0.0f;
        float b2 = 0.0f;
        switch ((int)h2) {
            case 0: {
                r2 = brightness;
                g2 = t2;
                b2 = p2;
                break;
            }
            case 1: {
                r2 = q2;
                g2 = brightness;
                b2 = p2;
                break;
            }
            case 2: {
                r2 = p2;
                g2 = brightness;
                b2 = t2;
                break;
            }
            case 3: {
                r2 = p2;
                g2 = q2;
                b2 = brightness;
                break;
            }
            case 4: {
                r2 = t2;
                g2 = p2;
                b2 = brightness;
                break;
            }
            case 5: {
                r2 = brightness;
                g2 = p2;
                b2 = q2;
            }
        }
        return new ColorRGBA((int)(r2 * 255.0f), (int)(g2 * 255.0f), (int)(b2 * 255.0f));
    }

    public float getHue() {
        return this.getHSBValues()[0];
    }

    public float getSaturation() {
        return this.getHSBValues()[2];
    }

    public float getBrightness() {
        return this.getHSBValues()[1];
    }

    private float[] getHSBValues() {
        if (this.hsbValues == null) {
            this.hsbValues = this.calculateHSB();
        }
        return this.hsbValues;
    }

    private float[] calculateHSB() {
        float r2 = (float)this.red / 255.0f;
        float g2 = (float)this.green / 255.0f;
        float b2 = (float)this.blue / 255.0f;
        float maxC = Math.max(r2, Math.max(g2, b2));
        float minC = Math.min(r2, Math.min(g2, b2));
        float delta = maxC - minC;
        float hue = 0.0f;
        if (delta != 0.0f) {
            hue = maxC == r2 ? (g2 - b2) / delta : (maxC == g2 ? (b2 - r2) / delta + 2.0f : (r2 - g2) / delta + 4.0f);
            if ((hue /= 6.0f) < 0.0f) {
                hue += 1.0f;
            }
        }
        float saturation = maxC == 0.0f ? 0.0f : delta / maxC;
        return new float[]{hue, saturation, maxC};
    }

    public ColorRGBA brighter(float amount) {
        amount = MathHelper.clamp((float)amount, (float)0.0f, (float)1.0f);
        return new ColorRGBA((int)((float)this.red + (255.0f - (float)this.red) * amount), (int)((float)this.green + (255.0f - (float)this.green) * amount), (int)((float)this.blue + (255.0f - (float)this.blue) * amount), this.alpha);
    }

    public boolean equals(Object o2) {
        if (this == o2) {
            return true;
        }
        if (o2 != null && this.getClass() == o2.getClass()) {
            ColorRGBA colorRGBA = (ColorRGBA)o2;
            return Float.compare(this.red, colorRGBA.red) == 0 && Float.compare(this.green, colorRGBA.green) == 0 && Float.compare(this.blue, colorRGBA.blue) == 0 && Float.compare(this.alpha, colorRGBA.alpha) == 0;
        }
        return false;
    }

    public float difference(ColorRGBA colorRGBA) {
        return Math.abs(this.getHue() - colorRGBA.getHue()) + Math.abs(this.getBrightness() - colorRGBA.getBrightness()) + Math.abs(this.getSaturation() - colorRGBA.getSaturation());
    }

    public int hashCode() {
        return Objects.hash(this.red, this.green, this.blue, this.alpha);
    }
    public int getRed() {
        return this.red;
    }
    public int getGreen() {
        return this.green;
    }
    public int getBlue() {
        return this.blue;
    }
    public int getAlpha() {
        return this.alpha;
    }
}

