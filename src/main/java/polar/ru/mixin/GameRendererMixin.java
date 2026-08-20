package polar.ru.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.render.RenderUtils;
import polar.ru.client.modules.impl.render.Removals;

@Mixin(value={GameRenderer.class})
public class GameRendererMixin {
    @Inject(method={"showFloatingItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$hideTotemAnimation(ItemStack stack, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null || stack == null || !stack.isOf(Items.TOTEM_OF_UNDYING)) {
            return;
        }
        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isTotemAnimationDisabled()) {
            ci.cancel();
        }
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V")})
    private void polar$captureBlurBeforeHud(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        RenderUtils.beginLiquidBlurFrame();
    }

    @Inject(method={"bobView"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void cancelBobView(CallbackInfo ci) {
        Removals removals;
        Removals removals2 = removals = ModuleClass.INSTANCE != null ? ModuleClass.removals : null;
        if (removals != null && (removals.isEnabled("Тряску при уроне") || removals.isEnabled("Тряску экрана"))) {
            ci.cancel();
        }
    }
}

