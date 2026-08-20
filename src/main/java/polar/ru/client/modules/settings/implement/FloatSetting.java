package polar.ru.client.modules.settings.implement;

import java.util.function.Supplier;
import net.minecraft.util.math.MathHelper;
import polar.ru.client.modules.settings.Setting;

public class FloatSetting
extends Setting {
    private float value;
    private final float min;
    private final float max;
    private final float increment;
    private boolean active;

    public FloatSetting(String name, float value, float min, float max, float increment) {
        super(name);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public Number getValue() {
        return Float.valueOf(MathHelper.clamp((float)this.value, (float)this.getMin(), (float)this.getMax()));
    }

    public void setValue(float value) {
        this.value = MathHelper.clamp((float)value, (float)this.getMin(), (float)this.getMax());
    }

    public float get() {
        return this.getValue().floatValue();
    }

    public FloatSetting visible(Supplier<Boolean> state) {
        this.visible = state;
        return this;
    }
    public float getMin() {
        return this.min;
    }
    public float getMax() {
        return this.max;
    }
    public float getIncrement() {
        return this.increment;
    }
    public boolean isActive() {
        return this.active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
}

