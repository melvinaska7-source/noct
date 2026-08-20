package polar.ru.mixin.figura;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets={"org/figuramc/figura/gui/PopupMenu"}, remap=false)
public class FiguraPopupMenuMixin {
    @Inject(method={"render"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private static void polar$blockRender(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method={"setEnabled"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private static void polar$blockEnable(boolean enabled, CallbackInfo ci) {
        if (enabled) {
            ci.cancel();
        }
    }
}

