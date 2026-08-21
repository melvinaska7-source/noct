package zov.alphadlc.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import zov.alphadlc.event.list.EventPlayerUpdate;
import zov.alphadlc.module.Module;
import zov.alphadlc.module.ModuleCategory;
import zov.alphadlc.module.ModuleInformation;
import zov.alphadlc.module.settings.BooleanSetting;
import zov.alphadlc.module.settings.SliderSetting;
import zov.alphadlc.util.base.Instance;
import zov.alphadlc.util.friend.FriendRepository;
import zov.alphadlc.util.player.other.InventoryUtil;

@ModuleInformation(moduleName = "TargetPearl", moduleDesc = "Метает эндер-перл точно в цель", moduleCategory = ModuleCategory.COMBAT)
public class TargetPearl extends Module {

    private final BooleanSetting onlyTarget = new BooleanSetting("Только по таргету KillAura", false);
    private final SliderSetting maxRange = new SliderSetting("Макс. дистанция", 40, 10, 120, 5);
    private final SliderSetting minDistance = new SliderSetting("Мин. дистанция", 8, 3, 40, 1);
    private final SliderSetting throwDelay = new SliderSetting("Задержка (мс)", 1500, 200, 4000, 100);
    private final BooleanSetting restoreLook = new BooleanSetting("Вернуть взгляд", true);

    private static final int MAX_SIM_TICKS = 160;
    private static final double THROW_VELOCITY = 1.5;

    private long lastThrow;

    @Subscribe
    private void onUpdate(EventPlayerUpdate ignored) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.player.isGliding()) return;
        if (System.currentTimeMillis() - lastThrow < (long) throwDelay.getValue()) return;
        if (mc.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.ENDER_PEARL))) return;
        if (InventoryUtil.searchItemHotbar(Items.ENDER_PEARL) == -1) return;

        LivingEntity target = resolveTarget();
        if (target == null) return;

        Vec3d landing = target.getPos();
        float[] rot = findRotation(landing);
        if (rot == null) return;

        throwPearl(rot[0], rot[1]);
        lastThrow = System.currentTimeMillis();
    }

    private LivingEntity resolveTarget() {
        if (onlyTarget.getValue()) {
            KillAura aura = Instance.get(KillAura.class);
            LivingEntity auraTarget = aura == null ? null : aura.getTarget();
            if (isValid(auraTarget)) return auraTarget;
            return null;
        }

        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        double min = minDistance.getValue();
        double max = maxRange.getValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!isValid(player)) continue;
            double distance = mc.player.distanceTo(player);
            if (distance < min || distance > max) continue;
            if (distance < bestDist) {
                bestDist = distance;
                best = player;
            }
        }
        return best;
    }

    private boolean isValid(LivingEntity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive()) return false;
        if (entity instanceof PlayerEntity player && FriendRepository.isFriend(player.getNameForScoreboard()))
            return false;
        return true;
    }

    // Прямой yaw на цель + перебор питча по баллистической симуляции.
    private float[] findRotation(Vec3d target) {
        Vec3d eye = mc.player.getEyePos();
        double dx = target.x - eye.x;
        double dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;

        float bestPitch = 0f;
        double bestError = Double.MAX_VALUE;

        for (float pitch = 80f; pitch >= -30f; pitch -= 0.5f) {
            Vec3d landing = simulate(yaw, pitch);
            if (landing == null) continue;
            double error = landing.squaredDistanceTo(target);
            if (error < bestError) {
                bestError = error;
                bestPitch = pitch;
            }
        }

        if (bestError == Double.MAX_VALUE) return null;
        // Слишком грубое попадание — не бросаем впустую.
        if (bestError > 6.0 * 6.0) return null;
        return new float[]{yaw, MathHelper.clamp(bestPitch, -90f, 90f)};
    }

    private Vec3d simulate(float yaw, float pitch) {
        double yr = Math.toRadians(yaw);
        double pr = Math.toRadians(pitch);

        double x = mc.player.getX() - Math.cos(yr) * 0.16;
        double y = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()) - 0.1;
        double z = mc.player.getZ() - Math.sin(yr) * 0.16;

        double vx = -Math.sin(yr) * Math.cos(pr) * THROW_VELOCITY;
        double vy = -Math.sin(pr) * THROW_VELOCITY + mc.player.getVelocity().y;
        double vz = Math.cos(yr) * Math.cos(pr) * THROW_VELOCITY;

        double bottom = mc.world.getBottomY();

        for (int i = 0; i < MAX_SIM_TICKS; i++) {
            x += vx;
            y += vy;
            z += vz;
            vx *= 0.99;
            vy = vy * 0.99 - 0.03;
            vz *= 0.99;

            if (y <= bottom) return snap(x, y, z);

            BlockPos bp = BlockPos.ofFloored(x, y, z);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
                return snap(x, y, z);
            }
        }
        return null;
    }

    private Vec3d snap(double x, double y, double z) {
        return new Vec3d(MathHelper.floor(x) + 0.5, MathHelper.floor(y), MathHelper.floor(z) + 0.5);
    }

    private void throwPearl(float yaw, float pitch) {
        int slot = InventoryUtil.searchItemHotbar(Items.ENDER_PEARL);
        if (slot == -1) return;

        int previous = mc.player.getInventory().selectedSlot;
        float prevYaw = mc.player.getYaw();
        float prevPitch = mc.player.getPitch();

        if (slot != previous) {
            mc.player.getInventory().selectedSlot = slot;
            mc.interactionManager.syncSelectedSlot();
        }

        // Пакетный взгляд: interactItem отправит yaw/pitch игрока на момент броска.
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (slot != previous) {
            mc.player.getInventory().selectedSlot = previous;
            mc.interactionManager.syncSelectedSlot();
        }

        if (restoreLook.getValue()) {
            mc.player.setYaw(prevYaw);
            mc.player.setPitch(prevPitch);
        }
    }
}
