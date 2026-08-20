package polar.ru.api.utils.render.fonts.ttf;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class CFont {
    protected static final int IMG_SIZE = 512;
    protected CharData[] charData = new CharData[1104];
    protected Font font;
    protected boolean antiAlias;
    protected boolean fractionalMetrics;
    protected int fontHeight = -1;
    protected int charOffset = 0;
    protected Identifier textureId;
    protected int glTextureId;
    private static int textureCounter = 0;

    public CFont(Font font, boolean antiAlias, boolean fractionalMetrics) {
        this.font = font;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().getTextureManager() != null) {
            this.setupTexture(font, antiAlias, fractionalMetrics, this.charData);
        }
    }

    public void checkTexture() {
        if (this.glTextureId == 0 && MinecraftClient.getInstance() != null && MinecraftClient.getInstance().getTextureManager() != null) {
            this.setupTexture(this.font, this.antiAlias, this.fractionalMetrics, this.charData);
        }
    }

    protected synchronized void setupTexture(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
        if (MinecraftClient.getInstance() == null || MinecraftClient.getInstance().getTextureManager() == null) {
            return;
        }
        BufferedImage img = this.generateFontImage(font, antiAlias, fractionalMetrics, chars);
        try {
            NativeImage nativeImage = new NativeImage(img.getWidth(), img.getHeight(), false);
            for (int y2 = 0; y2 < img.getHeight(); ++y2) {
                for (int x2 = 0; x2 < img.getWidth(); ++x2) {
                    int argb = img.getRGB(x2, y2);
                    int a2 = argb >> 24 & 0xFF;
                    int r2 = argb >> 16 & 0xFF;
                    int g2 = argb >> 8 & 0xFF;
                    int b2 = argb & 0xFF;
                    nativeImage.setColorArgb(x2, y2, a2 << 24 | r2 << 16 | g2 << 8 | b2);
                }
            }
            NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
            this.glTextureId = texture.getGlId();
            String name = "cfont_" + textureCounter++;
            this.textureId = Identifier.of((String)"customfont", (String)name);
            MinecraftClient.getInstance().getTextureManager().registerTexture(this.textureId, (AbstractTexture)texture);
        }
        catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    protected BufferedImage generateFontImage(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
        BufferedImage bufferedImage = new BufferedImage(512, 512, 2);
        Graphics2D g2 = (Graphics2D)bufferedImage.getGraphics();
        g2.setFont(font);
        g2.setColor(new Color(255, 255, 255, 0));
        g2.fillRect(0, 0, 512, 512);
        g2.setColor(Color.WHITE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        FontMetrics fontMetrics = g2.getFontMetrics();
        int charHeight = 0;
        int positionX = 0;
        int positionY = 1;
        for (int i2 = 0; i2 < chars.length; ++i2) {
            char ch = (char)i2;
            if ((ch <= 'Џ' || ch >= 'ѐ') && ch >= 'Ā') continue;
            CharData charData = new CharData();
            Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(ch), g2);
            charData.width = dimensions.getBounds().width + 8;
            charData.height = dimensions.getBounds().height;
            if (positionX + charData.width >= 512) {
                positionX = 0;
                positionY += charHeight;
                charHeight = 0;
            }
            if (charData.height > charHeight) {
                charHeight = charData.height;
            }
            charData.storedX = positionX;
            charData.storedY = positionY;
            if (charData.height > this.fontHeight) {
                this.fontHeight = charData.height;
            }
            chars[i2] = charData;
            g2.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());
            positionX += charData.width;
        }
        return bufferedImage;
    }

    public void drawChar(CharData[] chars, char c2, float x2, float y2, Matrix4f matrix, BufferBuilder buffer) {
        try {
            if (chars[c2] == null) {
                return;
            }
            this.drawQuad(x2, y2, chars[c2].width, chars[c2].height, chars[c2].storedX, chars[c2].storedY, chars[c2].width, chars[c2].height, matrix, buffer);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    protected void drawQuad(float x2, float y2, float width, float height, float srcX, float srcY, float srcWidth, float srcHeight, Matrix4f matrix, BufferBuilder buffer) {
        float renderSRCX = srcX / 512.0f;
        float renderSRCY = srcY / 512.0f;
        float renderSRCWidth = srcWidth / 512.0f;
        float renderSRCHeight = srcHeight / 512.0f;
        buffer.vertex(matrix, x2 + width, y2, 0.0f).texture(renderSRCX + renderSRCWidth, renderSRCY);
        buffer.vertex(matrix, x2, y2, 0.0f).texture(renderSRCX, renderSRCY);
        buffer.vertex(matrix, x2, y2 + height, 0.0f).texture(renderSRCX, renderSRCY + renderSRCHeight);
        buffer.vertex(matrix, x2, y2 + height, 0.0f).texture(renderSRCX, renderSRCY + renderSRCHeight);
        buffer.vertex(matrix, x2 + width, y2 + height, 0.0f).texture(renderSRCX + renderSRCWidth, renderSRCY + renderSRCHeight);
        buffer.vertex(matrix, x2 + width, y2, 0.0f).texture(renderSRCX + renderSRCWidth, renderSRCY);
    }

    public int getStringHeight(String text) {
        return this.getFontHeight();
    }

    public int getFontHeight() {
        return (this.fontHeight - 8) / 2;
    }

    public int getStringWidth(String text) {
        int width = 0;
        for (char c2 : text.toCharArray()) {
            if (c2 >= this.charData.length || this.charData[c2] == null) continue;
            width += this.charData[c2].width - 8 + this.charOffset;
        }
        return width / 2;
    }

    public void setAntiAlias(boolean antiAlias) {
        if (this.antiAlias != antiAlias) {
            this.antiAlias = antiAlias;
            this.setupTexture(this.font, antiAlias, this.fractionalMetrics, this.charData);
        }
    }

    public void setFractionalMetrics(boolean fractionalMetrics) {
        if (this.fractionalMetrics != fractionalMetrics) {
            this.fractionalMetrics = fractionalMetrics;
            this.setupTexture(this.font, this.antiAlias, fractionalMetrics, this.charData);
        }
    }

    public void setFont(Font font) {
        this.font = font;
        this.setupTexture(font, this.antiAlias, this.fractionalMetrics, this.charData);
    }
    public Font getFont() {
        return this.font;
    }
    public boolean isAntiAlias() {
        return this.antiAlias;
    }
    public boolean isFractionalMetrics() {
        return this.fractionalMetrics;
    }
    public Identifier getTextureId() {
        return this.textureId;
    }
    public int getGlTextureId() {
        return this.glTextureId;
    }

    protected static class CharData {
        public int width;
        public int height;
        public int storedX;
        public int storedY;

        protected CharData() {
        }
    }
}

