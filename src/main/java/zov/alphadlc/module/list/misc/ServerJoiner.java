package zov.alphadlc.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.player.other.InventoryUtil;

@ModuleInformation(moduleName = "ServerJoiner", moduleDesc = "Автовход на сервер через компас/GUI", moduleCategory = ModuleCategory.MISC)
public class ServerJoiner extends Module {

    private final ModeSetting server = new ModeSetting("Сервер", "РиллиВорлд", "РиллиВорлд", "СпукиТайм дуэли");
    private final SliderSetting griefNumber = new SliderSetting("Номер грифа", 1, 1, 54, 1)
            .setVisible(() -> server.is("РиллиВорлд"));
    // TextSetting в OneTap нет — используем ModeSetting с предустановленными названиями компаса.
    private final ModeSetting compassName = new ModeSetting("Название компаса", "выберите режим",
            "выберите режим", "дуэли", "мясорубка", "квадромясо")
            .setVisible(() -> server.is("СпукиТайм дуэли"));

    private long lastActionTime;

    @Subscribe
    private void onTick(EventTick ignored) {
        if (!isEnabled()) return;

        // Аварийное отключение по Insert.
        if (mc.getWindow() != null
                && GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_INSERT) == GLFW.GLFW_PRESS) {
            setEnabled(false);
            return;
        }

        if (mc.player == null || mc.world == null) return;

        if (mc.currentScreen == null) {
            if (server.is("СпукиТайм дуэли")) {
                if (System.currentTimeMillis() - lastActionTime > 250) useCompass();
            } else if (mc.player.age < 5) {
                useCompass();
            }
            return;
        }

        if (mc.currentScreen instanceof GenericContainerScreen container) {
            navigateGui(container);
        }
    }

    private void navigateGui(GenericContainerScreen container) {
        var handler = container.getScreenHandler();
        String title = container.getTitle().getString().toLowerCase();

        for (int i = 0; i < handler.slots.size(); i++) {
            if (!handler.slots.get(i).hasStack()) continue;
            String slotName = handler.slots.get(i).getStack().getName().getString().toLowerCase();

            if (server.is("РиллиВорлд")) {
                if (slotName.contains("гриферское выживание") && canClick()) {
                    click(handler.syncId, i, SlotActionType.PICKUP);
                }
                int number = griefNumber.getIntValue();
                if (slotName.contains("гриф #" + number) && canClick()) {
                    click(handler.syncId, i, SlotActionType.PICKUP);
                }
            } else if (server.is("СпукиТайм дуэли") && title.contains("выберите режим") && canClick()) {
                click(handler.syncId, 14, SlotActionType.QUICK_MOVE);
            }
        }
    }

    private void click(int syncId, int slot, SlotActionType action) {
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(syncId, slot, 0, action, mc.player);
        lastActionTime = System.currentTimeMillis();
    }

    private void useCompass() {
        // Защита disconnect-пути: обработчик может вызвать это при отсутствии игрока/сети.
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || mc.interactionManager == null) {
            return;
        }

        int slot = InventoryUtil.searchItemHotbar(Items.COMPASS);
        if (slot == -1) return;

        mc.player.getInventory().selectedSlot = slot;
        mc.interactionManager.syncSelectedSlot();
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        lastActionTime = System.currentTimeMillis();
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (!isEnabled() || e.getType() != EventPacket.Type.RECEIVE) return;
        if (!(e.getPacket() instanceof DisconnectS2CPacket packet)) return;

        String message = packet.reason().getString().toLowerCase();
        boolean retry = message.contains("сервер переполнен")
                || message.contains("подождите")
                || message.contains("вы уже подключены")
                || message.contains("вы были кикнуты")
                || message.contains("большой поток игроков")
                || message.contains("сервер заполнен");

        // На момент дисконнекта player/world могут быть null — useCompass это учитывает.
        if (retry) useCompass();
    }

    private boolean canClick() {
        return System.currentTimeMillis() - lastActionTime > 50;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        lastActionTime = 0;
    }
}
