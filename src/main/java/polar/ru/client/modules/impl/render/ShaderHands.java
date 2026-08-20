package polar.ru.client.modules.impl.render;

import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.utils.render.hands.ShaderHandsRenderer;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class ShaderHands
extends Module {
    public static ShaderHands INSTANCE = new ShaderHands();
    private static final ShaderHandsRenderer RENDERER = ShaderHandsRenderer.getInstance();
    public final ModeSetting mode = new ModeSetting("Режим", "Свечение", "Свечение", "Красивый", "Дым");
    public final FloatSetting waveSpeed = new FloatSetting("Скорость волн", 1.2f, 0.1f, 5.0f, 0.1f).visible(() -> this.mode.is("Красивый"));
    public final FloatSetting waveScale = new FloatSetting("Частота волн", 1.0f, 1.0f, 3.0f, 0.1f).visible(() -> this.mode.is("Красивый"));
    public final FloatSetting smokeSpeed = new FloatSetting("Скорость дыма", 0.8f, 0.1f, 3.0f, 0.1f).visible(() -> this.mode.is("Дым"));
    public final FloatSetting smokeScale = new FloatSetting("Размер дыма", 1.5f, 0.5f, 5.0f, 0.1f).visible(() -> this.mode.is("Дым"));
    public final FloatSetting smokeDensity = new FloatSetting("Плотность дыма", 0.8f, 0.1f, 1.0f, 0.1f).visible(() -> this.mode.is("Дым"));
    public final FloatSetting outline = new FloatSetting("Ширина обводки", 1.2f, 0.1f, 5.0f, 0.1f);
    public final FloatSetting glow = new FloatSetting("Сила свечения", 1.0f, 0.0f, 5.0f, 0.1f);
    public final FloatSetting fill = new FloatSetting("Заливка", 0.6f, 0.0f, 1.0f, 0.01f);
    public final FloatSetting alpha = new FloatSetting("Прозрачность", 1.0f, 0.0f, 1.0f, 0.05f);

    public ShaderHands() {
        super("ShaderHands", "Красивый шейдер на руки и предметы", Module.ModuleCategory.RENDER);
        this.addSettings(this.mode, this.waveSpeed, this.waveScale, this.smokeSpeed, this.smokeScale, this.smokeDensity, this.outline, this.glow, this.fill, this.alpha);
    }

    @EventLink(priority=0)
    public void onRender2D(EventRender.Default event) {
        if (!this.isEnable()) {
            return;
        }
        RENDERER.renderOverlayIfPending(event.getPartialTicks());
    }
}

