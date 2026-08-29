package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.event.list.EventWorldRender;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ColorSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.render.esp.GlowEspRenderer;

import java.awt.Color;

@ModuleInformation(moduleName = "Glow ESP", moduleDesc = "Glow вокруг игроков как Fill в GlassHands", moduleCategory = ModuleCategory.RENDER)
public class GlowEsp extends Module {

    private static GlowEsp instance;

    public GlowEsp() {
        instance = this;
    }

    public static GlowEsp getInstance() {
        return instance;
    }

    public final SliderSetting glowRadius   = new SliderSetting("Размытие",       4.0, 1.0, 6.0, 1.0);
    public final SliderSetting glowExposure = new SliderSetting("Яркость",        2.0, 0.5, 5.0, 0.1);
    public final SliderSetting saturation   = new SliderSetting("Насыщенность",   1.0, 0.0, 2.0, 0.1);

    public final BooleanSetting autoColor   = new BooleanSetting("Авто цвет", true);
    public final BooleanSetting renderSelf  = new BooleanSetting("На себя", false);

    public final ColorSetting glowColor1    = new ColorSetting("Цвет 1", new Color(138, 152, 255, 255).getRGB())
            .setVisible(() -> !autoColor.getValue());
    public final ColorSetting glowColor2    = new ColorSetting("Цвет 2", new Color(255, 107, 172, 255).getRGB())
            .setVisible(() -> !autoColor.getValue());

    public final BooleanSetting rainbow     = new BooleanSetting("Радужный цвет", false)
            .setVisible(() -> !autoColor.getValue());
    public final SliderSetting rainbowSpeed = new SliderSetting("Скорость радуги", 1.0, 0.1, 5.0, 0.1)
            .setVisible(() -> !autoColor.getValue() && rainbow.getValue());

    public final BooleanSetting outlineEnabled = new BooleanSetting("Обводка", false);
    public final SliderSetting outlineWidth    = new SliderSetting("Толщина обводки", 1.0, 0.5, 3.0, 0.5)
            .setVisible(() -> outlineEnabled.getValue());
    public final ColorSetting outlineColor     = new ColorSetting("Цвет обводки", new Color(138, 152, 255, 255).getRGB())
            .setVisible(() -> outlineEnabled.getValue() && !autoColor.getValue());

    @Override
    public void onDisable() {
        GlowEspRenderer.getInstance().invalidate();
        super.onDisable();
    }

    @Subscribe
    public void onWorldRender(EventWorldRender event) {
        GlowEspRenderer.getInstance().renderMask(this, event.getMatrixStack(), event.getTickDelta());
    }

    @Subscribe
    public void onHUD(EventHUD event) {
        GlowEspRenderer.getInstance().compositeIfReady(this);
    }
}
