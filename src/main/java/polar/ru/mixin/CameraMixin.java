package polar.ru.mixin;

import java.lang.reflect.InvocationTargetException;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.events.implement.EventRotation;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.client.modules.impl.render.InterpolateF5;
import polar.ru.mixin.ICameraMixin;

@Mixin(value={Camera.class})
public abstract class CameraMixin {
    @Redirect(method={"update"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void redirectSetRotation(Camera instance, float yaw, float pitch, BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        EventRotation event = new EventRotation(yaw, pitch, tickDelta);
        EventInvoker.invoke(event);
        float newYaw = event.getYaw();
        float newPitch = event.getPitch();
        if (thirdPerson && inverseView) {
            newYaw += 180.0f;
            newPitch = -newPitch;
        }
        ((ICameraMixin)instance).setCustomRotation(newYaw, newPitch);
    }

    @Redirect(method={"update"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/render/Camera;clipToSpace(F)F"))
    private float redirectClipToSpace(Camera instance, float distance, BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
        InterpolateF5 module;
        if (!thirdPerson) {
            return ((ICameraMixin)instance).setClipToSpace(distance);
        }
        InterpolateF5 interpolateF5 = module = ModuleClass.INSTANCE != null ? ModuleClass.interpolateF5 : null;
        if (module != null && module.isEnable()) {
            return ((ICameraMixin)instance).setClipToSpace(module.getInterpolatedDistance(tickDelta));
        }
        return ((ICameraMixin)instance).setClipToSpace(distance);
    }

    @Redirect(method={"update"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/render/Camera;moveBy(FFF)V"))
    private void redirectMoveBy(Camera instance, float x2, float y2, float z2, BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
        float newY = y2;
        if (thirdPerson) {
            InterpolateF5 module;
            InterpolateF5 interpolateF5 = module = ModuleClass.INSTANCE != null ? ModuleClass.interpolateF5 : null;
            if (module != null && module.isEnable()) {
                newY += module.getInterpolatedHeightOffset(tickDelta);
            }
        }
        ((ICameraMixin)instance).setCustomMoveBy(x2, newY, z2);
    }
}

