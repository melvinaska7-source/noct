package zov.alphadlc.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zov.alphadlc.event.list.EventHUD;
import zov.alphadlc.module.list.render.hud.Interface;
import zov.alphadlc.util.base.Instance;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At(value = "HEAD"))
    private void beginBlurFrame(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        zov.alphadlc.util.render.renderers.impl.BuiltBlur.beginFrame();
    }

    @Inject(method = "render", at = @At(value = "RETURN"))
    private void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        new EventHUD(context, tickCounter).post();
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void hideVanillaStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Interface iface = Instance.get(Interface.class);
        if (iface != null && iface.isPotionsActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void hideVanillaHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Interface iface = Instance.get(Interface.class);
        if (iface != null && iface.isCustomHotbarActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void hideVanillaExperienceBar(DrawContext context, int x, CallbackInfo ci) {
        Interface iface = Instance.get(Interface.class);
        if (iface != null && iface.isCustomHotbarActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void hideVanillaExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Interface iface = Instance.get(Interface.class);
        if (iface != null && iface.isCustomHotbarActive()) {
            ci.cancel();
        }
    }
}
