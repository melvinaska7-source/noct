package polar.ru.api.utils.baritone;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class BaritoneAntiStuck {
    private static final String PROTECTED_BLOCK_MESSAGE = "Извините, но вы не можете сломать блок здесь";
    private static final long STUCK_TIMEOUT_MS = 7000L;
    private static final double PROGRESS_DISTANCE_SQ = 1.0;
    private static final int RECOVERY_TICKS = 12;
    private static final double PRIVATE_ESCAPE_DISTANCE_SQ = 2500.0;
    private static final long PRIVATE_ESCAPE_TIMEOUT_MS = 25000L;
    private static final double SIDE_OFFSET = 0.95;
    private static final double FORWARD_OFFSET = 0.35;
    private static final String BARITONE_API_CLASS = "baritone.api.BaritoneAPI";
    private static final String INPUT_ENUM_CLASS = "baritone.api.utils.input.Input";
    private static Vec3d anchorPos;
    private static long lastProgressAtMs;
    private static int recoveryTicksRemaining;
    private static boolean strafeRightNext;
    private static boolean privateEscapePending;
    private static boolean privateEscapeActive;
    private static boolean privateEscapeRight;
    private static Vec3d privateEscapeStartPos;
    private static long privateEscapeStartedAtMs;

    private BaritoneAntiStuck() {
    }

    public static void onGameMessage(String message) {
        if (message == null || !message.contains(PROTECTED_BLOCK_MESSAGE)) {
            return;
        }
        privateEscapePending = true;
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            BaritoneAntiStuck.resetState();
            return;
        }
        try {
            Object baritone = BaritoneAntiStuck.getPrimaryBaritone();
            if (baritone == null) {
                BaritoneAntiStuck.resetState();
                return;
            }
            Object pathing = BaritoneAntiStuck.invoke(baritone, "getPathingBehavior");
            Object input = BaritoneAntiStuck.invoke(baritone, "getInputOverrideHandler");
            if (pathing == null || input == null || !Boolean.TRUE.equals(BaritoneAntiStuck.invoke(pathing, "isPathing"))) {
                BaritoneAntiStuck.clearRecovery(input);
                BaritoneAntiStuck.resetTracking();
                return;
            }
            long now = System.currentTimeMillis();
            Vec3d currentPos = mc.player.getPos();
            if (anchorPos == null) {
                anchorPos = currentPos;
                lastProgressAtMs = now;
            }
            if (privateEscapePending && BaritoneAntiStuck.isMiningNow(mc, input)) {
                BaritoneAntiStuck.startPrivateEscape(mc, currentPos);
                privateEscapePending = false;
            }
            if (privateEscapeActive) {
                if (currentPos.squaredDistanceTo(privateEscapeStartPos) >= 2500.0 || now - privateEscapeStartedAtMs >= 25000L) {
                    BaritoneAntiStuck.clearAllKeys(input);
                    privateEscapeActive = false;
                    anchorPos = currentPos;
                    lastProgressAtMs = now;
                    return;
                }
                BaritoneAntiStuck.applyPrivateEscapeInput(mc, input);
                anchorPos = currentPos;
                lastProgressAtMs = now;
                return;
            }
            if (recoveryTicksRemaining > 0) {
                BaritoneAntiStuck.applyRecoveryInput(mc, input);
                if (--recoveryTicksRemaining <= 0) {
                    BaritoneAntiStuck.clearAllKeys(input);
                    anchorPos = mc.player.getPos();
                    lastProgressAtMs = now;
                }
                return;
            }
            if (BaritoneAntiStuck.isMiningNow(mc, input)) {
                anchorPos = currentPos;
                lastProgressAtMs = now;
                return;
            }
            if (!BaritoneAntiStuck.isTryingToMove(input)) {
                anchorPos = currentPos;
                lastProgressAtMs = now;
                return;
            }
            if (currentPos.squaredDistanceTo(anchorPos) >= 1.0) {
                anchorPos = currentPos;
                lastProgressAtMs = now;
                return;
            }
            if (now - lastProgressAtMs < 7000L) {
                return;
            }
            recoveryTicksRemaining = 12;
            strafeRightNext = BaritoneAntiStuck.chooseRecoverySide(mc, strafeRightNext, true);
            BaritoneAntiStuck.applyRecoveryInput(mc, input);
            anchorPos = currentPos;
            lastProgressAtMs = now;
        }
        catch (Throwable ignored) {
            BaritoneAntiStuck.resetState();
        }
    }

    private static Object getPrimaryBaritone() throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName(BARITONE_API_CLASS);
        Object provider = apiClass.getMethod("getProvider", new Class[0]).invoke(null, new Object[0]);
        return provider == null ? null : provider.getClass().getMethod("getPrimaryBaritone", new Class[0]).invoke(provider, new Object[0]);
    }

    private static boolean isMiningNow(MinecraftClient mc, Object input) throws ReflectiveOperationException {
        return mc.interactionManager != null && mc.interactionManager.isBreakingBlock() || BaritoneAntiStuck.isInputForcedDown(input, "CLICK_LEFT");
    }

    private static boolean isTryingToMove(Object input) throws ReflectiveOperationException {
        return BaritoneAntiStuck.isInputForcedDown(input, "MOVE_FORWARD") || BaritoneAntiStuck.isInputForcedDown(input, "MOVE_BACK") || BaritoneAntiStuck.isInputForcedDown(input, "MOVE_LEFT") || BaritoneAntiStuck.isInputForcedDown(input, "MOVE_RIGHT") || BaritoneAntiStuck.isInputForcedDown(input, "JUMP");
    }

    private static void startPrivateEscape(MinecraftClient mc, Vec3d currentPos) {
        privateEscapeActive = true;
        privateEscapeStartPos = currentPos;
        privateEscapeStartedAtMs = System.currentTimeMillis();
        privateEscapeRight = BaritoneAntiStuck.chooseRecoverySide(mc, privateEscapeRight, false);
    }

    private static void applyRecoveryInput(MinecraftClient mc, Object input) throws ReflectiveOperationException {
        BaritoneAntiStuck.clearAllKeys(input);
        BaritoneAntiStuck.setInputForceState(input, "MOVE_FORWARD", true);
        BaritoneAntiStuck.setInputForceState(input, strafeRightNext ? "MOVE_RIGHT" : "MOVE_LEFT", true);
        if (mc.player != null && mc.player.isOnGround()) {
            BaritoneAntiStuck.setInputForceState(input, "JUMP", true);
        }
    }

    private static void applyPrivateEscapeInput(MinecraftClient mc, Object input) throws ReflectiveOperationException {
        BaritoneAntiStuck.clearAllKeys(input);
        BaritoneAntiStuck.setInputForceState(input, "MOVE_BACK", true);
        BaritoneAntiStuck.setInputForceState(input, privateEscapeRight ? "MOVE_RIGHT" : "MOVE_LEFT", true);
        if (mc.player != null && mc.player.isOnGround()) {
            BaritoneAntiStuck.setInputForceState(input, "JUMP", true);
        }
    }

    private static boolean chooseRecoverySide(MinecraftClient mc, boolean fallbackRight, boolean moveForward) {
        double rightScore;
        if (mc.player == null) {
            return fallbackRight;
        }
        double yawRad = Math.toRadians(mc.player.getYaw());
        Vec3d forwardDirection = new Vec3d((double)(-MathHelper.sin((float)((float)yawRad))), 0.0, (double)MathHelper.cos((float)((float)yawRad)));
        Vec3d left = new Vec3d(forwardDirection.z, 0.0, -forwardDirection.x);
        Vec3d right = left.multiply(-1.0);
        Vec3d direction = moveForward ? forwardDirection : forwardDirection.multiply(-1.0);
        double leftScore = BaritoneAntiStuck.freeSpaceScore(mc, left.multiply(0.95).add(direction.multiply(0.35)));
        if (leftScore == (rightScore = BaritoneAntiStuck.freeSpaceScore(mc, right.multiply(0.95).add(direction.multiply(0.35))))) {
            return fallbackRight;
        }
        return rightScore > leftScore;
    }

    private static double freeSpaceScore(MinecraftClient mc, Vec3d offset) {
        Box shifted = mc.player.getBoundingBox().offset(offset);
        double score = 0.0;
        if (mc.world.isSpaceEmpty((Entity)mc.player, shifted)) {
            score += 1.0;
        }
        if (mc.world.isSpaceEmpty((Entity)mc.player, shifted.offset(0.0, 1.0, 0.0))) {
            score += 0.35;
        }
        return score;
    }

    private static void clearRecovery(Object input) {
        if (recoveryTicksRemaining > 0 && input != null) {
            try {
                BaritoneAntiStuck.clearAllKeys(input);
            }
            catch (ReflectiveOperationException reflectiveOperationException) {
                // empty catch block
            }
        }
        recoveryTicksRemaining = 0;
        if (privateEscapeActive && input != null) {
            try {
                BaritoneAntiStuck.clearAllKeys(input);
            }
            catch (ReflectiveOperationException reflectiveOperationException) {
                // empty catch block
            }
        }
        privateEscapeActive = false;
        privateEscapePending = false;
    }

    private static void resetTracking() {
        anchorPos = null;
        lastProgressAtMs = 0L;
    }

    private static void resetState() {
        recoveryTicksRemaining = 0;
        anchorPos = null;
        lastProgressAtMs = 0L;
        privateEscapePending = false;
        privateEscapeActive = false;
        privateEscapeStartPos = null;
        privateEscapeStartedAtMs = 0L;
    }

    private static boolean isInputForcedDown(Object inputOverrideHandler, String inputName) throws ReflectiveOperationException {
        Object input = BaritoneAntiStuck.getInputEnum(inputName);
        Object result = inputOverrideHandler.getClass().getMethod("isInputForcedDown", input.getClass()).invoke(inputOverrideHandler, input);
        return Boolean.TRUE.equals(result);
    }

    private static void setInputForceState(Object inputOverrideHandler, String inputName, boolean forced) throws ReflectiveOperationException {
        Object input = BaritoneAntiStuck.getInputEnum(inputName);
        inputOverrideHandler.getClass().getMethod("setInputForceState", input.getClass(), Boolean.TYPE).invoke(inputOverrideHandler, input, forced);
    }

    private static void clearAllKeys(Object inputOverrideHandler) throws ReflectiveOperationException {
        inputOverrideHandler.getClass().getMethod("clearAllKeys", new Class[0]).invoke(inputOverrideHandler, new Object[0]);
    }

    private static Object getInputEnum(String inputName) throws ReflectiveOperationException {
        Class<?> inputEnum = Class.forName(INPUT_ENUM_CLASS);
        Enum value = Enum.valueOf(inputEnum.asSubclass(Enum.class), inputName);
        return value;
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        return target.getClass().getMethod(methodName, new Class[0]).invoke(target, new Object[0]);
    }
}

