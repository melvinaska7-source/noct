package polar.ru.client.modules.impl.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.math.TimerUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class AutoDuel
extends Module {
    public static AutoDuel INSTANCE = new AutoDuel();
    public ModeSetting mode = new ModeSetting("Режим", "Шары", "Щит", "Шипы", "Лук", "Тотемы", "Нодебафф", "Шары", "Классик", "Читер", "Незер");
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");
    private final List<String> sent = new ArrayList<String>();
    private final TimerUtils duelT = new TimerUtils();
    private final TimerUtils clrT = new TimerUtils();
    private final TimerUtils pickT = new TimerUtils();
    private final TimerUtils setT = new TimerUtils();
    private Vec3d lastPos;
    private boolean inDuel;

    public AutoDuel() {
        super("AutoDuel", "Автоматически кидает дуель", Module.ModuleCategory.MISC);
        this.addSettings(this.mode);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.sent.clear();
        this.inDuel = false;
        if (AutoDuel.mc.player != null) {
            this.lastPos = AutoDuel.mc.player.getPos();
        }
        this.duelT.reset();
        this.clrT.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.sent.clear();
        this.inDuel = false;
    }

    @EventLink
        public void onUpdate(EventUpdate e2) {
        if (AutoDuel.mc.player == null || AutoDuel.mc.world == null || this.inDuel) {
            return;
        }
        if (this.lastPos != null && AutoDuel.mc.player.getPos().distanceTo(this.lastPos) > 500.0) {
            this.toggle();
            return;
        }
        this.lastPos = AutoDuel.mc.player.getPos();
        if (this.clrT.getElapsedTime() >= 30000L) {
            this.sent.clear();
            this.clrT.reset();
        }
        if (this.duelT.getElapsedTime() >= 1000L) {
            this.sendDuel();
            this.duelT.reset();
        }
        this.handleGui();
    }

    @EventLink
    public void onPacket(EventPacket e2) {
        GameMessageS2CPacket p2;
        String msg;
        Packet<?> var_2596_2;
        if (AutoDuel.mc.player == null || AutoDuel.mc.world == null) {
            return;
        }
        if (e2.getType() == EventPacket.Type.RECEIVE && (var_2596_2 = e2.getPacket()) instanceof GameMessageS2CPacket && ((msg = (p2 = (GameMessageS2CPacket)var_2596_2).content().getString().toLowerCase()).contains("начало") && msg.contains("через") && msg.contains("секунд") || msg.contains("поединок начался") || msg.contains("во время поединка"))) {
            this.inDuel = true;
            this.toggle();
        }
    }

    private void sendDuel() {
        for (String p2 : this.getPlayers()) {
            if (this.sent.contains(p2) || p2.equals(AutoDuel.mc.player.getName().getString())) continue;
            mc.getNetworkHandler().sendChatCommand("duel " + p2);
            this.sent.add(p2);
            break;
        }
    }

    private void handleGui() {
        Screen var_437_2 = AutoDuel.mc.currentScreen;
        if (!(var_437_2 instanceof GenericContainerScreen)) {
            return;
        }
        GenericContainerScreen s2 = (GenericContainerScreen)var_437_2;
        int id = ((GenericContainerScreenHandler)s2.getScreenHandler()).syncId;
        String t2 = s2.getTitle().getString();
        if (t2.contains("Выбор набора") && this.pickT.getElapsedTime() >= 150L) {
            AutoDuel.mc.interactionManager.clickSlot(id, this.getModeSlot(), 0, SlotActionType.QUICK_MOVE, (PlayerEntity)AutoDuel.mc.player);
            this.pickT.reset();
        } else if (t2.contains("Настройка поединка") && this.setT.getElapsedTime() >= 150L) {
            AutoDuel.mc.interactionManager.clickSlot(id, 0, 0, SlotActionType.QUICK_MOVE, (PlayerEntity)AutoDuel.mc.player);
            this.setT.reset();
        }
    }

    private int getModeSlot() {
        if (this.mode.is("Щит")) {
            return 0;
        }
        if (this.mode.is("Шипы")) {
            return 1;
        }
        if (this.mode.is("Лук")) {
            return 2;
        }
        if (this.mode.is("Тотемы")) {
            return 3;
        }
        if (this.mode.is("Нодебафф")) {
            return 4;
        }
        if (this.mode.is("Шары")) {
            return 5;
        }
        if (this.mode.is("Классик")) {
            return 6;
        }
        if (this.mode.is("Читер")) {
            return 7;
        }
        if (this.mode.is("Незер")) {
            return 8;
        }
        return 5;
    }

    private List<String> getPlayers() {
        ArrayList<String> list = new ArrayList<String>();
        if (mc.getNetworkHandler() == null) {
            return list;
        }
        for (PlayerListEntry e2 : mc.getNetworkHandler().getPlayerList()) {
            String n2 = e2.getProfile().getName();
            if (!NAME_PATTERN.matcher(n2).matches()) continue;
            list.add(n2);
        }
        return list;
    }
}

