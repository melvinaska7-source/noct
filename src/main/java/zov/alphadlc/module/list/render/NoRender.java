package zov.alphadlc.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.effect.StatusEffects;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeListSetting;

@ModuleInformation(moduleName = "Removals", moduleDesc = "Убирает лишние элементы интерфейса", moduleCategory = ModuleCategory.RENDER)
public class NoRender extends Module {

    public final ModeListSetting elements = new ModeListSetting("Убрать элементы",
            new BooleanSetting("Огонь",true),
            new BooleanSetting("Размытие в воде",true),
            new BooleanSetting("Зрение в блоках",true),
            new BooleanSetting("Камераклип",true),
            new BooleanSetting("Тряска камеры",true),
            new BooleanSetting("Тотем",true),
            new BooleanSetting("Удочка",true),
            new BooleanSetting("Дождь",true),
            new BooleanSetting("Плохие эффекты",true)
    );

    @Subscribe
    public void onUpdate(EventTick e) {
        if (mc.player == null) return;
        
        if (elements.isEnabled("Тряска камеры")) {
            mc.options.getDamageTiltStrength().setValue(0.0);
        } else {
            mc.options.getDamageTiltStrength().setValue(0.5);
        }
        
        if (elements.isEnabled("Плохие эффекты")) {
            // Убираем все плохие эффекты
            mc.player.removeStatusEffect(StatusEffects.BLINDNESS);
            mc.player.removeStatusEffect(StatusEffects.NAUSEA);
            mc.player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
            mc.player.removeStatusEffect(StatusEffects.WEAKNESS);
            mc.player.removeStatusEffect(StatusEffects.SLOWNESS);
            mc.player.removeStatusEffect(StatusEffects.WITHER);
            mc.player.removeStatusEffect(StatusEffects.POISON);
            mc.player.removeStatusEffect(StatusEffects.HUNGER);
            mc.player.removeStatusEffect(StatusEffects.LEVITATION);
            mc.player.removeStatusEffect(StatusEffects.UNLUCK);
            mc.player.removeStatusEffect(StatusEffects.DARKNESS);
        }
    }

    @Override
    public void onDisable() {
        mc.options.getDamageTiltStrength().setValue(0.5);
        super.onDisable();
    }
}