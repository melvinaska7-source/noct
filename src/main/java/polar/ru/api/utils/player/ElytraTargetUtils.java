package polar.ru.api.utils.player;

import net.minecraft.entity.LivingEntity;
import polar.ru.api.QClient;
import polar.ru.client.modules.impl.combat.ElytraMotion;
import polar.ru.client.modules.impl.combat.ElytraTarget;

public class ElytraTargetUtils
implements QClient {
    public static void updateNumber() {
    }

    public static float getJitterYaw(LivingEntity entity) {
        return 0.0f;
    }

    public static float getJitterPitch(LivingEntity entity) {
        return 0.0f;
    }

    public static boolean canTarget(LivingEntity target) {
        if (target == null) {
            return false;
        }
        if (ElytraTargetUtils.mc.player.getAttackCooldownProgress(1.5f) < 0.94f) {
            return true;
        }
        return ElytraTargetUtils.mc.player.isUsingItem();
    }

    public static boolean fullCheck() {
        if (ElytraTargetUtils.mc.player == null || ElytraTargetUtils.mc.world == null) {
            return false;
        }
        ElytraTarget elytraTarget = ElytraTarget.INSTANCE;
        ElytraMotion elytraMotion = ElytraMotion.INSTANCE;
        return elytraTarget != null && elytraTarget.isEnable() && ElytraTargetUtils.mc.player.isGliding() && (elytraMotion == null || !elytraMotion.isEnable());
    }
}

