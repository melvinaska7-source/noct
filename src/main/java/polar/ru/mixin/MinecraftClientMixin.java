package polar.ru.mixin;

import java.lang.reflect.InvocationTargetException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.events.implement.EventGameUpdate;
import polar.ru.api.events.implement.EventTickPost;
import polar.ru.api.events.implement.EventTickPre;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.baritone.BaritoneAntiStuck;
import polar.ru.api.utils.player.Counter;
import polar.ru.client.figura.FiguraOverlaySuppressor;
import polar.ru.client.modules.impl.render.Chams;
import polar.ru.client.modules.impl.render.ShaderEsp;

@Mixin(value={MinecraftClient.class})
public abstract class MinecraftClientMixin {
    @Unique
    private long lastHookTime = Util.getMeasuringTimeNano();
    @Unique
    private int accumulatedCalls = 0;

    @Inject(method={"tick"}, at={@At(value="HEAD")})
    public void tick(CallbackInfo ci) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (EventInvoker.hasListeners(EventTickPre.class)) {
            EventTickPre event = new EventTickPre();
            EventInvoker.invoke(event);
        }
        Counter.updateFPS();
    }

    @Inject(method={"handleInputEvents"}, at={@At(value="RETURN")}, order=2000)
    private void polar$afterFiguraInput(CallbackInfo ci) {
        FiguraOverlaySuppressor.afterInput();
    }

    @Inject(method={"tick"}, at={@At(value="RETURN")})
    public void tickEnd(CallbackInfo ci) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (EventInvoker.hasListeners(EventTickPost.class)) {
            EventTickPost event = new EventTickPost();
            EventInvoker.invoke(event);
        }
        BaritoneAntiStuck.tick();
        FiguraOverlaySuppressor.tick();
    }

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void render(boolean tick, CallbackInfo ci) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (!EventInvoker.hasListeners(EventGameUpdate.class)) {
            this.lastHookTime = Util.getMeasuringTimeNano();
            this.accumulatedCalls = 0;
            return;
        }
        long now = Util.getMeasuringTimeNano();
        long delta = now - this.lastHookTime;
        this.accumulatedCalls += (int)(delta / 4166666L);
        this.lastHookTime += (long)this.accumulatedCalls * 4166666L;
        this.accumulatedCalls = Math.min(this.accumulatedCalls, 240);
        while (this.accumulatedCalls > 0) {
            EventInvoker.invoke(new EventGameUpdate());
            --this.accumulatedCalls;
        }
    }

    @Inject(method={"hasOutline"}, at={@At(value="HEAD")}, cancellable=true)
    private void polar$hasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player;
        if (ModuleClass.INSTANCE == null) {
            return;
        }
        ShaderEsp shaderEsp = ModuleClass.shaderEsp;
        if (shaderEsp != null && shaderEsp.shouldOutline(entity)) {
            cir.setReturnValue(true);
            return;
        }
        Chams chams = ModuleClass.chams;
        if (chams != null && entity instanceof PlayerEntity && chams.shouldUseOutlineAssist(player = (PlayerEntity)entity)) {
            cir.setReturnValue(true);
        }
    }
}

