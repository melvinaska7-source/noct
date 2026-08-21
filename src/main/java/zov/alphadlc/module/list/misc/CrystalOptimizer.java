package zov.alphadlc.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import zov.alphadlc.event.list.EventAttack;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;

@ModuleInformation(moduleName = "Crystal Optimizer", moduleDesc = "Оптимизация рендера кристаллов", moduleCategory = ModuleCategory.MISC)
public class CrystalOptimizer extends Module {
    @Subscribe
    private void onAttack(EventAttack e) {
        if (e.getEntity() instanceof EndCrystalEntity entity) {
            entity.remove(Entity.RemovalReason.DISCARDED);
        }
    }
}