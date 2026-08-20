package polar.ru.mixin;

import java.util.Map;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={ItemCooldownManager.class})
public interface ItemCooldownManagerAccessor {
    @Accessor(value="entries")
    public Map<Identifier, Object> polar$getEntries();

    @Accessor(value="tick")
    public int polar$getTick();
}

