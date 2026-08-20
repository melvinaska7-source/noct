package polar.ru.api.utils.render.fonts.ttf;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.awt.Font;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import polar.ru.api.utils.render.fonts.ttf.CFont;

public class MCFontRenderer
extends CFont {
    private final int[] colorCode = new int[32];
    protected CFont.CharData[] boldChars = new CFont.CharData[1104];
    protected CFont.CharData[] italicChars = new CFont.CharData[1104];
    protected CFont.CharData[] boldItalicChars = new CFont.CharData[1104];
    protected int texBold;
    protected int texItalic;
    protected int texItalicBold;

    public MCFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
        super(font, antiAlias, fractionalMetrics);
        this.setupBoldItalicIDs();
        for (int index = 0; index < 32; ++index) {
            int noClue = (index >> 3 & 1) * 85;
            int red = (index >> 2 & 1) * 170 + noClue;
            int green = (index >> 1 & 1) * 170 + noClue;
            int blue = (index & 1) * 170 + noClue;
            if (index == 6) {
                red += 85;
            }
            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            this.colorCode[index] = (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
        }
    }

    public float drawStringWithShadow(String text, double x2, double y2, int color) {
        float shadowWidth = this.drawString(text, x2 + 0.5, y2 + 0.5, color, true);
        return Math.max(shadowWidth, this.drawString(text, x2, y2, color, false));
    }

    public float drawGradientString(String text, float x2, float y2, int topColor, int bottomColor) {
        this.checkTexture();
        if (text == null) {
            return 0.0f;
        }
        x2 -= 1.0f;
        if ((topColor & 0xFC000000) == 0) {
            topColor |= 0xFF000000;
        }
        if ((bottomColor & 0xFC000000) == 0) {
            bottomColor |= 0xFF000000;
        }
        float topAlpha = (float)(topColor >> 24 & 0xFF) / 255.0f;
        float topRed = (float)(topColor >> 16 & 0xFF) / 255.0f;
        float topGreen = (float)(topColor >> 8 & 0xFF) / 255.0f;
        float topBlue = (float)(topColor & 0xFF) / 255.0f;
        float botAlpha = (float)(bottomColor >> 24 & 0xFF) / 255.0f;
        float botRed = (float)(bottomColor >> 16 & 0xFF) / 255.0f;
        float botGreen = (float)(bottomColor >> 8 & 0xFF) / 255.0f;
        float botBlue = (float)(bottomColor & 0xFF) / 255.0f;
        double posX = (double)x2 * 2.0;
        double posY = ((double)y2 - 3.0) * 2.0;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        Matrix4f matrix = new Matrix4f();
        matrix.scale(0.5f, 0.5f, 0.5f);
        CFont.CharData[] currentData = this.charData;
        int size = text.length();
        for (int i2 = 0; i2 < size; ++i2) {
            char character = text.charAt(i2);
            if (character >= currentData.length || currentData[character] == null) {
                if (character != ' ' && character != ' ') continue;
                posX += 8.0;
                continue;
            }
            RenderSystem.setShaderTexture((int)0, (int)this.glTextureId);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            CFont.CharData cd = currentData[character];
            float charXPos = cd.storedX;
            float charYPos = cd.storedY;
            float width = cd.width;
            float height = cd.height;
            float u0 = charXPos / 512.0f;
            float v0 = charYPos / 512.0f;
            float u1 = (charXPos + width) / 512.0f;
            float v1 = (charYPos + height) / 512.0f;
            buffer.vertex(matrix, (float)posX, (float)posY, 0.0f).texture(u0, v0).color(topRed, topGreen, topBlue, topAlpha);
            buffer.vertex(matrix, (float)posX, (float)posY + height, 0.0f).texture(u0, v1).color(botRed, botGreen, botBlue, botAlpha);
            buffer.vertex(matrix, (float)posX + width, (float)posY + height, 0.0f).texture(u1, v1).color(botRed, botGreen, botBlue, botAlpha);
            buffer.vertex(matrix, (float)posX + width, (float)posY, 0.0f).texture(u1, v0).color(topRed, topGreen, topBlue, topAlpha);
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
            posX += (double)(cd.width - 8 + this.charOffset);
        }
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        return (float)posX / 2.0f;
    }

    public float drawGradientStringHorizontal(String text, float x2, float y2, int leftColor, int rightColor) {
        if (text == null) {
            return 0.0f;
        }
        x2 -= 1.0f;
        if ((leftColor & 0xFC000000) == 0) {
            leftColor |= 0xFF000000;
        }
        if ((rightColor & 0xFC000000) == 0) {
            rightColor |= 0xFF000000;
        }
        double posX = (double)x2 * 2.0;
        double posY = ((double)y2 - 3.0) * 2.0;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        Matrix4f matrix = new Matrix4f();
        matrix.scale(0.5f, 0.5f, 0.5f);
        CFont.CharData[] currentData = this.charData;
        int size = text.length();
        float totalWidth = (float)this.getStringWidth(text) * 2.0f;
        float currentWidth = 0.0f;
        for (int i2 = 0; i2 < size; ++i2) {
            char character = text.charAt(i2);
            if (character >= currentData.length || currentData[character] == null) {
                if (character != ' ' && character != ' ') continue;
                posX += 8.0;
                currentWidth += 8.0f;
                continue;
            }
            RenderSystem.setShaderTexture((int)0, (int)this.glTextureId);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            CFont.CharData cd = currentData[character];
            float charXPos = cd.storedX;
            float charYPos = cd.storedY;
            float width = cd.width;
            float height = cd.height;
            float charWidth = cd.width - 8 + this.charOffset;
            float u0 = charXPos / 512.0f;
            float v0 = charYPos / 512.0f;
            float u1 = (charXPos + width) / 512.0f;
            float v1 = (charYPos + height) / 512.0f;
            float firstMix = totalWidth <= 0.0f ? 0.0f : currentWidth / totalWidth;
            float lastMix = totalWidth <= 0.0f ? 1.0f : (currentWidth + charWidth) / totalWidth;
            int firstColor = this.colorMix(leftColor, rightColor, firstMix);
            int lastColor = this.colorMix(leftColor, rightColor, lastMix);
            float firstAlpha = (float)(firstColor >> 24 & 0xFF) / 255.0f;
            float firstRed = (float)(firstColor >> 16 & 0xFF) / 255.0f;
            float firstGreen = (float)(firstColor >> 8 & 0xFF) / 255.0f;
            float firstBlue = (float)(firstColor & 0xFF) / 255.0f;
            float lastAlpha = (float)(lastColor >> 24 & 0xFF) / 255.0f;
            float lastRed = (float)(lastColor >> 16 & 0xFF) / 255.0f;
            float lastGreen = (float)(lastColor >> 8 & 0xFF) / 255.0f;
            float lastBlue = (float)(lastColor & 0xFF) / 255.0f;
            buffer.vertex(matrix, (float)posX, (float)posY, 0.0f).texture(u0, v0).color(firstRed, firstGreen, firstBlue, firstAlpha);
            buffer.vertex(matrix, (float)posX, (float)posY + height, 0.0f).texture(u0, v1).color(firstRed, firstGreen, firstBlue, firstAlpha);
            buffer.vertex(matrix, (float)posX + width, (float)posY + height, 0.0f).texture(u1, v1).color(lastRed, lastGreen, lastBlue, lastAlpha);
            buffer.vertex(matrix, (float)posX + width, (float)posY, 0.0f).texture(u1, v0).color(lastRed, lastGreen, lastBlue, lastAlpha);
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
            posX += (double)charWidth;
            currentWidth += charWidth;
        }
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        return (float)posX / 2.0f;
    }

    private int colorMix(int startColor, int endColor, float mix) {
        float startAlpha = (float)(startColor >> 24 & 0xFF) / 255.0f;
        float startRed = (float)(startColor >> 16 & 0xFF) / 255.0f;
        float startGreen = (float)(startColor >> 8 & 0xFF) / 255.0f;
        float startBlue = (float)(startColor & 0xFF) / 255.0f;
        float endAlpha = (float)(endColor >> 24 & 0xFF) / 255.0f;
        float endRed = (float)(endColor >> 16 & 0xFF) / 255.0f;
        float endGreen = (float)(endColor >> 8 & 0xFF) / 255.0f;
        float endBlue = (float)(endColor & 0xFF) / 255.0f;
        int mixAlpha = (int)(((1.0f - mix) * startAlpha + mix * endAlpha) * 255.0f);
        int mixRed = (int)(((1.0f - mix) * startRed + mix * endRed) * 255.0f);
        int mixGreen = (int)(((1.0f - mix) * startGreen + mix * endGreen) * 255.0f);
        int mixBlue = (int)(((1.0f - mix) * startBlue + mix * endBlue) * 255.0f);
        return mixAlpha << 24 | mixRed << 16 | mixGreen << 8 | mixBlue;
    }

    public float drawString(String text, float x2, float y2, int color) {
        return this.drawString(text, x2, y2, color, false);
    }

    public float drawCenteredString(String text, float x2, float y2, int color) {
        return this.drawString(text, x2 - (float)this.getStringWidth(text) / 2.0f, y2, color);
    }

    public float drawCenteredStringWithShadow(String text, float x2, float y2, int color) {
        return this.drawStringWithShadow(text, x2 - (float)this.getStringWidth(text) / 2.0f, y2, color);
    }

    public float drawString(String text, double x2, double y2, int color, boolean shadow) {
        this.checkTexture();
        x2 -= 1.0;
        if (text == null) {
            return 0.0f;
        }
        if (color == 0x20FFFFFF) {
            color = 0xFFFFFF;
        }
        if ((color & 0xFC000000) == 0) {
            color |= 0xFF000000;
        }
        if (shadow) {
            color = (color & 0xFCFCFC) >> 2 | color & new Color(20, 20, 20, 200).getRGB();
        }
        CFont.CharData[] currentData = this.charData;
        float alpha = (float)(color >> 24 & 0xFF) / 255.0f;
        boolean bold = false;
        boolean italic = false;
        boolean strikethrough = false;
        boolean underline = false;
        x2 *= 2.0;
        y2 = (y2 - 3.0) * 2.0;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float)((float)(color >> 16 & 0xFF) / 255.0f), (float)((float)(color >> 8 & 0xFF) / 255.0f), (float)((float)(color & 0xFF) / 255.0f), (float)alpha);
        Matrix4f matrix = new Matrix4f();
        matrix.scale(0.5f, 0.5f, 0.5f);
        int size = text.length();
        int currentTexture = this.glTextureId;
        RenderSystem.setShaderTexture((int)0, (int)currentTexture);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX);
        for (int i2 = 0; i2 < size; ++i2) {
            char character = text.charAt(i2);
            if (String.valueOf(character).equals("§") && i2 < size - 1) {
                int colorIndex = 21;
                try {
                    colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i2 + 1));
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                }
                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    currentTexture = this.glTextureId;
                    currentData = this.charData;
                    if (colorIndex < 0 || colorIndex > 15) {
                        colorIndex = 15;
                    }
                    if (shadow) {
                        colorIndex += 16;
                    }
                    int colorcode = this.colorCode[colorIndex];
                    RenderSystem.setShaderColor((float)((float)(colorcode >> 16 & 0xFF) / 255.0f), (float)((float)(colorcode >> 8 & 0xFF) / 255.0f), (float)((float)(colorcode & 0xFF) / 255.0f), (float)alpha);
                } else if (colorIndex == 17) {
                    bold = true;
                    if (italic) {
                        currentTexture = this.texItalicBold;
                        currentData = this.boldItalicChars;
                    } else {
                        currentTexture = this.texBold;
                        currentData = this.boldChars;
                    }
                } else if (colorIndex == 18) {
                    strikethrough = true;
                } else if (colorIndex == 19) {
                    underline = true;
                } else if (colorIndex == 20) {
                    italic = true;
                    if (bold) {
                        currentTexture = this.texItalicBold;
                        currentData = this.boldItalicChars;
                    } else {
                        currentTexture = this.texItalic;
                        currentData = this.italicChars;
                    }
                } else if (colorIndex == 21) {
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    RenderSystem.setShaderColor((float)((float)(color >> 16 & 0xFF) / 255.0f), (float)((float)(color >> 8 & 0xFF) / 255.0f), (float)((float)(color & 0xFF) / 255.0f), (float)alpha);
                    currentTexture = this.glTextureId;
                    currentData = this.charData;
                }
                ++i2;
                continue;
            }
            if (character >= currentData.length || currentData[character] == null) continue;
            RenderSystem.setShaderTexture((int)0, (int)currentTexture);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);
            this.drawChar(currentData, character, (float)x2, (float)y2, matrix, buffer);
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
            if (strikethrough) {
                this.drawLine(x2, y2 + (double)((float)currentData[character].height / 2.0f), x2 + (double)currentData[character].width - 8.0, y2 + (double)((float)currentData[character].height / 2.0f), 1.0f, matrix);
            }
            if (underline) {
                this.drawLine(x2, y2 + (double)currentData[character].height - 2.0, x2 + (double)currentData[character].width - 8.0, y2 + (double)currentData[character].height - 2.0, 1.0f, matrix);
            }
            x2 += (double)(currentData[character].width - 8 + this.charOffset);
        }
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        return (float)x2 / 2.0f;
    }

    @Override
    public int getStringWidth(String text) {
        int width = 0;
        CFont.CharData[] currentData = this.charData;
        boolean bold = false;
        boolean italic = false;
        int size = text.length();
        for (int i2 = 0; i2 < size; ++i2) {
            char character = text.charAt(i2);
            if (String.valueOf(character).equals("§") && i2 < size - 1) {
                int colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i2 + 1));
                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                } else if (colorIndex == 17) {
                    bold = true;
                    currentData = italic ? this.boldItalicChars : this.boldChars;
                } else if (colorIndex == 20) {
                    italic = true;
                    currentData = bold ? this.boldItalicChars : this.italicChars;
                } else if (colorIndex == 21) {
                    bold = false;
                    italic = false;
                    currentData = this.charData;
                }
                ++i2;
                continue;
            }
            if (character >= currentData.length || currentData[character] == null) continue;
            width += currentData[character].width - 8 + this.charOffset;
        }
        return width / 2;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        this.setupBoldItalicIDs();
    }

    @Override
    public void setAntiAlias(boolean antiAlias) {
        super.setAntiAlias(antiAlias);
        this.setupBoldItalicIDs();
    }

    @Override
    public void setFractionalMetrics(boolean fractionalMetrics) {
        super.setFractionalMetrics(fractionalMetrics);
        this.setupBoldItalicIDs();
    }

    @Override
    public void checkTexture() {
        super.checkTexture();
        if (this.texBold == 0 && MinecraftClient.getInstance() != null && MinecraftClient.getInstance().getTextureManager() != null) {
            this.setupBoldItalicIDs();
        }
    }

    private void setupBoldItalicIDs() {
        if (MinecraftClient.getInstance() == null || MinecraftClient.getInstance().getTextureManager() == null) {
            return;
        }
        CFont boldFont = new CFont(this.font.deriveFont(1), this.antiAlias, this.fractionalMetrics);
        this.texBold = boldFont.getGlTextureId();
        this.boldChars = boldFont.charData;
        CFont italicFont = new CFont(this.font.deriveFont(2), this.antiAlias, this.fractionalMetrics);
        this.texItalic = italicFont.getGlTextureId();
        this.italicChars = italicFont.charData;
        CFont boldItalicFont = new CFont(this.font.deriveFont(3), this.antiAlias, this.fractionalMetrics);
        this.texItalicBold = boldItalicFont.getGlTextureId();
        this.boldItalicChars = boldItalicFont.charData;
    }

    private void drawLine(double x2, double y2, double x1, double y1, float width, Matrix4f matrix) {
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION);
        RenderSystem.lineWidth((float)width);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION);
        buffer.vertex(matrix, (float)x2, (float)y2, 0.0f);
        buffer.vertex(matrix, (float)x1, (float)y1, 0.0f);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    public void drawStringWithOutline(String text, double x2, double y2, int color) {
        this.drawString(text, x2 - 0.5, y2, Color.BLACK.getRGB(), false);
        this.drawString(text, x2 + 0.5, y2, Color.BLACK.getRGB(), false);
        this.drawString(text, x2, y2 - 0.5, Color.BLACK.getRGB(), false);
        this.drawString(text, x2, y2 + 0.5, Color.BLACK.getRGB(), false);
        this.drawString(text, x2, y2, color, false);
    }

    public void drawCenteredStringWithOutline(String text, float x2, float y2, int color) {
        this.drawCenteredString(text, x2 - 0.5f, y2, Color.BLACK.getRGB());
        this.drawCenteredString(text, x2 + 0.5f, y2, Color.BLACK.getRGB());
        this.drawCenteredString(text, x2, y2 - 0.5f, Color.BLACK.getRGB());
        this.drawCenteredString(text, x2, y2 + 0.5f, Color.BLACK.getRGB());
        this.drawCenteredString(text, x2, y2, color);
    }
}

