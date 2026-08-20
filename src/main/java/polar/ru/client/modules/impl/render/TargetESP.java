package polar.ru.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL11;
import polar.ru.api.events.EventLink;
import polar.ru.api.events.implement.Event3DRender;
import polar.ru.api.events.implement.EventRender;
import polar.ru.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import polar.ru.api.utils.animation.Easings;
import polar.ru.api.utils.color.ColorUtils;
import polar.ru.client.modules.Module;
import polar.ru.client.modules.impl.combat.Aura;
import polar.ru.client.modules.impl.render.CubeParticle;
import polar.ru.client.modules.settings.implement.BooleanSetting;
import polar.ru.client.modules.settings.implement.FloatSetting;
import polar.ru.client.modules.settings.implement.ModeSetting;

public class TargetESP
extends Module {
    public static TargetESP INSTANCE = new TargetESP();
    private static final float GHOST_ALPHA_MULT = 0.6f;
    private static final float CELKA_SPEED_MULT = 1.2f;
    private static final float SCALE_FACTOR = 0.007f;
    static final long CUBE_ATTACH_LIFE_MS = 560L;
    static final long CUBE_FADE_LIFE_MS = 320L;
    static final int MAX_CUBE_PARTICLES = 72;
    static final byte[][] CUBE_EDGES = new byte[][]{{-1, -1, -1, 1, -1, -1}, {1, -1, -1, 1, -1, 1}, {1, -1, 1, -1, -1, 1}, {-1, -1, 1, -1, -1, -1}, {-1, 1, -1, 1, 1, -1}, {1, 1, -1, 1, 1, 1}, {1, 1, 1, -1, 1, 1}, {-1, 1, 1, -1, 1, -1}, {-1, -1, -1, -1, 1, -1}, {1, -1, -1, 1, 1, -1}, {1, -1, 1, 1, 1, 1}, {-1, -1, 1, -1, 1, 1}};
    private final ModeSetting mode = new ModeSetting("Режим", "Картинка 1", "Картинка 1", "Картинка 2", "Кольцо", "Кольцо 2", "Души", "Целка", "Цепи", "Кубы", "Кристаллы", "BlackHole");
    private final FloatSetting size = new FloatSetting("Размер", 1.15f, 0.6f, 2.5f, 0.05f);
    private final FloatSetting ringRadius = new FloatSetting("Радиус кольца", 0.5f, 0.3f, 1.5f, 0.05f);
    private final FloatSetting ringSpeed = new FloatSetting("Скорость кольца", 1.0f, 0.3f, 3.0f, 0.1f);
    private final FloatSetting rotateSpeed = new FloatSetting("Скорость вращения", 1.2f, 0.2f, 4.0f, 0.05f);
    private final BooleanSetting hurtColor = new BooleanSetting("Окрашивание при ударе", true);
    private final BooleanSetting saturationMode = new BooleanSetting("Режим насыщенности", true);
    private final BooleanSetting containerDetection = new BooleanSetting("Контейнеры", false);
    private final FloatSetting bmwGhostCount = new FloatSetting("Кол-во призраков", 3.0f, 2.0f, 5.0f, 1.0f);
    private final FloatSetting bmwGhostLife = new FloatSetting("Время жизни (мс)", 350.0f, 150.0f, 500.0f, 25.0f);
    private final FloatSetting bmwStrengthXZ = new FloatSetting("Цикл XZ", 2000.0f, 1000.0f, 5000.0f, 100.0f);
    private final FloatSetting bmwStrengthY = new FloatSetting("Цикл Y", 1700.0f, 1000.0f, 5000.0f, 100.0f);
    private float appearValue = 0.0f;
    private float scaleValue = 0.0f;
    private float rotProgress = 0.0f;
    private float rotFrom = -280.0f;
    private float rotTo = 280.0f;
    private long lastRotateUpdate = System.currentTimeMillis();
    private LivingEntity lastTarget = null;
    private LivingEntity lastHandledTarget = null;
    private Vec3d lastTargetPos = null;
    private float lastTargetHeight = 1.8f;
    private float lastTargetWidth = 0.6f;
    private final CopyOnWriteArrayList<GlowPoint> bmwPoints = new CopyOnWriteArrayList();
    private float crystalRotationAngle = 0.0f;
    private float crystalAnimation = 0.0f;
    private float spawnAccumulator = 0.0f;
    private long lastCubeTime = 0L;
    private final ArrayList<CubeParticle> cubeParticles = new ArrayList();
    private final ArrayList<CubeParticle> renderCubeParticles = new ArrayList();
    private static final float SPAWN_INTERVAL = 0.022f;
    private static final int PARTICLES_PER_SPAWN = 1;
    private float ring2Angle = 0.0f;
    private long ring2LastUpdateMs = 0L;
    private static final int RING2_GLOW_MAX = 8192;
    private final float[] ring2GlowX = new float[8192];
    private final float[] ring2GlowY = new float[8192];
    private final float[] ring2GlowZ = new float[8192];
    private final long[] ring2GlowTime = new long[8192];
    private final int[] ring2GlowHue = new int[8192];
    private int ring2GlowHead = 0;
    private int ring2GlowCount = 0;
    private float chainSpawnAnimation = 0.0f;
    private Vec3d chainLastRenderPosition = null;
    private LivingEntity chainLastTarget = null;
    private float blackHoleAngle = 0.0f;

    public TargetESP() {
        super("TargetESP", "Подсветка и эффекты на цели", Module.ModuleCategory.RENDER);
        this.size.visible(() -> this.isImageMode() || this.mode.is("BlackHole"));
        this.rotateSpeed.visible(() -> this.isImageMode() || this.mode.is("BlackHole"));
        this.saturationMode.visible(() -> this.isImageMode() || this.mode.is("Кольцо 2") || this.mode.is("Цепи") || this.mode.is("BlackHole"));
        this.bmwGhostCount.visible(() -> this.mode.is("Райдер"));
        this.bmwGhostLife.visible(() -> this.mode.is("Райдер"));
        this.bmwStrengthXZ.visible(() -> this.mode.is("Райдер"));
        this.bmwStrengthY.visible(() -> this.mode.is("Райдер"));
        this.ringRadius.visible(() -> this.mode.is("Кольцо"));
        this.ringSpeed.visible(() -> this.mode.is("Кольцо"));
        this.addSettings(this.mode, this.size, this.rotateSpeed, this.hurtColor, this.saturationMode, this.containerDetection, this.ringRadius, this.ringSpeed, this.bmwGhostCount, this.bmwGhostLife, this.bmwStrengthXZ, this.bmwStrengthY);
    }

    @Override
    public void onDisable() {
        this.appearValue = 0.0f;
        this.scaleValue = 0.0f;
        this.lastTarget = null;
        this.lastHandledTarget = null;
        this.lastTargetPos = null;
        this.rotProgress = 0.0f;
        this.rotFrom = -280.0f;
        this.rotTo = 280.0f;
        this.bmwPoints.clear();
        this.crystalRotationAngle = 0.0f;
        this.crystalAnimation = 0.0f;
        this.spawnAccumulator = 0.0f;
        this.lastCubeTime = 0L;
        this.cubeParticles.clear();
        this.renderCubeParticles.clear();
        this.ring2Angle = 0.0f;
        this.ring2GlowCount = 0;
        this.ring2GlowHead = 0;
        this.ring2LastUpdateMs = 0L;
        this.chainSpawnAnimation = 0.0f;
        this.chainLastRenderPosition = null;
        this.chainLastTarget = null;
        this.blackHoleAngle = 0.0f;
        super.onDisable();
    }

    private boolean isImageMode() {
        return this.mode.is("Картинка 1") || this.mode.is("Картинка 2");
    }

    private Identifier getCaptureTexture() {
        if (this.mode.is("Картинка 2")) {
            return Identifier.of((String)"polar", (String)"textures/targetesp/targetesp_3.png");
        }
        return Identifier.of((String)"polar", (String)"textures/targetesp/targetesp_2.png");
    }

    private Identifier getBloomTexture() {
        return Identifier.of((String)"polar", (String)"textures/targetesp/bloom.png");
    }

    private Identifier getBlackHoleTexture() {
        return Identifier.of((String)"polar", (String)"textures/targetesp/blackhole.png");
    }

    private int getESPColor() {
        int color = ColorUtils.getThemeColor();
        if ((color >> 24 & 0xFF) == 0) {
            color |= 0xFF000000;
        }
        return color;
    }

    private float animateTo(float current, float target, float delta) {
        if (current < target) {
            current = Math.min(current + delta, target);
        } else if (current > target) {
            current = Math.max(current - delta, target);
        }
        return current;
    }

    private float getDistanceScale(Vec3d cameraPos, double worldX, double worldY, double worldZ) {
        double dx = worldX - cameraPos.x;
        double dy = worldY - cameraPos.y;
        double dz = worldZ - cameraPos.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return (float)Math.max(0.1, distance * (double)0.007f);
    }

    @EventLink(priority=-100)
    public void onRender3D(Event3DRender event) {
        if (mc == null || TargetESP.mc.player == null || TargetESP.mc.world == null) {
            return;
        }
        Aura aura = ModuleClass.aura;
        boolean auraEnabled = aura != null && aura.isEnable();
        LivingEntity target = auraEnabled ? aura.getTarget() : null;
        boolean hasTarget = target != null && target.isAlive();
        ContainerMatch containerMatch = null;
        if (this.containerDetection.isState() && !hasTarget && TargetESP.mc.player != null && (containerMatch = this.findNearestContainer(TargetESP.mc.player.getBlockPos())) != null) {
            this.lastTargetPos = new Vec3d(containerMatch.pos.x, containerMatch.pos.y - 0.75, containerMatch.pos.z);
            this.lastTargetHeight = 1.0f;
            this.lastTargetWidth = 1.0f;
            hasTarget = true;
        }
        float speed = 0.05f;
        this.appearValue = this.animateTo(this.appearValue, hasTarget ? 1.0f : 0.0f, speed);
        this.scaleValue = this.animateTo(this.scaleValue, hasTarget ? 1.0f : 0.5f, speed);
        if (hasTarget && target != null) {
            this.lastTarget = target;
            this.lastHandledTarget = target;
        }
        if (this.mode.is("Кристаллы")) {
            float crystalSpeed = hasTarget ? 0.07f : 0.045f;
            this.crystalAnimation = this.animateTo(this.crystalAnimation, hasTarget ? 1.0f : 0.0f, crystalSpeed);
            if (hasTarget) {
                this.crystalRotationAngle += 0.8f;
            }
        }
        if (this.appearValue <= 0.001f && !hasTarget && (!this.mode.is("Кристаллы") || this.crystalAnimation <= 0.001f)) {
            this.lastTarget = null;
            this.lastTargetPos = null;
            return;
        }
        if (hasTarget && target != null) {
            float td = event.getTickDelta();
            this.lastTargetPos = new Vec3d(MathHelper.lerp((double)td, (double)target.lastRenderX, (double)target.getX()), MathHelper.lerp((double)td, (double)target.lastRenderY, (double)target.getY()), MathHelper.lerp((double)td, (double)target.lastRenderZ, (double)target.getZ()));
            this.lastTargetHeight = target.getHeight();
            this.lastTargetWidth = target.getWidth();
        }
        if (this.lastTargetPos == null) {
            return;
        }
        if (this.mode.is("Райдер")) {
            if (hasTarget && target != null) {
                this.addBMWGhosts(target, event.getTickDelta(), Math.max(1, Math.round(this.bmwGhostCount.getValue().floatValue())), Math.max(1, Math.round(this.bmwGhostLife.getValue().floatValue())), this.getESPColor());
            }
            this.bmwPoints.removeIf(GlowPoint::shouldRemove);
            this.drawBMW3D(event);
            return;
        }
        if (this.mode.is("Кристаллы")) {
            LivingEntity crystalTarget;
            LivingEntity var_1309_2 = crystalTarget = hasTarget ? target : this.lastTarget;
            if ((crystalTarget != null || this.lastTargetPos != null) && this.crystalAnimation > 0.01f) {
                this.renderCrystals3D(event.getMatrices(), crystalTarget, event.getTickDelta());
            }
            return;
        }
        if (this.isImageMode()) {
            this.renderMarker3D(event);
        }
        if (this.mode.is("Души")) {
            this.drawSouls3D(event);
        }
        if (this.mode.is("Призраки")) {
            this.drawCelka3D(event);
        }
        if (this.mode.is("Кольцо")) {
            this.drawRing3D(event);
        }
        if (this.mode.is("Кольцо 2")) {
            this.drawRing2_3D(event);
        }
        if (this.mode.is("Целка")) {
            this.drawCelka2_3D(event);
        }
        if (this.mode.is("Цепи")) {
            this.drawChains3D(event, target, hasTarget);
        }
        if (this.mode.is("Кубы")) {
            this.renderCubes(event, target, hasTarget);
        }
        if (this.mode.is("BlackHole")) {
            this.drawBlackHole3D(event);
        }
    }

    private void drawBlackHole3D(Event3DRender event) {
        if (this.lastTargetPos == null || this.appearValue <= 0.001f) {
            return;
        }
        Vec3d cam = TargetESP.mc.gameRenderer.getCamera().getPos();
        double worldX = this.lastTargetPos.x;
        double worldY = this.lastTargetPos.y + (double)(this.lastTargetHeight * 0.5f);
        double worldZ = this.lastTargetPos.z;
        float baseSize = this.size.getValue().floatValue() * 14.0f;
        float renderSize = baseSize * this.scaleValue;
        this.blackHoleAngle += this.rotateSpeed.getValue().floatValue() * 3.0f;
        if (this.blackHoleAngle >= 360.0f) {
            this.blackHoleAngle -= 360.0f;
        }
        float hurtPC = this.getHurtPC(this.lastTarget);
        int baseColor = this.multAlpha(this.getESPColor(), this.appearValue);
        int redColor = this.multAlpha(this.getSoftRedColor(), this.appearValue);
        int color = this.overCol(baseColor, redColor, hurtPC);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.getBlackHoleTexture());
        this.drawBillboard(event.getMatrices(), cam, worldX, worldY, worldZ, renderSize, color, this.blackHoleAngle);
        this.drawBillboard(event.getMatrices(), cam, worldX, worldY, worldZ, renderSize * 0.7f, -16777216, -this.blackHoleAngle * 1.5f);
        if (this.saturationMode.isState()) {
            this.drawBillboard(event.getMatrices(), cam, worldX, worldY, worldZ, renderSize * 1.2f, this.multAlpha(color, 0.5f), this.blackHoleAngle);
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderCubes(Event3DRender event, LivingEntity target, boolean hasTarget) {
        int i2;
        long now = System.currentTimeMillis();
        if (this.lastCubeTime == 0L) {
            this.lastCubeTime = now;
        }
        float dt = Math.min((float)(now - this.lastCubeTime) / 1000.0f, 0.1f);
        this.lastCubeTime = now;
        if (!Float.isFinite(dt) || TargetESP.mc.gameRenderer == null || TargetESP.mc.gameRenderer.getCamera() == null) {
            return;
        }
        if (hasTarget && target != null) {
            this.lastTarget = target;
            this.spawnAccumulator += dt;
            while (this.spawnAccumulator >= 0.022f) {
                this.spawnAccumulator -= 0.022f;
                if (this.cubeParticles.size() < 72) {
                    for (i2 = 0; i2 < 1; ++i2) {
                        double rand = Math.random() * 360.0;
                        double px = Math.cos(Math.toRadians(rand)) * 0.7;
                        double py = 0.02 + Math.random() * 0.1;
                        double pz = Math.sin(Math.toRadians(rand)) * 0.7;
                        this.cubeParticles.add(new CubeParticle(target, px, py, pz));
                    }
                    continue;
                }
                break;
            }
        } else {
            this.spawnAccumulator = 0.0f;
        }
        this.renderCubeParticles.clear();
        for (i2 = this.cubeParticles.size() - 1; i2 >= 0; --i2) {
            CubeParticle particle = this.cubeParticles.get(i2);
            try {
                particle.update(dt, now, (LivingEntity)(hasTarget ? target : null));
                if (particle.shouldRemove(now)) {
                    this.cubeParticles.remove(i2);
                    continue;
                }
                this.renderCubeParticles.add(particle);
                continue;
            }
            catch (Throwable ignored) {
                this.cubeParticles.remove(i2);
            }
        }
        if (this.renderCubeParticles.isEmpty()) {
            return;
        }
        float partialTicks = event.getTickDelta();
        MatrixStack matrices = event.getMatrices();
        Vec3d camPos = TargetESP.mc.gameRenderer.getCamera().getPos();
        LivingEntity colorTarget = hasTarget ? target : this.lastTarget;
        float hurtPC = this.getHurtPC(colorTarget);
        int baseColor = this.getESPColor();
        int redColor = this.getSoftRedColor();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder faceBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        boolean hasFaces = false;
        int size = this.renderCubeParticles.size();
        for (int i3 = 0; i3 < size; ++i3) {
            CubeParticle particle = this.renderCubeParticles.get(i3);
            try {
                int particleColor = particle.getRenderColor(baseColor, redColor, hurtPC, now);
                if ((particleColor >> 24 & 0xFF) <= 0 || !particle.appendCubeFaces(faceBuilder, matrices, camPos, partialTicks, particleColor)) continue;
                hasFaces = true;
                continue;
            }
            catch (Throwable particleColor) {
                // empty catch block
            }
        }
        if (hasFaces) {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)faceBuilder.end());
        }
        BufferBuilder lineBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        boolean hasLines = false;
        int size2 = this.renderCubeParticles.size();
        for (int i4 = 0; i4 < size2; ++i4) {
            CubeParticle particle = this.renderCubeParticles.get(i4);
            try {
                int particleColor = particle.getRenderColor(baseColor, redColor, hurtPC, now);
                if ((particleColor >> 24 & 0xFF) <= 0 || !particle.appendCubeLines(lineBuilder, matrices, camPos, partialTicks, particleColor)) continue;
                hasLines = true;
                continue;
            }
            catch (Throwable particleColor) {
                // empty catch block
            }
        }
        if (hasLines) {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)lineBuilder.end());
        }
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.getBloomTexture());
        BufferBuilder bloomBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        boolean hasBloom = false;
        float camYaw = TargetESP.mc.gameRenderer.getCamera().getYaw();
        float camPitch = TargetESP.mc.gameRenderer.getCamera().getPitch();
        int size3 = this.renderCubeParticles.size();
        for (int i5 = 0; i5 < size3; ++i5) {
            CubeParticle particle = this.renderCubeParticles.get(i5);
            try {
                int particleColor = particle.getRenderColor(baseColor, redColor, hurtPC, now);
                if (!particle.appendBloom(bloomBuilder, matrices, camPos, camYaw, camPitch, partialTicks, particleColor, now)) continue;
                hasBloom = true;
                continue;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        if (hasBloom) {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)bloomBuilder.end());
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private void drawRing3D(Event3DRender event) {
        Vec3d vec;
        float entityHeight;
        if (this.appearValue <= 0.001f || this.lastTargetPos == null) {
            return;
        }
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        LivingEntity target = this.lastTarget;
        if (target != null && target.isAlive()) {
            vec = new Vec3d(MathHelper.lerp((double)partialTicks, (double)target.lastRenderX, (double)target.getX()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderY, (double)target.getY()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderZ, (double)target.getZ()));
            entityHeight = target.getHeight();
        } else {
            vec = this.lastTargetPos;
            entityHeight = this.lastTargetHeight;
        }
        Vec3d cam = TargetESP.mc.gameRenderer.getCamera().getPos();
        double x2 = vec.x - cam.x;
        double y2 = vec.y - cam.y;
        double z2 = vec.z - cam.z;
        double duration = 2000.0 / (double)this.ringSpeed.get();
        double elapsed = System.currentTimeMillis() % (long)duration;
        boolean side = elapsed > duration / 2.0;
        double progress = elapsed / (duration / 2.0);
        progress = side ? (progress -= 1.0) : 1.0 - progress;
        progress = progress < 0.5 ? 2.0 * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;
        double eased = (double)entityHeight / 1.2 * (progress > 0.5 ? 1.0 - progress : progress) * (double)(side ? -1 : 1);
        int baseCol = this.getESPColor();
        float hurtPC = this.getHurtPC(target);
        int redCol = this.getSoftRedColor();
        int mainColor = this.overCol(baseCol, redCol, hurtPC);
        int colorWithAlpha = this.setAlpha(mainColor, 0.88235295f * this.appearValue);
        int colorTransparent = this.setAlpha(mainColor, 0.003921569f * this.appearValue);
        int colorFull = this.setAlpha(mainColor, this.appearValue);
        double radius = this.ringRadius.get();
        MatrixStack matrices = event.getMatrices();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.disableCull();
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i2 = 0; i2 <= 360; ++i2) {
            double rad = Math.toRadians(i2);
            float px = (float)(x2 + Math.cos(rad) * radius);
            float pz = (float)(z2 + Math.sin(rad) * radius);
            float py1 = (float)(y2 + (double)entityHeight * progress);
            float py2 = (float)(y2 + (double)entityHeight * progress + eased);
            buffer.vertex(matrix, px, py1, pz).color(colorWithAlpha);
            buffer.vertex(matrix, px, py2, pz).color(colorTransparent);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.lineWidth((float)1.5f);
        BufferBuilder lineBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i3 = 0; i3 <= 360; ++i3) {
            double rad = Math.toRadians(i3);
            float px = (float)(x2 + Math.cos(rad) * radius);
            float pz = (float)(z2 + Math.sin(rad) * radius);
            float py = (float)(y2 + (double)entityHeight * progress);
            lineBuffer.vertex(matrix, px, py, pz).color(colorFull);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)lineBuffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
    }

    private int setAlpha(int color, float alpha) {
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        return color & 0xFFFFFF | (int)(alpha * 255.0f) << 24;
    }

    @EventLink(priority=-100)
    public void onRender2D(EventRender.Default event) {
        if (!this.mode.is("Кристаллы") || this.crystalAnimation <= 0.001f || this.lastTargetPos == null) {
            return;
        }
        LivingEntity crystalTarget = this.lastTarget != null && this.lastTarget.isAlive() ? this.lastTarget : null;
        this.drawCrystalGlow2D(event.getContext().getMatrices(), crystalTarget);
    }

    private int multAlpha(int color, float mult) {
        int a2 = (int)((float)(color >> 24 & 0xFF) * mult);
        a2 = Math.max(0, Math.min(255, a2));
        return a2 << 24 | color & 0xFFFFFF;
    }

    private int replAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return alpha << 24 | color & 0xFFFFFF;
    }

    int overCol(int color1, int color2, float factor) {
        factor = Math.max(0.0f, Math.min(1.0f, factor));
        int r1 = color1 >> 16 & 0xFF;
        int g1 = color1 >> 8 & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = color1 >> 24 & 0xFF;
        int r2 = color2 >> 16 & 0xFF;
        int g2 = color2 >> 8 & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = color2 >> 24 & 0xFF;
        int r3 = (int)((float)r1 + (float)(r2 - r1) * factor);
        int g3 = (int)((float)g1 + (float)(g2 - g1) * factor);
        int b3 = (int)((float)b1 + (float)(b2 - b1) * factor);
        int a3 = (int)((float)a1 + (float)(a2 - a1) * factor);
        return a3 << 24 | r3 << 16 | g3 << 8 | b3;
    }

    private int getSoftRedColor() {
        return ColorUtils.rgb(255, 180, 180);
    }

    private float getHurtPC(LivingEntity target) {
        if (!this.hurtColor.isState() || target == null) {
            return 0.0f;
        }
        float partialTicks = mc != null ? mc.getRenderTickCounter().getTickDelta(true) : 0.0f;
        float hurtTicks = MathHelper.clamp((float)((float)target.hurtTime - partialTicks), (float)0.0f, (float)10.0f);
        float progress = hurtTicks / 10.0f;
        return progress * progress * (3.0f - 2.0f * progress);
    }

    private void drawBillboard(MatrixStack matrices, Vec3d cameraPos, double worldX, double worldY, double worldZ, float baseScreenSize, int color, float rotation) {
        float distScale = this.getDistanceScale(cameraPos, worldX, worldY, worldZ);
        float half = baseScreenSize * distScale * 0.5f;
        this.drawBillboardInternal(matrices, cameraPos, worldX, worldY, worldZ, half, color, rotation);
    }

    private void drawStaticBillboard(MatrixStack matrices, Vec3d cameraPos, double worldX, double worldY, double worldZ, float worldSize, int color, float rotation) {
        float half = worldSize * 0.5f;
        this.drawBillboardInternal(matrices, cameraPos, worldX, worldY, worldZ, half, color, rotation);
    }

    private void drawBillboardInternal(MatrixStack matrices, Vec3d cameraPos, double worldX, double worldY, double worldZ, float half, int color, float rotation) {
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        int a2 = color >> 24 & 0xFF;
        if (a2 <= 0) {
            return;
        }
        matrices.push();
        matrices.translate(worldX - cameraPos.x, worldY - cameraPos.y, worldZ - cameraPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-TargetESP.mc.gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(TargetESP.mc.gameRenderer.getCamera().getPitch()));
        if (rotation != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        }
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix, -half, -half, 0.0f).texture(0.0f, 1.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, -half, half, 0.0f).texture(0.0f, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, half, half, 0.0f).texture(1.0f, 0.0f).color(r2, g2, b2, a2);
        buffer.vertex(matrix, half, -half, 0.0f).texture(1.0f, 1.0f).color(r2, g2, b2, a2);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        matrices.pop();
    }

    private void renderMarker3D(Event3DRender event) {
        if (this.lastTargetPos == null || this.appearValue <= 0.001f) {
            return;
        }
        Vec3d cam = TargetESP.mc.gameRenderer.getCamera().getPos();
        double worldX = this.lastTargetPos.x;
        double worldY = this.lastTargetPos.y + (double)((this.lastTargetHeight + 0.4f) * 0.5f);
        double worldZ = this.lastTargetPos.z;
        float baseSize = this.size.getValue().floatValue() * 12.0f;
        float renderSize = baseSize * this.scaleValue;
        long now = System.currentTimeMillis();
        float dt = Math.max(0.001f, (float)(now - this.lastRotateUpdate) / 1000.0f);
        this.lastRotateUpdate = now;
        float cycleDuration = Math.max(0.35f, 2.2f / this.rotateSpeed.getValue().floatValue());
        this.rotProgress += dt / cycleDuration;
        while (this.rotProgress >= 1.0f) {
            this.rotProgress -= 1.0f;
            this.rotFrom = this.rotTo;
            this.rotTo = this.rotTo > 0.0f ? -280.0f : 280.0f;
        }
        float accel = (float)Easings.SINE_IN_OUT.ease(this.rotProgress);
        float rotation = MathHelper.lerp((float)accel, (float)this.rotFrom, (float)this.rotTo);
        float hurtPC = this.getHurtPC(this.lastTarget);
        int baseColor = this.multAlpha(this.getESPColor(), this.appearValue);
        int redColor = this.multAlpha(this.getSoftRedColor(), this.appearValue);
        int color = this.overCol(baseColor, redColor, hurtPC);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.getCaptureTexture());
        this.drawBillboard(event.getMatrices(), cam, worldX, worldY, worldZ, renderSize, color, rotation);
        if (this.saturationMode.isState()) {
            this.drawBillboard(event.getMatrices(), cam, worldX, worldY, worldZ, renderSize, color, rotation);
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawSouls3D(Event3DRender event) {
        int glowColor;
        int color;
        int red;
        int col;
        float alphaTrail;
        float sz;
        double worldZ;
        double worldY;
        double worldX;
        double c2;
        double s2;
        double angle;
        float trailFactor;
        int i2;
        float height;
        Vec3d vec;
        float entityHeight;
        if (this.appearValue <= 0.001f || this.lastTargetPos == null) {
            return;
        }
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        LivingEntity target = this.lastTarget;
        if (target != null && target.isAlive()) {
            vec = new Vec3d(MathHelper.lerp((double)partialTicks, (double)target.lastRenderX, (double)target.getX()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderY, (double)target.getY()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderZ, (double)target.getZ()));
            height = target.getHeight();
        } else {
            vec = this.lastTargetPos;
            height = this.lastTargetHeight;
        }
        Vec3d cam = TargetESP.mc.gameRenderer.getCamera().getPos();
        double baseX = vec.x;
        double baseY = vec.y + (double)(height / 2.0f);
        double baseZ = vec.z;
        double radius = 0.7;
        float fixedSize = 4.0f;
        long time = System.currentTimeMillis();
        float hurtPC = this.getHurtPC(target);
        int baseCol = this.getESPColor();
        int redCol = this.getSoftRedColor();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.getBloomTexture());
        MatrixStack matrices = event.getMatrices();
        for (i2 = 0; i2 < 20; ++i2) {
            trailFactor = 1.0f - (float)i2 / 20.0f * 0.7f;
            angle = 0.15 * ((double)time - (double)i2 * 10.0) / 25.0;
            s2 = Math.sin(angle) * radius;
            c2 = Math.cos(angle) * radius;
            worldX = baseX + s2;
            worldY = baseY + c2;
            worldZ = baseZ - c2;
            sz = fixedSize * trailFactor;
            alphaTrail = this.appearValue * 0.6f;
            col = this.multAlpha(baseCol, alphaTrail * this.appearValue);
            red = this.multAlpha(redCol, alphaTrail * this.appearValue);
            color = this.overCol(col, red, hurtPC);
            this.drawStaticBillboard(matrices, cam, worldX, worldY, worldZ, sz * 0.12f, color, 0.0f);
            glowColor = this.multAlpha(color, 0.45f);
            this.drawStaticBillboard(matrices, cam, worldX, worldY, worldZ, sz * 0.21f, glowColor, 0.0f);
        }
        for (i2 = 0; i2 < 20; ++i2) {
            trailFactor = 1.0f - (float)i2 / 20.0f * 0.7f;
            angle = 0.15 * ((double)time - (double)i2 * 10.0) / 25.0;
            s2 = Math.sin(angle) * radius;
            c2 = Math.cos(angle) * radius;
            worldX = baseX - s2;
            worldY = baseY + s2;
            worldZ = baseZ - c2;
            sz = fixedSize * trailFactor;
            alphaTrail = this.appearValue * 0.6f;
            col = this.multAlpha(baseCol, alphaTrail * this.appearValue);
            red = this.multAlpha(this.getSoftRedColor(), alphaTrail * this.appearValue);
            color = this.overCol(col, red, hurtPC);
            this.drawStaticBillboard(matrices, cam, worldX, worldY, worldZ, sz * 0.12f, color, 0.0f);
            glowColor = this.multAlpha(color, 0.45f);
            this.drawStaticBillboard(matrices, cam, worldX, worldY, worldZ, sz * 0.21f, glowColor, 0.0f);
        }
        for (i2 = 0; i2 < 20; ++i2) {
            trailFactor = 1.0f - (float)i2 / 20.0f * 0.7f;
            angle = 0.15 * ((double)time - (double)i2 * 10.0) / 25.0;
            s2 = Math.sin(angle) * radius;
            c2 = Math.cos(angle) * radius;
            worldX = baseX - s2;
            worldY = baseY - s2;
            worldZ = baseZ + c2;
            sz = fixedSize * trailFactor;
            alphaTrail = this.appearValue * 0.6f;
            col = this.multAlpha(baseCol, alphaTrail * this.appearValue);
            red = this.multAlpha(redCol, alphaTrail * this.appearValue);
            color = this.overCol(col, red, hurtPC);
            this.drawStaticBillboard(matrices, cam, worldX, worldY, worldZ, sz * 0.12f, color, 0.0f);
            glowColor = this.multAlpha(color, 0.45f);
            this.drawStaticBillboard(matrices, cam, worldX, worldY, worldZ, sz * 0.21f, glowColor, 0.0f);
        }
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask((boolean)true);
    }

    private void addBMWGhosts(LivingEntity entity, float partialTicks, int cornersCount, int maxTime, int colorBase) {
        float xzRange = 0.7f;
        float yRange = entity.getHeight();
        int delayXZ = (int)this.bmwStrengthXZ.getValue().floatValue();
        int delayY = (int)this.bmwStrengthY.getValue().floatValue();
        long time = System.currentTimeMillis();
        float rotateProgress = (float)(time % (long)delayXZ) / (float)delayXZ;
        float xzRotate = rotateProgress * 360.0f;
        float yProgress = (float)(time % (long)delayY) / (float)delayY;
        float yLrpPC = 0.5f - 0.5f * MathHelper.cos((float)(yProgress * ((float)Math.PI * 2)));
        for (int corner = 0; corner < cornersCount; ++corner) {
            float cornersPC = (float)corner / (float)cornersCount;
            double yawRad = Math.toRadians(MathHelper.wrapDegrees((float)(cornersPC * 360.0f + xzRotate)));
            float offsetX = -((float)Math.sin(yawRad)) * xzRange;
            float offsetY = yRange * yLrpPC;
            float offsetZ = (float)Math.cos(yawRad) * xzRange;
            this.bmwPoints.add(new GlowPoint(offsetX, offsetY, offsetZ, maxTime, colorBase));
        }
    }

    private void drawBMW3D(Event3DRender event) {
        LivingEntity renderTarget;
        if (this.bmwPoints.isEmpty() || this.appearValue <= 0.001f) {
            return;
        }
        LivingEntity var_1309_2 = renderTarget = this.lastTarget != null ? this.lastTarget : this.lastHandledTarget;
        if (renderTarget == null && this.lastTargetPos == null) {
            return;
        }
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        Vec3d basePos = renderTarget != null && renderTarget.isAlive() ? new Vec3d(MathHelper.lerp((double)partialTicks, (double)renderTarget.lastRenderX, (double)renderTarget.getX()), MathHelper.lerp((double)partialTicks, (double)renderTarget.lastRenderY, (double)renderTarget.getY()), MathHelper.lerp((double)partialTicks, (double)renderTarget.lastRenderZ, (double)renderTarget.getZ())) : this.lastTargetPos;
        if (basePos == null) {
            return;
        }
        Vec3d cam = TargetESP.mc.gameRenderer.getCamera().getPos();
        float hurtPC = this.getHurtPC(renderTarget);
        float fixedScreenSize = 6.0f;
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.getBloomTexture());
        MatrixStack matrices = event.getMatrices();
        for (GlowPoint point : this.bmwPoints) {
            float timePC = point.getTimeProgress();
            float trailFactor = 1.0f - timePC * 0.6f;
            double worldX = basePos.x + (double)point.x;
            double worldY = basePos.y + (double)point.y;
            double worldZ = basePos.z + (double)point.z;
            float sz = fixedScreenSize * trailFactor;
            int alpha = (int)(255.0f * this.appearValue * trailFactor * 0.8f);
            alpha = Math.max(0, Math.min(255, alpha));
            int col = this.replAlpha(point.baseColor, alpha);
            int red = this.replAlpha(this.getSoftRedColor(), alpha);
            int finalColor = this.overCol(col, red, hurtPC);
            this.drawBillboard(matrices, cam, worldX, worldY, worldZ, sz, finalColor, 0.0f);
        }
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask((boolean)true);
    }

    private void drawCelka3D(Event3DRender event) {
        Vec3d vec;
        float entityHeight;
        if (this.appearValue <= 0.001f || this.lastTargetPos == null) {
            return;
        }
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        LivingEntity target = this.lastTarget;
        if (target != null && target.isAlive()) {
            vec = new Vec3d(MathHelper.lerp((double)partialTicks, (double)target.lastRenderX, (double)target.getX()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderY, (double)target.getY()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderZ, (double)target.getZ()));
            entityHeight = target.getHeight();
        } else {
            vec = this.lastTargetPos;
            entityHeight = this.lastTargetHeight;
        }
        Vec3d cam = TargetESP.mc.gameRenderer.getCamera().getPos();
        double bx = vec.x;
        double by = vec.y;
        double bz = vec.z;
        double t2 = (double)System.currentTimeMillis() / 384.61539872299335 * (double)1.2f;
        double tv = (double)System.currentTimeMillis() / 666.6666666666666 * (double)1.2f;
        int baseCol = this.getESPColor();
        float fixedSize = 4.0f;
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.getBloomTexture());
        MatrixStack matrices = event.getMatrices();
        float radius = 0.65f;
        for (int k2 = 0; k2 < 4; ++k2) {
            for (int j2 = 0; j2 < 20; ++j2) {
                float kf = (float)j2 / 20.0f;
                float sizeFactor = 1.0f - kf * 0.55f;
                double tj = t2 - (double)j2 * 0.05;
                double tvj = tv - (double)j2 * 0.05;
                double cyc = (Math.sin(tvj) + 1.0) * 0.5;
                double baseAngle = Math.toRadians((double)k2 * 90.0 + tj * 50.0 % 360.0);
                double offX = Math.cos(baseAngle) * (double)radius;
                double offZ = Math.sin(baseAngle) * (double)radius;
                double offY = k2 % 2 == 0 ? 0.1 + 1.7 * cyc : 1.8 - 1.7 * cyc;
                double worldX = bx + offX;
                double worldY = by + offY;
                double worldZ = bz + offZ;
                float sz = fixedSize * sizeFactor;
                int finalAlpha = (int)(255.0f * this.appearValue * 0.6f);
                int color = this.replAlpha(baseCol, finalAlpha);
                this.drawBillboard(matrices, cam, worldX, worldY, worldZ, sz, color, 0.0f);
                int glowColor = this.multAlpha(color, 0.45f);
                this.drawBillboard(matrices, cam, worldX, worldY, worldZ, sz * 1.75f, glowColor, 0.0f);
            }
            radius *= -1.0f;
        }
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask((boolean)true);
    }

    private void renderCrystals3D(MatrixStack ms, LivingEntity target, float partialTicks) {
        if (this.lastTargetPos == null || this.crystalAnimation <= 0.01f) {
            return;
        }
        Vec3d cameraPos = TargetESP.mc.gameRenderer.getCamera().getPos();
        int baseColor = ColorUtils.getThemeColor();
        int color = this.multAlpha(baseColor, this.crystalAnimation);
        int glowColor = this.multAlpha(baseColor, this.crystalAnimation * 0.28f);
        float hurtPC = this.getHurtPC(target);
        if (hurtPC > 0.0f) {
            int hurtColor = this.multAlpha(this.getSoftRedColor(), this.crystalAnimation);
            color = this.overCol(color, hurtColor, hurtPC);
            glowColor = this.overCol(glowColor, this.multAlpha(hurtColor, 0.65f), hurtPC);
        }
        float entityWidth = target != null ? target.getWidth() : this.lastTargetWidth;
        float entityHeight = target != null ? target.getHeight() : this.lastTargetHeight;
        float width = entityWidth * 1.5f;
        Vec3d renderPos = target != null && target.isAlive() ? new Vec3d(MathHelper.lerp((double)partialTicks, (double)target.lastRenderX, (double)target.getX()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderY, (double)target.getY()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderZ, (double)target.getZ())) : this.lastTargetPos;
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        float orbitScale = 1.2f - 0.5f * this.crystalAnimation;
        ms.push();
        ms.translate(renderPos.x - cameraPos.x, renderPos.y - cameraPos.y, renderPos.z - cameraPos.z);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i2 = 0; i2 < 360; i2 += 20) {
            float offsetZ;
            float dirZ;
            float angleRad = (float)Math.toRadians((float)i2 + this.crystalRotationAngle);
            float sin = (float)(Math.sin(angleRad) * (double)width * (double)orbitScale);
            float cos = (float)(Math.cos(angleRad) * (double)width * (double)orbitScale);
            float crystalSize = 0.1f;
            float offsetX = sin;
            float dirX = -offsetX;
            float targetCenterY = entityHeight / 2.0f;
            float yOffset = 0.1f + entityHeight * Math.abs(MathHelper.sin((float)i2));
            float offsetY = yOffset;
            float dirY = targetCenterY - offsetY;
            float length = (float)Math.sqrt(dirX * dirX + dirY * dirY + (dirZ = -(offsetZ = cos)) * dirZ);
            if (length < 0.001f) continue;
            ms.push();
            ms.translate(offsetX, offsetY, offsetZ);
            Vector3f initial = new Vector3f(0.0f, 1.0f, 0.0f);
            Vector3f dir = new Vector3f(dirX /= length, dirY /= length, dirZ /= length);
            Vector3f axis = new Vector3f();
            initial.cross((Vector3fc)dir, axis);
            float axisLen = axis.length();
            if (axisLen >= 0.001f) {
                axis.div(axisLen);
                float dot = Math.max(-1.0f, Math.min(1.0f, initial.dot((Vector3fc)dir)));
                float angle = (float)Math.acos(dot);
                ms.multiply(new Quaternionf().setAngleAxis(angle, axis.x, axis.y, axis.z));
            }
            this.renderCrystalShape(buffer, ms.peek().getPositionMatrix(), crystalSize, color);
            ms.pop();
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        ms.pop();
        float glowBaseSize = 4.5f + entityWidth * 3.0f;
        float outerGlowSize = glowBaseSize * 1.28f;
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.getBloomTexture());
        for (int i3 = 0; i3 < 360; i3 += 20) {
            float angleRad = (float)Math.toRadians((float)i3 + this.crystalRotationAngle);
            float sin = (float)(Math.sin(angleRad) * (double)width * (double)orbitScale);
            float cos = (float)(Math.cos(angleRad) * (double)width * (double)orbitScale);
            float yOffset = 0.1f + entityHeight * Math.abs(MathHelper.sin((float)i3));
            double worldX = renderPos.x + (double)sin;
            double worldY = renderPos.y + (double)yOffset;
            double worldZ = renderPos.z + (double)cos;
            this.drawBillboard(ms, cameraPos, worldX, worldY, worldZ, outerGlowSize, this.multAlpha(glowColor, 0.24f), this.crystalRotationAngle + (float)i3);
            this.drawBillboard(ms, cameraPos, worldX, worldY, worldZ, glowBaseSize, glowColor, -(this.crystalRotationAngle + (float)i3 * 0.5f));
        }
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask((boolean)true);
    }

    private void renderCrystalShape(BufferBuilder buffer, Matrix4f matrix, float size, int color) {
        int r2 = color >> 16 & 0xFF;
        int g2 = color >> 8 & 0xFF;
        int b2 = color & 0xFF;
        int a2 = color >> 24 & 0xFF;
        float w2 = 0.06f;
        float h2 = 0.2f;
        this.tri(buffer, matrix, 0.0f, h2, 0.0f, w2, 0.0f, 0.0f, 0.0f, 0.0f, w2, r2, g2, b2, a2);
        this.tri(buffer, matrix, 0.0f, h2, 0.0f, 0.0f, 0.0f, w2, -w2, 0.0f, 0.0f, r2, g2, b2, a2);
        this.tri(buffer, matrix, 0.0f, h2, 0.0f, -w2, 0.0f, 0.0f, 0.0f, 0.0f, -w2, r2, g2, b2, a2);
        this.tri(buffer, matrix, 0.0f, h2, 0.0f, 0.0f, 0.0f, -w2, w2, 0.0f, 0.0f, r2, g2, b2, a2);
        this.tri(buffer, matrix, 0.0f, -h2, 0.0f, w2, 0.0f, 0.0f, 0.0f, 0.0f, w2, r2, g2, b2, a2);
        this.tri(buffer, matrix, 0.0f, -h2, 0.0f, 0.0f, 0.0f, w2, -w2, 0.0f, 0.0f, r2, g2, b2, a2);
        this.tri(buffer, matrix, 0.0f, -h2, 0.0f, -w2, 0.0f, 0.0f, 0.0f, 0.0f, -w2, r2, g2, b2, a2);
        this.tri(buffer, matrix, 0.0f, -h2, 0.0f, 0.0f, 0.0f, -w2, w2, 0.0f, 0.0f, r2, g2, b2, a2);
    }

    private void drawCrystalGlow2D(MatrixStack matrix, LivingEntity target) {
    }

    private void drawRing2_3D(Event3DRender event) {
        float height;
        Vec3d vec;
        float entityHeight;
        if (this.appearValue <= 0.001f || this.lastTargetPos == null) {
            this.ring2Angle = 0.0f;
            this.ring2GlowCount = 0;
            this.ring2GlowHead = 0;
            return;
        }
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        LivingEntity target = this.lastTarget;
        if (target != null && target.isAlive()) {
            vec = new Vec3d(MathHelper.lerp((double)partialTicks, (double)target.lastRenderX, (double)target.getX()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderY, (double)target.getY()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderZ, (double)target.getZ()));
            height = target.getHeight();
        } else {
            vec = this.lastTargetPos;
            height = this.lastTargetHeight;
        }
        Vec3d cam = TargetESP.mc.gameRenderer.getCamera().getPos();
        double duration = 1800.0;
        double elapsed = (double)System.currentTimeMillis() % duration;
        boolean side = elapsed > duration / 2.0;
        double progress = elapsed / (duration / 2.0);
        progress = side ? progress - 1.0 : 1.0 - progress;
        progress = progress < 0.5 ? 2.0 * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;
        double baseX = vec.x;
        double baseY = vec.y;
        double baseZ = vec.z;
        long nowMs = System.currentTimeMillis();
        float dt = this.ring2LastUpdateMs == 0L ? 0.016f : Math.min((float)(nowMs - this.ring2LastUpdateMs) / 1000.0f, 0.1f);
        this.ring2LastUpdateMs = nowMs;
        this.ring2Angle += 360.0f * dt;
        float ringH = (float)((double)height * progress);
        float radius = this.lastTargetWidth * 0.7f;
        int SPAWN = 180;
        for (int s2 = 0; s2 < 180; ++s2) {
            float spawnAngle = 2.0f * (float)s2;
            double rad = Math.toRadians(spawnAngle);
            int slot = this.ring2GlowHead & 0x1FFF;
            this.ring2GlowX[slot] = (float)(baseX + Math.cos(rad) * (double)radius);
            this.ring2GlowY[slot] = (float)(baseY + (double)ringH);
            this.ring2GlowZ[slot] = (float)(baseZ + Math.sin(rad) * (double)radius);
            this.ring2GlowTime[slot] = nowMs;
            this.ring2GlowHue[slot] = (int)(spawnAngle * 8.0f);
            ++this.ring2GlowHead;
            if (this.ring2GlowCount >= 8192) continue;
            ++this.ring2GlowCount;
        }
        if (this.ring2GlowCount == 0) {
            return;
        }
        int baseCol = this.getESPColor();
        float hurtPC = this.getHurtPC(target);
        int redCol = this.getSoftRedColor();
        GL11.glDisable((int)2929);
        GL11.glDepthMask((boolean)false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.getBloomTexture());
        MatrixStack matrices = event.getMatrices();
        long trailLifeMs = 160L;
        int total = Math.min(this.ring2GlowCount, 8192);
        for (int i2 = 0; i2 < total; ++i2) {
            float life;
            float a2;
            int slot = this.ring2GlowHead - 1 - i2 & 0x1FFF;
            long age = nowMs - this.ring2GlowTime[slot];
            if (age >= trailLifeMs || (a2 = this.appearValue * (life = 1.0f - (float)age / (float)trailLifeMs) * life) <= 0.01f) continue;
            double wx = (double)this.ring2GlowX[slot] - cam.x;
            double wy = (double)this.ring2GlowY[slot] - cam.y;
            double wz = (double)this.ring2GlowZ[slot] - cam.z;
            int hue = this.ring2GlowHue[slot];
            int color = ColorUtils.getThemeColor(hue);
            int blended = this.overCol(this.multAlpha(color, a2), this.multAlpha(redCol, a2), hurtPC);
            int finalColor = this.replAlpha(blended, MathHelper.clamp((int)((int)(255.0f * a2)), (int)0, (int)255));
            this.drawStaticBillboard(matrices, cam, wx + cam.x, wy + cam.y, wz + cam.z, 0.1f, finalColor, 0.0f);
            if (!this.saturationMode.isState()) continue;
            this.drawStaticBillboard(matrices, cam, wx + cam.x, wy + cam.y, wz + cam.z, 0.1f, finalColor, 0.0f);
        }
        GL11.glDepthMask((boolean)true);
        GL11.glEnable((int)2929);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawCelka2_3D(Event3DRender event) {
        Vec3d vec;
        float entityHeight;
        if (this.appearValue <= 0.001f || this.lastTargetPos == null) {
            return;
        }
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);
        LivingEntity target = this.lastTarget;
        if (target != null && target.isAlive()) {
            vec = new Vec3d(MathHelper.lerp((double)partialTicks, (double)target.lastRenderX, (double)target.getX()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderY, (double)target.getY()), MathHelper.lerp((double)partialTicks, (double)target.lastRenderZ, (double)target.getZ()));
            entityHeight = target.getHeight();
        } else {
            vec = this.lastTargetPos;
            entityHeight = this.lastTargetHeight;
        }
        Vec3d cam = TargetESP.mc.gameRenderer.getCamera().getPos();
        double bx = vec.x;
        double by = vec.y;
        double bz = vec.z;
        float speed = 1.3f;
        int trailLength = 20;
        float radiusConst = 0.7f;
        float upperPosition = 1.8f;
        float lowerPosition = 0.1f;
        long time = System.currentTimeMillis();
        double t2 = (double)time / 384.61539872299335;
        double tv = (double)time / 666.6666666666666;
        int baseCol = this.getESPColor();
        float hurtPC = this.getHurtPC(target);
        int redCol = ColorUtils.rgb(190, 100, 100);
        GL11.glDepthMask((boolean)false);
        GL11.glDisable((int)2929);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int)770, (int)1);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture((int)0, (Identifier)Identifier.of((String)"polar", (String)"textures/trajectories/glow.png"));
        MatrixStack matrices = event.getMatrices();
        float radius = 0.7f;
        for (int k2 = 0; k2 < 4; ++k2) {
            for (int j2 = 0; j2 < 20; ++j2) {
                float kf = (float)j2 / 20.0f;
                double tj = t2 - (double)j2 * 0.05;
                double tvj = tv - (double)j2 * 0.05;
                double cyc = (Math.sin(tvj) + 1.0) * 0.5;
                double baseAngle = Math.toRadians((double)k2 * 90.0 + tj * 50.0 % 360.0);
                double offX = Math.cos(baseAngle) * (double)radius;
                double offZ = Math.sin(baseAngle) * (double)radius;
                double offY = k2 % 2 == 0 ? (double)0.1f + 1.6999999284744263 * cyc : (double)1.8f - 1.6999999284744263 * cyc;
                float sizeFactor = 1.0f - kf * 0.6f;
                float dynSize = 0.45f * sizeFactor;
                int dynAlpha = MathHelper.clamp((int)((int)(255.0f * this.appearValue)), (int)0, (int)255);
                int color = this.replAlpha(this.overCol(this.multAlpha(baseCol, this.appearValue), this.multAlpha(redCol, this.appearValue), hurtPC), dynAlpha);
                double worldX = bx + offX;
                double worldY = by + offY;
                double worldZ = bz + offZ;
                this.drawStaticBillboard(matrices, cam, worldX, worldY, worldZ, dynSize, color, 0.0f);
            }
            radius *= -1.0f;
        }
        GL11.glEnable((int)2929);
        GL11.glDepthMask((boolean)true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawChains3D(Event3DRender event, LivingEntity target, boolean hasTarget) {
        boolean chainTargetPresent;
        boolean isChainMode = this.mode.is("Цепи");
        LivingEntity chainTarget = isChainMode && hasTarget ? target : null;
        boolean chainTargetAlive = chainTarget != null && chainTarget.isAlive();
        boolean bl = chainTargetPresent = chainTarget != null;
        if (chainTargetAlive && this.chainSpawnAnimation < 1.0f) {
            this.chainSpawnAnimation += 0.04f;
        } else if (!chainTargetPresent && this.chainSpawnAnimation > 0.0f) {
            this.chainSpawnAnimation -= 0.04f;
        }
        this.chainSpawnAnimation = MathHelper.clamp((float)this.chainSpawnAnimation, (float)0.0f, (float)1.0f);
        if (chainTarget != null) {
            this.chainLastTarget = chainTarget;
        }
        if (this.chainSpawnAnimation > 0.0f && this.chainLastTarget != null) {
            float hurtPC = this.getHurtPC(this.chainLastTarget);
            this.renderChains(this.chainLastTarget, event.getMatrices(), event.getTickDelta(), this.chainSpawnAnimation, hurtPC);
        } else if (this.chainSpawnAnimation <= 0.0f) {
            this.chainLastRenderPosition = null;
            this.chainLastTarget = null;
        }
    }

    private void renderChains(LivingEntity currentTarget, MatrixStack ms, float partialTicks, float alphaPC, float hurtPC) {
        if (currentTarget == null) {
            return;
        }
        ms.push();
        Vec3d cam = TargetESP.mc.gameRenderer.getCamera().getPos();
        Vec3d interpolated = new Vec3d(MathHelper.lerp((double)partialTicks, (double)currentTarget.lastRenderX, (double)currentTarget.getX()), MathHelper.lerp((double)partialTicks, (double)currentTarget.lastRenderY, (double)currentTarget.getY()), MathHelper.lerp((double)partialTicks, (double)currentTarget.lastRenderZ, (double)currentTarget.getZ()));
        double entX = interpolated.x - cam.x;
        double entY = interpolated.y - cam.y - 0.5;
        double entZ = interpolated.z - cam.z;
        float movingValue = (float)(System.currentTimeMillis() % 3600000L) / 10.0f;
        float gradusX = (float)(20.0 * Math.min(1.0 + Math.sin(Math.toRadians(movingValue)), 1.0));
        float gradusZ = (float)(20.0 * (Math.min(1.0 + Math.sin(Math.toRadians(movingValue)), 2.0) - 1.0));
        float radiusAnim = 1.25f - 0.5f * alphaPC;
        float width = currentTarget.getWidth() * 1.8f * radiusAnim;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((int)770, (int)1);
        int baseColor = this.getESPColor();
        int redColor = this.getSoftRedColor();
        int blendedColor = this.overCol(baseColor, redColor, hurtPC);
        Tessellator tess = Tessellator.getInstance();
        GL11.glDisable((int)2884);
        RenderSystem.depthMask((boolean)true);
        Identifier chainTexture = Identifier.of((String)"polar", (String)"textures/targetesp/chain.png");
        RenderSystem.setShaderTexture((int)0, (Identifier)chainTexture);
        int linksStep = 18;
        int totalAngle = 720;
        float chainSize = 4.0f;
        float down = 1.0f;
        for (int chain = 0; chain < 2; ++chain) {
            float val = 1.2f - 0.5f * (chain == 0 ? 1.0f : 0.9f);
            ms.push();
            ms.translate(entX, entY + (double)(currentTarget.getHeight() / 2.0f), entZ);
            float x2 = 0.0f;
            float y2 = 0.0f;
            float z2 = 0.0f;
            Matrix4f matrix = ms.peek().getPositionMatrix();
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(chain == 0 ? gradusX : -gradusX));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(chain == 0 ? gradusZ : -gradusZ));
            int alphaVal = MathHelper.clamp((int)((int)(alphaPC * 255.0f)), (int)0, (int)255);
            int color = this.replAlpha(blendedColor, alphaVal);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            int modif = linksStep / 2;
            for (int i2 = 0; i2 < totalAngle; i2 += modif) {
                float prevSin = (float)((double)(x2 + (chain == 0 ? gradusX : -gradusX) / 100.0f) + Math.sin(Math.toRadians((float)(i2 - modif) + movingValue * 0.5f)) * (double)width * (double)val);
                float prevCos = (float)((double)(z2 + (chain == 0 ? -gradusZ : gradusZ) / 100.0f) + Math.cos(Math.toRadians((float)(i2 - modif) + movingValue * 0.5f)) * (double)width * (double)val);
                float sin = (float)((double)(x2 + (chain == 0 ? gradusX : -gradusX) / 100.0f) + Math.sin(Math.toRadians((float)i2 + movingValue * 0.5f)) * (double)width * (double)val);
                float cos = (float)((double)(z2 + (chain == 0 ? -gradusZ : gradusZ) / 100.0f) + Math.cos(Math.toRadians((float)i2 + movingValue * 0.5f)) * (double)width * (double)val);
                int r2 = color >> 16 & 0xFF;
                int g2 = color >> 8 & 0xFF;
                int b2 = color & 0xFF;
                int a2 = color >> 24 & 0xFF;
                buffer.vertex(matrix, prevSin, y2, prevCos).texture(0.0027777778f * (float)(i2 - modif) * chainSize, 0.0f).color(r2, g2, b2, a2);
                buffer.vertex(matrix, sin, y2, cos).texture(0.0027777778f * (float)i2 * chainSize, 0.0f).color(r2, g2, b2, a2);
                buffer.vertex(matrix, sin, y2 + down, cos).texture(0.0027777778f * (float)i2 * chainSize, 0.99f).color(r2, g2, b2, a2);
                buffer.vertex(matrix, prevSin, y2 + down, prevCos).texture(0.0027777778f * (float)(i2 - modif) * chainSize, 0.99f).color(r2, g2, b2, a2);
            }
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
            ms.pop();
            if (!this.saturationMode.isState()) continue;
            ms.push();
            ms.translate(entX, entY + (double)(currentTarget.getHeight() / 2.0f), entZ);
            matrix = ms.peek().getPositionMatrix();
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(chain == 0 ? gradusX : -gradusX));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(chain == 0 ? gradusZ : -gradusZ));
            int saturatedColor = color;
            BufferBuilder satBuffer = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (int i3 = 0; i3 < totalAngle; i3 += modif) {
                float prevSin = (float)((double)(x2 + (chain == 0 ? gradusX : -gradusX) / 100.0f) + Math.sin(Math.toRadians((float)(i3 - modif) + movingValue * 0.5f)) * (double)width * (double)val);
                float prevCos = (float)((double)(z2 + (chain == 0 ? -gradusZ : gradusZ) / 100.0f) + Math.cos(Math.toRadians((float)(i3 - modif) + movingValue * 0.5f)) * (double)width * (double)val);
                float sin = (float)((double)(x2 + (chain == 0 ? gradusX : -gradusX) / 100.0f) + Math.sin(Math.toRadians((float)i3 + movingValue * 0.5f)) * (double)width * (double)val);
                float cos = (float)((double)(z2 + (chain == 0 ? -gradusZ : gradusZ) / 100.0f) + Math.cos(Math.toRadians((float)i3 + movingValue * 0.5f)) * (double)width * (double)val);
                int r2 = saturatedColor >> 16 & 0xFF;
                int g2 = saturatedColor >> 8 & 0xFF;
                int b2 = saturatedColor & 0xFF;
                int a2 = saturatedColor >> 24 & 0xFF;
                satBuffer.vertex(matrix, prevSin, y2, prevCos).texture(0.0027777778f * (float)(i3 - modif) * chainSize, 0.0f).color(r2, g2, b2, a2);
                satBuffer.vertex(matrix, sin, y2, cos).texture(0.0027777778f * (float)i3 * chainSize, 0.0f).color(r2, g2, b2, a2);
                satBuffer.vertex(matrix, sin, y2 + down, cos).texture(0.0027777778f * (float)i3 * chainSize, 0.99f).color(r2, g2, b2, a2);
                satBuffer.vertex(matrix, prevSin, y2 + down, prevCos).texture(0.0027777778f * (float)(i3 - modif) * chainSize, 0.99f).color(r2, g2, b2, a2);
            }
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)satBuffer.end());
            ms.pop();
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        GL11.glEnable((int)2884);
        ms.pop();
    }

    private void tri(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int r2, int g2, int b2, int a2) {
        buffer.vertex(matrix, x1, y1, z1).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x2, y2, z2).color(r2, g2, b2, a2);
        buffer.vertex(matrix, x3, y3, z3).color(r2, g2, b2, a2);
    }

    private ContainerMatch findNearestContainer(BlockPos origin) {
        if (mc == null || TargetESP.mc.world == null || TargetESP.mc.player == null) {
            return null;
        }
        int searchRadius = 8;
        for (int x2 = -searchRadius; x2 <= searchRadius; ++x2) {
            for (int z2 = -searchRadius; z2 <= searchRadius; ++z2) {
                for (int y2 = -2; y2 <= 2; ++y2) {
                    BlockPos pos = new BlockPos(origin.getX() + x2, origin.getY() + y2, origin.getZ() + z2);
                    BlockState state = TargetESP.mc.world.getBlockState(pos);
                    Block block = state.getBlock();
                    ItemStack stack = null;
                    if (block instanceof BarrelBlock) {
                        stack = new ItemStack((ItemConvertible)Items.BARREL);
                    } else if (block instanceof ChestBlock) {
                        stack = new ItemStack((ItemConvertible)Items.CHEST);
                    } else if (block instanceof EnderChestBlock) {
                        stack = new ItemStack((ItemConvertible)Items.ENDER_CHEST);
                    } else if (block instanceof ShulkerBoxBlock) {
                        stack = new ItemStack((ItemConvertible)Items.SHULKER_BOX);
                    }
                    if (stack == null) continue;
                    Vec3d center = Vec3d.ofCenter((Vec3i)pos).add(0.0, 0.75, 0.0);
                    return new ContainerMatch(center, stack);
                }
            }
        }
        for (int yOffset = -2; yOffset <= 0; ++yOffset) {
            BlockPos checkPos = new BlockPos(origin.getX(), origin.getY() + yOffset, origin.getZ());
            BlockState state = TargetESP.mc.world.getBlockState(checkPos);
            Block block = state.getBlock();
            if (block != Blocks.STONE && block != Blocks.DEEPSLATE && block != Blocks.COBBLESTONE && block != Blocks.BEDROCK && block != Blocks.OBSIDIAN && !(block instanceof SpawnerBlock)) continue;
            Vec3d center = Vec3d.ofCenter((Vec3i)checkPos).add(0.0, 0.75, 0.0);
            return new ContainerMatch(center, new ItemStack((ItemConvertible)Items.CHEST));
        }
        return null;
    }

    private static class ContainerMatch {
        final Vec3d pos;
        final ItemStack itemStack;

        ContainerMatch(Vec3d pos, ItemStack itemStack) {
            this.pos = pos;
            this.itemStack = itemStack;
        }
    }

    private static class GlowPoint {
        final float x;
        final float y;
        final float z;
        final long startTime;
        final int maxLife;
        final int baseColor;

        GlowPoint(float x2, float y2, float z2, int maxLife, int baseColor) {
            this.x = x2;
            this.y = y2;
            this.z = z2;
            this.startTime = System.currentTimeMillis();
            this.maxLife = maxLife;
            this.baseColor = baseColor;
        }

        boolean shouldRemove() {
            return System.currentTimeMillis() - this.startTime >= (long)this.maxLife;
        }

        float getTimeProgress() {
            return MathHelper.clamp((float)((float)(System.currentTimeMillis() - this.startTime) / (float)this.maxLife), (float)0.0f, (float)1.0f);
        }
    }
}

