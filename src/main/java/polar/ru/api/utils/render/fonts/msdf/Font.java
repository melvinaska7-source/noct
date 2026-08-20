package polar.ru.api.utils.render.fonts.msdf;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import polar.ru.api.QClient;
import polar.ru.api.utils.render.ShaderUtils;
import polar.ru.api.utils.render.fonts.msdf.MsdfFont;

public class Font
implements QClient {
    private static final char FORMATTING_CODE_PREFIX = '§';
    private final MsdfFont font;
    private final float size;

    public Font(MsdfFont font, float size) {
        this.font = font;
        this.size = size;
    }

    public Font(String name, float size) {
        this.font = MsdfFont.builder().atlas(name).data(name).build();
        this.size = size;
    }

    public void drawString(MatrixStack matrixStack, String text, double x2, double y2, int color) {
        this.draw(matrixStack, text, (float)x2, (float)y2, color);
    }

    public void drawString(MatrixStack matrixStack, String text, float x2, float y2, int color) {
        this.draw(matrixStack, text, x2, y2, color);
    }

    public void drawString(String text, float x2, float y2, int color) {
        MatrixStack stack = new MatrixStack();
        this.draw(stack, text, x2, y2, color);
    }

    public void drawCenteredString(MatrixStack matrixStack, String text, double x2, double y2, int color) {
        this.draw(matrixStack, text, (float)(x2 - (double)this.getStringWidth(text) / 2.0), (float)y2, color);
    }

    public void drawCenteredString(MatrixStack matrixStack, String text, float x2, float y2, int color) {
        this.draw(matrixStack, text, x2 - this.getStringWidth(text) / 2.0f, y2, color);
    }

    public void drawRight(MatrixStack matrixStack, String text, double x2, double y2, int color) {
        this.draw(matrixStack, text, (float)(x2 - (double)this.getStringWidth(text)), (float)y2, color);
    }

    public void drawRight(MatrixStack matrixStack, String text, float x2, float y2, int color) {
        this.draw(matrixStack, text, x2 - this.getStringWidth(text), y2, color);
    }

    public void draw(MatrixStack stack, String text, double x2, double y2, int color) {
        this.draw(stack, text, (float)x2, (float)y2, color);
    }

    public void draw(MatrixStack stack, String text, float x2, float y2, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        float localSize = this.size * 0.5f;
        if (!this.hasDrawableGlyphs(text, localSize)) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.fontsMsdf);
        if (shader == null) {
            return;
        }
        this.setupShaderUniforms(shader, color);
        RenderSystem.setShaderTexture(0, this.font.getAtlasIdentifier());
        this.font.setFiltered();
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        this.font.applyGlyphs(matrix, (VertexConsumer)buffer, localSize, text, 0.0f, x2, y2 + 0.8046f * localSize, 0.0f, 255, 255, 255, 255);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.fontsMsdf);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public void drawGradientStringHorizontal(String text, float x2, float y2, int leftColor, int rightColor) {
        MatrixStack stack = new MatrixStack();
        this.drawGradientStringHorizontal(stack, text, x2, y2, leftColor, rightColor);
    }

    public void drawGradientStringHorizontal(MatrixStack stack, String text, float x2, float y2, int leftColor, int rightColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        float totalWidth = this.getStringWidth(text);
        float currentX = x2;
        for (int i2 = 0; i2 < text.length(); ++i2) {
            char c2 = text.charAt(i2);
            String charStr = String.valueOf(c2);
            float charWidth = this.getStringWidth(charStr);
            float progress = totalWidth > 0.0f ? (currentX - x2) / totalWidth : 0.0f;
            int color = Font.interpolateColor(leftColor, rightColor, progress);
            this.draw(stack, charStr, currentX, y2, color);
            currentX += charWidth;
        }
    }

    public void drawGradientStringHorizontal(MatrixStack stack, String text, float x2, float y2, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        float totalWidth = this.getStringWidth(text);
        float currentX = x2;
        for (int i2 = 0; i2 < text.length(); ++i2) {
            char c2 = text.charAt(i2);
            String charStr = String.valueOf(c2);
            float charWidth = this.getStringWidth(charStr);
            float progress = totalWidth > 0.0f ? (currentX - x2) / totalWidth : 0.0f;
            int topColor = Font.interpolateColor(topLeftColor, topRightColor, progress);
            int bottomColor = Font.interpolateColor(bottomLeftColor, bottomRightColor, progress);
            int color = Font.interpolateColor(topColor, bottomColor, 0.5f);
            this.draw(stack, charStr, currentX, y2, color);
            currentX += charWidth;
        }
    }

    public void drawGradientStringVertical(MatrixStack stack, String text, float x2, float y2, int topColor, int bottomColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int color = Font.interpolateColor(topColor, bottomColor, 0.5f);
        this.draw(stack, text, x2, y2, color);
    }

    public void drawStringWithFade(MatrixStack stack, String text, float x2, float y2, float maxWidth, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (maxWidth <= 1.0f) {
            return;
        }
        int originalAlpha = color >>> 24 & 0xFF;
        if (originalAlpha == 0) {
            originalAlpha = 255;
        }
        if (originalAlpha <= 4) {
            return;
        }
        float localSize = this.size * 0.5f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.fontsMsdf);
        if (shader == null) {
            return;
        }
        GlUniform textureSizeUniform = shader.getUniform("TextureSize");
        GlUniform rangeUniform = shader.getUniform("Range");
        GlUniform thicknessUniform = shader.getUniform("Thickness");
        GlUniform edgeStrengthUniform = shader.getUniform("EdgeStrength");
        GlUniform colorUniform = shader.getUniform("Color");
        GlUniform outlineUniform = shader.getUniform("Outline");
        GlUniform outlineThicknessUniform = shader.getUniform("OutlineThickness");
        GlUniform outlineColorUniform = shader.getUniform("OutlineColor");
        if (textureSizeUniform != null) {
            textureSizeUniform.set(this.font.getAtlasWidth(), this.font.getAtlasHeight());
        }
        if (rangeUniform != null) {
            rangeUniform.set(this.font.getRange());
        }
        if (thicknessUniform != null) {
            thicknessUniform.set(0.0f);
        }
        if (edgeStrengthUniform != null) {
            edgeStrengthUniform.set(0.5f);
        }
        if (outlineUniform != null) {
            outlineUniform.set(0);
        }
        if (outlineThicknessUniform != null) {
            outlineThicknessUniform.set(0.0f);
        }
        if (outlineColorUniform != null) {
            outlineColorUniform.set(1.0f, 1.0f, 1.0f, 1.0f);
        }
        RenderSystem.setShaderTexture(0, this.font.getAtlasIdentifier());
        this.font.setFiltered();
        float currentX = x2;
        float fadeZoneWidth = 25.0f;
        float fadeStartX = x2 + maxWidth - fadeZoneWidth;
        for (int i2 = 0; i2 < text.length(); ++i2) {
            String charStr = String.valueOf(text.charAt(i2));
            float charWidth = this.getStringWidth(charStr);
            if (currentX > x2 + maxWidth && i2 > 0) break;
            int finalColor = color;
            if (currentX > fadeStartX) {
                float progressIntoFade = (currentX - fadeStartX) / fadeZoneWidth;
                progressIntoFade = Math.max(0.0f, Math.min(1.0f, progressIntoFade));
                float fadeFactor = (float)Math.cos((double)progressIntoFade * Math.PI / 2.0);
                int newAlpha = (int)((float)originalAlpha * fadeFactor);
                finalColor = color & 0xFFFFFF | newAlpha << 24;
            }
            if ((finalColor >>> 24 & 0xFF) > 4) {
                float[] rgba = this.extractRgba(finalColor);
                if (colorUniform != null) {
                    colorUniform.set(rgba[0], rgba[1], rgba[2], rgba[3]);
                }
                Matrix4f matrix = stack.peek().getPositionMatrix();
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                this.font.applyGlyphs(matrix, (VertexConsumer)buffer, localSize, charStr, 0.0f, currentX, y2 + 0.8046f * localSize, 0.0f, 255, 255, 255, 255);
                RenderSystem.setShader((ShaderProgramKey)ShaderUtils.fontsMsdf);
                BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
            }
            currentX += charWidth;
        }
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public void drawAnimatedGradientStringHorizontal(String text, float x2, float y2, int leftColor, int rightColor, float speed) {
        MatrixStack stack = new MatrixStack();
        this.drawAnimatedGradientStringHorizontal(stack, text, x2, y2, leftColor, rightColor, speed, 1.15f);
    }

    public void drawAnimatedGradientStringHorizontal(MatrixStack stack, String text, float x2, float y2, int leftColor, int rightColor, float speed) {
        this.drawAnimatedGradientStringHorizontal(stack, text, x2, y2, leftColor, rightColor, speed, 1.15f);
    }

    public void drawAnimatedGradientStringHorizontal(String text, float x2, float y2, int leftColor, int rightColor, float speed, float waveScale) {
        MatrixStack stack = new MatrixStack();
        this.drawAnimatedGradientStringHorizontal(stack, text, x2, y2, leftColor, rightColor, speed, waveScale);
    }

    public void drawAnimatedGradientStringHorizontal(MatrixStack stack, String text, float x2, float y2, int leftColor, int rightColor, float speed, float waveScale) {
        if (text == null || text.isEmpty()) {
            return;
        }
        float totalWidth = this.getStringWidth(text);
        float currentX = x2;
        double timeOffset = (double)System.currentTimeMillis() * 0.001 * (double)Math.max(0.01f, speed) % 2.0;
        float safeWaveScale = Math.max(0.01f, waveScale);
        for (int i2 = 0; i2 < text.length(); ++i2) {
            char c2 = text.charAt(i2);
            String charStr = String.valueOf(c2);
            float charWidth = this.getStringWidth(charStr);
            float baseProgress = totalWidth > 0.0f ? (currentX - x2) / totalWidth : 0.0f;
            float animatedProgress = Font.pingPong01(baseProgress * safeWaveScale + (float)timeOffset);
            int color = Font.interpolateColor(leftColor, rightColor, animatedProgress);
            this.draw(stack, charStr, currentX, y2, color);
            currentX += charWidth;
        }
    }

    public void drawStringWithOutline(MatrixStack stack, String text, float x2, float y2, int color, int outlineColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        this.draw(stack, text, x2 - 1.0f, y2, outlineColor);
        this.draw(stack, text, x2 + 1.0f, y2, outlineColor);
        this.draw(stack, text, x2, y2 - 1.0f, outlineColor);
        this.draw(stack, text, x2, y2 + 1.0f, outlineColor);
        this.draw(stack, text, x2, y2, color);
    }

    public void drawStringWithShadow(MatrixStack stack, String text, float x2, float y2, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int shadowColor = 0x55000000;
        this.draw(stack, text, x2 + 1.0f, y2 + 1.0f, shadowColor);
        this.draw(stack, text, x2, y2, color);
    }

    public void drawParagraph(MatrixStack stack, String text, double x2, double y2, int defaultColor) {
        this.drawParagraph(stack, text, (float)x2, (float)y2, defaultColor);
    }

    public void drawParagraph(MatrixStack stack, String text, float x2, float y2, int defaultColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        float localSize = this.size * 0.5f;
        y2 -= 1.5f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.fontsMsdf);
        if (shader == null) {
            return;
        }
        GlUniform textureSizeUniform = shader.getUniform("TextureSize");
        GlUniform rangeUniform = shader.getUniform("Range");
        GlUniform thicknessUniform = shader.getUniform("Thickness");
        GlUniform edgeStrengthUniform = shader.getUniform("EdgeStrength");
        GlUniform colorUniform = shader.getUniform("Color");
        if (textureSizeUniform != null) {
            textureSizeUniform.set(this.font.getAtlasWidth(), this.font.getAtlasHeight());
        }
        if (rangeUniform != null) {
            rangeUniform.set(this.font.getRange());
        }
        if (thicknessUniform != null) {
            thicknessUniform.set(0.0f);
        }
        if (edgeStrengthUniform != null) {
            edgeStrengthUniform.set(0.5f);
        }
        RenderSystem.setShaderTexture(0, this.font.getAtlasIdentifier());
        this.font.setFiltered();
        float currentX = x2;
        int currentColor = defaultColor;
        StringBuilder segment = new StringBuilder();
        for (int i2 = 0; i2 < text.length(); ++i2) {
            char c2 = text.charAt(i2);
            if (c2 == '§' && i2 + 1 < text.length()) {
                char code;
                int newColor;
                if (!segment.isEmpty()) {
                    this.drawSegment(stack, colorUniform, segment.toString(), currentX, y2 + this.font.getBaselineHeight() * localSize, localSize, currentColor);
                    currentX += this.getStringWidth(segment.toString());
                    segment.setLength(0);
                }
                if ((newColor = this.getColorFromCode(code = text.charAt(i2 + 1), defaultColor)) != -1) {
                    currentColor = newColor;
                }
                ++i2;
                continue;
            }
            segment.append(c2);
        }
        if (!segment.isEmpty()) {
            this.drawSegment(stack, colorUniform, segment.toString(), currentX, y2 + 0.8046f * localSize, localSize, currentColor);
        }
        RenderSystem.setShaderTexture((int)0, (int)0);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawSegment(MatrixStack stack, GlUniform colorUniform, String text, float x2, float y2, float size, int color) {
        if (!this.hasDrawableGlyphs(text, size)) {
            return;
        }
        float[] rgba = this.extractRgba(color);
        if (colorUniform != null) {
            colorUniform.set(rgba[0], rgba[1], rgba[2], rgba[3]);
        }
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        this.font.applyGlyphs(matrix, (VertexConsumer)buffer, size, text, 0.0f, x2, y2, 0.0f, 255, 255, 255, 255);
        RenderSystem.setShader((ShaderProgramKey)ShaderUtils.fontsMsdf);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private boolean hasDrawableGlyphs(String text, float renderSize) {
        return text != null && !text.isEmpty() && this.font.getWidth(text, renderSize) > 0.0f;
    }

    private void setupShaderUniforms(ShaderProgram shader, int color) {
        GlUniform textureSizeUniform = shader.getUniform("TextureSize");
        GlUniform rangeUniform = shader.getUniform("Range");
        GlUniform thicknessUniform = shader.getUniform("Thickness");
        GlUniform edgeStrengthUniform = shader.getUniform("EdgeStrength");
        GlUniform colorUniform = shader.getUniform("Color");
        GlUniform outlineUniform = shader.getUniform("Outline");
        GlUniform outlineThicknessUniform = shader.getUniform("OutlineThickness");
        GlUniform outlineColorUniform = shader.getUniform("OutlineColor");
        if (textureSizeUniform != null) {
            textureSizeUniform.set(this.font.getAtlasWidth(), this.font.getAtlasHeight());
        }
        if (rangeUniform != null) {
            rangeUniform.set(this.font.getRange());
        }
        if (thicknessUniform != null) {
            thicknessUniform.set(0.0f);
        }
        if (edgeStrengthUniform != null) {
            edgeStrengthUniform.set(0.5f);
        }
        if (outlineUniform != null) {
            outlineUniform.set(0);
        }
        if (outlineThicknessUniform != null) {
            outlineThicknessUniform.set(0.0f);
        }
        if (outlineColorUniform != null) {
            outlineColorUniform.set(0.0f, 0.0f, 0.0f, 1.0f);
        }
        float[] rgba = this.extractRgba(color);
        if (colorUniform != null) {
            colorUniform.set(rgba[0], rgba[1], rgba[2], rgba[3]);
        }
    }

    private int getColorFromCode(char code, int defaultColor) {
        int alpha = defaultColor >> 24 & 0xFF;
        if (alpha == 0) {
            alpha = 255;
        }
        return switch (code) {
            case '0' -> alpha << 24 | 0;
            case '1' -> alpha << 24 | 0xAA;
            case '2' -> alpha << 24 | 0xAA00;
            case '3' -> alpha << 24 | 0xAAAA;
            case '4' -> alpha << 24 | 0xAA0000;
            case '5' -> alpha << 24 | 0xAA00AA;
            case '6' -> alpha << 24 | 0xFFAA00;
            case '7' -> alpha << 24 | 0xAAAAAA;
            case '8' -> alpha << 24 | 0x555555;
            case '9' -> alpha << 24 | 0x5555FF;
            case 'A', 'a' -> alpha << 24 | 0x55FF55;
            case 'B', 'b' -> alpha << 24 | 0x55FFFF;
            case 'C', 'c' -> alpha << 24 | 0xFF5555;
            case 'D', 'd' -> alpha << 24 | 0xFF55FF;
            case 'E', 'e' -> alpha << 24 | 0xFFFF55;
            case 'F', 'f' -> alpha << 24 | 0xFFFFFF;
            case 'R', 'r' -> defaultColor;
            default -> -1;
        };
    }

    private float[] extractRgba(int color) {
        int a2 = color >> 24 & 0xFF;
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        if (a2 == 0) {
            a2 = 255;
        }
        return new float[]{(float)r2 / 255.0f, (float)g2 / 255.0f, (float)b2 / 255.0f, (float)a2 / 255.0f};
    }

    public static int interpolateColor(int color1, int color2, float progress) {
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        int a1 = color1 >> 24 & 0xFF;
        int r1 = color1 >> 16 & 0xFF;
        int g1 = color1 >> 8 & 0xFF;
        int b1 = color1 & 0xFF;
        int a2 = color2 >> 24 & 0xFF;
        int r2 = color2 >> 16 & 0xFF;
        int g2 = color2 >> 8 & 0xFF;
        int b2 = color2 & 0xFF;
        if (a1 == 0) {
            a1 = 255;
        }
        if (a2 == 0) {
            a2 = 255;
        }
        int a3 = (int)((float)a1 + (float)(a2 - a1) * progress);
        int r3 = (int)((float)r1 + (float)(r2 - r1) * progress);
        int g3 = (int)((float)g1 + (float)(g2 - g1) * progress);
        int b3 = (int)((float)b1 + (float)(b2 - b1) * progress);
        return a3 << 24 | r3 << 16 | g3 << 8 | b3;
    }

    private static float pingPong01(float value) {
        float wrapped = value % 2.0f;
        if (wrapped < 0.0f) {
            wrapped += 2.0f;
        }
        return wrapped > 1.0f ? 2.0f - wrapped : wrapped;
    }

    public float getStringWidth(String text) {
        if (text == null) {
            return 0.0f;
        }
        return this.font.getWidth(this.stripFormattingCodes(text), this.size) / 2.0f;
    }

    public float getWidth(String text) {
        return this.getStringWidth(text);
    }

    public float getHeight() {
        return 0.8046f * this.size * 0.5f;
    }

    public float getFontHeight() {
        return this.getHeight();
    }

    public MsdfFont getFont() {
        return this.font;
    }

    public float getSize() {
        return this.size;
    }

    private String stripFormattingCodes(String text) {
        if (text == null || text.indexOf(167) < 0) {
            return text;
        }
        StringBuilder clean = new StringBuilder(text.length());
        for (int i2 = 0; i2 < text.length(); ++i2) {
            char current = text.charAt(i2);
            if (current == '§' && i2 + 1 < text.length()) {
                ++i2;
                continue;
            }
            clean.append(current);
        }
        return clean.toString();
    }
}

