package polar.ru.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.client.figura.FiguraLuaChatFilter;

@Mixin(value={ChatHud.class})
public class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void polar$filterLuaMessage(Text message, CallbackInfo ci) {
        if (FiguraLuaChatFilter.shouldSuppress(message)) {
            ci.cancel();
        }
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), cancellable = true)
    private void polar$filterLuaMessageSigned(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        if (FiguraLuaChatFilter.shouldSuppress(message)) {
            ci.cancel();
        }
    }
}

