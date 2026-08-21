package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ColorSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.render.hands.GlassHandsRenderer;




@ModuleInformation(moduleName = "Hands", moduleDesc = "Красивые Шейдеры на руки", moduleCategory = ModuleCategory.RENDER)
public class    GlassHands extends Module {

    public final ModeSetting mode = new ModeSetting("Режим", "Блюр",  "Блюр", "Стекло", "Заливка", "Волна");

    public final SliderSetting waveSpeed = new SliderSetting("Скорость волн", 1.2, 0.1, 5.0, 0.1)
            .setVisible(() -> mode.is("Красивый"));
    public final SliderSetting waveScale = new SliderSetting("Частота волн", 1.0, 1.0, 3.0, 0.1)
            .setVisible(() -> mode.is("Красивый"));

    public final SliderSetting outline = new SliderSetting("Ширина обводки", 1.2, 0.01, 5.0, 0.01)
            .setVisible(() -> !mode.is("Блюр") && !mode.is("Обводка") && !mode.is("Стекло") && !mode.is("Заливка") && !mode.is("Волна"));
    public final SliderSetting glow    = new SliderSetting("Сила свечения",  1.0, 0.0, 5.0, 0.01)
            .setVisible(() -> !mode.is("Блюр") && !mode.is("Обводка") && !mode.is("Стекло") && !mode.is("Заливка") && !mode.is("Волна"));
    public final SliderSetting fill    = new SliderSetting("Заливка",        0.6, 0.0, 1.0, 0.01)
            .setVisible(() -> !mode.is("Блюр") && !mode.is("Обводка") && !mode.is("Стекло") && !mode.is("Заливка") && !mode.is("Волна"));
    public final SliderSetting alpha   = new SliderSetting("Прозрачность",   1.0, 0.0, 1.0, 0.05)
            .setVisible(() -> !mode.is("Блюр") && !mode.is("Обводка") && !mode.is("Стекло") && !mode.is("Заливка") && !mode.is("Волна"));
    public final BooleanSetting glowItemColor = new BooleanSetting("Цвет предмета", false)
            .setVisible(() -> mode.is("Свечение"));

    public final SliderSetting blurStrength = new SliderSetting("Сила блюра", 4.0, 1.0, 8.0, 1.0)
            .setVisible(() -> mode.is("Блюр"));
    public final SliderSetting blurTint     = new SliderSetting("Оттенок", 0.3, 0.0, 1.0, 0.05)
            .setVisible(() -> mode.is("Блюр"));
    public final BooleanSetting blurRainbow = new BooleanSetting("Радужный цвет", false)
            .setVisible(() -> mode.is("Блюр"));
    public final SliderSetting blurRainbowSpeed = new SliderSetting("Скорость радуги", 1.0, 0.1, 5.0, 0.1)
            .setVisible(() -> mode.is("Блюр") && blurRainbow.getValue());

    // "Шлейф" mode settings
    public final SliderSetting trailIntensity = new SliderSetting("Яркость шлейфа", 0.8, 0.3, 3.0, 0.1)
            .setVisible(() -> mode.is("Шлейф"));
    public final SliderSetting trailGlowSize  = new SliderSetting("Размер свечения", 2.0, 0.5, 5.0, 0.1)
            .setVisible(() -> mode.is("Шлейф"));
    public final SliderSetting trailBlur      = new SliderSetting("Размытие", 6.0, 1.0, 10.0, 1.0)
            .setVisible(() -> mode.is("Шлейф"));
    public final BooleanSetting rainbow       = new BooleanSetting("Радужный цвет", false)
            .setVisible(() -> mode.is("Шлейф"));
    public final SliderSetting rainbowSpeed   = new SliderSetting("Скорость радуги", 1.0, 0.1, 5.0, 0.1)
            .setVisible(() -> mode.is("Шлейф") && rainbow.getValue());

