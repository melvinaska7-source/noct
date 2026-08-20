package polar.ru.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets={"net/minecraft/entity/player/ItemCooldownManager$Entry"})
public interface ItemCooldownManagerEntryAccessor {
    @Accessor(value="startTick")
    public int polar$getStartTick();

    @Accessor(value="endTick")
    public int polar$getEndTick();
}

