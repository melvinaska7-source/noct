package polar.ru.client.modules.impl.misc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.chat.ChatUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.polar;

public class AutoLeave
extends Module {
    public static final AutoLeave INSTANCE = new AutoLeave();
    private static final Set<String> STAFF_PREFIXES = new HashSet<String>(Arrays.asList("supp", "mod", "der", "adm", "wne", "curat", "dev", "yt", "мод", "помо", "адм", "владе", "курато", "сапп", "ютуб", "стажер", "сотрудник"));
    private final FloatSetting leaveDistance = new FloatSetting("Дистанция срабатывания", 5.0f, 3.0f, 50.0f, 1.0f);
    private final ListSetting leaveIfSeen = new ListSetting("Выходить если замечен", new BooleanSetting("Игрок", true), new BooleanSetting("Модератор", false));
    private final ModeSetting leaveType = new ModeSetting("Тип выхода", "В мейн меню", "В мейн меню", "/hub", "/home", "/spawn");
    private final BooleanSetting stopBaritone = new BooleanSetting("Выключать баритон", false);
    private final BooleanSetting leaveDisable = new BooleanSetting("Выключать после выхода", true);
    private int cooldownTicks;

    public AutoLeave() {
        super("AutoLeave", "Выходит с сервера, когда замечает поблизости игрока", Module.ModuleCategory.MISC);
        this.addSettings(this.leaveDistance, this.leaveIfSeen, this.leaveType, this.stopBaritone, this.leaveDisable);
    }

    @Override
    public void onEnable() {
        this.cooldownTicks = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.cooldownTicks = 0;
        super.onDisable();
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (AutoLeave.mc.player == null || AutoLeave.mc.world == null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            --this.cooldownTicks;
            return;
        }
        float maxDistance = this.leaveDistance.get();
        for (PlayerEntity player : AutoLeave.mc.world.getPlayers()) {
            if (player == null || player == AutoLeave.mc.player || !(AutoLeave.mc.player.distanceTo((Entity)player) <= maxDistance) || !this.shouldLeaveFor(player)) continue;
            this.triggerLeave();
            break;
        }
    }

    private boolean shouldLeaveFor(PlayerEntity player) {
        if (this.isModerator(player)) {
            return this.leaveIfSeen.is("Модератор");
        }
        return this.leaveIfSeen.is("Игрок");
    }

    private boolean isModerator(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        String name = player.getName().getString();
        if (polar.INSTANCE != null && polar.INSTANCE.staffStorage != null && polar.INSTANCE.staffStorage.isStaff(name)) {
            return true;
        }
        Team team = player.getScoreboardTeam();
        if (team == null) {
            return false;
        }
        String prefix = team.getPrefix().getString().toLowerCase(Locale.ROOT);
        for (String candidate : STAFF_PREFIXES) {
            if (!prefix.contains(candidate)) continue;
            return true;
        }
        return false;
    }

    private void triggerLeave() {
        this.tryStopBaritone();
        switch (this.leaveType.getCurrent()) {
            case "В мейн меню": {
                this.disconnectLeave();
                break;
            }
            case "/hub": {
                this.commandLeave("hub");
                break;
            }
            case "/home": {
                this.commandLeave("home home");
                break;
            }
            case "/spawn": {
                this.commandLeave("spawn");
            }
        }
    }

    private void tryStopBaritone() {
        if (!this.stopBaritone.isState() || mc.getNetworkHandler() == null) {
            return;
        }
        mc.getNetworkHandler().sendChatMessage("#stop");
    }

    private void disconnectLeave() {
        if (mc.getNetworkHandler() == null) {
            ChatUtils.sendMessage("Модуль не работает в одиночном мире");
            return;
        }
        mc.getNetworkHandler().getConnection().disconnect((Text)Text.literal((String)"AutoLeave"));
        if (this.leaveDisable.isState()) {
            this.toggle();
        }
    }

    private void commandLeave(String command) {
        if (mc.getNetworkHandler() == null) {
            ChatUtils.sendMessage("AutoLeave нельзя использовать в одиночной игре!");
            return;
        }
        mc.getNetworkHandler().sendChatCommand(command);
        int n2 = this.cooldownTicks = this.leaveDisable.isState() ? 10 : 30;
        if (this.leaveDisable.isState()) {
            this.toggle();
        }
    }
}

