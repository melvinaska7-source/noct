package polar.ru.api.utils.render.fonts.msdf;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

public final class MsdfGlyph {
    private final int code;
    private final float minU;
    private final float maxU;
    private final float minV;
    private final float maxV;
    private final float advance;
    private final float planeLeft;
    private final float planeTop;
    private final float width;
    private final float height;

    public MsdfGlyph(int unicode, float advance, float planeLeft, float planeTop, float planeRight, float planeBottom, float atlasLeft, float atlasTop, float atlasRight, float atlasBottom, float atlasWidth, float atlasHeight) {
        this.code = unicode;
        this.advance = advance;
        if (atlasLeft != 0.0f || atlasRight != 0.0f || atlasTop != 0.0f || atlasBottom != 0.0f) {
            this.minU = atlasLeft / atlasWidth;
            this.maxU = atlasRight / atlasWidth;
            this.minV = 1.0f - atlasTop / atlasHeight;
            this.maxV = 1.0f - atlasBottom / atlasHeight;
        } else {
            this.minU = 0.0f;
            this.maxU = 0.0f;
            this.minV = 0.0f;
            this.maxV = 0.0f;
        }
        this.planeLeft = planeLeft;
        this.planeTop = planeTop;
        this.width = planeRight - planeLeft;
        this.height = planeTop - planeBottom;
    }

    public float apply(Matrix4f matrix, VertexConsumer consumer, float size, float x2, float y2, float z2, int red, int green, int blue, int alpha) {
        if (this.minU != this.maxU && this.minV != this.maxV && this.width > 0.0f && this.height > 0.0f) {
            float rx1 = x2 + this.planeLeft * size;
            float rx2 = rx1 + this.width * size;
            float ry1 = y2 - this.planeTop * size;
            float ry2 = ry1 + this.height * size;
            consumer.vertex(matrix, rx1, ry1, z2).texture(this.minU, this.minV).color(red, green, blue, alpha);
            consumer.vertex(matrix, rx1, ry2, z2).texture(this.minU, this.maxV).color(red, green, blue, alpha);
            consumer.vertex(matrix, rx2, ry2, z2).texture(this.maxU, this.maxV).color(red, green, blue, alpha);
            consumer.vertex(matrix, rx2, ry1, z2).texture(this.maxU, this.minV).color(red, green, blue, alpha);
        }
        return this.advance * size;
    }

    public float getWidth(float size) {
        return this.advance * size;
    }

    public int getCharCode() {
        return this.code;
    }
}

