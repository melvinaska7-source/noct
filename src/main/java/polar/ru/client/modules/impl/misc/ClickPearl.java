package polar.ru.client.modules.impl.misc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventBinding;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.player.InventoryUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BindSetting;
import polar.ru.client.modules.settings.implement.BooleanSetting;

public class ClickPearl
extends Module {
    public static ClickPearl INSTANCE = new ClickPearl();
    private final BindSetting keyToPearl = new BindSetting("Кнопка", -1);
    private final BooleanSetting bypass = new BooleanSetting("Обход", true);
    private boolean use;

    public ClickPearl() {
        super("ClickPearl", "Кидает перку по внутреннему бинду", Module.ModuleCategory.PLAYER);
        this.addSettings(this.keyToPearl, this.bypass);
    }

    @Override
    public void onEnable() {
        this.use = false;
        super.onEnable();
    }

    @EventLink
    public void onEvent(EventBinding event) {
        if (ClickPearl.mc.currentScreen != null) {
            return;
        }
        if (event.getKey() == this.keyToPearl.getKey()) {
            this.use = true;
        }
    }

    @EventLink
    public void onEvent(EventUpdate event) {
        if (!this.use) {
            return;
        }
        if (ClickPearl.mc.player == null || ClickPearl.mc.world == null) {
            this.use = false;
            return;
        }
        int oldSlot = ClickPearl.mc.player.getInventory().selectedSlot;
        int pearlSlot = InventoryUtils.find(Items.ENDER_PEARL, 0, 36);
        if (pearlSlot > 9 && this.use) {
            ClickPearl.mc.player.setSprinting(false);
        }
        if (pearlSlot == -1) {
            this.use = false;
            return;
        }
        if (this.bypass.isState()) {
            ClickPearl.mc.player.getInventory().selectedSlot = pearlSlot;
            ClickPearl.mc.interactionManager.interactItem((PlayerEntity)ClickPearl.mc.player, Hand.MAIN_HAND);
            ClickPearl.mc.player.getInventory().selectedSlot = oldSlot;
        } else {
            InventoryUtils.swapAndUseHvH(Items.ENDER_PEARL);
        }
        this.use = false;
    }
}

