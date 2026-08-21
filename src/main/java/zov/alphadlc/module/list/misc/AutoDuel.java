package zov.alphadlc.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.ModeListSetting;
import zov.alphadlc.util.math.StopWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ModuleInformation(moduleName = "Auto Duel", moduleDesc = "Автоматически рассылает /duel и выбирает кит", moduleCategory = ModuleCategory.MISC)
public class AutoDuel extends Module {

    private final ModeListSetting kits = new ModeListSetting("Разрешённые киты",
            new BooleanSetting("Щиты", false),
            new BooleanSetting("Шипы 3", false),
            new BooleanSetting("Лук", false),
            new BooleanSetting("Тотемы", false),
            new BooleanSetting("НоДебафф", false),
            new BooleanSetting("Шары", true),
            new BooleanSetting("Классик", false),
            new BooleanSetting("Читерский рай", false),
            new BooleanSetting("Без эндер-жемчуга", false)
    );

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");

    private final List<String> sent = new ArrayList<>();
    private final StopWatch duelDelay = new StopWatch();
    private final StopWatch clearSentDelay = new StopWatch();
    private final StopWatch kitChoiceDelay = new StopWatch();
    private final StopWatch duelSetupDelay = new StopWatch();

    @Override
    public void onEnable() {
        super.onEnable();
        sent.clear();
    }

    @Subscribe
    public void onTick(EventTick e) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        List<String> players = getOnlinePlayers();

        if (clearSentDelay.isReached(800L * Math.max(players.size(), 1))) {
            sent.clear();
            clearSentDelay.reset();
        }

        for (String player : players) {
            if (sent.contains(player) || player.equals(mc.player.getGameProfile().getName())) continue;
            if (duelDelay.isReached(1000L)) {
                mc.getNetworkHandler().sendChatCommand("duel " + player);
                sent.add(player);
                duelDelay.reset();
            }
            break;
        }

        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            String title = screen.getTitle().getString();
            if (title.contains("Выбор набора") || title.contains("Kit selection")) {
                handleKitSelection();
            } else if (title.contains("Настройка поединка") || title.contains("Duel setup")) {
                handleDuelSetup();
            }
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (e.getPacket() instanceof GameMessageS2CPacket packet) {
            String text = packet.content().getString().toLowerCase();
            if ((text.contains("начало") && text.contains("через") && text.contains("секунд")) ||
                    text.contains("во время поединка запрещено использовать команды") ||
                    (text.contains("duel") && text.contains("during") && text.contains("forbidden"))) {
                if (isEnabled()) setEnabled(false);
            }
        }
    }

    private List<String> getOnlinePlayers() {
        return mc.getNetworkHandler().getPlayerList().stream()
                .map(entry -> entry.getProfile().getName())
                .filter(name -> NAME_PATTERN.matcher(name).matches())
                .collect(Collectors.toList());
    }

    private void handleKitSelection() {
        List<Integer> slots = new ArrayList<>();
        List<BooleanSetting> settings = kits.getSettings();
        for (int i = 0; i < settings.size(); i++) {
            if (settings.get(i).getValue()) slots.add(i);
        }
        if (slots.isEmpty()) return;

        Collections.shuffle(slots);
        int slotId = slots.get(0);

        if (kitChoiceDelay.isReached(90)) {
            clickSlot(slotId);
            kitChoiceDelay.reset();
        }
    }

    private void handleDuelSetup() {
        if (duelSetupDelay.isReached(90)) {
            clickSlot(0);
            duelSetupDelay.reset();
        }
    }

    private void clickSlot(int slotId) {
        if (mc.interactionManager == null || mc.player == null) return;
        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
    }
}
