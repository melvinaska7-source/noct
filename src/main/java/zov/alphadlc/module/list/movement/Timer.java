package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.math.TimerUtil;

@ModuleInformation(moduleName = "Timer", moduleDesc = "Изменяет скорость игры", moduleCategory = ModuleCategory.MOVEMENT)
public class Timer extends Module {
    
    private final SliderSetting speed = new SliderSetting("Скорость", 2.0f, 0.1f, 10.0f, 0.1f);

    @Subscribe
    private void onTick(EventTick e) {
        TimerUtil.setTimer((float) speed.getValue());
    }

    @Override
    public void onDisable() {
        TimerUtil.setTimer(1.0f);
        super.onDisable();
    }
}
