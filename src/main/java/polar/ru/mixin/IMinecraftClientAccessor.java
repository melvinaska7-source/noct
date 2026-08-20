package polar.ru.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={MinecraftClient.class})
public interface IMinecraftClientAccessor {
    @Mutable
    @Accessor(value="session")
    public void setSession(Session var1);

    @Mutable
    @Accessor(value="itemUseCooldown")
    public void setItemUseCooldown(int var1);
}

