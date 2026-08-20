package polar.ru.api.utils.render.fonts.ttf;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Font;
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
import polar.ru.api.utils.render.fonts.ttf.MCFontRenderer;

public class GradientFontRenderer
extends MCFontRenderer {
    public GradientFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
        super(font, antiAlias, fractionalMetrics);
    }

    public int drawGradientString(String text, float x2, float y2, int topColor, int bottomColor, boolean dropShadow, boolean horizontal) {
        int i2;
        if (dropShadow) {
            i2 = this.renderGradientString(text, x2 + 1.0f, y2 + 1.0f, topColor, bottomColor, true, horizontal);
            i2 = Math.max(i2, this.renderGradientString(text, x2, y2, topColor, bottomColor, false, horizontal));
        } else {
            i2 = this.renderGradientString(text, x2, y2, topColor, bottomColor, false, horizontal);
        }
        return i2;
    }

    private int renderGradientString(String text, float x2, float y2, int startColor, int endColor, boolean dropShadow, boolean horizontal) {
        if (text == null) {
            return 0;
        }
        if ((startColor & 0xFC000000) == 0) {
            startColor |= 0xFF000000;
        }
        if ((endColor & 0xFC000000) == 0) {
            endColor |= 0xFF000000;
        }
        if (dropShadow) {
            startColor = (startColor & 0xFCFCFC) >> 2 | startColor & 0xFF000000;
            endColor = (endColor & 0xFCFCFC) >> 2 | endColor & 0xFF000000;
        }
        float posX = x2;
        float posY = y2;
        return this.renderGradientStringAtPos(text, posX, posY, dropShadow, startColor, endColor, horizontal);
    }

    private int renderGradientStringAtPos(String text, float posX, float posY, boolean shadow, int startColor, int endColor, boolean horizontal) {
        float totalWidth = this.getStringWidth(text);
        float currentCountWidth = 0.0f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f matrix = new Matrix4f();
        for (int i2 = 0; i2 < text.length(); ++i2) {
            char c0 = text.charAt(i2);
            if (c0 == ' ' || c0 == ' ') {
                posX += 4.0f;
                continue;
            }
            if (c0 >= this.charData.length || this.charData[c0] == null) continue;
            float charWidth = this.charData[c0].width - 8 + this.charOffset;
            if (horizontal) {
                float firstMix = currentCountWidth / totalWidth;
                float lastMix = (currentCountWidth + charWidth) / totalWidth;
                int firstColor = this.colorMix(startColor, endColor, firstMix);
                int lastColor = this.colorMix(startColor, endColor, lastMix);
                this.renderGradientChar(c0, posX, posY, firstColor, lastColor, true, matrix);
                currentCountWidth += charWidth;
            } else {
                this.renderGradientChar(c0, posX, posY, startColor, endColor, false, matrix);
            }
            posX += charWidth;
        }
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        return (int)posX;
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

    private void renderGradientChar(char ch, float posX, float posY, int startColor, int endColor, boolean horizontal, Matrix4f matrix) {
        if (ch >= this.charData.length || this.charData[ch] == null) {
            return;
        }
        float startAlpha = (float)(startColor >> 24 & 0xFF) / 255.0f;
        float startRed = (float)(startColor >> 16 & 0xFF) / 255.0f;
        float startGreen = (float)(startColor >> 8 & 0xFF) / 255.0f;
        float startBlue = (float)(startColor & 0xFF) / 255.0f;
        float endAlpha = (float)(endColor >> 24 & 0xFF) / 255.0f;
        float endRed = (float)(endColor >> 16 & 0xFF) / 255.0f;
        float endGreen = (float)(endColor >> 8 & 0xFF) / 255.0f;
        float endBlue = (float)(endColor & 0xFF) / 255.0f;
        CFont.CharData charData = this.charData[ch];
        float charXPos = charData.storedX;
        float charYPos = charData.storedY;
        int charWidth = charData.width;
        float width = (float)charWidth - 0.01f;
        float u0 = charXPos / 512.0f;
        float v0 = charYPos / 512.0f;
        float u1 = (charXPos + width - 1.0f) / 512.0f;
        float v1 = (charYPos + 7.99f) / 512.0f;
        RenderSystem.setShaderTexture((int)0, (int)this.glTextureId);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        if (horizontal) {
            buffer.vertex(matrix, posX, posY, 0.0f).texture(u0, v0).color(startRed, startGreen, startBlue, startAlpha);
            buffer.vertex(matrix, posX, posY + 7.99f, 0.0f).texture(u0, v1).color(startRed, startGreen, startBlue, startAlpha);
            buffer.vertex(matrix, posX + width - 1.0f, posY + 7.99f, 0.0f).texture(u1, v1).color(endRed, endGreen, endBlue, endAlpha);
            buffer.vertex(matrix, posX + width - 1.0f, posY, 0.0f).texture(u1, v0).color(endRed, endGreen, endBlue, endAlpha);
        } else {
            buffer.vertex(matrix, posX, posY, 0.0f).texture(u0, v0).color(startRed, startGreen, startBlue, startAlpha);
            buffer.vertex(matrix, posX, posY + 7.99f, 0.0f).texture(u0, v1).color(endRed, endGreen, endBlue, endAlpha);
            buffer.vertex(matrix, posX + width - 1.0f, posY + 7.99f, 0.0f).texture(u1, v1).color(endRed, endGreen, endBlue, endAlpha);
            buffer.vertex(matrix, posX + width - 1.0f, posY, 0.0f).texture(u1, v0).color(startRed, startGreen, startBlue, startAlpha);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }
}

