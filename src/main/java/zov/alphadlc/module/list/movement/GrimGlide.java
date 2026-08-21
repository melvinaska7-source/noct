package zov.alphadlc.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.event.list.EventOnTravelPost;
import zov.alphadlc.event.list.EventPacket;
import zov.alphadlc.event.list.EventTick;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.util.packet.NetworkUtils;

@ModuleInformation(moduleName = "Elytra Exploit", moduleDesc = "Эксплойт для элитр", moduleCategory = ModuleCategory.MOVEMENT)
public class GrimGlide extends Module {
    private int flagTicks;
    private boolean isFlag;

    @Subscribe
    public void onPacket(EventPacket e2) {
        if (this.mc.player == null) {
            return;
        }
        if (e2.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.flagTicks = 2;
            this.isFlag = true;
        }
        if (e2.getPacket() instanceof PlayerMoveC2SPacket) {
            if (this.mc.player.isGliding() && this.flagTicks == 0 && !this.isFlag) {
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, true));
                e2.cancelEvent();
            }
            this.isFlag = false;
        }
    }

    @Subscribe
    public void onTick(EventTick e2) {
        if (this.flagTicks > 0) {
            --this.flagTicks;
        }
    }

    @Subscribe
    public void onTravelPost(EventOnTravelPost eventOnTravelPost) {
        double i2;
        if (this.mc.player == null) {
            return;
        }
        Vec3d oldVelocity = this.mc.player.getVelocity();
        Vec3d vec3d = this.mc.player.getRotationVector();
        float f2 = this.mc.player.getPitch() * ((float)Math.PI / 180);
        double d2 = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        double e2 = oldVelocity.horizontalLength();
        boolean bl = this.mc.player.getVelocity().y <= 0.0;
        double g2 = bl && this.mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) ? Math.min(this.mc.player.getFinalGravity(), 0.01) : this.mc.player.getFinalGravity();
        double h2 = MathHelper.square((double)Math.cos(f2));
        oldVelocity = oldVelocity.add(0.0, g2 * (-1.0 + h2 * 0.75), 0.0);
        if (oldVelocity.y < 0.0 && d2 > 0.0) {
            i2 = oldVelocity.y * -0.1 * h2;
            oldVelocity = oldVelocity.add(vec3d.x * i2 / d2, i2, vec3d.z * i2 / d2);
        }
        if (f2 < 0.0f && d2 > 0.0) {
            i2 = e2 * (double)(-MathHelper.sin((float)f2)) * (double)0.04f;
            oldVelocity = oldVelocity.add(-vec3d.x * i2 / d2, i2 * 3.2, -vec3d.z * i2 / d2);
        }
        if (d2 > 0.0) {
            oldVelocity = oldVelocity.add((vec3d.x / d2 * e2 - oldVelocity.x) * 0.1, 0.0, (vec3d.z / d2 * e2 - oldVelocity.z) * 0.1);
        }
        double yaw = Math.toRadians(this.mc.player.getYaw());
        double xt = -Math.sin(yaw);
        double zt = Math.cos(yaw);
        if (this.flagTicks >= 1) {
            double bst = 0.09f;
            eventOnTravelPost.setOldVelocity(oldVelocity.multiply((double)0.99f, (double)0.98f, (double)0.99f).add(xt * bst, (double)0.03f, zt * bst));
        } else {
            eventOnTravelPost.setOldVelocity(oldVelocity.multiply((double)0.3f, (double)0.3f, (double)0.3f));
        }
    }
}