    // Trail mode settings (matching the reference shader uniforms)
    public final SliderSetting trailIntensityM  = new SliderSetting("Интенсивность",       0.72, 0.1,  1.5,  0.05)
            .setVisible(() -> mode.is("Trail"));
    public final SliderSetting trailSpeed       = new SliderSetting("Скорость",             1.05, 0.2,  3.0,  0.05)
            .setVisible(() -> mode.is("Trail"));
    public final SliderSetting trailLength      = new SliderSetting("Длина шлейфа",         0.32, 0.1,  1.0,  0.05)
            .setVisible(() -> mode.is("Trail"));
    public final SliderSetting trailSoftness    = new SliderSetting("Мягкость",             1.05, 0.4,  2.0,  0.05)
            .setVisible(() -> mode.is("Trail"));
    public final SliderSetting trailBlurRadius  = new SliderSetting("Размытие",             1.05, 0.2,  2.5,  0.05)
            .setVisible(() -> mode.is("Trail"));
    public final SliderSetting trailSmoke       = new SliderSetting("Дым",                  0.22, 0.0,  0.8,  0.05)
            .setVisible(() -> mode.is("Trail"));
    public final SliderSetting trailAttack      = new SliderSetting("Удар",                 0.58, 0.0,  1.0,  0.05)
            .setVisible(() -> mode.is("Trail"));
    public final SliderSetting trailCamera      = new SliderSetting("Следование камеры",    0.45, 0.0,  1.5,  0.05)
            .setVisible(() -> mode.is("Trail"));
    public final BooleanSetting trailItemColor  = new BooleanSetting("Цвет предмета", false)
            .setVisible(() -> mode.is("Trail"));

    // Glass mode settings
    public final SliderSetting glassMixFactor = new SliderSetting("Смешивание", 0.0, 0.0, 1.0, 0.01)
            .setVisible(() -> mode.is("Стекло"));
    public final BooleanSetting glassGlowEnabled = new BooleanSetting("Глов", true)
            .setVisible(() -> mode.is("Стекло"));
    public final SliderSetting glassGlowRadius = new SliderSetting("Радиус глова", 3.0, 1.0, 6.0, 1.0)
            .setVisible(() -> mode.is("Стекло") && glassGlowEnabled.getValue());
    public final BooleanSetting glassOuterGlow = new BooleanSetting("Внешний глов", true)
            .setVisible(() -> mode.is("Стекло") && glassGlowEnabled.getValue());
    public final SliderSetting glassGlowExposure = new SliderSetting("Яркость глова", 2.0, 0.5, 5.0, 0.1)
            .setVisible(() -> mode.is("Стекло") && glassGlowEnabled.getValue() && glassOuterGlow.getValue());
    public final ColorSetting glassGlowColor1 = new ColorSetting("Цвет глова 1", new java.awt.Color(138, 152, 255, 255).getRGB())
            .setVisible(() -> mode.is("Стекло") && glassGlowEnabled.getValue());
    public final ColorSetting glassGlowColor2 = new ColorSetting("Цвет глова 2", new java.awt.Color(255, 107, 172, 255).getRGB())
            .setVisible(() -> mode.is("Стекло") && glassGlowEnabled.getValue());
    public final BooleanSetting glassRainbow = new BooleanSetting("Радужный цвет", false)
            .setVisible(() -> mode.is("Стекло") && glassGlowEnabled.getValue());
    public final SliderSetting glassRainbowSpeed = new SliderSetting("Скорость радуги", 1.0, 0.1, 5.0, 0.1)
            .setVisible(() -> mode.is("Стекло") && glassGlowEnabled.getValue() && glassRainbow.getValue());

    // Plasma mode settings
    public final SliderSetting plasmaSpeed = new SliderSetting("Скорость", 1.0, 0.1, 3.0, 0.1)
            .setVisible(() -> mode.is("Plasma"));
    public final SliderSetting plasmaScale = new SliderSetting("Масштаб", 1.0, 0.5, 3.0, 0.1)
            .setVisible(() -> mode.is("Plasma"));
    public final BooleanSetting plasmaStars = new BooleanSetting("Звёзды", true)
            .setVisible(() -> mode.is("Plasma"));

