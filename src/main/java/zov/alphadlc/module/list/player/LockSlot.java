package zov.alphadlc.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.chat.ChatUtil;

import java.util.HashSet;
import java.util.Set;

@ModuleInformation(
    moduleName = "LockSlot",
    moduleDesc = "Блокировка выброса предметов из указанных слотов",
    moduleCategory = ModuleCategory.PLAYER,
    moduleKeybind = -1
)
public class LockSlot extends Module {

    private final SliderSetting slot1 = new SliderSetting("Слот 1", 0, 0, 8, 1);
    private final SliderSetting slot2 = new SliderSetting("Слот 2", 0, 0, 8, 1);
    private final SliderSetting slot3 = new SliderSetting("Слот 3", 0, 0, 8, 1);
    private final BooleanSetting lockAll = new BooleanSetting("Блокировать все", false);

    private final Set<Integer> lockedSlots = new HashSet<>();

    @Override
    public void onEnable() {
        lockedSlots.clear();
        if (!lockAll.getValue()) {
            if (slot1.getValue() > 0) lockedSlots.add((int) slot1.getValue() - 1);
            if (slot2.getValue() > 0) lockedSlots.add((int) slot2.getValue() - 1);
            if (slot3.getValue() > 0) lockedSlots.add((int) slot3.getValue() - 1);
        }
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null) return;

        if (lockAll.getValue()) {
            for (int i = 0; i < 9; i++) {
                if (!mc.player.getInventory().getStack(i).isEmpty()) {
                    lockedSlots.add(i);
                }
            }
        }
    }

    public boolean isSlotLocked(int slot) {
        return isEnabled() && lockedSlots.contains(slot);
    }

    public void addLockedSlot(int slot) {
        if (!lockedSlots.contains(slot)) {
            lockedSlots.add(slot);
            ChatUtil.send("Выброс предмета из слота " + (slot + 1) + " заблокирован");
        }
    }

    public void removeLockedSlot(int slot) {
        lockedSlots.remove(slot);
    }

    public void clearLockedSlots() {
        lockedSlots.clear();
    }

    public Set<Integer> getLockedSlots() {
        return new HashSet<>(lockedSlots);
    }
}
