package polar.ru.mixin;

import java.lang.reflect.Field;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.impl.render.SwingAnimations;

@Mixin(value={LivingEntity.class})
public abstract class LivingEntityMixin {
    @Inject(method={"getHandSwingDuration"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this != MinecraftClient.getInstance().player) {
            return;
        }
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        SwingAnimations tweaks = ModuleClass.swingAnimations;
        if (tweaks != null && tweaks.isEnable() && tweaks.smoothEnabled.isState()) {
            cir.setReturnValue(((int)tweaks.slowAnimationSpeed.get()));
        }
    }

    @Inject(method={"tick"}, at={@At(value="HEAD")})
    private void onTick(CallbackInfo ci) {
        if ((Object)this != MinecraftClient.getInstance().player) {
            return;
        }
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        if (ModuleClass.noJumpDelay != null && ModuleClass.noJumpDelay.isEnable()) {
            try {
                Field jumpCooldownField = null;
                try {
                    jumpCooldownField = LivingEntity.class.getDeclaredField("jumpCooldown");
                }
                catch (NoSuchFieldException e2) {
                    try {
                        jumpCooldownField = LivingEntity.class.getDeclaredField("noJumpCooldown");
                    }
                    catch (NoSuchFieldException e22) {
                        for (Field field : LivingEntity.class.getDeclaredFields()) {
                            if (field.getType() != Integer.TYPE || !field.getName().toLowerCase().contains("jump")) continue;
                            jumpCooldownField = field;
                            break;
                        }
                    }
                }
                if (jumpCooldownField != null) {
                    jumpCooldownField.setAccessible(true);
                    jumpCooldownField.set(this, 0);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }
}

