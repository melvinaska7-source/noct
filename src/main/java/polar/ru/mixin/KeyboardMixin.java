package polar.ru.mixin;

import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polar.ru.api.QClient;
import polar.ru.api.events.implement.EventChunkReload;
import polar.ru.api.utils.input.KeyBoardUtils;

@Mixin(value={Keyboard.class})
public class KeyboardMixin
implements QClient {
    @Inject(method={"onKey"}, at={@At(value="HEAD")})
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (KeyboardMixin.mc.currentScreen == null) {
            KeyBoardUtils.call(key, action);
        }
    }

    @Inject(method={"processF3"}, at={@At(value="RETURN")})
    private void processF3(int key, CallbackInfoReturnable<Boolean> cir) {
        if (key == 65 && ((Boolean)cir.getReturnValue()).booleanValue()) {
            new EventChunkReload().call();
        }
    }
}

