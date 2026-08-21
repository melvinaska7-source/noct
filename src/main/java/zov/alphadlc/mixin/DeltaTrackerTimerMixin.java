package zov.alphadlc.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import zov.alphadlc.util.timer.TimerManager;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTickCounter.Dynamic.class)
public abstract class DeltaTrackerTimerMixin {
    @ModifyExpressionValue(
            method = "advanceGameTime",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"
            )
    )
    private float xentrix$applyTimerSpeed(float targetMspt) {
        return targetMspt / TimerManager.getTimer();
    }
}

