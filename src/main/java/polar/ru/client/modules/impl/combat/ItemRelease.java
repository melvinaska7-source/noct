package polar.ru.client.modules.impl.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;

public class ItemRelease
extends Module {
    public static ItemRelease INSTANCE = new ItemRelease();
    private final ListSetting items = new ListSetting("Предметы", new BooleanSetting("Лук", true), new BooleanSetting("Трезубец", false), new BooleanSetting("Арбалет", true));
    private final FloatSetting tickBow = new FloatSetting("Задержка выстрела", 2.5f, 2.0f, 5.0f, 0.05f).visible(() -> this.items.is("Лук"));

    public ItemRelease() {
        super("ItemRelease", "Автоматически выпускает предмет когда он полностью натянут", Module.ModuleCategory.COMBAT);
        this.addSettings(this.items, this.tickBow);
    }

    @EventLink
        public void onUpdate(EventUpdate e2) {
        if (ItemRelease.mc.player == null || ItemRelease.mc.world == null) {
            return;
        }
        if (this.items.is("Лук") && ItemRelease.mc.player.getMainHandStack().getItem() instanceof BowItem && ItemRelease.mc.player.isUsingItem() && (float)ItemRelease.mc.player.getItemUseTime() >= this.tickBow.getValue().floatValue()) {
            ItemRelease.mc.interactionManager.stopUsingItem((PlayerEntity)ItemRelease.mc.player);
        }
        if (this.items.is("Трезубец") && ItemRelease.mc.player.getMainHandStack().getItem() instanceof TridentItem && ItemRelease.mc.player.isUsingItem() && ItemRelease.mc.player.getItemUseTime() >= 10) {
            ItemRelease.mc.interactionManager.stopUsingItem((PlayerEntity)ItemRelease.mc.player);
        }
        if (this.items.is("Арбалет") && ItemRelease.mc.player.getMainHandStack().getItem() instanceof CrossbowItem && ItemRelease.mc.player.isUsingItem() && ItemRelease.mc.player.getItemUseTime() >= CrossbowItem.getPullTime((ItemStack)ItemRelease.mc.player.getMainHandStack(), (LivingEntity)ItemRelease.mc.player)) {
            ItemRelease.mc.interactionManager.stopUsingItem((PlayerEntity)ItemRelease.mc.player);
        }
    }
}

