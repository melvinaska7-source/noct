package polar.ru.api.utils.rotate;

import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;

public class Rotation
implements QClient {
    private float yaw;
    private float pitch;

    public Rotation(Entity entity) {
        this.yaw = entity.getYaw();
        this.pitch = entity.getPitch();
    }

    public float getDelta(Rotation target) {
        float yawDelta = MathHelper.wrapDegrees((float)(target.getYaw() - this.yaw));
        float pitchDelta = target.getPitch() - this.pitch;
        return (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
    }

    public double getDeltaDouble(Rotation target) {
        double yawDelta = MathHelper.wrapDegrees((float)(target.getYaw() - this.yaw));
        double pitchDelta = MathHelper.wrapDegrees((float)(target.getPitch() - this.pitch));
        return Math.hypot(yawDelta, pitchDelta);
    }

    public static Vector2f camera() {
        return new Vector2f(Rotation.cameraYaw(), Rotation.cameraPitch());
    }

    public static float cameraYaw() {
        return MathHelper.wrapDegrees((float)(Rotation.mc.gameRenderer.getCamera().getYaw() + (float)(Rotation.mc.gameRenderer.getCamera().isThirdPerson() ? 180 : 0)));
    }

    public static float cameraPitch() {
        return (float)(Rotation.mc.gameRenderer.getCamera().isThirdPerson() ? -1 : 1) * Rotation.mc.gameRenderer.getCamera().getPitch();
    }

    public static Rotation from(PlayerEntity player, Entity target) {
        Vec3d playerPos = player.getCameraPosVec(0.0f);
        Vec3d targetPos = target.getPos().add(0.0, (double)target.getHeight() * 0.5, 0.0);
        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;
        double distanceXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, distanceXZ)));
        return new Rotation(yaw, pitch);
    }

    public final Vec3d toVector() {
        float f2 = this.pitch * ((float)Math.PI / 180);
        float g2 = -this.yaw * ((float)Math.PI / 180);
        float h2 = MathHelper.cos((float)g2);
        float i2 = MathHelper.sin((float)g2);
        float j2 = MathHelper.cos((float)f2);
        float k2 = MathHelper.sin((float)f2);
        return new Vec3d((double)(i2 * j2), (double)(-k2), (double)(h2 * j2));
    }
    public float getYaw() {
        return this.yaw;
    }
    public float getPitch() {
        return this.pitch;
    }
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
    public boolean equals(Object o2) {
        if (o2 == this) {
            return true;
        }
        if (!(o2 instanceof Rotation)) {
            return false;
        }
        Rotation other = (Rotation)o2;
        if (!other.canEqual(this)) {
            return false;
        }
        if (Float.compare(this.getYaw(), other.getYaw()) != 0) {
            return false;
        }
        return Float.compare(this.getPitch(), other.getPitch()) == 0;
    }
    protected boolean canEqual(Object other) {
        return other instanceof Rotation;
    }
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + Float.floatToIntBits(this.getYaw());
        result = result * 59 + Float.floatToIntBits(this.getPitch());
        return result;
    }
    public String toString() {
        return "Rotation(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
    }
    public Rotation() {
    }
    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
}

