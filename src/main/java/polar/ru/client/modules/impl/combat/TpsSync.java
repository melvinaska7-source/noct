package polar.ru.client.modules.impl.combat;

import net.minecraft.util.math.MathHelper;
import polar.ru.client.modules.Module;
import polar.ru.polar;

public class TpsSync
extends Module {
    public static TpsSync INSTANCE = new TpsSync();

    public TpsSync() {
        super("TpsSync", "Синхронизация с TPS сервера", Module.ModuleCategory.COMBAT);
    }

    public float getCurrentTPS() {
        if (polar.INSTANCE == null || polar.INSTANCE.tpsCalc == null) {
            return 20.0f;
        }
        float tps = polar.INSTANCE.tpsCalc.getTPS();
        return MathHelper.clamp((float)tps, (float)0.1f, (float)20.0f);
    }

    public long getAdjustedCooldown(long baseCooldown) {
        if (!this.isEnable()) {
            return baseCooldown;
        }
        float tps = this.getCurrentTPS();
        if (tps >= 20.0f) {
            return baseCooldown;
        }
        float multiplier = 20.0f / tps;
        float additionalFactor = 1.0f + (20.0f - tps) * 0.05f;
        long adjusted = (long)((float)baseCooldown * multiplier * additionalFactor);
        return Math.min(adjusted, 3000L);
    }

    public boolean canAttack(long lastAttackTime, long baseCooldown, long currentTime) {
        if (!this.isEnable()) {
            return currentTime >= lastAttackTime + baseCooldown;
        }
        long adjustedCooldown = this.getAdjustedCooldown(baseCooldown);
        return currentTime >= lastAttackTime + adjustedCooldown;
    }
}

