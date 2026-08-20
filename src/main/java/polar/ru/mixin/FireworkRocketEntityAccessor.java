package polar.ru.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={FireworkRocketEntity.class})
public interface FireworkRocketEntityAccessor {
    @Accessor(value="shooter")
    public LivingEntity polar$getShooter();
}

