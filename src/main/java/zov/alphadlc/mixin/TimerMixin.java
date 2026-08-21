package zov.alphadlc.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import zov.alphadlc.util.math.TimerUtil;

@Mixin(MinecraftClient.class)
public abstract class TimerMixin {
    @Unique
    private long alphadlcLastRealTime = 0L;
    @Unique
    private long alphadlcFakeTime = 0L;

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;beginRenderTick(JZ)I"), index = 0)
    private long modifyRenderTime(long currentTime) {
        if (TimerUtil.speed == 1.0f) {
            this.alphadlcLastRealTime = currentTime;
            this.alphadlcFakeTime = currentTime;
            return currentTime;
        }
        if (this.alphadlcLastRealTime == 0L) {
            this.alphadlcLastRealTime = currentTime;
            this.alphadlcFakeTime = currentTime;
            return currentTime;
        }
        long realElapsed = currentTime - this.alphadlcLastRealTime;
        this.alphadlcLastRealTime = currentTime;
        this.alphadlcFakeTime += (long)((double)realElapsed * (double)TimerUtil.speed);
        return this.alphadlcFakeTime;
    }
}
