package polar.ru.client.modules.impl.render.base;

import polar.ru.api.QClient;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.draggable.Draggable;
import polar.ru.client.modules.impl.render.Interface;
import polar.ru.client.modules.impl.render.base.GlassSettings;

public class InterfaceProcessing
implements QClient {
    public final Draggable draggable;
    private boolean unusualRectType = true;
    protected GlassSettings glassSettings = new GlassSettings();

    public boolean isUnusualRectType() {
        return this.unusualRectType;
    }

    public void setUnusualRectType(boolean unusualRectType) {
        this.unusualRectType = unusualRectType;
    }

    public void updateGlassSettings(GlassSettings settings) {
        if (settings != null) {
            this.glassSettings = settings;
        }
    }

    public boolean isFlatStyle() {
        return Interface.INSTANCE != null && Interface.INSTANCE.isFlatStyle();
    }

    public void onUpdate(EventUpdate eventUpdate) {
    }

    public void onRender(EventRender.Default eventRender) {
    }
    public InterfaceProcessing(Draggable draggable) {
        this.draggable = draggable;
    }
}

