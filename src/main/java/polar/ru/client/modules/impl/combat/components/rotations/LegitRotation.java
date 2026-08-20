package polar.ru.client.modules.impl.combat.components.rotations;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.QClient;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.api.utils.rotate.RotationUtils;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;

public class LegitRotation
extends RotationsSystem
implements QClient {
    @Override
    public void updateRotations(LivingEntity target) {
        Vec3d eyePos = LegitRotation.mc.player.getCameraPosVec(1.0f);
        Vec3d lookVec = LegitRotation.mc.player.getRotationVec(1.0f);
        Vec3d reachVec = eyePos.add(lookVec.multiply(999.0));
        Box box = this.getPredictedBox(target);
        double shrinkXZ = target.isGliding() ? -0.5 : (double)0.1f;
        double shrinkY = target.isGliding() ? -0.5 : (double)0.1f;
        box = new Box(box.minX + box.getLengthX() * shrinkXZ / 2.0, box.minY, box.minZ + box.getLengthZ() * shrinkXZ / 2.0, box.maxX - box.getLengthX() * shrinkXZ / 2.0, box.maxY - box.getLengthY() * shrinkY, box.maxZ - box.getLengthZ() * shrinkXZ / 2.0);
        Optional hit = box.raycast(eyePos, reachVec);
        boolean inside = box.contains(eyePos);
        if (hit.isPresent() || inside) {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw - ThreadLocalRandom.current().nextFloat(0.005f, 0.02f)), (float)0.0f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch - ThreadLocalRandom.current().nextFloat(0.005f, 0.02f)), (float)0.0f, (float)1.0f);
        } else if (LegitRotation.mc.player.isGliding()) {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw + ThreadLocalRandom.current().nextFloat(5.0E-4f, 0.005f)), (float)0.0f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch + ThreadLocalRandom.current().nextFloat(9.0E-4f, 0.009f)), (float)0.0f, (float)1.0f);
        } else if (target.isInSwimmingPose()) {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw + ThreadLocalRandom.current().nextFloat(9.0E-5f, 0.009f)), (float)0.0f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch + ThreadLocalRandom.current().nextFloat(9.0E-5f, 9.0E-4f)), (float)0.0f, (float)1.0f);
        } else {
            Aura.adjYaw = MathHelper.clamp((float)(Aura.adjYaw + ThreadLocalRandom.current().nextFloat(9.0E-5f, 0.009f)), (float)0.0f, (float)1.0f);
            Aura.adjPitch = MathHelper.clamp((float)(Aura.adjPitch + ThreadLocalRandom.current().nextFloat(9.0E-4f, 0.009f)), (float)0.0f, (float)1.0f);
        }
        Vec2f targetRot = RotationUtils.getRotations(this.getPredictedPoint(target, target.getLeashPos(1.0f)));
        float currentYaw = LegitRotation.mc.player.getYaw();
        float currentPitch = LegitRotation.mc.player.getPitch();
        float diffYaw = MathHelper.wrapDegrees((float)(targetRot.x - currentYaw));
        float diffPitch = MathHelper.wrapDegrees((float)(targetRot.y - currentPitch));
        float newYaw = currentYaw + diffYaw * Aura.adjYaw;
        float newPitch = currentPitch + diffPitch * Aura.adjPitch;
        Aura.otvodkaYaw = 0.0f;
        Aura.otvodkaPitch = 0.0f;
        RotationStorage.update(new Rotation(newYaw, newPitch), 360.0f, 360.0f, 40.0f, 35.0f, 1, 1, Aura.clientLook.isState());
    }
}

