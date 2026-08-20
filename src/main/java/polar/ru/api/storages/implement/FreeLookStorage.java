package polar.ru.api.storages.implement;

import net.minecraft.util.math.MathHelper;
import polar.ru.api.QClient;
import polar.ru.api.events.EventInvoker;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventLook;
import polar.ru.api.events.implement.EventRotation;

public class FreeLookStorage
implements QClient {
    private static boolean active;
    private static float freeYaw;
    private static float freePitch;

    public FreeLookStorage() {
        EventInvoker.register(this);
    }

    public static boolean isActive() {
        return active;
    }

    @EventLink
    public void onLook(EventLook event) {
        if (active) {
            this.rotateTowards(event.getYaw(), event.getPitch());
            event.cancel();
        }
    }

    @EventLink
    public void onRotation(EventRotation event) {
        if (active) {
            event.setYaw(freeYaw);
            event.setPitch(freePitch);
        } else {
            freeYaw = event.getYaw();
            freePitch = event.getPitch();
        }
    }

    private void rotateTowards(double targetYaw, double targetPitch) {
        freePitch = MathHelper.clamp((float)((float)((double)freePitch + targetPitch * 0.15)), (float)-90.0f, (float)90.0f);
        freeYaw = (float)((double)freeYaw + targetYaw * 0.15);
    }
    public static void setActive(boolean active) {
        FreeLookStorage.active = active;
    }
    public static float getFreeYaw() {
        return freeYaw;
    }
    public static float getFreePitch() {
        return freePitch;
    }
    public static void setFreeYaw(float freeYaw) {
        FreeLookStorage.freeYaw = freeYaw;
    }
    public static void setFreePitch(float freePitch) {
        FreeLookStorage.freePitch = freePitch;
    }
}

