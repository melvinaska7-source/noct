package polar.ru.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={Camera.class})
public interface ICameraMixin {
    @Invoker(value="setRotation")
    public void setCustomRotation(float var1, float var2);

    @Invoker(value="clipToSpace")
    public float setClipToSpace(float var1);

    @Invoker(value="moveBy")
    public void setCustomMoveBy(float var1, float var2, float var3);
}

