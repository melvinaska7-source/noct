package polar.ru.client.modules.impl.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.player.MoveUtils;
import polar.ru.api.utils.player.ViaProtocolUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;

public class Sprint
extends Module {
    public static Sprint INSTANCE = new Sprint();
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private final BooleanSetting keepInWater = new BooleanSetting("Сохранять в воде", false);
    private static boolean sprinting;
    private static long time;
    private static int pauseDepth;
    private static boolean restoreAfterPause;
    private ClientPlayerEntity lastPlayer;

    public Sprint() {
        super("Sprint", "Автоматический бег", Module.ModuleCategory.MOVEMENT);
        this.addSettings(this.keepInWater);
    }

    @Override
    public void onEnable() {
        Sprint.resetPauseState();
        sprinting = true;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        Sprint.resetPauseState();
        sprinting = false;
        this.lastPlayer = null;
        if (Sprint.mc.options != null) {
            Sprint.mc.options.sprintKey.setPressed(false);
        }
        if (Sprint.mc.player != null) {
            Sprint.mc.player.setSprinting(false);
        }
        super.onDisable();
    }

    @EventLink
        public void onEvent(EventUpdate ignored) {
        boolean shouldSprint;
        if (Sprint.mc.player == null) {
            this.lastPlayer = null;
            Sprint.resetPauseState();
            if (Sprint.mc.options != null) {
                Sprint.mc.options.sprintKey.setPressed(false);
            }
            return;
        }
        if (this.lastPlayer != Sprint.mc.player) {
            this.lastPlayer = Sprint.mc.player;
            Sprint.resetPauseState();
            sprinting = true;
        }
        boolean legacyProtocol = ViaProtocolUtils.isTargetProtocolBelowOneNineteen();
        boolean inWater = Sprint.mc.player.isTouchingWater() || Sprint.mc.player.isSubmergedInWater();
        boolean bl = shouldSprint = pauseDepth == 0 && System.currentTimeMillis() >= time && sprinting && MoveUtils.isMoving() && Sprint.mc.player.input.movementForward > 0.0f && (!legacyProtocol || !Sprint.mc.player.horizontalCollision && !Sprint.mc.player.collidedSoftly) && !Sprint.mc.player.isGliding();
        if (this.keepInWater.isState() && inWater && Sprint.mc.player.isSprinting()) {
            shouldSprint = true;
        }
        Sprint.mc.options.sprintKey.setPressed(shouldSprint);
        Sprint.mc.player.setSprinting(shouldSprint);
    }

    public boolean shouldKeepSprintInWater() {
        return this.isEnable() && this.keepInWater.isState();
    }

    public static void pushPause(long delayMs) {
        restoreAfterPause |= Sprint.shouldRestoreAfterPause();
        ++pauseDepth;
        time = Math.max(time, System.currentTimeMillis() + Math.max(0L, delayMs));
        sprinting = false;
        if (Sprint.CLIENT.options != null) {
            Sprint.CLIENT.options.sprintKey.setPressed(false);
        }
        if (Sprint.CLIENT.player != null) {
            Sprint.CLIENT.player.setSprinting(false);
        }
    }

    public static void popPause() {
        if (pauseDepth > 0) {
            --pauseDepth;
        }
        if (pauseDepth > 0) {
            return;
        }
        time = 0L;
        sprinting = restoreAfterPause;
        restoreAfterPause = false;
    }

    private static boolean shouldRestoreAfterPause() {
        if (Sprint.CLIENT.player != null && Sprint.CLIENT.player.isSprinting()) {
            return true;
        }
        return ModuleClass.sprint != null && ModuleClass.sprint.isEnable() && sprinting;
    }

    private static void resetPauseState() {
        pauseDepth = 0;
        restoreAfterPause = false;
        time = 0L;
    }
    public static boolean isSprinting() {
        return sprinting;
    }
    public static void setSprinting(boolean sprinting) {
        Sprint.sprinting = sprinting;
    }
    public static long getTime() {
        return time;
    }
    public static void setTime(long time) {
        Sprint.time = time;
    }

    static {
        time = 0L;
        pauseDepth = 0;
        restoreAfterPause = false;
    }
}

