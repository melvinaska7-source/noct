package polar.ru.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polar.ru.api.utils.render.ShaderUtils;

@Mixin(value = {ShaderProgramKeys.class})
public class CoreShadersMixin {
    @Inject(method = {"getAll"}, at = {@At(value = "RETURN")}, cancellable = true)
    private static void polar$registerCoreShaders(CallbackInfoReturnable<List<ShaderProgramKey>> cir) {
        List<ShaderProgramKey> original = cir.getReturnValue();
        ArrayList<ShaderProgramKey> combined = new ArrayList<ShaderProgramKey>(original);
        combined.addAll(ShaderUtils.ALL_SHADERS);
        cir.setReturnValue(combined);
    }
}
