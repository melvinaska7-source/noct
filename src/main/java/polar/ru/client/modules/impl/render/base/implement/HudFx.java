package polar.ru.client.modules.impl.render.base.implement;

import net.minecraft.client.util.math.MatrixStack;
import polar.ru.api.utils.animation.AnimationUtils;
import polar.ru.api.utils.animation.Easings;

public final class HudFx {
    public static final float PANEL_APPEAR_SPEED = 8.5f;
    public static final float SLIDE_DISTANCE = 6.0f;
    public static final float MIN_SCALE = 0.9f;

    private HudFx() {
    }

    public static AnimationUtils newAppearAnimation() {
        return new AnimationUtils(0.0f, 8.5f, Easings.QUAD_OUT);
    }

    public static AnimationUtils newValueAnimation(float initial, float speed) {
        return new AnimationUtils(initial, speed, Easings.QUAD_OUT);
    }

    private static float smooth(float t2) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t2));
        return 1.0f - (1.0f - clamped) * (1.0f - clamped) * (1.0f - clamped);
    }

    public static float pushTransform(MatrixStack matrices, float progress, float pivotX, float pivotY) {
        float eased = HudFx.smooth(progress);
        float scale = 0.9f + 0.100000024f * eased;
        float slideY = 6.0f * (1.0f - eased);
        matrices.push();
        matrices.translate(pivotX, pivotY, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate(-pivotX, -pivotY - slideY, 0.0f);
        return eased;
    }

    public static void popTransform(MatrixStack matrices) {
        matrices.pop();
    }
}

