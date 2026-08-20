package polar.ru.client.modules.impl.render;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;

public class SeeInvisibles
extends Module {
    public static final float INVISIBLE_ALPHA = 0.7f;
    public static final int INVISIBLE_COLOR = Math.round(178.5f) << 24 | 0xFFFFFF;
    public static SeeInvisibles INSTANCE = new SeeInvisibles();

    public SeeInvisibles() {
        super("SeeInvisibles", "Показывает невидимых игроков", Module.ModuleCategory.RENDER);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (SeeInvisibles.mc.player == null || SeeInvisibles.mc.world == null) {
            return;
        }
        for (PlayerEntity player : SeeInvisibles.mc.world.getPlayers()) {
            if (!this.shouldRenderInvisible(player)) continue;
            player.setInvisible(false);
        }
    }

    public boolean shouldRenderInvisible(PlayerEntity player) {
        return this.isEnable() && SeeInvisibles.mc.player != null && player != null && player != SeeInvisibles.mc.player && (player.isInvisible() || player.hasStatusEffect(StatusEffects.INVISIBILITY));
    }
}

