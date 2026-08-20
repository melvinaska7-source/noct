package polar.ru.client.modules.impl.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventGameUpdate;
import polar.ru.api.storages.implement.RotationStorage;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.api.utils.rotate.Rotation;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.combat.components.gcd.GCDUtil;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ListSetting;
import polar.ru.polar;

public class AimBot
extends Module {
    public static AimBot INSTANCE = new AimBot();
    private final ListSetting targetTypes = new ListSetting("Типы целей", new BooleanSetting("Игроки", true), new BooleanSetting("В броне", true), new BooleanSetting("Без брони", false), new BooleanSetting("Мобы", false), new BooleanSetting("Зомби", false));
    private final FloatSetting range = new FloatSetting("Дистанция", 40.0f, 10.0f, 100.0f, 1.0f);
    private final FloatSetting aimTime = new FloatSetting("Время наводки (тики)", 10.0f, 0.0f, 40.0f, 1.0f);
    private final BooleanSetting silentRotations = new BooleanSetting("Тихие повороты", true);
    private final BooleanSetting showCrosshair = new BooleanSetting("Показать прицел", true);
    private final FloatSetting crosshairSize = new FloatSetting("Размер прицела", 1.0f, 0.3f, 3.0f, 0.1f);
    private LivingEntity target = null;
    private boolean isAiming = false;
    private float aimProgress = 0.0f;
    private Rotation targetRotation = null;

    public AimBot() {
        super("AimBot", "Авто-наведение для лука и арбалета", Module.ModuleCategory.COMBAT);
        this.addSettings(this.targetTypes, this.range, this.aimTime, this.silentRotations, this.showCrosshair, this.crosshairSize);
    }

    private Identifier getCrosshairTexture() {
        return Identifier.of((String)"polar", (String)"textures/cross/hit.png");
    }

        private boolean isHoldingBowOrCrossbow() {
        ItemStack mainHand = AimBot.mc.player.getMainHandStack();
        ItemStack offHand = AimBot.mc.player.getOffHandStack();
        return mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem || offHand.getItem() instanceof BowItem || offHand.getItem() instanceof CrossbowItem;
    }

    private boolean isUsingBowOrCrossbow() {
        return AimBot.mc.player.isUsingItem() && this.isHoldingBowOrCrossbow();
    }

        private boolean isValidTarget(LivingEntity entity) {
        if (entity == AimBot.mc.player) {
            return false;
        }
        if (!entity.isAlive() || entity.getHealth() <= 0.0f) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            if (!this.targetTypes.is("Игроки")) {
                return false;
            }
            if (polar.INSTANCE.friendStorage.isFriend(entity.getName().getString())) {
                return false;
            }
            boolean hasArmor = false;
            PlayerEntity player = (PlayerEntity)entity;
            for (ItemStack armor : player.getArmorItems()) {
                if (armor.isEmpty()) continue;
                hasArmor = true;
                break;
            }
            if (this.targetTypes.is("В броне") && hasArmor) {
                return true;
            }
            if (this.targetTypes.is("Без брони") && !hasArmor) {
                return true;
            }
            return !this.targetTypes.is("В броне") && !this.targetTypes.is("Без брони");
        }
        if (entity instanceof ZombieEntity) {
            return this.targetTypes.is("Зомби");
        }
        if (entity instanceof HostileEntity) {
            return this.targetTypes.is("Мобы");
        }
        return false;
    }

        private LivingEntity findBestTarget() {
        ArrayList<LivingEntity> targets = new ArrayList<LivingEntity>();
        Box searchBox = AimBot.mc.player.getBoundingBox().expand((double)this.range.getValue().floatValue());
        for (LivingEntity entity2 : AimBot.mc.world.getEntitiesByClass(LivingEntity.class, searchBox, e2 -> true)) {
            double dist;
            if (!this.isValidTarget(entity2) || (dist = (double)AimBot.mc.player.distanceTo((Entity)entity2)) > (double)this.range.getValue().floatValue()) continue;
            targets.add(entity2);
        }
        if (targets.isEmpty()) {
            return null;
        }
        targets.sort(Comparator.comparingDouble(entity -> AimBot.mc.player.distanceTo((Entity)entity)));
        return (LivingEntity)targets.get(0);
    }

        private Rotation calculateBowRotation(LivingEntity target) {
        Vec3d eyes = AimBot.mc.player.getEyePos();
        Vec3d targetPos = target.getBoundingBox().getCenter();
        double dx = targetPos.x - eyes.x;
        double dy = targetPos.y - eyes.y;
        double dz = targetPos.z - eyes.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, distance)));
        return new Rotation(yaw, pitch);
    }

    @EventLink
    public void onRender3D(Event3DRender event) {
        if (!this.showCrosshair.isState() || this.target == null || !this.isAiming) {
            return;
        }
        float partialTicks = event.getTickDelta();
        Vec3d targetPos = new Vec3d(MathHelper.lerp((double)partialTicks, (double)this.target.lastRenderX, (double)this.target.getX()), MathHelper.lerp((double)partialTicks, (double)this.target.lastRenderY, (double)this.target.getY()) + (double)this.target.getHeight() / 2.0, MathHelper.lerp((double)partialTicks, (double)this.target.lastRenderZ, (double)this.target.getZ()));
        Vec3d cameraPos = AimBot.mc.gameRenderer.getCamera().getPos();
        MatrixStack matrices = event.getMatrices();
        double renderX = targetPos.x - cameraPos.x;
        double renderY = targetPos.y - cameraPos.y;
        double renderZ = targetPos.z - cameraPos.z;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture((int)0, (Identifier)this.getCrosshairTexture());
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        matrices.push();
        matrices.translate(renderX, renderY, renderZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-AimBot.mc.gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(AimBot.mc.gameRenderer.getCamera().getPitch()));
        float size = this.crosshairSize.get() * 0.5f;
        int alpha = (int)(255.0f * this.aimProgress);
        int color = ColorUtils.getThemeColor();
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -size, -size, 0.0f).texture(0.0f, 1.0f).color(r2, g2, b2, alpha);
        buffer.vertex(matrix, -size, size, 0.0f).texture(0.0f, 0.0f).color(r2, g2, b2, alpha);
        buffer.vertex(matrix, size, size, 0.0f).texture(1.0f, 0.0f).color(r2, g2, b2, alpha);
        buffer.vertex(matrix, size, -size, 0.0f).texture(1.0f, 1.0f).color(r2, g2, b2, alpha);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        matrices.pop();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @EventLink
    public void onGameUpdate(EventGameUpdate e2) {
        if (AimBot.mc.player == null || AimBot.mc.world == null) {
            return;
        }
        this.isAiming = this.isUsingBowOrCrossbow();
        if (this.isAiming) {
            LivingEntity newTarget = this.findBestTarget();
            if (newTarget != null) {
                if (this.target != newTarget) {
                    this.target = newTarget;
                    this.aimProgress = 0.0f;
                }
                Rotation newRotation = this.calculateBowRotation(this.target);
                float maxStep = 1.0f / Math.max(1.0f, this.aimTime.getValue().floatValue());
                this.aimProgress = Math.min(this.aimProgress + maxStep, 1.0f);
                float currentYaw = AimBot.mc.player.getYaw();
                float currentPitch = AimBot.mc.player.getPitch();
                float targetYaw = newRotation.getYaw();
                float targetPitch = newRotation.getPitch();
                float yawDiff = MathHelper.wrapDegrees((float)(targetYaw - currentYaw));
                float pitchDiff = targetPitch - currentPitch;
                float stepYaw = yawDiff * this.aimProgress;
                float stepPitch = pitchDiff * this.aimProgress;
                this.targetRotation = new Rotation(currentYaw + stepYaw, currentPitch + stepPitch);
            }
        } else {
            this.target = null;
            this.targetRotation = null;
            this.aimProgress = 0.0f;
        }
    }

    @EventLink
    public void onUpdate(EventGameUpdate ignoredghj) {
        if (this.target != null && this.isAiming && this.targetRotation != null) {
            if (this.silentRotations.isState()) {
                float gcd = GCDUtil.getGCD();
                float yaw = this.targetRotation.getYaw();
                float pitch = this.targetRotation.getPitch();
                yaw -= (yaw - AimBot.mc.player.getYaw()) % gcd;
                pitch -= (pitch - AimBot.mc.player.getPitch()) % gcd;
                RotationStorage.update(new Rotation(yaw, pitch), 180.0f, 180.0f, 45.0f, 45.0f, 0, 2, false);
            } else {
                AimBot.mc.player.setYaw(this.targetRotation.getYaw());
                AimBot.mc.player.setPitch(this.targetRotation.getPitch());
            }
        }
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.target = null;
        this.isAiming = false;
        this.aimProgress = 0.0f;
        this.targetRotation = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.target = null;
        this.isAiming = false;
        this.aimProgress = 0.0f;
        this.targetRotation = null;
    }
}

