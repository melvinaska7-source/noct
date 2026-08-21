package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import zov.alphadlc.event.list.EventCloseInv;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.event.list.EventPlayerUpdate;
import zov.alphadlc.event.list.MoveInputEvent;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.ModeSetting;
import zov.alphadlc.util.packet.NetworkUtils;
import zov.alphadlc.util.player.move.MoveUtil;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(moduleName = "Gui Move", moduleDesc = "Взаимодействие с инвентарём при движении", moduleCategory = ModuleCategory.MOVEMENT)
public class GuiMove extends Module {

    private final ModeSetting mode = new ModeSetting("Обход", "Легит",
            "Vanilla", "Reallyworld", "Funtime", "Holyworld", "SpookyTime", "LonyGrief", "Легит");

    private final List<Packet<?>> pending = new ArrayList<>();
    private int freezeTicks;
    private boolean flushPending;

    @Override
    public void onDisable() {
        super.onDisable();
        flush();
        pending.clear();
        freezeTicks = 0;
        flushPending = false;
    }

    private boolean isVanilla() {
        return mode.is("Vanilla");
    }

    private boolean isLegit() {
        return mode.is("Легит");
    }

    private boolean usesBypass() {
        return !isVanilla() && !isLegit();
    }

    // Окно заморозки движения под каждый режим.
    private int freezeWindow() {
        return switch (mode.getValue()) {
            case "Reallyworld" -> 3;
            case "Funtime" -> 4;
            case "Holyworld" -> 5;
            case "SpookyTime" -> 3;
            case "LonyGrief" -> 2;
            default -> 0;
        };
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (mc.player == null || isVanilla()) return;
        if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) return;

        // Сервер пытается закрыть экран во время накопления кликов — держим окно открытым.
        if (e.getType() == EventPacket.Type.RECEIVE
                && e.getPacket() instanceof CloseScreenS2CPacket
                && usesBypass() && !pending.isEmpty()) {
            e.cancelEvent();
            return;
        }

        if (e.getType() != EventPacket.Type.SEND) return;
        if (!(e.getPacket() instanceof ClickSlotC2SPacket click)) return;
        if (!(mc.currentScreen instanceof InventoryScreen)) return;
        if (!MoveUtil.hasPlayerMovement()) return;

        if (isLegit()) {
            // Легит: просто не отправляем клик во время движения.
            e.cancelEvent();
            return;
        }

        // Bypass-режимы: копим клики в очередь, отправим тихо при закрытии.
        pending.add(click);
        e.cancelEvent();
    }

    @Subscribe
    private void onCloseInv(EventCloseInv e) {
        if (mc.player == null || isVanilla() || isLegit()) return;
        if (pending.isEmpty()) return;

        e.cancelEvent();
        flushPending = true;
        freezeTicks = freezeWindow();
    }

    @Subscribe
    private void onMoveInput(MoveInputEvent e) {
        if (freezeTicks > 0) e.cancel();
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate ignored) {
        if (mc.player == null || isVanilla()) return;

        if (freezeTicks > 0) {
            freezeTicks--;
            if (freezeTicks == 0 && flushPending) {
                flush();
                NetworkUtils.sendSilentPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                flushPending = false;
            }
        }

        if (mc.currentScreen == null || mc.currentScreen instanceof ChatScreen) return;

        for (KeyBinding key : new KeyBinding[]{
                mc.options.forwardKey, mc.options.backKey,
                mc.options.leftKey, mc.options.rightKey,
                mc.options.jumpKey
        }) {
            key.setPressed(InputUtil.isKeyPressed(
                    mc.getWindow().getHandle(),
                    InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey()).getCode()
            ));
        }
    }

    private void flush() {
        if (pending.isEmpty()) return;
        pending.forEach(NetworkUtils::sendSilentPacket);
        pending.clear();
    }
}
