package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.util.hit.HitResult;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.bot.BotSessionManager;

@ModuleInformation(moduleName = "Tape Mouse", moduleDesc = "Автоматические клики для фоновых ботов", moduleCategory = ModuleCategory.PLAYER)
public class TapeMouse extends Module {
    private final SliderSetting cps = new SliderSetting("CPS", 1, 0, 2, 0.05f);
    private final SliderSetting delay = new SliderSetting("Доп. задержка", 0, 0, 1000, 10);
    private final BooleanSetting rightClick = new BooleanSetting("Правая кнопка", false);
    private final BooleanSetting onlyEntity = new BooleanSetting("Только по entity", false);
    private final BooleanSetting onlyBots = new BooleanSetting("Только боты", true);

    private long lastClick;

    @Subscribe
    private void onTick(EventTick eventTick) {
        long cpsDelay = cps.getValue() > 0 ? (long) (1000.0f / cps.getValue()) : 1000L;
        long effectiveDelay = Math.max(cpsDelay, (long) delay.getValue());
        long now = System.currentTimeMillis();

        if (now - lastClick < effectiveDelay) {
            return;
        }

        if (!onlyBots.getValue()) {
            clickForLocalPlayer();
        }

        if (!BotSessionManager.getConnections().isEmpty()) {
            BotSessionManager.pulseBots(rightClick.getValue());
        }

        lastClick = now;
    }

    private void clickForLocalPlayer() {
        if (mc.player == null || mc.currentScreen != null) return;

        if (rightClick.getValue()) {
            mc.doItemUse();
            return;
        }

        if (onlyEntity.getValue() && (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY)) {
            return;
        }

        mc.doAttack();
    }
}
