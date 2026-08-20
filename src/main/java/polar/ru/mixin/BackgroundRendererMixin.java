package polar.ru.mixin;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.entity.Entity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.impl.render.Removals;
import polar.ru.client.modules.impl.render.WorldTweaks;

@Mixin(value={BackgroundRenderer.class})
public class BackgroundRendererMixin {
    @Inject(method={"getFogModifier"}, at={@At(value="HEAD")}, cancellable=true)
    private static void polar$getFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> cir) {
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Плохие эффекты")) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method={"applyFog"}, at={@At(value="RETURN")}, cancellable=true)
    private static void polar$applyFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        WorldTweaks tweaks = ModuleClass.worldTweaks;
        if (tweaks != null && tweaks.isFogEnabled()) {
            float fogDistance = Math.max(12.0f, tweaks.getFogDistance());
            float fogEnd = Math.min(viewDistance, fogDistance);
            float fogStart = Math.max(0.0f, fogEnd * 0.05f);
            int color1 = tweaks.getFogColor();
            cir.setReturnValue(new Fog(fogStart, fogEnd, FogShape.SPHERE, ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1), 1.0f));
            return;
        }
    }
}

