package polar.ru.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class ViewModel
extends Module {
    public static ViewModel INSTANCE = new ViewModel();
    public final FloatSetting mainHandX = new FloatSetting("Правая рука X", 0.0f, -2.0f, 2.0f, 0.01f);
    public final FloatSetting mainHandY = new FloatSetting("Правая рука Y", 0.0f, -2.0f, 2.0f, 0.01f);
    public final FloatSetting mainHandZ = new FloatSetting("Правая рука Z", 0.0f, -2.0f, 2.0f, 0.01f);
    public final FloatSetting mainHandScale = new FloatSetting("Правая рука размер", 1.0f, 0.1f, 3.0f, 0.01f);
    public final FloatSetting offHandX = new FloatSetting("Левая рука X", 0.0f, -2.0f, 2.0f, 0.01f);
    public final FloatSetting offHandY = new FloatSetting("Левая рука Y", 0.0f, -2.0f, 2.0f, 0.01f);
    public final FloatSetting offHandZ = new FloatSetting("Левая рука Z", 0.0f, -2.0f, 2.0f, 0.01f);
    public final FloatSetting offHandScale = new FloatSetting("Левая рука размер", 1.0f, 0.1f, 3.0f, 0.01f);
    public final BooleanSetting onlyAura = new BooleanSetting("Только с аурой", false);

    public ViewModel() {
        super("ViewModel", "Оффсеты рук от первого лица", Module.ModuleCategory.RENDER);
        this.addSettings(this.mainHandX, this.mainHandY, this.mainHandZ, this.mainHandScale, this.offHandX, this.offHandY, this.offHandZ, this.offHandScale, this.onlyAura);
    }

    public void applyHandPosition(MatrixStack matrices, Arm arm) {
        if (arm == Arm.RIGHT) {
            matrices.translate(this.mainHandX.get(), this.mainHandY.get(), this.mainHandZ.get());
            matrices.scale(this.mainHandScale.get(), this.mainHandScale.get(), this.mainHandScale.get());
        } else {
            matrices.translate(this.offHandX.get(), this.offHandY.get(), this.offHandZ.get());
            matrices.scale(this.offHandScale.get(), this.offHandScale.get(), this.offHandScale.get());
        }
    }
}

