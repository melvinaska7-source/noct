package polar.ru.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.utils.render.fonts.ttf.Fonts;

@Mixin(value={MinecraftClient.class})
public class FontInitMixin {
    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    private void onFinishedLoading(CallbackInfo ci) {
        Fonts.init();
        polar.ru.api.utils.render.fonts.msdf.Fonts.init();
    }
}

