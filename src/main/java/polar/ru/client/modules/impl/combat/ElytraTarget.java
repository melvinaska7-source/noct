package polar.ru.client.modules.impl.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventUpdate;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.combat.PredictUtils;
import polar.ru.api.utils.math.TimerUtils;
import polar.ru.api.utils.player.InventoryUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.movement.ElytraBoost;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;
import polar.ru.mixin.FireworkRocketEntityAccessor;

public class ElytraTarget
extends Module {
    public static ElytraTarget INSTANCE = new ElytraTarget();
    public final FloatSetting elytraDistance = new FloatSetting("Дистанция", 30.0f, 5.0f, 100.0f, 5.0f);
    public final FloatSetting forward;
    public final FloatSetting forwardValue = this.forward = new FloatSetting("Перегон", 3.0f, 0.0f, 10.0f, 0.1f);
    private final ModeSetting predictMode = new ModeSetting("Mode", "ReallyWorld", "ReallyWorld", "ReallyWorld - 2", "Default");
    private final ModeSetting useFirework = new ModeSetting("Фейерверк", "Никогда", "Никогда", "По таймеру");
    private final FloatSetting fireworkTiming = new FloatSetting("Задержка фейерверка", 1.0f, 0.1f, 5.0f, 0.1f).visible(() -> this.useFirework.is("По таймеру"));
    private final ModeSetting fireworkMode = new ModeSetting("Режим фейерверка", "Только с целью", "Только с целью", "Без цели", "Всегда").visible(() -> this.useFirework.is("По таймеру"));
    private final ModeSetting handMode = new ModeSetting("Рука", "Основная", "Основная", "Вспомогательная").visible(() -> this.useFirework.is("По таймеру"));
    private final BooleanSetting blockOnUse = new BooleanSetting("Блок при использовании", true).visible(() -> this.useFirework.is("По таймеру"));
    private final BooleanSetting target = new BooleanSetting("Target", false);
    private final BooleanSetting visualReverse = new BooleanSetting("Visual Reverse", false);
    private final FloatSetting distance = new FloatSetting("Predict Distance", 2.7f, 1.0f, 5.0f, 0.1f);
    private static boolean predictCondition = false;
    private static int movementTicks = 0;
    private static boolean prePredictCondition = false;
    private static final TimerUtils tickStopWatch = new TimerUtils();
    private final TimerUtils fireworkTimer = new TimerUtils();
    private Vec3d chasePos = Vec3d.ZERO;
    private boolean chaseBlockPos = false;
    private double predictedDistance = 0.0;
    private boolean visualReverseWasActive = false;
    private final BooleanSetting renderPredictCube = new BooleanSetting("Render Predict Cube", true);
    private final BooleanSetting predictFromTheme = new BooleanSetting("Theme Color", true);
    private final FloatSetting predictFillAlpha = new FloatSetting("Fill Alpha", 40.0f, 0.0f, 255.0f, 1.0f);
    private final ModeSetting predictBoxMode = new ModeSetting("Box Mode", "Normal", "Normal", "Diagonal");

    public ElytraTarget() {
        super("Elytra Target", "Элитра перегон", Module.ModuleCategory.COMBAT);
        this.addSettings(this.predictMode, this.target, this.visualReverse, this.distance, this.elytraDistance, this.forward, this.useFirework, this.fireworkTiming, this.fireworkMode, this.handMode, this.blockOnUse, this.renderPredictCube, this.predictFromTheme, this.predictFillAlpha, this.predictBoxMode);
    }

    public boolean isPredictionActive() {
        return ElytraTarget.mc.player != null && this.isEnable() && ElytraTarget.mc.player.isGliding();
    }

    public boolean isAuraActive() {
        return ElytraTarget.mc.player != null && this.isEnable() && ElytraTarget.mc.player.isGliding();
    }

    public int getForwardTicks() {
        return Math.max(0, Math.round(this.forward.getValue().floatValue()));
    }

        public boolean shouldSyncTargetFlight(LivingEntity target) {
        if (!(this.isAuraActive() && target != null && target.isAlive() && target.isGliding())) {
            return false;
        }
        Vec3d playerEye = ElytraTarget.mc.player.getEyePos();
        Vec3d targetEye = target.getEyePos();
        double horizontalGap = Math.hypot(targetEye.x - playerEye.x, targetEye.z - playerEye.z);
        double verticalGap = targetEye.y - playerEye.y;
        double maxSyncRange = MathHelper.clamp((double)(this.forward.getValue().doubleValue() * 1.8), (double)4.0, (double)7.5);
        if (horizontalGap > maxSyncRange) {
            return false;
        }
        if (verticalGap > 2.25 && horizontalGap > 2.0) {
            return false;
        }
        return Math.abs(verticalGap) <= 4.5;
    }

        public Vec3d getPredictedPoint(LivingEntity target, Vec3d point) {
        Vec3d predictedCenter;
        if (target == null) {
            return point;
        }
        if (this.shouldSyncTargetFlight(target)) {
            Vec3d vel = target.getVelocity();
            int ticks = this.getForwardTicks();
            predictedCenter = target.getBoundingBox().getCenter().add(vel.multiply((double)ticks));
        } else if (this.hasChasePosition()) {
            Vec3d vel = target.getVelocity();
            int ticks = this.getForwardTicks();
            Vec3d rawPredicted = target.getBoundingBox().getCenter().add(vel.multiply((double)ticks));
            predictedCenter = this.clampToExpandedBox(target, rawPredicted, 0.6);
        } else {
            return PredictUtils.predict(target, point, this.getForwardTicks());
        }
        return predictedCenter;
    }

        private Vec3d clampToExpandedBox(LivingEntity target, Vec3d pos, double margin) {
        Box box = target.getBoundingBox().expand(margin);
        double x2 = MathHelper.clamp((double)pos.x, (double)box.minX, (double)box.maxX);
        double z2 = MathHelper.clamp((double)pos.z, (double)box.minZ, (double)box.maxZ);
        double boxHeight = box.maxY - box.minY;
        double critY = box.minY + boxHeight * 0.75;
        double y2 = MathHelper.clamp((double)pos.y, (double)(critY - 0.15), (double)(box.maxY - 0.05));
        return new Vec3d(x2, y2, z2);
    }

        public Vec3d getPredictedCenter(LivingEntity target) {
        if (target == null) {
            return null;
        }
        Vec3d base = this.getPredictedPoint(target, target.getBoundingBox().getCenter());
        if (ElytraTarget.mc.player != null && base != null) {
            Vec3d playerEye = ElytraTarget.mc.player.getEyePos();
            double targetEyeY = target.getEyePos().y;
            if (playerEye.y > targetEyeY - 0.3) {
                double critOffset = MathHelper.clamp((double)(playerEye.y - targetEyeY), (double)0.0, (double)1.2) * 0.35;
                base = new Vec3d(base.x, targetEyeY - 0.25 - critOffset, base.z);
            }
        }
        return base;
    }

        public Vec3d getAimPoint(LivingEntity target) {
        double maxDrop;
        double heldY;
        if (ElytraTarget.mc.player == null || target == null) {
            return null;
        }
        if (!this.hasChasePosition()) {
            return this.getFollowAnchor(target);
        }
        Vec3d playerEye = ElytraTarget.mc.player.getEyePos();
        Vec3d targetEye = target.getEyePos();
        Vec3d aimPoint = this.chasePos;
        Vec3d forwardVec = this.getChaseForward(target, playerEye, targetEye);
        if (forwardVec.lengthSquared() > 1.0E-4) {
            aimPoint = aimPoint.add(forwardVec.normalize().multiply(2.0));
        }
        if (playerEye.y > targetEye.y + 0.35) {
            aimPoint = new Vec3d(aimPoint.x, Math.min(aimPoint.y, targetEye.y + 0.25), aimPoint.z);
        }
        if (this.shouldAimAtTargetXZ(playerEye, targetEye, forwardVec)) {
            double aimY = MathHelper.clamp((double)aimPoint.y, (double)(targetEye.y - 0.35), (double)(targetEye.y + 0.25));
            aimPoint = new Vec3d(targetEye.x, aimY, targetEye.z);
        }
        if (!this.hasActiveFireworkBoost() && aimPoint.y < playerEye.y && ElytraTarget.mc.player.getVelocity().y < 0.03 && aimPoint.y < (heldY = playerEye.y - (maxDrop = MathHelper.clamp((double)(0.45 + Math.hypot(aimPoint.x - playerEye.x, aimPoint.z - playerEye.z) * 0.08), (double)0.45, (double)0.95)))) {
            aimPoint = new Vec3d(aimPoint.x, MathHelper.lerp((double)0.2, (double)aimPoint.y, (double)heldY), aimPoint.z);
        }
        return aimPoint;
    }

    public Vec3d getAimVector(LivingEntity target) {
        if (ElytraTarget.mc.player == null) {
            return Vec3d.ZERO;
        }
        Vec3d aimPoint = this.getAimPoint(target);
        if (aimPoint == null) {
            return Vec3d.ZERO;
        }
        return aimPoint.subtract(ElytraTarget.mc.player.getEyePos());
    }

    public boolean shouldTarget(LivingEntity livingEntity) {
        if (!this.isEnable() || livingEntity == null || ElytraTarget.mc.player == null) {
            return false;
        }
        if (!this.target.isState() || !ElytraTarget.mc.player.isGliding()) {
            return false;
        }
        return livingEntity.isGliding();
    }

    public boolean isReverseActive() {
        LivingEntity auraTarget;
        if (!(this.isEnable() && this.target.isState() && this.visualReverse.isState() && ElytraTarget.mc.player != null)) {
            this.visualReverseWasActive = false;
            return false;
        }
        Aura aura = ModuleClass.aura;
        LivingEntity var_1309_2 = auraTarget = aura != null && aura.isEnable() ? aura.getTarget() : null;
        if (auraTarget == null || !this.shouldTarget(auraTarget)) {
            this.visualReverseWasActive = false;
            return false;
        }
        float distanceToTarget = ElytraTarget.mc.player.distanceTo((Entity)auraTarget);
        float enableDistance = 2.5f;
        float predictDistance = Math.max(enableDistance, this.distance.getValue().floatValue());
        float disableDistance = predictDistance + 3.0f;
        if (!this.visualReverseWasActive && distanceToTarget <= enableDistance) {
            this.visualReverseWasActive = true;
        }
        if (this.visualReverseWasActive && distanceToTarget >= disableDistance) {
            this.visualReverseWasActive = false;
        }
        return this.visualReverseWasActive;
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        LivingEntity auraTarget;
        if (ElytraTarget.mc.player == null || ElytraTarget.mc.world == null || !this.renderPredictCube.isState()) {
            return;
        }
        Aura aura = ModuleClass.aura;
        LivingEntity var_1309_2 = auraTarget = aura != null && aura.isEnable() ? aura.getTarget() : null;
        if (!(auraTarget != null && auraTarget.isGliding() && this.target.isState() && this.shouldTarget(auraTarget))) {
            return;
        }
        Vec3d predictedCenter = this.getPredictedCenter(auraTarget);
        if (predictedCenter == null) {
            return;
        }
        Vec3d cam = event.getCamera().getPos();
        Box box = new Box(predictedCenter.x - 0.35 - cam.x, predictedCenter.y - 0.35 - cam.y, predictedCenter.z - 0.35 - cam.z, predictedCenter.x + 0.35 - cam.x, predictedCenter.y + 0.35 - cam.y, predictedCenter.z + 0.35 - cam.z);
        int baseColor = this.predictFromTheme.isState() ? ColorUtils.getThemeColor() : ColorUtils.rgb(255, 255, 255);
        int fillColor = ColorUtils.setAlphaColor(baseColor, (int)this.predictFillAlpha.getValue().floatValue());
        int lineColor = ColorUtils.setAlphaColor(baseColor, 255);
        this.renderPredictCube(event, box, fillColor, lineColor);
    }

    private void renderPredictCube(Event3DRender event, Box box, int fillColor, int lineColor) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth((float)1.5f);
        Tessellator tessellator = Tessellator.getInstance();
        if ((fillColor >>> 24 & 0xFF) > 0) {
            this.drawFilledBox(tessellator, event, box, fillColor);
        }
        this.drawBoxOutline(tessellator, event, box, lineColor);
        if (this.predictBoxMode.is("Diagonal")) {
            this.drawBodyDiagonals(tessellator, event, box, lineColor);
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawFilledBox(Tessellator tessellator, Event3DRender event, Box box, int color) {
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void drawBoxOutline(Tessellator tessellator, Event3DRender event, Box box, int color) {
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void drawBodyDiagonals(Tessellator tessellator, Event3DRender event, Box box, int color) {
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, maxY, minZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), maxX, minY, maxZ).color(color);
        buffer.vertex(event.getMatrices().peek().getPositionMatrix(), minX, maxY, minZ).color(color);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    public static boolean isPredictCondition() {
        return predictCondition;
    }

    public static void resetPredictState() {
        predictCondition = false;
        movementTicks = 0;
        prePredictCondition = false;
    }

    public void resetChase() {
        this.chaseBlockPos = false;
        this.chasePos = Vec3d.ZERO;
        this.predictedDistance = 0.0;
    }

        public void updateChase(LivingEntity target, boolean shouldPredict) {
        Vec3d targetEye;
        this.resetChase();
        if (!this.isAuraActive() || target == null) {
            return;
        }
        boolean bl = this.chaseBlockPos = shouldPredict && target.isGliding();
        if (!this.chaseBlockPos) {
            return;
        }
        double power = this.forward.getValue().doubleValue();
        double distOffset = power < 4.0 ? 0.0 : power - 3.0;
        Vec3d playerEye = ElytraTarget.mc.player.getEyePos();
        Vec3d forwardVec = this.getChaseForward(target, playerEye, targetEye = target.getEyePos());
        if (forwardVec.lengthSquared() <= 1.0E-4) {
            this.chaseBlockPos = false;
            return;
        }
        this.chasePos = targetEye.add(forwardVec.normalize().multiply(power));
        Vec3d checkPos = targetEye.add(forwardVec.normalize().multiply(distOffset));
        this.predictedDistance = checkPos.distanceTo(playerEye);
    }

    public boolean hasChasePosition() {
        return this.chaseBlockPos && this.chasePos != null && this.chasePos.lengthSquared() > 1.0E-4;
    }

    public double getPredictedDistance() {
        return this.predictedDistance;
    }

        public Vec3d getFollowAnchor(LivingEntity target) {
        if (target == null) {
            return Vec3d.ZERO;
        }
        if (this.hasChasePosition()) {
            return this.chasePos;
        }
        Vec3d targetEye = target.getEyePos();
        Vec3d guide = this.resolveTargetForward(target);
        if (guide.lengthSquared() < 1.0E-4) {
            return targetEye;
        }
        double followDistance = MathHelper.clamp((double)(this.forward.getValue().doubleValue() * 0.75), (double)1.5, (double)4.0);
        return targetEye.add(guide.normalize().multiply(followDistance));
    }

        public void syncTargetFlightSpeed(LivingEntity target) {
        if (!this.shouldSyncTargetFlight(target) || !this.hasActiveFireworkBoost()) {
            return;
        }
        Vec3d anchor = this.getFollowAnchor(target);
        if (anchor == null || anchor.lengthSquared() < 1.0E-4) {
            return;
        }
        Vec3d playerEye = ElytraTarget.mc.player.getEyePos();
        Vec3d playerMotion = ElytraTarget.mc.player.getVelocity();
        Vec3d targetMotion = target.getVelocity();
        Vec3d toAnchor = anchor.subtract(playerEye);
        Vec3d horizontalDirection = new Vec3d(toAnchor.x, 0.0, toAnchor.z);
        double verticalOffset = toAnchor.y;
        double horizontalGap = Math.hypot(toAnchor.x, toAnchor.z);
        if (horizontalGap > MathHelper.clamp((double)(this.forward.getValue().doubleValue() * 1.8), (double)4.0, (double)7.0)) {
            return;
        }
        if (verticalOffset > 2.0 && horizontalGap > 2.0) {
            return;
        }
        if (Math.abs(verticalOffset) > 4.5) {
            return;
        }
        if (horizontalDirection.lengthSquared() < 1.0E-4) {
            horizontalDirection = new Vec3d(targetMotion.x, 0.0, targetMotion.z);
        }
        if (horizontalDirection.lengthSquared() < 1.0E-4) {
            Vec3d forwardVec = this.resolveTargetForward(target);
            horizontalDirection = new Vec3d(forwardVec.x, 0.0, forwardVec.z);
        }
        if (horizontalDirection.lengthSquared() < 1.0E-4) {
            return;
        }
        horizontalDirection = horizontalDirection.normalize();
        double targetHorizontalSpeed = Math.hypot(targetMotion.x, targetMotion.z);
        double playerHorizontalSpeed = Math.hypot(playerMotion.x, playerMotion.z);
        ElytraBoost elytraBoost = ElytraBoost.INSTANCE;
        boolean boosterActive = elytraBoost != null && elytraBoost.isEnable();
        double catchUpBoost = MathHelper.clamp((double)((horizontalGap - 1.8) * 0.04), (double)0.0, (double)(boosterActive ? 0.28 : 0.18));
        double desiredHorizontalSpeed = Math.max(targetHorizontalSpeed, 0.05) + catchUpBoost;
        if (boosterActive && playerHorizontalSpeed > desiredHorizontalSpeed && horizontalGap > 1.5) {
            desiredHorizontalSpeed += (playerHorizontalSpeed - desiredHorizontalSpeed) * 0.2;
        }
        double desiredY = targetMotion.y + MathHelper.clamp((double)(verticalOffset * 0.045), (double)-0.12, (double)0.12);
        double blend = boosterActive ? 0.12 : 0.18;
        double targetX = horizontalDirection.x * desiredHorizontalSpeed;
        double targetZ = horizontalDirection.z * desiredHorizontalSpeed;
        ElytraTarget.mc.player.setVelocity(playerMotion.x + (targetX - playerMotion.x) * blend, playerMotion.y + (desiredY - playerMotion.y) * blend, playerMotion.z + (targetZ - playerMotion.z) * blend);
    }

    @EventLink
        public void onUpdate(EventUpdate event) {
        LivingEntity target;
        if (ElytraTarget.mc.player == null || ElytraTarget.mc.world == null) {
            ElytraTarget.resetPredictState();
            this.resetChase();
            return;
        }
        Aura aura = ModuleClass.aura;
        LivingEntity var_1309_2 = target = aura != null && aura.isEnable() ? aura.getTarget() : null;
        if (target != null) {
            PredictUtils.updateEntity(target);
        }
        ElytraTarget.smartPredict(target);
        this.updateChase(target, predictCondition);
        this.syncTargetFlightSpeed(target);
        this.updateFireworks(target);
    }

    private void updateFireworks(LivingEntity target) {
        boolean useFireworkNow = false;
        if (!this.useFirework.is("По таймеру") || !this.isAuraActive()) {
            return;
        }
        if (this.blockOnUse.isState() && ElytraTarget.mc.player.isUsingItem()) {
            return;
        }
        if (ElytraTarget.mc.player.getItemCooldownManager().isCoolingDown(Items.FIREWORK_ROCKET.getDefaultStack())) {
            return;
        }
        switch (this.fireworkMode.getCurrent()) {
            case "Только с целью": {
                useFireworkNow = target != null;
                break;
            }
            case "Без цели": {
                useFireworkNow = target == null;
                break;
            }
            default: {
                useFireworkNow = true;
                break;
            }
        }
        if (!useFireworkNow) {
            return;
        }
        long delay = (long)(MathHelper.clamp((float)this.fireworkTiming.getValue().floatValue(), (float)0.1f, (float)5.0f) * 1000.0f);
        if (!this.fireworkTimer.finished(delay)) {
            return;
        }
        if (this.handMode.is("Основная")) {
            InventoryUtils.inventorySwapClick(Items.FIREWORK_ROCKET, true);
        } else {
            InventoryUtils.swapAndUseHvH(Items.FIREWORK_ROCKET);
        }
        this.fireworkTimer.reset();
    }

        public static void smartPredict(LivingEntity target) {
        if (!tickStopWatch.finished(50L)) {
            return;
        }
        if (target != null) {
            double dz;
            double dy;
            double dx = target.getX() - target.prevX;
            float speed = (float)Math.sqrt(dx * dx + (dy = target.getY() - target.prevY) * dy + (dz = target.getZ() - target.prevZ) * dz) * 20.0f;
            if (speed > 20.0f) {
                movementTicks = 0;
                prePredictCondition = false;
                predictCondition = true;
            } else {
                ++movementTicks;
                if (predictCondition) {
                    prePredictCondition = true;
                }
                if (movementTicks >= 3) {
                    predictCondition = false;
                    movementTicks = 0;
                    prePredictCondition = false;
                } else {
                    predictCondition = prePredictCondition;
                }
            }
        }
        tickStopWatch.reset();
    }

        private boolean shouldAimAtTargetXZ(Vec3d playerEye, Vec3d targetEye, Vec3d forwardVec) {
        double verticalGap = playerEye.y - targetEye.y;
        if (verticalGap < 3.0) {
            return false;
        }
        Vec3d horizontalForward = new Vec3d(forwardVec.x, 0.0, forwardVec.z);
        Vec3d targetToPlayer = new Vec3d(playerEye.x - targetEye.x, 0.0, playerEye.z - targetEye.z);
        if (horizontalForward.lengthSquared() <= 1.0E-4 || targetToPlayer.lengthSquared() <= 1.0E-4) {
            return false;
        }
        double behindDot = horizontalForward.normalize().dotProduct(targetToPlayer.normalize());
        return behindDot < -0.15;
    }

    private Vec3d getChaseForward(LivingEntity target, Vec3d playerEye, Vec3d targetEye) {
        Vec3d horizontalForward;
        Vec3d forwardVec = this.resolveTargetForward(target);
        if (forwardVec.lengthSquared() <= 1.0E-4) {
            return Vec3d.ZERO;
        }
        forwardVec = forwardVec.normalize();
        if (playerEye.y > targetEye.y + 0.35 && forwardVec.y > 0.0 && (horizontalForward = new Vec3d(forwardVec.x, 0.0, forwardVec.z)).lengthSquared() > 1.0E-4) {
            return horizontalForward.normalize();
        }
        return forwardVec;
    }

    private Vec3d resolveTargetForward(LivingEntity target) {
        Vec3d resolvedForward;
        PredictUtils.PositionData data = PredictUtils.getData(target);
        if (data != null && (resolvedForward = data.getResolvedForward()).lengthSquared() > 1.0E-4) {
            return resolvedForward;
        }
        Vec3d motion = target.getVelocity();
        Vec3d horizontalMotion = new Vec3d(motion.x, 0.0, motion.z);
        if (horizontalMotion.lengthSquared() > 1.0E-4) {
            return motion;
        }
        return target.getRotationVector();
    }

    private boolean hasActiveFireworkBoost() {
        if (ElytraTarget.mc.world == null || ElytraTarget.mc.player == null) {
            return false;
        }
        for (Entity entity : ElytraTarget.mc.world.getEntities()) {
            LivingEntity shooter;
            FireworkRocketEntity rocket;
            if (!(entity instanceof FireworkRocketEntity) || !(rocket = (FireworkRocketEntity)entity).isAlive() || (shooter = ((FireworkRocketEntityAccessor)rocket).polar$getShooter()) != ElytraTarget.mc.player) continue;
            return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        ElytraTarget.resetPredictState();
        this.resetChase();
        super.onDisable();
    }
}

