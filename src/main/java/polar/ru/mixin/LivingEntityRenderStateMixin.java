package polar.ru.mixin;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import polar.ru.client.modules.impl.render.SeeInvisiblesRenderState;

@Mixin(value={LivingEntityRenderState.class})
public class LivingEntityRenderStateMixin
implements SeeInvisiblesRenderState {
    @Unique
    private boolean polar$seeInvisiblesTarget;

    @Override
    public boolean polar$isSeeInvisiblesTarget() {
        return this.polar$seeInvisiblesTarget;
    }

    @Override
    public void polar$setSeeInvisiblesTarget(boolean value) {
        this.polar$seeInvisiblesTarget = value;
    }
}

