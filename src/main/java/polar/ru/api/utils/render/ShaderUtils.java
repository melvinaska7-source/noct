package polar.ru.api.utils.render;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import polar.ru.api.QClient;

public final class ShaderUtils
implements QClient {
    public static final java.util.List<ShaderProgramKey> ALL_SHADERS = new java.util.ArrayList<>();

    public static final ShaderProgramKey roundedRect = ShaderUtils.register("rect", "rounded_rect", VertexFormats.POSITION_COLOR);
    public static final ShaderProgramKey roundedRectOutline = ShaderUtils.register("rect", "rounded_rect_outline", VertexFormats.POSITION_COLOR);
    public static final ShaderProgramKey ringArc = ShaderUtils.register("ring_arc", "ring_arc", VertexFormats.POSITION_COLOR);
    public static final ShaderProgramKey roundedTexture = ShaderUtils.register("texture", "texture_rect", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey liquidGlass = ShaderUtils.register("liquidglass", "liquid", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey kawaseDown = ShaderUtils.register("kawase_down", "kawase_down", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey kawaseUp = ShaderUtils.register("kawase_up", "kawase_up", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey gradientRect = ShaderUtils.register("gradient_rect", "gradient", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shadowRect = ShaderUtils.register("shadow_rect", "shadow", VertexFormats.POSITION_TEXTURE);
    public static final ShaderProgramKey shadow6Rect = ShaderUtils.register("shadow6", "shadow", VertexFormats.POSITION_TEXTURE);
    public static final ShaderProgramKey fontsMsdf = ShaderUtils.register("fonts", "fonts", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey face = ShaderUtils.register("face", "face", VertexFormats.POSITION_TEXTURE);
    public static final ShaderProgramKey gradient6Rect = ShaderUtils.register("gradient6", "gradient", VertexFormats.POSITION_COLOR);
    public static final ShaderProgramKey scanEffect = ShaderUtils.register("sonar", "scan_effect", VertexFormats.POSITION_TEXTURE);
    public static final ShaderProgramKey sonar = scanEffect;
    public static final ShaderProgramKey blockOverlay = ShaderUtils.register("blockoverlay", "block_overlay", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey blockOverlayBalatro = ShaderUtils.register("blockoverlay", "block_overlay_balatro", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey blockOverlayPlasma = ShaderUtils.register("blockoverlay", "block_overlay_plasma", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey chamsFill = ShaderUtils.register("chams", "chams_fill", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsMaskDiff = ShaderUtils.register("hands", "hands_mask_diff", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsOverlay = ShaderUtils.register("hands", "hands_overlay", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsGlow = ShaderUtils.register("hands", "hands_glow", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsKawaseDown = ShaderUtils.register("hands", "hands_kawase_down", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsKawaseUp = ShaderUtils.register("hands", "hands_kawase_up", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderHandsSmoke = ShaderUtils.register("hands", "hands_smoke", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderEspGlow = ShaderUtils.register("shaderesp", "glow", VertexFormats.POSITION_TEXTURE_COLOR);
    public static final ShaderProgramKey shaderEspFill = ShaderUtils.register("shaderesp", "fill", VertexFormats.POSITION_TEXTURE_COLOR);

    private static ShaderProgramKey register(String shaderNamePackage, String shaderName, VertexFormat vertexFormat) {
        ShaderProgramKey shader = new ShaderProgramKey(Identifier.of((String)"polar", (String)("core/" + shaderNamePackage + "/" + shaderName)), vertexFormat, Defines.EMPTY);
        ALL_SHADERS.add(shader);
        return shader;
    }
    private ShaderUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

