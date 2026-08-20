package polar.ru.client.modules.impl.movement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.PlayerInput;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventPacket;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.movement.InputUtils;
import polar.ru.api.utils.network.NetworkUtils;
import polar.ru.api.utils.script.ScriptManager;
import polar.ru.api.utils.script.ScriptTask;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.client.ui.MenuPanel;

public class InventoryWalk
extends Module {
    public static InventoryWalk INSTANCE = new InventoryWalk();
    private final ModeSetting mode = new ModeSetting("Режим", "Обычный", "Обычный", "Обход", "Легит");
    private final List<Packet<?>> delayedPackets = new CopyOnWriteArrayList();
    private final ScriptManager scriptManager = new ScriptManager();
    private boolean processingPackets;
    private boolean movedInGui;
    public boolean swapBypass;

    public InventoryWalk() {
        super("GuiMove", "Позволяет перемещаться с открытым инвентарём, не прерывая процесс передвижения", Module.ModuleCategory.MOVEMENT);
        this.addSettings(this.mode);
    }

    public void setExternalMovementLock(boolean lock) {
        if (lock) {
            InputUtils.lockMovement();
        } else {
            InputUtils.unlockMovement();
        }
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (InventoryWalk.mc.player == null || InventoryWalk.mc.world == null) {
            this.cleanup();
            return;
        }
        this.scriptManager.tick(event);
        if (!this.isMovementScreen()) {
            if (!this.processingPackets && this.delayedPackets.isEmpty()) {
                this.movedInGui = false;
            }
            return;
        }
        if (InventoryWalk.mc.currentScreen instanceof ChatScreen || InventoryWalk.mc.currentScreen instanceof SignEditScreen) {
            return;
        }
        if (InventoryWalk.mc.currentScreen instanceof HandledScreen && !(InventoryWalk.mc.currentScreen instanceof InventoryScreen)) {
            return;
        }
        this.movedInGui |= this.movementKeysDown() && !this.delayedPackets.isEmpty();
        if (!InputUtils.isMovementLocked()) {
            InputUtils.syncMovementKeys(this.movementKeys(false));
        }
    }

    /*
     * Unable to fully structure code
     */
    @EventLink
    public void onPacket(EventPacket event) {
        if (InventoryWalk.mc.player == null || InventoryWalk.mc.world == null) {
            return;
        }
        if (event.getType() != EventPacket.Type.SEND) {
            return;
        }
        if (this.mode.is("Обычный") || this.swapBypass) {
            return;
        }
        boolean moving = this.movedInGui || this.movementKeysDown();
        this.movedInGui |= moving && !this.delayedPackets.isEmpty();
        Packet<?> packet = event.getPacket();
        if (packet instanceof ClickSlotC2SPacket) {
            ClickSlotC2SPacket clickPacket = (ClickSlotC2SPacket)packet;
            if (InventoryWalk.mc.currentScreen instanceof InventoryScreen && moving && this.shouldAllowMovement()) {
                this.delayedPackets.add(clickPacket);
                event.cancel();
            }
        } else if (packet instanceof CloseHandledScreenC2SPacket) {
            CloseHandledScreenC2SPacket closePacket = (CloseHandledScreenC2SPacket)packet;
            if (closePacket.getSyncId() == 0 && moving && !this.processingPackets) {
                if (this.delayedPackets.isEmpty()) {
                    event.cancel();
                } else {
                    this.delayedPackets.add(closePacket);
                    event.cancel();
                    this.processDelayedPackets();
                }
            }
        }
        if (this.processingPackets && packet instanceof PlayerInputC2SPacket) {
            event.cancel();
            NetworkUtils.sendSilentPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
        }
        if (!this.delayedPackets.isEmpty() && this.processingPackets && (packet instanceof HandSwingC2SPacket || packet instanceof PlayerInteractEntityC2SPacket || packet instanceof PlayerInteractItemC2SPacket || packet instanceof PlayerInteractBlockC2SPacket)) {
            event.cancel();
        }
    }

    private void processDelayedPackets() {
        this.processingPackets = true;
        ScriptTask task = new ScriptTask();
        this.scriptManager.addTask(task);
        if (this.mode.is("Обход")) {
            task.schedule(e2 -> {
                InputUtils.lockMovement();
                return true;
            }).schedule(e2 -> {
                for (Packet<?> p2 : this.delayedPackets) {
                    NetworkUtils.sendSilentPacket(p2);
                }
                this.delayedPackets.clear();
                this.processingPackets = false;
                this.movedInGui = false;
                return true;
            }).schedule(e2 -> {
                for (Packet<?> p2 : this.delayedPackets) {
                    if (!(p2 instanceof CloseHandledScreenC2SPacket)) continue;
                    NetworkUtils.sendSilentPacket(p2);
                }
                InputUtils.unlockMovement();
                return true;
            });
        } else {
            task.schedule(e2 -> {
                InputUtils.lockMovement();
                return true;
            }).schedule(e2 -> true).schedule(e2 -> true).schedule(e2 -> {
                for (Packet<?> p2 : this.delayedPackets) {
                    if (p2 instanceof CloseHandledScreenC2SPacket) continue;
                    NetworkUtils.sendSilentPacket(p2);
                }
                return true;
            }).schedule(e2 -> true).schedule(e2 -> {
                for (Packet<?> p2 : this.delayedPackets) {
                    if (!(p2 instanceof CloseHandledScreenC2SPacket)) continue;
                    NetworkUtils.sendSilentPacket(p2);
                }
                this.delayedPackets.clear();
                return true;
            }).schedule(e2 -> true).schedule(e2 -> {
                InputUtils.unlockMovement();
                this.processingPackets = false;
                this.movedInGui = false;
                return true;
            });
        }
    }

    private boolean movementKeysDown() {
        if (mc == null || mc.getWindow() == null || InventoryWalk.mc.options == null) {
            return false;
        }
        boolean inventory = InventoryWalk.mc.currentScreen instanceof InventoryScreen || InventoryWalk.mc.currentScreen instanceof CreativeInventoryScreen;
        for (KeyBinding binding : this.movementKeys(true)) {
            if (inventory && (binding == InventoryWalk.mc.options.sneakKey || binding == InventoryWalk.mc.options.sprintKey && !InventoryWalk.mc.options.forwardKey.equals(InventoryWalk.mc.options.sprintKey)) || !InputUtil.isKeyPressed((long)mc.getWindow().getHandle(), (int)binding.getDefaultKey().getCode())) continue;
            return true;
        }
        return false;
    }

    private KeyBinding[] movementKeys(boolean includeModifiers) {
        KeyBinding[] class_304Array;
        if (includeModifiers) {
            KeyBinding[] class_304Array2 = new KeyBinding[7];
            class_304Array2[0] = InventoryWalk.mc.options.forwardKey;
            class_304Array2[1] = InventoryWalk.mc.options.backKey;
            class_304Array2[2] = InventoryWalk.mc.options.rightKey;
            class_304Array2[3] = InventoryWalk.mc.options.leftKey;
            class_304Array2[4] = InventoryWalk.mc.options.jumpKey;
            class_304Array2[5] = InventoryWalk.mc.options.sneakKey;
            class_304Array = class_304Array2;
            class_304Array2[6] = InventoryWalk.mc.options.sprintKey;
        } else {
            KeyBinding[] class_304Array3 = new KeyBinding[5];
            class_304Array3[0] = InventoryWalk.mc.options.forwardKey;
            class_304Array3[1] = InventoryWalk.mc.options.backKey;
            class_304Array3[2] = InventoryWalk.mc.options.rightKey;
            class_304Array3[3] = InventoryWalk.mc.options.leftKey;
            class_304Array = class_304Array3;
            class_304Array3[4] = InventoryWalk.mc.options.jumpKey;
        }
        return class_304Array;
    }

    private boolean shouldAllowMovement() {
        return InventoryWalk.mc.player != null && InventoryWalk.mc.player.currentScreenHandler != null && InventoryWalk.mc.player.currentScreenHandler.slots.size() >= 27;
    }

    private boolean isMovementScreen() {
        return InventoryWalk.mc.currentScreen instanceof InventoryScreen || InventoryWalk.mc.currentScreen instanceof MenuPanel || InventoryWalk.mc.currentScreen instanceof CreativeInventoryScreen;
    }

    private void cleanup() {
        this.delayedPackets.clear();
        this.processingPackets = false;
        this.movedInGui = false;
        InputUtils.unlockMovement();
        this.scriptManager.clear();
    }

    @Override
    public void onDisable() {
        this.cleanup();
        this.swapBypass = false;
        super.onDisable();
    }
}

