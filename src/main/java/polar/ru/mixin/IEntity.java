package polar.ru.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={Entity.class})
public interface IEntity {
    @Invoker(value="adjustMovementForCollisions")
    public Vec3d invokeAdjustMovementForCollisions(Vec3d var1);
}

