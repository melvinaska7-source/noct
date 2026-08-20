package polar.ru.client.modules.impl.combat.components.rotations;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.combat.components.RotationsSystem;

public class CakeWorldRotation
extends RotationsSystem {
    private static float curYaw = Float.NaN;
    private static float curPitch = Float.NaN;
    private static float velYaw = 0.0f;
    private static float velPitch = 0.0f;
    private static float lastSentYaw = Float.NaN;
    private static float lastSentPitch = Float.NaN;
    private static double swayTime = Math.random() * 100.0;
    private static double pSwayTime = Math.random() * 100.0;

    public void reset() {
        curPitch = Float.NaN;
        curYaw = Float.NaN;
        velPitch = 0.0f;
        velYaw = 0.0f;
        lastSentPitch = Float.NaN;
        lastSentYaw = Float.NaN;
        swayTime = Math.random() * 100.0;
        pSwayTime = Math.random() * 100.0;
    }

    private static float[] getAngles(Vec3d eye, Vec3d aim) {
        swayTime += 0.048 + Math.random() * 0.01;
        pSwayTime += 0.035 + Math.random() * 0.01;
        Vec3d delta = aim.subtract(eye);
        float targetYaw = (float)Math.toDegrees(Math.atan2(delta.x, delta.z)) - 90.0f;
        float targetPitch = (float)(-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z))));
        if (Float.isNaN(curYaw)) {
            curYaw = targetYaw;
            curPitch = targetPitch;
            velPitch = 0.0f;
            velYaw = 0.0f;
        }
        float deltaYaw = MathHelper.wrapDegrees((float)(targetYaw - curYaw));
        float deltaPitch = targetPitch - curPitch;
        float dist = (float)Math.hypot(deltaYaw, deltaPitch);
        float tension = 0.25f + (float)(Math.sin(swayTime * 0.1) * 0.05);
        float damp = 0.55f + (float)(Math.cos(pSwayTime * 0.1) * 0.05);
        float adaptiveStiff = tension * MathHelper.clamp((float)(dist / 20.0f), (float)0.4f, (float)1.8f);
        velYaw = velYaw * damp + deltaYaw * adaptiveStiff;
        velPitch = velPitch * damp + deltaPitch * adaptiveStiff;
        float maxSpd = 32.0f + (float)(Math.random() * 5.0);
        velYaw = MathHelper.clamp((float)velYaw, (float)(-maxSpd), (float)maxSpd);
        velPitch = MathHelper.clamp((float)velPitch, (float)(-maxSpd), (float)maxSpd);
        curPitch = MathHelper.clamp((float)(curPitch + velPitch), (float)-89.9f, (float)89.9f);
        float sY = (float)(Math.sin(swayTime * 0.65) * 0.09) + (float)(Math.sin(swayTime * 1.85) * 0.03) + (float)((Math.random() - 0.5) * 0.04);
        float sP = (float)(Math.sin(pSwayTime * 0.7) * 0.05) + (float)((Math.random() - 0.5) * 0.03);
        float outYaw = MathHelper.wrapDegrees((float)((curYaw += velYaw) + sY));
        float outPitch = MathHelper.clamp((float)(curPitch + sP), (float)-89.9f, (float)89.9f);
        float[] gcd = CakeWorldRotation.applyGcd(outYaw, outPitch);
        return new float[]{MathHelper.wrapDegrees((float)gcd[0]), MathHelper.clamp((float)gcd[1], (float)-90.0f, (float)90.0f)};
    }

    private static float[] applyGcd(float yaw, float pitch) {
        double gcd = CakeWorldRotation.getGcd();
        if (gcd < 1.0E-6) {
            return new float[]{yaw, pitch};
        }
        if (!Float.isNaN(lastSentYaw)) {
            float dYaw = MathHelper.wrapDegrees((float)(yaw - lastSentYaw));
            float dPitch = pitch - lastSentPitch;
            dYaw = (float)((double)Math.round((double)dYaw / gcd) * gcd);
            dPitch = (float)((double)Math.round((double)dPitch / gcd) * gcd);
            yaw = MathHelper.wrapDegrees((float)(lastSentYaw + dYaw));
            pitch = MathHelper.clamp((float)(lastSentPitch + dPitch), (float)-90.0f, (float)90.0f);
        } else {
            yaw = (float)((double)Math.round((double)yaw / gcd) * gcd);
            pitch = (float)((double)Math.round((double)pitch / gcd) * gcd);
        }
        lastSentYaw = yaw;
        lastSentPitch = pitch;
        return new float[]{yaw, pitch};
    }

    private static double getGcd() {
        double sens = (Double)CakeWorldRotation.mc.options.getMouseSensitivity().getValue();
        double f2 = sens * 0.6 + 0.2;
        return f2 * f2 * f2 * 1.2;
    }

    @Override
    public void updateRotations(LivingEntity target) {
        if (CakeWorldRotation.mc.player == null || target == null) {
            return;
        }
        Vec3d eyePos = CakeWorldRotation.mc.player.getEyePos();
        Vec3d targetCenter = this.getPredictedPoint(target, target.getBoundingBox().getCenter());
        float[] rotations = CakeWorldRotation.getAngles(eyePos, targetCenter);
        RotationStorage.update(new Rotation(rotations[0], rotations[1]), 360.0f, 45.0f, 45.0f, 45.0f, 0, 1, Aura.clientLook.isState());
    }

    public void onAttack() {
    }
}

