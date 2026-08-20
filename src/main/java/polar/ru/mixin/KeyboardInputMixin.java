package polar.ru.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.events.implement.EventMoveInput;

@Mixin(value={KeyboardInput.class})
public abstract class KeyboardInputMixin
extends Input {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Inject(method={"tick"}, at={@At(value="TAIL")})
    private void onTickTail(CallbackInfo ci) {
        if (!EventInvoker.hasListeners(EventMoveInput.class)) {
            return;
        }
        EventMoveInput eventInput = new EventMoveInput(this.movementForward, this.movementSideways, this.playerInput.jump(), this.playerInput.sneak());
        eventInput.call();
        float forward = eventInput.getForward();
        float strafe = eventInput.getStrafe();
        this.playerInput = new PlayerInput(forward > 0.0f, forward < 0.0f, strafe > 0.0f, strafe < 0.0f, eventInput.isJump(), eventInput.isSneak(), this.playerInput.sprint());
        this.movementForward = forward;
        this.movementSideways = strafe;
    }
}

