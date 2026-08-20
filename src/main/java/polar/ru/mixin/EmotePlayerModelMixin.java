package polar.ru.mixin;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.QClient;
import polar.ru.client.modules.impl.render.Emotes;

@Mixin(value={PlayerEntityModel.class})
public class EmotePlayerModelMixin
implements QClient {
    @Inject(method={"setAngles"}, at={@At(value="TAIL")})
    private void applyEmoteAnimation(PlayerEntityRenderState state, CallbackInfo ci) {
        if (EmotePlayerModelMixin.mc.player == null || state == null) {
            return;
        }
        if (state.id != EmotePlayerModelMixin.mc.player.getId()) {
            return;
        }
        Emotes emotes = Emotes.INSTANCE;
        if (emotes == null || !emotes.isEnable()) {
            return;
        }
        BipedEntityModel model = (BipedEntityModel)(Object)this;
        emotes.applyEmoteToModel(model, mc.getRenderTickCounter().getTickDelta(true));
    }
}

