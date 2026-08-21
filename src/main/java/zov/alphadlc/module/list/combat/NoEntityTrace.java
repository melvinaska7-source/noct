package zov.alphadlc.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import zov.alphadlc.event.list.EventEntityHitBox;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;

@ModuleInformation(moduleName = "NoEntityTrace", moduleDesc = "Увеличение хитбокса сущностей", moduleCategory = ModuleCategory.COMBAT)
public class NoEntityTrace extends Module {
    private final BooleanSetting invisible = new BooleanSetting("Невидимки", true);

    @Subscribe
    private void onEntityHitBox(EventEntityHitBox e2) {
        Entity entity = e2.getEntity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity entity2 = (LivingEntity)entity;
        if (entity2.isInvisible() && !this.invisible.getValue()) {
            return;
        }
        e2.setSize(-0.75f);
    }
}
