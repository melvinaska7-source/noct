package polar.ru.mixin;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={WorldRenderer.class})
public interface WorldRendererAccessor {
    @Accessor(value="entityOutlineFramebuffer")
    public Framebuffer polar$getEntityOutlineFramebufferRaw();
}