    // Fill mode settings (Заливка)
    public final BooleanSetting fillGlassMode = new BooleanSetting("Стекло", false)
            .setVisible(() -> mode.is("Заливка"));
    public final SliderSetting fillGlassMix = new SliderSetting("Смешивание", 0.0, 0.0, 1.0, 0.01)
            .setVisible(() -> mode.is("Заливка") && fillGlassMode.getValue());
    public final ColorSetting fillColor = new ColorSetting("Цвет заливки", new java.awt.Color(255, 68, 68, 255).getRGB())
            .setVisible(() -> mode.is("Заливка") && !fillGlassMode.getValue());
    public final BooleanSetting fillRainbow = new BooleanSetting("Радужный цвет", false)
            .setVisible(() -> mode.is("Заливка") && !fillGlassMode.getValue());
    public final SliderSetting fillRainbowSpeed = new SliderSetting("Скорость радуги", 1.0, 0.1, 5.0, 0.1)
            .setVisible(() -> mode.is("Заливка") && !fillGlassMode.getValue() && fillRainbow.getValue());
    public final SliderSetting fillAlpha = new SliderSetting("Прозрачность заливки", 0.8, 0.0, 1.0, 0.05)
            .setVisible(() -> mode.is("Заливка") && !fillGlassMode.getValue());
    public final BooleanSetting fillKeepShading = new BooleanSetting("Сохранить тени", true)
            .setVisible(() -> mode.is("Заливка") && !fillGlassMode.getValue());
    public final SliderSetting fillShadingStrength = new SliderSetting("Сила теней", 0.3, 0.0, 1.0, 0.05)
            .setVisible(() -> mode.is("Заливка") && !fillGlassMode.getValue() && fillKeepShading.getValue());

    // Fill mode - Outline settings
    public final BooleanSetting fillOutlineEnabled = new BooleanSetting("Обводка", false)
            .setVisible(() -> mode.is("Заливка"));
    public final SliderSetting fillOutlineWidth = new SliderSetting("Толщина обводки", 1.0, 0.5, 3.0, 0.5)
            .setVisible(() -> mode.is("Заливка") && fillOutlineEnabled.getValue());
    public final ColorSetting fillOutlineColor = new ColorSetting("Цвет обводки", new java.awt.Color(138, 152, 255, 255).getRGB())
            .setVisible(() -> mode.is("Заливка") && fillOutlineEnabled.getValue());

