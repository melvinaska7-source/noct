package polar.ru.mixin;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={PlayerMoveC2SPacket.class})
public interface IPlayerMoveC2SPacketAccessor {
    @Accessor(value="x")
    public void setX(double var1);

    @Accessor(value="y")
    public void setY(double var1);

    @Accessor(value="z")
    public void setZ(double var1);

    @Mutable
    @Accessor(value="horizontalCollision")
    public void setHorizontalCollision(boolean var1);
}

