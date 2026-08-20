package polar.ru.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.impl.render.Removals;

@Mixin(value={InGameOverlayRenderer.class})
public class InGameOverlayRendererMixin {
    @Inject(method={"renderFireOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private static void polar$renderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Огонь")) {
            ci.cancel();
        }
    }

    @Inject(method={"renderInWallOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private static void polar$renderInWallOverlay(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("Оверлей в блоке")) {
            ci.cancel();
        }
    }
}

