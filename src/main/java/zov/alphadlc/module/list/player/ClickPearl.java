package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.Items;
import zov.alphadlc.event.list.EventKeyInput;
import zov.alphadlc.event.list.EventPlayerSync;
import zov.alphadlc.event.list.EventPlayerUpdate;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BindSetting;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.util.player.other.InventoryUtil;

@ModuleInformation(moduleName = "Click Pearl", moduleDesc = "Быстрый бросок перлы по биндy", moduleCategory = ModuleCategory.PLAYER)
public class ClickPearl extends Module {
    private final ModeSetting mode = new ModeSetting("Мод", "Обычный", "Обычный", "Легитный");
    private final BindSetting key = new BindSetting("Клавиша броска", -98);

    private boolean pearlUsed;
    private int ticksExisted;

    @Subscribe
    private void onKey(EventKeyInput e) {
        if (e.getAction() == 0) return;
        if (e.getKey() == key.getValue()) {
            if (mode.is("Обычный")) InventoryUtil.swapAndUseHvH(Items.ENDER_PEARL);
            else pearlUsed = true;
        }
    }

    @Subscribe
    private void onPlayerTick(final EventPlayerUpdate ignored) {
        if (mc.player == null) return;
        if (!pearlUsed && ticksExisted > 0) ticksExisted--;
        if (pearlUsed || ticksExisted > 0) mc.player.setSprinting(false);
    }

    @Subscribe
    private void onPlayerSync(final EventPlayerSync ignored) {
        if (mc.player == null || !pearlUsed) return;
        var slotHotbar = InventoryUtil.searchItem(Items.ENDER_PEARL, 0, 9);
        if (slotHotbar != -1) InventoryUtil.swapAndUseLegit(Items.ENDER_PEARL);
        else {
            if (ticksExisted == 0) {
                ticksExisted++;
                return;
            }
            InventoryUtil.swapAndUseLegit(Items.ENDER_PEARL);
            ticksExisted = 2;
        }
        pearlUsed = false;
    }
}