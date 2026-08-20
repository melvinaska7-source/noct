package polar.ru.mixin;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.polar;

@Mixin(value={Main.class})
public class MainMixin {
    @Inject(method={"main"}, at={@At(value="HEAD")}, remap=false)
    private static void onMain(String[] args, CallbackInfo ci) {
        if (polar.INSTANCE.isServer) {
            try {
                polar.INSTANCE.closeMinecraft();
            }
            catch (Exception e2) {
                e2.printStackTrace();
            }
            polar.INSTANCE.isServer = false;
        }
    }
}

