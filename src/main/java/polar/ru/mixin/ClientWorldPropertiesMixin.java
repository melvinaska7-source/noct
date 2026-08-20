package polar.ru.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.impl.render.WorldTweaks;

@Mixin(value={ClientWorld.Properties.class})
public class ClientWorldPropertiesMixin {
    @Shadow
    private long timeOfDay;

    @Inject(method={"getTimeOfDay"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$getTimeOfDay(CallbackInfoReturnable<Long> cir) {
        WorldTweaks tweaks = ClientWorldPropertiesMixin.getTweaks();
        if (tweaks != null && tweaks.isTimeEnabled()) {
            cir.setReturnValue(tweaks.getForcedTime());
        }
    }

    @Inject(method={"setTimeOfDay"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$setTimeOfDay(long timeOfDay, CallbackInfo ci) {
        WorldTweaks tweaks = ClientWorldPropertiesMixin.getTweaks();
        if (tweaks == null || !tweaks.isTimeEnabled()) {
            return;
        }
        this.timeOfDay = tweaks.getForcedTime();
        ci.cancel();
    }

    private static WorldTweaks getTweaks() {
        return ModuleClass.INSTANCE != null ? ModuleClass.worldTweaks : null;
    }
}

