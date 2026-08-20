package polar.ru.client.modules.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.utils.player.InventoryUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;

public class ElytraResolver
extends Module {
    public static ElytraResolver INSTANCE = new ElytraResolver();
    private final FloatSetting distance = new FloatSetting("Дистанция отлета", 6.0f, 4.0f, 8.0f, 0.1f);
    private final BooleanSetting autoFirework = new BooleanSetting("Авто-Фейерверк", true);
    private static final float MIN_HEIGHT = 4.0f;
    private boolean escaping;
    private Vec3d escapePos;
    private long escapeStartTime;
    private int returnFireworkTicks = -1;
    private Vec3d lastEscapeDirection;

    public ElytraResolver() {
        super("ElytraResolver", "Отлет на элитрах", Module.ModuleCategory.COMBAT);
        this.addSettings(this.distance, this.autoFirework);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.escaping = false;
        this.escapePos = null;
        this.returnFireworkTicks = -1;
        this.lastEscapeDirection = null;
    }

        public void onAuraAttack() {
        if (!this.isEnable() || ElytraResolver.mc.player == null || !ElytraResolver.mc.player.isGliding()) {
            return;
        }
        Vec3d bestPos = this.calculateSmartEscape(ElytraResolver.mc.player.getPos(), this.distance.get());
        if (bestPos != null) {
            this.escapePos = bestPos;
            this.escaping = true;
            this.escapeStartTime = System.currentTimeMillis();
            if (this.autoFirework.isState()) {
                this.useFirework();
            }
        }
    }

    @EventLink
        public void onUpdate(EventUpdate event) {
        double dist;
        if (ElytraResolver.mc.player == null || ElytraResolver.mc.world == null || !ElytraResolver.mc.player.isGliding()) {
            this.escaping = false;
            this.returnFireworkTicks = -1;
            return;
        }
        if (this.returnFireworkTicks > 0) {
            --this.returnFireworkTicks;
        } else if (this.returnFireworkTicks == 0) {
            if (this.autoFirework.isState()) {
                this.useFirework();
            }
            this.returnFireworkTicks = -1;
        }
        if (this.escaping && this.escapePos != null && ((dist = ElytraResolver.mc.player.getPos().distanceTo(this.escapePos)) < 2.0 || System.currentTimeMillis() - this.escapeStartTime > 1000L)) {
            this.escaping = false;
            if (this.autoFirework.isState()) {
                this.returnFireworkTicks = 2;
            }
        }
    }

    public boolean isEscaping() {
        return this.isEnable() && this.escaping && this.escapePos != null && ElytraResolver.mc.player != null && ElytraResolver.mc.player.isGliding();
    }

    public Vec3d getEscapePos() {
        return this.escapePos;
    }

    private Vec3d calculateSmartEscape(Vec3d pPos, float d2) {
        Vec3d playerLook = ElytraResolver.mc.player.getRotationVector();
        Vec3d playerVelocity = ElytraResolver.mc.player.getVelocity();
        Vec3d[] directions = this.generateSmartDirections(playerLook, playerVelocity);
        ArrayList<EscapePoint> validPoints = new ArrayList<EscapePoint>();
        for (Vec3d dir : directions) {
            Vec3d target = pPos.add(dir.multiply((double)d2));
            if (target.y < pPos.y + 4.0) continue;
            RaycastContext context = new RaycastContext(pPos, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)ElytraResolver.mc.player);
            BlockHitResult hit = ElytraResolver.mc.world.raycast(context);
            double actualDistance = d2;
            Vec3d finalPos = target;
            if (hit.getType() != HitResult.Type.MISS) {
                double hitDist = hit.getPos().distanceTo(pPos);
                if (!(hitDist > 2.0)) continue;
                actualDistance = hitDist;
                finalPos = hit.getPos().add(dir.multiply(-1.0));
            }
            double score = this.calculateEscapeScore(dir, playerLook, playerVelocity, actualDistance, finalPos);
            validPoints.add(new EscapePoint(finalPos, actualDistance, score));
        }
        if (validPoints.isEmpty()) {
            return null;
        }
        validPoints.sort(Comparator.comparingDouble(p2 -> -p2.score));
        this.lastEscapeDirection = ((EscapePoint)validPoints.get((int)0)).pos.subtract(pPos).normalize();
        return ((EscapePoint)validPoints.get((int)0)).pos;
    }

    private Vec3d[] generateSmartDirections(Vec3d playerLook, Vec3d velocity) {
        Vec3d back = new Vec3d(-playerLook.x, 0.0, -playerLook.z).normalize();
        Vec3d right = new Vec3d(-playerLook.z, 0.0, playerLook.x).normalize();
        Vec3d left = right.multiply(-1.0);
        Vec3d up = new Vec3d(0.0, 1.0, 0.0);
        ArrayList<Vec3d> dirs = new ArrayList<Vec3d>();
        dirs.add(back.add(up).normalize());
        dirs.add(back.add(right).add(up).normalize());
        dirs.add(back.add(left).add(up).normalize());
        dirs.add(right.add(up).normalize());
        dirs.add(left.add(up).normalize());
        dirs.add(back.add(right.multiply(0.5)).add(up.multiply(1.5)).normalize());
        dirs.add(back.add(left.multiply(0.5)).add(up.multiply(1.5)).normalize());
        dirs.add(back.add(up.multiply(2.0)).normalize());
        dirs.add(right.add(up.multiply(1.5)).normalize());
        dirs.add(left.add(up.multiply(1.5)).normalize());
        dirs.add(back.multiply(0.7).add(right.multiply(0.3)).add(up.multiply(1.2)).normalize());
        dirs.add(back.multiply(0.7).add(left.multiply(0.3)).add(up.multiply(1.2)).normalize());
        dirs.add(back.multiply(0.5).add(up.multiply(1.8)).normalize());
        dirs.add(right.multiply(0.8).add(up.multiply(1.3)).normalize());
        dirs.add(left.multiply(0.8).add(up.multiply(1.3)).normalize());
        if (velocity.lengthSquared() > 0.01) {
            Vec3d perpendicular = new Vec3d(-velocity.z, 0.0, velocity.x).normalize();
            dirs.add(perpendicular.add(up).normalize());
            dirs.add(perpendicular.multiply(-1.0).add(up).normalize());
            dirs.add(perpendicular.add(up.multiply(1.5)).normalize());
            dirs.add(perpendicular.multiply(-1.0).add(up.multiply(1.5)).normalize());
        }
        return dirs.toArray(new Vec3d[0]);
    }

    private double calculateEscapeScore(Vec3d direction, Vec3d playerLook, Vec3d velocity, double distance, Vec3d finalPos) {
        double groundDistance;
        double similarity;
        double score = 0.0;
        double backwardBonus = -direction.dotProduct(new Vec3d(playerLook.x, 0.0, playerLook.z).normalize());
        score += backwardBonus * 30.0;
        score += direction.y * 25.0;
        score += distance * 2.0;
        if (velocity.lengthSquared() > 0.01) {
            Vec3d velNorm = velocity.normalize();
            double perpendicular = Math.abs(direction.dotProduct(new Vec3d(-velNorm.z, 0.0, velNorm.x)));
            score += perpendicular * 15.0;
        }
        if (this.lastEscapeDirection != null && (similarity = direction.dotProduct(this.lastEscapeDirection)) > 0.7) {
            score -= 20.0;
        }
        if ((groundDistance = finalPos.y - (double)ElytraResolver.mc.world.getBottomY()) < 10.0) {
            score -= (10.0 - groundDistance) * 5.0;
        }
        return score;
    }

    private void useFirework() {
        if (ElytraResolver.mc.player == null) {
            return;
        }
        int slotFirework = InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET);
        if (slotFirework != -1) {
            InventoryUtils.swapAndUseHvH(Items.FIREWORK_ROCKET);
        }
    }

    private static class EscapePoint {
        Vec3d pos;
        double distance;
        double score;

        EscapePoint(Vec3d pos, double distance, double score) {
            this.pos = pos;
            this.distance = distance;
            this.score = score;
        }
    }
}

