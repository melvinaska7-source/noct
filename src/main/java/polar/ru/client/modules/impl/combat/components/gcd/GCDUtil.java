package polar.ru.client.modules.impl.combat.components.gcd;

import polar.ru.api.QClient;

public class GCDUtil
implements QClient {
    public static float getFixedRotation(float rot) {
        return GCDUtil.getDeltaMouse(rot) * GCDUtil.getGCDValue();
    }

    public static float getGCDValue() {
        return (float)((double)GCDUtil.getGCD() * 0.15);
    }

    public static float getGCD() {
        double f2 = 0.5000000149011612;
        return (float)(f2 * f2 * f2 * 8.0);
    }

    public static float getDeltaMouse(float delta) {
        return Math.round(delta / GCDUtil.getGCDValue());
    }
}

