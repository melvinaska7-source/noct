package zov.alphadlc.module.list.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityType;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ColorSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.render.providers.ResourceProvider;

@ModuleInformation(moduleName = "Chams", moduleDesc = "Рисует игроков моделькой как в Totem Angel", moduleCategory = ModuleCategory.RENDER)
public class Chams extends Module {
    private static final ShaderProgramKey FILL_SHADER = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("chams_fill"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
    private static final ShaderProgramKey WAVE_SHADER = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("chams_wave"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private static Chams instance;

    private final ModeSetting mode = new ModeSetting("Режим", "Vanilla", "Vanilla", "Animated Fill", "Wave");
    private final ColorSetting color = new ColorSetting("Цвет", 0xFF7657FF);
    private final SliderSetting alpha = new SliderSetting("Прозрачность", 0.35f, 0.05f, 1.0f, 0.05f);
    private final SliderSetting shaderAlpha = new SliderSetting("Прозрачность шейдера", 0.35f, 0.05f, 1.0f, 0.05f)
            .setVisible(() -> mode.is("Animated Fill") || mode.is("Wave"));
    private final SliderSetting brightness = new SliderSetting("Яркость", 0.6f, 0.1f, 1.0f, 0.05f);
    private final SliderSetting shaderSpeed = new SliderSetting("Скорость", 1.0f, 0.1f, 3.0f, 0.05f)
            .setVisible(() -> !mode.is("Vanilla"));
    private final SliderSetting shaderScale = new SliderSetting("Масштаб", 1.35f, 0.5f, 3.0f, 0.05f)
            .setVisible(() -> mode.is("Animated Fill"));
    private final SliderSetting lineWidth = new SliderSetting("Толщина линий", 1.0f, 0.1f, 3.0f, 0.05f);
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", true);
    private final BooleanSetting glow = new BooleanSetting("Свечение", true);
    private final BooleanSetting blink = new BooleanSetting("Мигать", false);
    private final SliderSetting glowIntensity = new SliderSetting("Сила свечения", 1.5f, 0.5f, 4.0f, 0.1f);
    private final SliderSetting glowLayers = new SliderSetting("Слои свечения", 3.0f, 1.0f, 6.0f, 1.0f);

    @SuppressWarnings("unchecked")
    public Chams() {
        instance = this;
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityType == EntityType.PLAYER) {
                registrationHelper.register(new ChamsRenderer(
                        (FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) entityRenderer));
            }
        });
    }

    static Chams getInstance() {
        return instance;
    }

    boolean isMode(String value) {
        return mode.is(value);
    }

    int getColor() {
        return color.getValue();
    }

    float getAlpha() {
        return alpha.getFloatValue();
    }

    float getShaderAlpha() {
        return shaderAlpha.getFloatValue();
    }

    float getBrightness() {
        return brightness.getFloatValue();
    }

    float getShaderSpeed() {
        return shaderSpeed.getFloatValue();
    }

    float getShaderScale() {
        return shaderScale.getFloatValue();
    }

    float getLineWidth() {
        return lineWidth.getFloatValue();
    }

    boolean isThroughWalls() {
        return throughWalls.getValue();
    }

    boolean hasGlow() {
        return glow.getValue();
    }

    boolean isBlinking() {
        return blink.getValue();
    }

    float getGlowIntensity() {
        return glowIntensity.getFloatValue();
    }

    int getGlowLayers() {
        return Math.max(1, glowLayers.getIntValue());
    }

    static ShaderProgram bindAnimatedFill(float time, float speed, float scale, float glowStrength) {
        ShaderProgram shader = RenderSystem.setShader(FILL_SHADER);
        setUniform(shader, "time", time);
        setUniform(shader, "speedX", 0.22f * speed);
        setUniform(shader, "speedY", 0.15f * speed);
        setUniform(shader, "scale", scale);
        setUniform(shader, "density", 1.15f);
        setUniform(shader, "glowStrength", glowStrength);
        return shader;
    }

    static ShaderProgram bindWave(float time) {
        ShaderProgram shader = RenderSystem.setShader(WAVE_SHADER);
        setUniform(shader, "Time", time);
        return shader;
    }

    private static void setUniform(ShaderProgram shader, String name, float value) {
        if (shader != null && shader.getUniform(name) != null) {
            shader.getUniform(name).set(value);
        }
    }
}
