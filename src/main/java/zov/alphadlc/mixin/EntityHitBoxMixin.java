package zov.alphadlc.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zov.alphadlc.event.list.EventEntityHitBox;

@Mixin(Entity.class)
public class EntityHitBoxMixin {
    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void onGetTargetingMargin(CallbackInfoReturnable<Float> cir) {
        Entity entity = (Entity)(Object)this;
        EventEntityHitBox event = new EventEntityHitBox(entity, 1.0f);
        event.post();
        if (event.getSize() != 1.0f) {
            cir.setReturnValue(event.getSize());
        }
    }
}
