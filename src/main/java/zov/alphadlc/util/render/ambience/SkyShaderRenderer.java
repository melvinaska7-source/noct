package zov.alphadlc.util.render.ambience;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import zov.alphadlc.module.list.render.Ambience;
import zov.alphadlc.util.render.providers.ResourceProvider;

/** Draws the selected procedural shader on an inward-facing sky cube. */
public final class SkyShaderRenderer {
    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final ShaderProgramKey SKY = key("sky");
    private static final ShaderProgramKey AURORA = key("sky_aurora");
    private static final ShaderProgramKey NEBULA = key("sky_nebula");
    private static final ShaderProgramKey MATRIX = key("sky_matrix");
    private static final ShaderProgramKey PLASMA = key("sky_plasma");

    private static volatile Config config;

    private SkyShaderRenderer() {
    }

    private static ShaderProgramKey key(String name) {
        return new ShaderProgramKey(ResourceProvider.getShaderIdentifier(name), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    }

    public static void updateConfig(Ambience module) {
        if (module == null) return;
        int primary = module.getSkyShaderColor();
        float r = ((primary >> 16) & 0xFF) / 255.0f;
        float g = ((primary >> 8) & 0xFF) / 255.0f;
        float b = (primary & 0xFF) / 255.0f;
        int fog = module.getFogColor();
        float fogR = ((fog >> 16) & 0xFF) / 255.0f;
        float fogG = ((fog >> 8) & 0xFF) / 255.0f;
        float fogB = (fog & 0xFF) / 255.0f;
        config = new Config(module.getSkyShaderMode(), primary, r, g, b, fog, fogR, fogG, fogB,
                module.getFogDensity(), module.showStars.getValue(), module.getStarDensity(),
                module.getNebulaStrength(), module.getPlasmaScale(), module.getPlasmaSpeed());
    }

    public static void render(Ambience module) {
        if (module == null || !module.isEnabled() || !module.isSkyShaderEnabled()) return;
        Config c = config;
        if (c == null || !c.mode.equals(module.getSkyShaderMode()) || c.color != module.getSkyShaderColor()
                || c.fogColor != module.getFogColor() || c.fogDensity != module.getFogDensity()) {
            updateConfig(module);
            c = config;
        }
        if (c == null) return;

        ShaderProgramKey key = switch (c.mode) {
            case "Aurora" -> AURORA;
            case "Nebula" -> NEBULA;
            case "Matrix" -> MATRIX;
            case "Plasma" -> PLASMA;
            default -> SKY;
        };
        ShaderProgram shader = RenderSystem.setShader(key);
        if (shader == null) return;

        float time = (System.currentTimeMillis() - module.getShaderStartTime()) / 1000.0f;
        set(shader, "AnimTime", time);
        set(shader, "iTime", time);
        set(shader, "SkyMode", genericMode(c.mode));
        set(shader, "StarDensity", c.showStars ? c.starDensity : 0.0f);
        set(shader, "ShowStars", c.showStars ? 1.0f : 0.0f);
        set(shader, "uShowStars", c.showStars ? 1 : 0);
        set(shader, "NebulaStrength", c.nebulaStrength);
        set(shader, "NebIntensity", 0.8f);
        set(shader, "PlasmaScale", c.plasmaScale);
        set(shader, "PlasmaSpeed", c.plasmaSpeed);
        set(shader, "uScale", c.plasmaScale);
        set(shader, "uSpeed", c.plasmaSpeed);
        set(shader, "uIntensity", 1.0f);
        set(shader, "SkyFogColor", c.fogR, c.fogG, c.fogB);
        set(shader, "FogDensity", c.fogDensity);

        applyColor(shader, c);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        drawSkyCube();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private static void applyColor(ShaderProgram shader, Config c) {
        // Dedicated shaders use uColor; the shared sky shader uses the palette uniforms.
        set(shader, "uColor", c.r, c.g, c.b);
        set(shader, "SkyZenith", c.r * 0.10f, c.g * 0.10f, c.b * 0.18f + 0.02f);
        set(shader, "SkyHorizon", c.r * 0.45f + 0.04f, c.g * 0.45f + 0.04f, c.b * 0.45f + 0.06f);
        set(shader, "NebColor1", c.r, c.g, c.b);
        set(shader, "NebColor2", c.b, Math.min(1.0f, c.r + 0.25f), Math.min(1.0f, c.g + 0.20f));
        set(shader, "StarColor", 0.90f, 0.94f, 1.0f);
    }

    private static float genericMode(String mode) {
        return switch (mode) {
            case "Cosmic Veil" -> 2.0f;
            case "Deep Space" -> 3.0f;
            case "Void" -> 4.0f;
            default -> 0.0f;
        };
    }

    private static void drawSkyCube() {
        float s = 100.0f;
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        face(b, -s, -s, s, s, -s, s, s, s, s, -s, s, s);
        face(b, s, -s, -s, -s, -s, -s, -s, s, -s, s, s, -s);
        face(b, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s);
        face(b, s, -s, s, s, -s, -s, s, s, -s, s, s, s);
        face(b, -s, s, s, s, s, s, s, s, -s, -s, s, -s);
        face(b, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private static void face(BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4) {
        b.vertex(x1, y1, z1).color(0xFFFFFFFF);
        b.vertex(x2, y2, z2).color(0xFFFFFFFF);
        b.vertex(x3, y3, z3).color(0xFFFFFFFF);
        b.vertex(x4, y4, z4).color(0xFFFFFFFF);
    }

    private static void set(ShaderProgram shader, String name, float value) {
        if (shader.getUniform(name) != null) shader.getUniform(name).set(value);
    }

    private static void set(ShaderProgram shader, String name, int value) {
        if (shader.getUniform(name) != null) shader.getUniform(name).set(value);
    }

    private static void set(ShaderProgram shader, String name, float x, float y, float z) {
        if (shader.getUniform(name) != null) shader.getUniform(name).set(x, y, z);
    }

    private record Config(String mode, int color, float r, float g, float b,
                          int fogColor, float fogR, float fogG, float fogB, float fogDensity,
                          boolean showStars, float starDensity, float nebulaStrength,
                          float plasmaScale, float plasmaSpeed) {
    }
}