    // Fill mode - Glow settings
    public final BooleanSetting fillGlowEnabled = new BooleanSetting("Глов", true)
            .setVisible(() -> mode.is("Заливка"));
    public final SliderSetting fillGlowRadius = new SliderSetting("Размытие", 4.0, 1.0, 6.0, 1.0)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue());
    public final BooleanSetting fillOuterGlow = new BooleanSetting("Внешний глов", true)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue());
    public final SliderSetting fillGlowExposure = new SliderSetting("Яркость", 2.0, 0.5, 5.0, 0.1)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && fillOuterGlow.getValue());

    public final BooleanSetting fillAutoColor = new BooleanSetting("Авто цвет", false)
            .setVisible(() -> mode.is("Заливка") && (fillGlowEnabled.getValue() || fillOutlineEnabled.getValue()));
    public final SliderSetting fillSaturation = new SliderSetting("Насыщенность", 1.4, 0.5, 3.0, 0.1)
            .setVisible(() -> mode.is("Заливка") && (fillGlowEnabled.getValue() || fillOutlineEnabled.getValue()) && fillAutoColor.getValue());

    public final ColorSetting fillGlowColor1 = new ColorSetting("Цвет глова 1", new java.awt.Color(138, 152, 255, 255).getRGB())
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && !fillAutoColor.getValue());
    public final ColorSetting fillGlowColor2 = new ColorSetting("Цвет глова 2", new java.awt.Color(255, 107, 172, 255).getRGB())
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && !fillAutoColor.getValue());

    // Fill mode - Trail settings
    public final BooleanSetting fillTrailEnabled = new BooleanSetting("Шлейф", true)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && fillOuterGlow.getValue());
    public final SliderSetting fillTrailFade = new SliderSetting("Скорость затухания", 0.009, 0.002, 0.2, 0.002)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && fillOuterGlow.getValue() && fillTrailEnabled.getValue());
    public final SliderSetting fillTrailRise = new SliderSetting("Подъём", 0.14, 0.0, 1.5, 0.05)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && fillOuterGlow.getValue() && fillTrailEnabled.getValue());
    public final SliderSetting fillTrailSway = new SliderSetting("Качание", 0.025, 0.0, 0.2, 0.005)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && fillOuterGlow.getValue() && fillTrailEnabled.getValue());
    public final SliderSetting fillTrailTurb = new SliderSetting("Турбулентность", 0.0, 0.0, 0.6, 0.01)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && fillOuterGlow.getValue() && fillTrailEnabled.getValue());
    public final SliderSetting fillTrailFlicker = new SliderSetting("Мерцание", 0.0, 0.0, 0.2, 0.01)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && fillOuterGlow.getValue() && fillTrailEnabled.getValue());
    public final BooleanSetting fillTrailBurst = new BooleanSetting("Сдув при ударе", true)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && fillOuterGlow.getValue() && fillTrailEnabled.getValue());
    public final SliderSetting fillTrailBurstPower = new SliderSetting("Сила сдува", 2.5, 1.0, 10.0, 0.5)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && fillOuterGlow.getValue() && fillTrailEnabled.getValue() && fillTrailBurst.getValue());
    public final BooleanSetting fillTrailModel = new BooleanSetting("Шлейф модели", true)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && fillOuterGlow.getValue() && fillTrailEnabled.getValue());
    public final SliderSetting fillTrailModelAlpha = new SliderSetting("Прозрачность модели", 0.4, 0.1, 1.0, 0.05)
            .setVisible(() -> mode.is("Заливка") && fillGlowEnabled.getValue() && fillOuterGlow.getValue() && fillTrailEnabled.getValue() && fillTrailModel.getValue());

    // Wave mode settings
    public final SliderSetting waveSpeedX    = new SliderSetting("Скорость X",    0.22, 0.0, 1.5, 0.01)
            .setVisible(() -> mode.is("Волна"));
    public final SliderSetting waveSpeedY    = new SliderSetting("Скорость Y",    0.15, 0.0, 1.5, 0.01)
            .setVisible(() -> mode.is("Волна"));
    public final SliderSetting waveScaleM    = new SliderSetting("Масштаб",       1.35, 0.2, 4.0, 0.05)
            .setVisible(() -> mode.is("Волна"));
    public final SliderSetting waveDensity   = new SliderSetting("Плотность",     1.15, 0.5, 3.0, 0.05)
            .setVisible(() -> mode.is("Волна"));
    public final SliderSetting waveGlow      = new SliderSetting("Сила волн",     1.0,  0.2, 3.0, 0.05)
            .setVisible(() -> mode.is("Волна"));
    public final SliderSetting waveFillAlpha = new SliderSetting("Прозрачность",  0.85, 0.1, 1.0, 0.05)
            .setVisible(() -> mode.is("Волна"));
    public final SliderSetting waveModelVisibility = new SliderSetting("Видимость модели", 0.25, 0.0, 1.0, 0.05)
            .setVisible(() -> mode.is("Волна"));

    // Wave - Glow settings (1:1 как в Заливка)
    public final BooleanSetting waveGlowEnabled = new BooleanSetting("Глов", true)
            .setVisible(() -> mode.is("Волна"));
    public final SliderSetting waveGlowRadius = new SliderSetting("Размытие", 4.0, 1.0, 6.0, 1.0)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue());
    public final BooleanSetting waveOuterGlow = new BooleanSetting("Внешний глов", true)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue());
    public final SliderSetting waveGlowExposure = new SliderSetting("Яркость", 2.0, 0.5, 5.0, 0.1)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveOuterGlow.getValue());
    public final BooleanSetting waveAutoColor = new BooleanSetting("Авто цвет", false)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue());
    public final SliderSetting waveSaturation = new SliderSetting("Насыщенность", 1.4, 0.5, 3.0, 0.1)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveAutoColor.getValue());
    public final ColorSetting waveGlowColor1 = new ColorSetting("Цвет глова 1", new java.awt.Color(138, 152, 255, 255).getRGB())
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && !waveAutoColor.getValue());
    public final ColorSetting waveGlowColor2 = new ColorSetting("Цвет глова 2", new java.awt.Color(255, 107, 172, 255).getRGB())
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && !waveAutoColor.getValue());
    public final BooleanSetting waveRainbow = new BooleanSetting("Радужный цвет", false)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && !waveAutoColor.getValue());
    public final SliderSetting waveRainbowSpeed = new SliderSetting("Скорость радуги", 1.0, 0.1, 5.0, 0.1)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && !waveAutoColor.getValue() && waveRainbow.getValue());

    // Wave - Trail settings (1:1 как в Заливка)
    public final BooleanSetting waveTrailEnabled = new BooleanSetting("Шлейф", true)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveOuterGlow.getValue());
    public final SliderSetting waveTrailFade = new SliderSetting("Скорость затухания", 0.009, 0.002, 0.2, 0.002)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveOuterGlow.getValue() && waveTrailEnabled.getValue());
    public final SliderSetting waveTrailRise = new SliderSetting("Подъём", 0.14, 0.0, 1.5, 0.05)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveOuterGlow.getValue() && waveTrailEnabled.getValue());
    public final SliderSetting waveTrailSway = new SliderSetting("Качание", 0.025, 0.0, 0.2, 0.005)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveOuterGlow.getValue() && waveTrailEnabled.getValue());
    public final SliderSetting waveTrailTurb = new SliderSetting("Турбулентность", 0.0, 0.0, 0.6, 0.01)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveOuterGlow.getValue() && waveTrailEnabled.getValue());
    public final SliderSetting waveTrailFlicker = new SliderSetting("Мерцание", 0.0, 0.0, 0.2, 0.01)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveOuterGlow.getValue() && waveTrailEnabled.getValue());
    public final BooleanSetting waveTrailBurst = new BooleanSetting("Сдув при ударе", true)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveOuterGlow.getValue() && waveTrailEnabled.getValue());
    public final SliderSetting waveTrailBurstPower = new SliderSetting("Сила сдува", 2.5, 1.0, 10.0, 0.5)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveOuterGlow.getValue() && waveTrailEnabled.getValue() && waveTrailBurst.getValue());
    public final BooleanSetting waveTrailModel = new BooleanSetting("Шлейф модели", true)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveOuterGlow.getValue() && waveTrailEnabled.getValue());
    public final SliderSetting waveTrailModelAlpha = new SliderSetting("Прозрачность модели", 0.4, 0.1, 1.0, 0.05)
            .setVisible(() -> mode.is("Волна") && waveGlowEnabled.getValue() && waveOuterGlow.getValue() && waveTrailEnabled.getValue() && waveTrailModel.getValue());

    private final GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();

    @Override
    public void onDisable() {
        renderer.invalidateState();
        super.onDisable();
    }

    @Subscribe
    public void onHUD(EventHUD event) {
        if (!isEnabled()) return;
        renderer.renderOverlayIfPending(this);
    }
}
