package polar.ru.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polar.ru.api.utils.IMinecraft;
import polar.ru.client.modules.impl.render.Cape;
import polar.ru.polar;

@Mixin(value={PlayerListEntry.class}, priority=2000)
public class CapeFeatureRendererMixin
implements IMinecraft {
    private static final Identifier CUSTOM_CAPE = Identifier.of((String)"polar", (String)"textures/cape.png");
    @Shadow
    @Final
    private GameProfile profile;

    @Inject(method={"getSkinTextures"}, at={@At(value="RETURN")}, cancellable=true)
    public void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        try {
            if (polar.INSTANCE == null || polar.INSTANCE.moduleStorage == null) {
                return;
            }
            Cape capeModule = Cape.INSTANCE;
            if (capeModule != null && capeModule.isCustomCapeEnabled()) {
                boolean isFriend;
                String playerName = this.profile.getName();
                boolean isOurPlayer = CapeFeatureRendererMixin.mc.player != null && playerName.equals(CapeFeatureRendererMixin.mc.player.getName().getString());
                boolean bl = isFriend = polar.INSTANCE.friendStorage != null && polar.INSTANCE.friendStorage.isFriend(playerName);
                if (isOurPlayer || isFriend) {
                    SkinTextures original = (SkinTextures)cir.getReturnValue();
                    cir.setReturnValue(new SkinTextures(original.texture(), original.textureUrl(), CUSTOM_CAPE, original.elytraTexture(), original.model(), original.secure()));
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

